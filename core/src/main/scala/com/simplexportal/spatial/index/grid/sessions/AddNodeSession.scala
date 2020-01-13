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
import com.simplexportal.spatial.index.grid.Grid.{
  NodeLookUpTypeKey,
  TileTypeKey
}
import com.simplexportal.spatial.index.grid.lookups.{
  LookUpNodeEntityIdGen,
  NodeLookUpActor
}
import com.simplexportal.spatial.index.grid.tile
import com.simplexportal.spatial.index.grid.tile.TileIndexEntityIdGen

/**
  * AddNode per session actor that update all indices and stop.
  */
// scalastyle:off method.length
@deprecated("Reuse AddBatchSession")
object AddNodeSession {
  def apply(
      sharding: ClusterSharding,
      addNode: tile.AddNode,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Behavior[NotUsed] =
    Behaviors
      .setup[AnyRef] { context =>
        val nodeEntityId = LookUpNodeEntityIdGen.entityId(addNode.id)
        val tileIdx = tileIndexEntityIdGen.tileIdx(addNode.lat, addNode.lon)
        val nodeLookUpActor =
          sharding.entityRefFor(NodeLookUpTypeKey, nodeEntityId)
        val tileIndexActor =
          sharding.entityRefFor(TileTypeKey, tileIdx.entityId)

        // Add node in the lookUp index.
        nodeLookUpActor ! NodeLookUpActor.Put(
          addNode.id,
          tileIdx,
          addNode.replyTo.map(_ => context.self)
        )

        // Add node in the tiles index.
        tileIndexActor ! addNode.copy(
          replyTo = addNode.replyTo.map(_ => context.self)
        )

        addNode.replyTo match {
          case Some(clientRef) =>
            var lookUpResponse: Option[NodeLookUpActor.Done] = None
            var tileResponse: Option[tile.Done] = None

            def nextBehavior(): Behavior[AnyRef] =
              (lookUpResponse, tileResponse) match {
                case (Some(_), Some(_)) =>
                  // we got both responses, "session" is completed!
                  clientRef ! tile.Done()
                  Behaviors.stopped
                case _ =>
                  // Wait for the next response.
                  Behaviors.same
              }

            Behaviors.receiveMessage {
              case resp: NodeLookUpActor.Done =>
                lookUpResponse = Some(resp)
                nextBehavior()
              case resp: tile.Done =>
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
// scalastyle:on method.length
