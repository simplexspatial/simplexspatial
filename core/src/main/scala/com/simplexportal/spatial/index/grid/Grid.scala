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

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityTypeKey
}
import akka.util.Timeout
import com.simplexportal.spatial.index.grid.lookups.{
  NodeLookUpActor,
  WayLookUpActor
}
import com.simplexportal.spatial.index.grid.sessions._
import com.simplexportal.spatial.index.grid.tile.actor.{
  Command,
  TileIndexActor,
  TileIndexEntityIdGen
}
import com.simplexportal.spatial.index.protocol._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

// TODO: Every context.spawn(*Session(...), ... ) should be replaced by a spawn in the cluster, to start the session in a free node and not in this one.
object Grid {

  implicit class GridAddNodeEnricher(cmd: GridBatchCommand) {
    def toBatch(): GridAddBatch = cmd match {
      case addNode: GridAddNode =>
        GridAddBatch(Seq(addNode.copy(replyTo = None)), addNode.replyTo)
      case addWay: GridAddWay =>
        GridAddBatch(Seq(addWay.copy(replyTo = None)), addWay.replyTo)
    }
  }

  val TileTypeKey = EntityTypeKey[Command]("TileEntity")
  val NodeLookUpTypeKey =
    EntityTypeKey[NodeLookUpActor.Command]("NodeLookUpEntity")
  val WayLookUpTypeKey =
    EntityTypeKey[WayLookUpActor.Command]("WayLookUpEntity")

  def apply(gridConfig: GridConfig): Behavior[GridRequest] =
    Behaviors.setup { context =>
      context.log.info(gridConfig.description)

      implicit val tileEntityFn =
        TileIndexEntityIdGen(gridConfig.lonPartitions, gridConfig.latPartitions)

      implicit val sharding = initSharding(
        gridConfig.indexId,
        gridConfig.wayLookUpPartitions,
        gridConfig.nodeLookUpPartitions,
        gridConfig.latPartitions * gridConfig.lonPartitions,
        context.system
      )

      implicit val ctx = context
      implicit val timeout: Timeout = 6.seconds
      implicit val scheduler = context.system.executionContext

      Behaviors.receiveMessage {

        // Commands
        case cmd: GridBatchCommand =>
          AddBatchSession.processRequest(cmd.toBatch, context)
          Behaviors.same

        case cmd: GridAddBatch =>
          AddBatchSession.processRequest(cmd, context)
          Behaviors.same

        // Queries
        case cmd: GridGetNode =>
          GetNodeSession.processRequest(cmd, context)
          Behaviors.same

        case cmd: GridGetWay =>
          GetWaySession.processRequest(cmd, context)
          Behaviors.same

        case cmd: GridNearestNode =>
          GetNearestNodeSession.processRequest(cmd, context)
          Behaviors.same

      }
    }

  def initSharding(
      indexId: String,
      wayLookUpShards: Int,
      nodeLookUpShards: Int,
      tileIndexShards: Int,
      system: ActorSystem[_]
  ): ClusterSharding = {
    val sharding = ClusterSharding(system)

    sharding.init(
      Entity(TileTypeKey) { entityContext =>
        TileIndexActor(indexId, entityContext.entityId)
      }
        withSettings (overwriteNumOfShards(tileIndexShards, system))
    )

    sharding.init(
      Entity(NodeLookUpTypeKey) { entityContext =>
        NodeLookUpActor(indexId, entityContext.entityId)
      }.withSettings(overwriteNumOfShards(nodeLookUpShards, system))
    )

    sharding.init(
      Entity(WayLookUpTypeKey) { entityContext =>
        WayLookUpActor(indexId, entityContext.entityId)
      }.withSettings(overwriteNumOfShards(wayLookUpShards, system))
    )

    sharding
  }

  private def overwriteNumOfShards(numShards: Int, system: ActorSystem[_]) =
    ClusterShardingSettings.fromConfig(
      ConfigFactory
        .parseString(s"number-of-shards = ${numShards}")
        .withFallback(
          system.settings.config.getConfig("akka.cluster.sharding")
        )
    )

}
