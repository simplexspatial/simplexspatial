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
import com.simplexportal.spatial.index.grid.CommonInternalSerializer
import com.simplexportal.spatial.index.grid.Grid.{TileTypeKey, WayLookUpTypeKey}
import com.simplexportal.spatial.index.grid.lookups.WayLookUpActor.{
  GetResponse => LookUpReply
}
import com.simplexportal.spatial.index.grid.lookups.{
  LookUpWayEntityIdGen,
  WayLookUpActor
}
import com.simplexportal.spatial.index.grid.tile.actor.{
  GetWay,
  TileIndexEntityIdGen,
  GetWayResponse => TileReply
}
import com.simplexportal.spatial.index.protocol.{
  GridGetWay,
  GridGetWayReply,
  GridRequest
}
import com.simplexportal.spatial.model.{Location, Node, Way}
import io.jvm.uuid.UUID

import scala.annotation.tailrec

object GetWaySession {

  def processRequest(
      cmd: GridGetWay,
      context: ActorContext[GridRequest]
  )(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Unit =
    context.spawn(
      apply(cmd),
      s"getting_way_${UUID.randomString}"
    )

  def apply(getWay: GridGetWay)(
      implicit sharding: ClusterSharding
  ): Behavior[ForeignResponse] =
    Behaviors
      .setup { context =>
        val adapter = adapters(context)

        var expectedTiles = 0
        var wayParts = Set[Way]()

        sharding.entityRefFor(
          WayLookUpTypeKey,
          LookUpWayEntityIdGen.entityId(getWay.id)
        ) ! WayLookUpActor.Get(getWay.id, adapter)

        Behaviors.receiveMessage {
          case LookUpReplyWrapper(LookUpReply(wayId, Some(tileIds))) =>
            expectedTiles += tileIds.size
            tileIds.foreach(tileIdx =>
              sharding.entityRefFor(
                TileTypeKey,
                tileIdx.entityId
              ) ! GetWay(wayId, adapter)
            )
            Behaviors.same

          case LookUpReplyWrapper(LookUpReply(_, None)) =>
            getWay.replyTo ! GridGetWayReply(Right(None))
            Behaviors.stopped

          case TileReplyWrapper(TileReply(wayId, maybeWay)) => {
            expectedTiles -= 1
            maybeWay match {
              case None =>
              case Some(way) =>
                wayParts = wayParts + way
            }
            if (expectedTiles == 0) {
              getWay.replyTo ! GridGetWayReply(Right(joinWayParts(wayParts)))
              Behaviors.stopped
            }
            Behaviors.same
          }
          case _ =>
            Behaviors.unhandled
        }
      }

  private def adapters(
      context: ActorContext[ForeignResponse]
  ): ActorRef[AnyRef] =
    context.messageAdapter {
      case msg: LookUpReply => LookUpReplyWrapper(msg)
      case msg: TileReply   => TileReplyWrapper(msg)
    }

  def joinWayParts(parts: Set[Way]): Option[Way] = {

    def removeHeadConnector(nodes: Seq[Node]) =
      if (nodes(0).location == Location.NaL) {
        nodes.tail
      } else {
        nodes
      }

    val idxByFirstNodeID = parts
      .map(way => removeHeadConnector(way.nodes))
      .map(nodes => nodes(0).id -> nodes)
      .toMap

    val firstPart = parts.find(_.nodes(0).location != Location.NaL)

    @tailrec
    def rec(nodes: Seq[Node], newNodes: Seq[Node]): Seq[Node] = nodes match {
      case Nil                            => newNodes
      case Node(id, Location.NaL, _) :: _ => rec(idxByFirstNodeID(id), newNodes)
      case node :: tail                   => rec(tail, newNodes :+ node)
    }

    firstPart.map { way =>
      way.copy(nodes = rec(way.nodes, Seq()))
    }

  }

  protected sealed trait ForeignResponse extends CommonInternalSerializer

  protected case class LookUpReplyWrapper(response: LookUpReply)
      extends ForeignResponse

  protected case class TileReplyWrapper(response: TileReply)
      extends ForeignResponse

}
