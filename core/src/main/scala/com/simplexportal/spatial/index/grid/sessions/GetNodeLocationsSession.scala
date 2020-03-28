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

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.index.grid.CommonInternalSerializer
import com.simplexportal.spatial.index.grid.Grid.NodeLookUpTypeKey
import com.simplexportal.spatial.index.grid.lookups.{LookUpNodeEntityIdGen, NodeLookUpActor}
import com.simplexportal.spatial.index.grid.tile.actor.TileIdx

object GetNodeLocationsSession {

  sealed trait Messages extends CommonInternalSerializer
  sealed trait Response extends Messages
  private sealed trait ForeignResponse extends Messages

  case class NodeLocations(locations: Map[Long, Option[TileIdx]]) extends Response

  private case class NodeLookUpResponseWrapper(
      response: NodeLookUpActor.GetsResponse
  ) extends ForeignResponse

  def apply(
      sharding: ClusterSharding,
      ids: Set[Long],
      replyTo: ActorRef[NodeLocations]
  ): Behavior[Messages] =
    Behaviors.setup[Messages] { context =>
      val nodeLookUpResponseAdapter: ActorRef[NodeLookUpActor.GetsResponse] =
        context.messageAdapter { resp =>
          NodeLookUpResponseWrapper(resp)
        }

      var expectedResponses = 0;

      // Group by look-up entity id to get node locations.
      val idsByEntityId = ids
        .map(id => (id, LookUpNodeEntityIdGen.entityId(id)))
        .groupBy(_._2)
        .map(e => e._1 -> e._2.map(_._1))

      if (idsByEntityId.isEmpty) {
        replyTo ! NodeLocations(Map.empty)
      } else {
        idsByEntityId
          .foreach {
            case (entityId, ids) =>
              expectedResponses += 1
              sharding.entityRefFor(
                NodeLookUpTypeKey,
                entityId
              ) ! NodeLookUpActor.Gets(ids, nodeLookUpResponseAdapter)
          }
      }

      handleLookUpResponses(
        expectedResponses,
        Map.empty,
        replyTo
      )

    }

  def handleLookUpResponses(
      remainingResponses: Int,
      locations: Map[Long, Option[TileIdx]],
      replyTo: ActorRef[NodeLocations]
  ): Behavior[Messages] =
    Behaviors.receiveMessagePartial {
      case NodeLookUpResponseWrapper(response) =>
        val newLocs = locations ++ response.gets.map(loc => loc.id -> loc.maybeNodeEntityId)
        if (remainingResponses == 1) {
          replyTo ! NodeLocations(newLocs)
          Behaviors.stopped
        } else {
          handleLookUpResponses(remainingResponses - 1, newLocs, replyTo)
        }
    }

}
