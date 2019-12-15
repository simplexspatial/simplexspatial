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

package com.simplexportal.spatial.index.grid

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.index.grid.Grid.{NodeLookUpTypeKey, TileTypeKey}
import com.simplexportal.spatial.index.grid.NodeLookUpActor.{GetResponse, NodeEntityId}

// FIXME: Fix style
// scalastyle:off cyclomatic.complexity
// scalastyle:off method.length
object GetNodesSession {

  def apply(
      sharding: ClusterSharding,
      getNodes: TileIndexActor.GetNodes,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Behavior[NotUsed] =

    Behaviors
      .setup[AnyRef] { context =>

      // Request locations of every node in the tile index.
      val uniqueNodes = getNodes.ids.distinct

        // Group by look-up entity id to get node locations.
      uniqueNodes
        .map(id => (id, LookUpNodeEntityIdGen.entityId(id)))
        .groupBy(_._2)
        .map( e => e._1 -> e._2.map(_._1))
        .foreach { case (entityId, ids) =>
            sharding.entityRefFor(
              NodeLookUpTypeKey,
              entityId
            ) ! NodeLookUpActor.Gets(ids, context.self)
        }

        // Map that will store nodes locations while arriving.
        var nodeLocations: Seq[(Long, Option[NodeEntityId] )] = Seq.empty

        var responsesCounter = 0
        var response = TileIndexActor.GetNodesResponse(Seq.empty)

        Behaviors.receiveMessage {
          case NodeLookUpActor.GetsResponse(nodeEntities) =>
            nodeLocations = nodeLocations ++ nodeEntities.map {case GetResponse(id, entityId) => (id, entityId)}
            if(nodeLocations.size == uniqueNodes.size) {
              // All nodes locations arrived, so group per tile entity id and get node value.
              nodeLocations
                .groupBy(_._2)
                .map( e => e._1 -> e._2.map { case (id, _) => id })
                .foreach {
                  case(Some(entityId), ids) =>
                    responsesCounter += 1
                    sharding.entityRefFor(
                      TileTypeKey,
                      tileIndexEntityIdGen.id(entityId.latIdx, entityId.lonIdx)
                    ) ! TileIndexActor.GetNodes(ids, context.self)
                  case(None, ids) =>
                    response = TileIndexActor.GetNodesResponse(
                      response.nodes ++ ids.map(TileIndexActor.GetNodeResponse(_, None))
                    )
                }
            }
            Behaviors.same
          case TileIndexActor.GetNodesResponse(nodes) =>
            responsesCounter -= 1
            response = TileIndexActor.GetNodesResponse(response.nodes ++ nodes)
            if(responsesCounter == 0) {
              getNodes.replyTo ! response
              Behaviors.stopped
            } else {
              Behaviors.same
            }
          case _ =>
            Behaviors.unhandled
        }
      }.narrow[NotUsed]

}
