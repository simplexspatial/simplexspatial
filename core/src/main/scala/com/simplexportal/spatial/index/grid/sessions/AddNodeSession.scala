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
import com.simplexportal.spatial.index.grid.Grid.{NodeLookUpTypeKey, TileTypeKey}
import com.simplexportal.spatial.index.grid.tile.actor
import com.simplexportal.spatial.index.grid.tile.actor.TileIndexEntityIdGen
import com.simplexportal.spatial.index.grid.tile.actor.TileIndexProtocol.{AddNode, Done}
import com.simplexportal.spatial.index.lookup.node.{LookUpNodeEntityIdGen, NodeLookUpProtocol}

/**
  * AddNode per session actor that update all indices and stop.
  */
object AddNodeSession {
  def apply(addNode: AddNode)(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Behavior[NotUsed] =
    Behaviors
      .setup[AnyRef] { context =>
        val nodeEntityId = LookUpNodeEntityIdGen.entityId(addNode.node.id)
        val tileIdx = tileIndexEntityIdGen.tileIdx(addNode.node.location.lat, addNode.node.location.lon)
        val nodeLookUpActor =
          sharding.entityRefFor(NodeLookUpTypeKey, nodeEntityId)
        val tileIndexActor =
          sharding.entityRefFor(TileTypeKey, tileIdx.entityId)

        // Add node in the lookUp index.
        nodeLookUpActor ! NodeLookUpProtocol.Put(
          addNode.node.id,
          tileIdx,
          addNode.replyTo.map(_ => context.self)
        )

        // Add node in the tiles index.
        tileIndexActor ! addNode.copy(
          replyTo = addNode.replyTo.map(_ => context.self)
        )

        addNode.replyTo match {
          case Some(clientRef) =>
            var lookUpResponse: Option[NodeLookUpProtocol.Done] = None
            var tileResponse: Option[Done] = None

            def nextBehavior(): Behavior[AnyRef] =
              (lookUpResponse, tileResponse) match {
                case (Some(_), Some(_)) =>
                  // we got both responses, "session" is completed!
                  clientRef ! Done()
                  Behaviors.stopped
                case _ =>
                  // Wait for the next response.
                  Behaviors.same
              }

            Behaviors.receiveMessage {
              case resp: NodeLookUpProtocol.Done =>
                lookUpResponse = Some(resp)
                nextBehavior()
              case resp: Done =>
                tileResponse = Some(resp)
                nextBehavior()
              case _ =>
                Behaviors.unhandled
            }
          case None =>
            Behaviors.stopped
        }
      }
      .narrow[NotUsed]
}
