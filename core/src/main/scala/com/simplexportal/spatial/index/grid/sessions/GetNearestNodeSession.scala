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
import com.simplexportal.spatial.index.CommonInternalSerializer
import com.simplexportal.spatial.index.grid.Grid
import com.simplexportal.spatial.index.grid.GridProtocol.{GridNearestNode, GridNearestNodeReply, GridRequest}
import com.simplexportal.spatial.index.grid.tile.actor.TileIndexProtocol.{GetNearestNodeResponse => TileReply}
import com.simplexportal.spatial.index.grid.tile.actor.{
  TileIdx,
  TileIndexEntityIdGen,
  TileIndexProtocol => tileProptocol
}
import com.simplexportal.spatial.index.grid.tile.impl.NearestNode
import com.simplexportal.spatial.utils.ModelEnrichers._
import io.jvm.uuid.UUID
import org.locationtech.jts.algorithm.Distance
import org.locationtech.jts.geom.Coordinate

object GetNearestNodeSession {

  def processRequest(
      cmd: GridNearestNode,
      context: ActorContext[GridRequest]
  )(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Unit =
    context.spawn(
      apply(cmd),
      s"getting_way_${UUID.randomString}"
    )

  // scalastyle:off method.length
  def apply(cmd: GridNearestNode)(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Behavior[ForeignResponse] =
    Behaviors.setup { context =>
      val adapter = adapters(context)
      val originTile = TileIdx(cmd.location)
      val originCoords = cmd.location.toJTS()

      var remainingRequests = 0;
      var currentFound: Option[NearestNode] = None
      var visited = Set.empty[TileIdx]

      /**
        * 1. Calculate all tiles in the {layer} layer.
        * 2. Remove all tiles of the layer that are further that the current nearest node.
        * 3. Request for the nearest node in every tile remaining in the list
        *
        * @param layer Layer to calculate
        * @return Number of request sent.
        */
      @inline def requestTilesInLayer(layer: Int): Set[TileIdx] =
        originTile
          .layer(layer)
          .filterNot(visited.contains)
          .filter(tile =>
            currentFound match {
              case None                                                             => true
              case Some(NearestNode(_, dist)) if isNearer(originCoords, tile, dist) => true
              case _                                                                => false
            }
          )
          .map(id => {
            sharding.entityRefFor(Grid.TileTypeKey, id.entityId) ! tileProptocol.GetNearestNode(cmd.location, adapter)
            id
          })

      def nextLayer(layer: Int): Behavior[ForeignResponse] = {
        requestTilesInLayer(layer) match {
          case requested if requested.isEmpty =>
            cmd.replyTo ! GridNearestNodeReply(Right(currentFound.map(n => n.nodes).getOrElse(Set.empty)))
            Behaviors.stopped
          case requested =>
            remainingRequests = requested.size
            visited = visited ++ requested
            Behaviors.receiveMessage {
//              case TileReplyWrapper(TileReply(id, _, newMaybeNearest)) =>
//                remainingRequests -= 1
//                currentFound = chooseTheNearest(currentFound, newMaybeNearest)
//                remainingRequests match {
//                  case 0 => nextLayer(layer + 1)
//                  case _ => Behaviors.same
//                }
              case msg =>
                context.log.error("Unexpected message {}", msg)
                Behaviors.unhandled
            }
        }
      }

      nextLayer(0)
    }

  private def adapters(
      context: ActorContext[ForeignResponse]
  ): ActorRef[AnyRef] =
    context.messageAdapter {
      case msg: TileReply => TileReplyWrapper(msg)
    }

  @inline private def isNearer(origin: Coordinate, id: TileIdx, d: Double)(implicit tileIdxGen: TileIndexEntityIdGen) =
    Distance.pointToSegmentString(origin, id.bbox().toLine().toJTSArrayCoords()) <= d

  // TODO: Move to NearestNode class
  private def chooseTheNearest(
      maybeOld: Option[NearestNode],
      maybeNew: Option[NearestNode]
  ): Option[NearestNode] =
    (maybeOld, maybeNew) match {
      case (None, _)                 => maybeNew
      case (_, None)                 => maybeOld
      case (Some(old), Some(newOne)) => Some(old.chooseNearest(newOne))
    }

  protected sealed trait ForeignResponse extends CommonInternalSerializer

  protected case class TileReplyWrapper(response: TileReply) extends ForeignResponse

}
