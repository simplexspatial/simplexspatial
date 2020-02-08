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

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.index.grid.Grid
import com.simplexportal.spatial.index.grid.Grid.WayLookUpTypeKey
import com.simplexportal.spatial.index.grid.lookups.{
  LookUpWayEntityIdGen,
  WayLookUpActor
}
import com.simplexportal.spatial.index.grid.tile.actor._
import com.simplexportal.spatial.index.grid.tile.impl.TileIndex
import com.simplexportal.spatial.model.Location
import io.jvm.uuid.UUID

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

@deprecated("Reuse AddBatchSession", "Simplexspatial Core 0.0.1")
object AddWaySession {

  def apply(addWay: AddWay)(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Behavior[NotUsed] =
    Behaviors
      .setup[AnyRef] { context =>
        var pendingResponses = 0

        // Translate nodes ids into nodes.
        context.spawn(
          GetInternalNodesSession(
            GetInternalNodes(addWay.nodeIds, context.self)
          ),
          s"getting_node_${UUID.randomString}"
        )

        Behaviors.receiveMessage {
          case GetInternalNodesResponse(nodes) =>
            validateNodes(nodes) match {
              case Success(nodes) =>
                splitNodesInShards(nodes).foreach {
                  case (tileIdx, nodes) =>
                    // Register the node in the the tile index.
                    pendingResponses += 1
                    sharding.entityRefFor(Grid.TileTypeKey, tileIdx.entityId) ! addWay
                      .copy(
                        nodeIds = nodes.map(_.id),
                        replyTo = Some(context.self)
                      )

                    // Register in the ways lookup.
                    pendingResponses += 1
                    val wayLookUpId = LookUpWayEntityIdGen.entityId(addWay.id)
                    sharding.entityRefFor(WayLookUpTypeKey, wayLookUpId) ! WayLookUpActor
                      .Put(addWay.id, tileIdx, Some(context.self))
                }
                Behaviors.same
              case Failure(exception) =>
                addWay.replyTo.foreach(_ ! NotDone(exception.getMessage))
                Behaviors.stopped
            }
          case Done() | WayLookUpActor.Done() =>
            pendingResponses -= 1
            if (pendingResponses == 0) {
              addWay.replyTo.foreach(_ ! Done())
              Behaviors.stopped
            } else {
              Behaviors.same
            }
          case _ =>
            Behaviors.unhandled
        }
      }
      .narrow[NotUsed]

  /**
    * Validate all nodes in the response, and return a validated sequence of nodes.
    * Currently, the only validation is check that all nodes are in the database.
    *
    * @param responses The response with possible don't know nodes.
    * @return Return the right sequence of nodes or error.
    */
  def validateNodes(
      responses: Seq[GetInternalNodeResponse]
  ): Try[Seq[TileIndex.InternalNode]] = Try {
    responses.map { resp =>
      resp.node.getOrElse(throw new Exception(s"Node [${resp.id}] not found."))
    }
  }

  // TODO: The connector node should be a special class with only the id ???
  /**
    * From a list of nodes, create a list of shards, where every element contains the shard Id and the list of nodes in
    * there.
    * Also, it add at the end and at the begining the "connector nodes", that are the connection with the next/previous
    * node in the other shard.
    *
    * Let's suppose the one way is not going to have the same node as connector in the same shard.
    *
    * @param nodes
    * @param entityIdGen
    * @return
    */
  def splitNodesInShards(nodes: Seq[TileIndex.InternalNode])(
      implicit entityIdGen: TileIndexEntityIdGen
  ): Seq[(TileIdx, Seq[TileIndex.InternalNode])] = {

    def entityIdFrom =
      (loc: Location) => entityIdGen.tileIdx(loc.lat, loc.lon)

    @tailrec
    def rec(
        nodes: Seq[TileIndex.InternalNode],
        acc: Seq[(TileIdx, Seq[TileIndex.InternalNode])],
        currentShard: (TileIdx, Seq[TileIndex.InternalNode])
    ): Seq[(TileIdx, Seq[TileIndex.InternalNode])] =
      nodes match {
        case Nil => acc :+ currentShard
        case node :: tail =>
          val entityId = entityIdFrom(node.location)
          val updated_shard = (currentShard._1, currentShard._2 :+ node)
          if (entityId == currentShard._1) {
            rec(tail, acc, updated_shard)
          } else {
            rec(
              tail,
              acc :+ updated_shard,
              (entityId, currentShard._2.last +: Seq(node))
            )
          }
      }

    rec(
      nodes.tail,
      Seq.empty,
      (entityIdFrom(nodes.head.location), Seq(nodes.head)) // FIXME: Don't use nodes.head abd use headOption
    )
  }

}
