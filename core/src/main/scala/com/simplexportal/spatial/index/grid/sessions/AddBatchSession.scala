/*
 * Copyright 2019 SimplexPortal Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.simplexportal.spatial.index.grid.sessions

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.index.grid.lookups.{
  LookUpNodeEntityIdGen,
  LookUpWayEntityIdGen,
  NodeLookUpActor,
  WayLookUpActor
}
import com.simplexportal.spatial.index.grid.tile.{
  AddNode,
  AddWay,
  BatchActions,
  TileIdx
}
import com.simplexportal.spatial.index.grid.{
  CommonInternalSerializer,
  Grid,
  tile
}
import io.jvm.uuid.UUID

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object AddBatchSession {

  sealed trait Messages extends CommonInternalSerializer
  sealed trait Command extends Messages
  sealed trait Response extends Messages
  private sealed trait ForeignResponse extends Messages

  private case class LocationsWrapper(
      locs: GetNodeLocationsSession.NodeLocations
  ) extends ForeignResponse

  private case class DoneWrapper() extends ForeignResponse
  private case class NotDoneWrapper(msg: String) extends ForeignResponse

  // scalastyle:off method.length
  def apply(
      sharding: ClusterSharding,
      cmds: Seq[tile.BatchActions],
      maybeReplyTo: Option[ActorRef[tile.ACK]],
      tileEntityFn: tile.TileIndexEntityIdGen
  ): Behavior[Messages] = Behaviors.setup[Messages] { context =>
    val locsResponseAdapter: ActorRef[GetNodeLocationsSession.NodeLocations] =
      context.messageAdapter { LocationsWrapper }

    val (newNodes, unknownNodes) = splitKnowNodesTileIdxs(cmds, tileEntityFn)

    context.spawn(
      GetNodeLocationsSession(sharding, unknownNodes, locsResponseAdapter),
      s"node_locations_${UUID.randomString}"
    )

    Behaviors.receiveMessagePartial {
      case LocationsWrapper(GetNodeLocationsSession.NodeLocations(locations)) =>
        Try(locations.flatMap {
          case (id, None) =>
            throw new Exception(s"Node with id ${id} not found.")
          case (id, Some(tileIdx)) => Some((id -> tileIdx))
        } ++ newNodes) match {
          case Success(locationsIdx) =>
            updateIndexes(
              sharding,
              cmds,
              locationsIdx,
              maybeReplyTo
            )
          case Failure(exception) =>
            maybeReplyTo.foreach(_ ! tile.NotDone(exception.getMessage))
            Behaviors.stopped
        }
    }
  }

  def splitKnowNodesTileIdxs(
      commands: Seq[tile.BatchActions],
      tileEntityFn: tile.TileIndexEntityIdGen
  ): (Map[Long, tile.TileIdx], Seq[Long]) = {

    val (newNodes, unknownNodes) =
      commands.foldLeft[(Map[Long, tile.TileIdx], Seq[Long])](
        (Map.empty, Seq.empty)
      ) {
        case (resp, cmd) =>
          cmd match {
            case n: tile.AddNode =>
              (
                resp._1 + (n.id -> tileEntityFn.tileIdx(n.lat, n.lon)),
                resp._2
              )
            case w: tile.AddWay =>
              (resp._1, resp._2 ++ w.nodeIds)
          }
      }

    (newNodes, unknownNodes.filterNot(newNodes.contains))
  }

  def groupByTileIdx(
      cmds: Seq[tile.BatchActions],
      locationsIdx: Map[Long, tile.TileIdx]
  ): Map[tile.TileIdx, Seq[tile.BatchActions]] =
    cmds.foldLeft(Map.empty[tile.TileIdx, Seq[tile.BatchActions]]) {
      case (acc, n: tile.AddNode) =>
        val tileIdx = locationsIdx(n.id)
        acc + (tileIdx -> (acc.getOrElse(tileIdx, Seq()) :+ n))
      case (acc, w: tile.AddWay) =>
        splitWayByTile(w, locationsIdx).foldLeft(acc) {
          case (acc, (tileIdx, way)) =>
            acc + (tileIdx -> (acc.getOrElse(tileIdx, Seq()) :+ way))
        }
    }

  def splitWayByTile(
      way: tile.AddWay,
      nodeLocs: Map[Long, TileIdx]
  ): Seq[(tile.TileIdx, tile.AddWay)] =
    splitWayNodesPerTile(way.nodeIds, nodeLocs).map {
      case (tileIdx, nodeIds) => (tileIdx, way.copy(nodeIds = nodeIds))
    }

  /**
    * It will split a way into a set of ways bounded to a tile.
    * It will add connector as well.
    *
    * @param nodeIds Original sequence of node ids that define the way.
    * @param nodeLocs index of nodesId -> tileIdx
    * @return Pairs tileIdx, Way fragment.
    */
  def splitWayNodesPerTile(
      nodeIds: Seq[Long],
      nodeLocs: Map[Long, tile.TileIdx]
  ): Seq[(tile.TileIdx, Seq[Long])] = {

    @tailrec
    def rec(
        nodes: Seq[Long],
        acc: Seq[(tile.TileIdx, Seq[Long])],
        currentShard: (tile.TileIdx, Seq[Long])
    ): Seq[(tile.TileIdx, Seq[Long])] = {
      nodes match {
        case Nil => acc :+ currentShard
        case nodeId :: tail =>
          val entityId = nodeLocs(nodeId)
          val updated_shard = (currentShard._1, currentShard._2 :+ nodeId)
          if (entityId == currentShard._1) {
            rec(tail, acc, updated_shard)
          } else {
            rec(
              tail,
              acc :+ updated_shard,
              (entityId, currentShard._2.last +: Seq(nodeId))
            )
          }
      }
    }

    rec(
      nodeIds.tail,
      Seq.empty,
      (nodeLocs(nodeIds.head), Seq(nodeIds.head))
    )
  }

  private def adapters(context: ActorContext[Messages]): ActorRef[AnyRef] =
    context.messageAdapter {
      case tile.Done()                  => DoneWrapper()
      case tile.NotDone(msg)            => NotDoneWrapper(msg)
      case NodeLookUpActor.Done()       => DoneWrapper()
      case NodeLookUpActor.NotDone(msg) => NotDoneWrapper(msg)
      case WayLookUpActor.Done()        => DoneWrapper()
      case WayLookUpActor.NotDone(msg)  => NotDoneWrapper(msg)
    }

  def updateIndexes(
      sharding: ClusterSharding,
      cmds: Seq[tile.BatchActions],
      locationsIdx: Map[Long, tile.TileIdx],
      maybeReplyTo: Option[ActorRef[tile.ACK]]
  ): Behavior[Messages] = Behaviors.setup[Messages] { context =>
    val adapter = adapters(context)

    val cmdsPerTileIdx = groupByTileIdx(cmds, locationsIdx)

    var expectedResponses = 0

    val (nodes, ways) = splitLookUps(cmdsPerTileIdx)

    // Update Nodes lookUp.
    nodes.foreach {
      case (shardId, items) =>
        expectedResponses += 1
        sharding.entityRefFor(Grid.NodeLookUpTypeKey, shardId) !
          NodeLookUpActor.PutBatch(items.map {
            case (id, tileIdx) => NodeLookUpActor.Put(id, tileIdx, None)
          }, Some(adapter))
    }

    // Update Ways lookUp.
    ways.foreach {
      case (shardId, items) =>
        expectedResponses += 1
        sharding.entityRefFor(Grid.WayLookUpTypeKey, shardId) !
          WayLookUpActor.PutBatch(items.map {
            case (id, tileIdx) => WayLookUpActor.Put(id, tileIdx, None)
          }, Some(adapter))
    }

    // Update Tile index.
    cmdsPerTileIdx.foreach {
      case (tileIdx, cmds) =>
        expectedResponses += 1
        sharding.entityRefFor(Grid.TileTypeKey, tileIdx.entityId) !
          tile.AddBatch(cmds, Some(adapter))
    }

    collectAddCommandsResponses(expectedResponses, Seq.empty, maybeReplyTo)
  }

  def splitLookUps(
      cmdsPerTileIdx: Map[tile.TileIdx, Seq[tile.BatchActions]]
  ): (Map[String, Seq[(Long, TileIdx)]], Map[String, Seq[(Long, TileIdx)]]) = {

    @tailrec
    def rec(
        remaining: Seq[(TileIdx, BatchActions)],
        nodes: Map[String, Seq[(Long, TileIdx)]],
        ways: Map[String, Seq[(Long, TileIdx)]]
    ): (Map[String, Seq[(Long, TileIdx)]], Map[String, Seq[(Long, TileIdx)]]) =
      remaining match {
        case Nil => (nodes, ways)
        case head :: tail =>
          head match {
            case (tileIdx, AddNode(id, _, _, _, _)) =>
              val shard = LookUpNodeEntityIdGen.entityId(id)
              val itemsPerShard = nodes.getOrElse(shard, Seq.empty) :+ (id, tileIdx)
              rec(
                tail,
                nodes + (shard -> itemsPerShard),
                ways
              )
            case (tileIdx, AddWay(id, _, _, _)) =>
              val shard = LookUpWayEntityIdGen.entityId(id)
              val itemsPerShard = ways.getOrElse(shard, Seq.empty) :+ (id, tileIdx)
              rec(
                tail,
                nodes,
                ways + (shard -> itemsPerShard)
              )
          }
      }

    val actions = cmdsPerTileIdx.flatMap {
      case (tileIdx, actions) =>
        actions.map((tileIdx, _))
    }.toList

    rec(actions, Map.empty, Map.empty)

  }

  def collectAddCommandsResponses(
      expectedResponses: Int,
      errors: Seq[String],
      maybeReplyTo: Option[ActorRef[tile.ACK]]
  ): Behavior[Messages] = {

    def reply(errors: Seq[String]) =
      maybeReplyTo.foreach { replyTo =>
        errors match {
          case Seq() => replyTo ! tile.Done()
          case _     => replyTo ! tile.NotDone(errors.mkString("\n"))
        }
      }

    def next(remainingResponses: Int, errors: Seq[String]): Behavior[Messages] =
      if (remainingResponses == 0) {
        reply(errors)
        Behaviors.stopped
      } else {
        rec(remainingResponses, errors)
      }

    def rec(remainingResponses: Int, errors: Seq[String]): Behavior[Messages] =
      Behaviors.receiveMessagePartial {
        case DoneWrapper() =>
          next(remainingResponses - 1, errors)
        case NotDoneWrapper(error) =>
          next(remainingResponses - 1, errors :+ error)
      }

    rec(expectedResponses, Seq.empty)
  }
}
