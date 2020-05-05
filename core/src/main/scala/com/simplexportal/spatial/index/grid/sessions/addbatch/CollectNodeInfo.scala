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

package com.simplexportal.spatial.index.grid.sessions.addbatch

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorTags, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.index.grid.GridProtocol._
import com.simplexportal.spatial.index.grid.sessions.GetNodeLocationsSession
import com.simplexportal.spatial.index.grid.tile.actor.TileIndexEntityIdGen
import io.jvm.uuid.UUID

import scala.util.{Failure, Success, Try}

/**
  * To update/insert data ways, it is necessary to collect all information about nodes that are part of the way.
  * This object collect all functions used to do it.
  */
protected trait CollectNodeInfo extends Adapter with UpdateIndices with DataDistribution {

  /**
    * Calculate a Map of
    * @param cmd
    * @param sharding
    * @param tileIndexEntityIdGen
    * @return
    */
  def collectNodeIdx(cmd: GridAddBatch)(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Behavior[ForeignResponse] = Behaviors.setup[ForeignResponse] { context =>
    val locsResponseAdapter: ActorRef[GetNodeLocationsSession.NodeLocations] =
      context.messageAdapter { LocationsWrapper }

    val nodesDefinitions = extractNodesTileIdxs(cmd.commands)
    val nodesUsedInWays = extractNodesUsedInWays(cmd.commands)
    val nodesNotPresentInThisBlock = nodesUsedInWays.filterNot(nodesDefinitions.contains)

    context.spawn(
      GetNodeLocationsSession(sharding, nodesNotPresentInThisBlock, locsResponseAdapter),
      s"node_locations_${UUID.randomString}",
      ActorTags("session", "session-collect-node-info")
    )

    Behaviors.receiveMessagePartial {
      case LocationsWrapper(GetNodeLocationsSession.NodeLocations(locations)) =>
        Try(
          locations.flatMap {
            case (id, None) =>
              throw new Exception(s"Node with id ${id} not found.")
            case (id, Some(tileIdx)) =>
              Some((id -> tileIdx))
          } ++ nodesDefinitions
        ) match {
          case Success(locationsIdx) =>
            updateIndices(
              cmd.commands,
              locationsIdx,
              cmd.replyTo
            )(sharding)
          case Failure(exception) =>
            cmd.replyTo.foreach(_ ! GridNotDone(exception.getMessage))
            Behaviors.stopped
        }
    }
  }

  def extractNodesUsedInWays(commands: Seq[GridBatchCommand]): Set[Long] =
    commands.flatMap {
      case w: GridAddWay => w.nodeIds
      case _             => Seq.empty
    }.toSet
}
