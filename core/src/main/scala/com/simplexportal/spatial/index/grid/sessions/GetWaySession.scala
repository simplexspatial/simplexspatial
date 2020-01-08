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
import com.simplexportal.spatial.index.grid.Grid.{TileTypeKey, WayLookUpTypeKey}
import com.simplexportal.spatial.index.grid.lookups.{
  LookUpWayEntityIdGen,
  WayLookUpActor
}
import com.simplexportal.spatial.index.grid.tile
import com.simplexportal.spatial.index.grid.tile.GetWayResponse
import com.simplexportal.spatial.model.{Location, Node, Way}

import scala.annotation.tailrec

object GetWaySession {

  def apply(
      sharding: ClusterSharding,
      getWay: tile.GetWay
  ): Behavior[NotUsed] =
    Behaviors
      .setup[AnyRef] { context =>
        var expectedTiles = 0
        var wayParts = Set[Way]()

        sharding.entityRefFor(
          WayLookUpTypeKey,
          LookUpWayEntityIdGen.entityId(getWay.id)
        ) ! WayLookUpActor.Get(getWay.id, context.self)

        Behaviors.receiveMessage {
          case WayLookUpActor.GetResponse(wayId, Some(tileIdxs)) =>
            expectedTiles += tileIdxs.size
            tileIdxs.foreach(tileIdx =>
              sharding.entityRefFor(
                TileTypeKey,
                tileIdx.entityId
              ) ! tile.GetWay(wayId, context.self)
            )
            Behaviors.same
          case tile.GetWayResponse(id, Some(way)) =>
            expectedTiles -= 1
            wayParts = wayParts + way
            if (expectedTiles == 0) {
              getWay.replyTo ! GetWayResponse(id, joinWayParts(wayParts).map {
                nodes =>
                  way.copy(nodes = nodes)
              })
              Behaviors.stopped
            }
            Behaviors.same
          case _ =>
            Behaviors.unhandled
        }
      }
      .narrow[NotUsed]

  def joinWayParts(
      parts: Set[Way]
  ): Option[Seq[Node]] = {

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
      rec(way.nodes, Seq())
    }
  }

}
