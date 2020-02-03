/*
 * Copyright 2020 SimplexPortal Ltd
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
import com.simplexportal.spatial.index.grid.tile.actor.{
  TileIdx,
  TileIndexEntityIdGen
}
import com.simplexportal.spatial.index.grid.tile.impl.NearestNode
import com.simplexportal.spatial.index.grid.tile.{actor => tile}
import com.simplexportal.spatial.index.grid.{CommonInternalSerializer, Grid}
import com.simplexportal.spatial.model.{LineSegment, Location}
import com.simplexportal.spatial.utils.ModelEnrichers._
import org.locationtech.jts.algorithm.Distance
import org.locationtech.jts.geom.Coordinate

object GetNearestNodeSession {

  sealed trait Messages extends CommonInternalSerializer
  sealed trait Response extends Messages
  private sealed trait ForeignResponse extends Messages

  case class GetNearestNodeResponse(
      nodes: Option[NearestNode]
  ) extends Response

  private case class GetNearestInternalNodeResponseWrapper(
      tileId: String,
      nodes: Option[NearestNode]
  ) extends ForeignResponse

  def apply(
      origin: Location,
      replyTo: ActorRef[GetNearestNodeResponse]
  )(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Behavior[Messages] = Behaviors.setup[Messages] { context =>
    val adapter = adapters(context)

    // Search the tile for the initial location.
    val initTileIdx = tileIndexEntityIdGen.tileIdx(origin.lat, origin.lon)

    request(Set(initTileIdx.entityId), origin, adapter)

    /**
      * Process the new list of nearest nodes for a tile.
      * It is going to replace the previous one if:
      *  - No previous nearest nodes found.
      *  - the new distance is smaller.
      *
      * Check if the origin is nearer to the current nearest nodes than to any of the edges/vertex in the current tile.
      * If it is not nearer, then it will be necessary to search in adjacent tiles.
      *
      * @param remainingResponses Remaining responses to arrive.
      * @return
      */
    // FIXME: Split this function in two, one to replace or not the previouse one and another to search in adjacent tiles if necessary.
    def collectResponses(
        remainingResponses: Int,
        visitedTiles: Set[TileIdx],
        current: Option[NearestNode]
    ): Behavior[Messages] = Behaviors.receiveMessage {
      case GetNearestInternalNodeResponseWrapper(tileId, newer) =>
        TileIdx(tileId) match {
          case Left(error) => ???
          case Right(tileIdx) =>
            val nearestNode = updateNearestNode(current, newer)
            val tilesToRequest = adjacentlyToRequest(
              origin,
              nearestNode,
              tileIdx,
              visitedTiles
            )

            remainingResponses - 1 + tilesToRequest.size match {
              case 0 =>
                replyTo ! GetNearestNodeResponse(nearestNode)
                Behaviors.stopped
              case remaining =>
                collectResponses(
                  remaining,
                  visitedTiles ++ tilesToRequest,
                  nearestNode
                )
            }
        }
      case _ => Behaviors.unhandled
    }

    collectResponses(1, Set(initTileIdx), None)

  }

  private def request(
      tileIds: Set[String],
      origin: Location,
      replyTo: ActorRef[AnyRef]
  )(implicit sharding: ClusterSharding) =
    tileIds.foreach(tileId =>
      sharding.entityRefFor(Grid.TileTypeKey, tileId) ! tile
        .GetNearestNode(origin, replyTo)
    )

  private def updateNearestNode(
      maybeOld: Option[NearestNode],
      maybeNew: Option[NearestNode]
  ): Option[NearestNode] = (maybeOld, maybeNew) match {
    case (None, _) => maybeNew
    case (_, None) => maybeOld
    case (Some(old), Some(newOne)) if old.distance > newOne.distance =>
      Some(newOne)
    case (Some(old), Some(newOne)) if old.distance == newOne.distance =>
      Some(old.copy(nodes = old.nodes ++ newOne.nodes))
  }

  private def adapters(context: ActorContext[Messages]): ActorRef[AnyRef] =
    context.messageAdapter {
      case tile.GetInternalNearestNodeResponse(tileId, _, nodes) =>
        GetNearestInternalNodeResponseWrapper(tileId, nodes)
    }

  /**
    * Calculated the adjacent tiles that is necessary to call.
    *
    * @param origin
    * @param maybeNearest
    * @param currentTile
    * @return List of tiles requested.
    */
  private def adjacentlyToRequest(
      origin: Location,
      maybeNearest: Option[NearestNode],
      currentTile: TileIdx,
      visitedTiles: Set[TileIdx]
  )(
      implicit tileIndexFn: TileIndexEntityIdGen
  ): Set[TileIdx] =
    maybeNearest match {
      case None =>
        tileIndexFn
          .clockNeighbours(currentTile)
          .filterNot(visitedTiles.contains(_))
          .toSet
      case Some(nearest) =>
        neighbourDistance(origin.toJTS(), currentTile)
          .filter {
            case (i, d) => d < nearest.distance && !visitedTiles.contains(i)
          }
          .map(_._1)
    }

  /**
    * Distance from one point to every neighbour tile.
    *
    * @param origin
    * @param tileIdx
    * @return A clock ordered list of all neighbours and the distance from the origin
    */
  private def neighbourDistance(origin: Coordinate, tileIdx: TileIdx)(
      implicit tileIndexFn: TileIndexEntityIdGen
  ): Set[(TileIdx, Double)] =
    tileIndexFn
      .clockNeighbours(tileIdx)
      .zip(
        tileIndexFn
          .boundingBox(tileIdx)
          .clockNeighbours()
      )
      .map {
        case (idx, geom) =>
          geom match {
            case loc: Location =>
              (
                idx,
                origin.distance(loc.toJTS())
              )
            case line: LineSegment =>
              (
                idx,
                Distance.pointToSegmentString(origin, line.toJTSArrayCoords())
              )
          }
      }
      .toSet

}
