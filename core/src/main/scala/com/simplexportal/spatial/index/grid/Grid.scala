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

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import io.jvm.uuid._

object Grid {

  def logInfo(context: ActorContext[_], indexId: String, nodeLookUpPartitions: Int, latPartitions: Int, lonPartitions: Int): Unit = {
    context.log.info(
      """
        | Starting Guardian sharding [{}] with [{}] nodes lookup partitions, [{}] lat. partitions and [{}] lon. partitions.
        | Every shard in the index is going to cover a fixed area of [{}] km2 approx. [{}] Km. lat. x [{}] Km. lon.
        |""".stripMargin,
      indexId toString,
      nodeLookUpPartitions toString,
      latPartitions toString,
      lonPartitions toString,
      (( 40075 / lonPartitions ) * ( 40007 / latPartitions ))  toString,
      40007 / latPartitions toString,
      40075 / lonPartitions toString,
    )
  }

  val TileTypeKey = EntityTypeKey[TileIndexActor.Command]("TileEntity")
  val NodeLookUpTypeKey = EntityTypeKey[NodeLookUpActor.Command]("NodeLookUpEntity")

  def overwriteNumOfShards(numShards: Int, system: ActorSystem[_]) = ClusterShardingSettings.fromConfig(
    ConfigFactory.parseString(s"number-of-shards = ${numShards}").withFallback(
      system.settings.config.getConfig("akka.cluster.sharding")
    )
  )

  def initSharding(indexId: String, nodeLookUpShards: Int, tileIndexShards: Int, system: ActorSystem[_]): ClusterSharding = {
    val sharding = ClusterSharding(system)

    sharding.init(
      Entity(TileTypeKey) { entityContext =>
        TileIndexActor(indexId, entityContext.entityId)
      }
      withSettings(overwriteNumOfShards(tileIndexShards, system))
    )

    sharding.init(
      Entity(NodeLookUpTypeKey) { entityContext =>
        NodeLookUpActor(indexId, entityContext.entityId)
      }
      .withSettings (overwriteNumOfShards(nodeLookUpShards, system))
    )

    sharding
  }

  def apply(indexId: String, nodeLookUpPartitions: Int, latPartitions: Int, lonPartitions: Int): Behavior[TileIndexActor.Command] =
    Behaviors.setup { context =>
      logInfo(context, indexId, nodeLookUpPartitions, latPartitions, lonPartitions)

      val tileEntityFn = new TileIndexEntityIdGen(lonPartitions, latPartitions)

      val sharding = initSharding(indexId, nodeLookUpPartitions, latPartitions * lonPartitions, context.system)

      implicit val ctx = context
      implicit val timeout: Timeout = 6.seconds
      implicit val scheduler = context.system.executionContext

      Behaviors.receiveMessage {
        case cmd: TileIndexActor.AddNode =>
          context.spawn(
            AddNodeSession(sharding, cmd, LookUpNodeEntityIdGen.entityId(cmd.id), tileEntityFn.info(cmd.lat, cmd.lon)),
            s"adding_node_${UUID.randomString}"
          )
          Behaviors.same

        case addWayCmd: TileIndexActor.AddWay =>
//          sharding.entityRefFor(TileActor.TypeKey, partitionId(addWayCmd, lonPartitions, latPartitions)) ! addWayCmd
//          Behaviors.same
          ???
        case addBatchCmd: TileIndexActor.AddBatch =>
//          sharding.entityRefFor(TileActor.TypeKey, partitionId(addBatchCmd, lonPartitions, latPartitions)) ! addBatchCmd
//          Behaviors.same
          ???
        case TileIndexActor.GetMetrics(replyTo) =>
          ???
        case getNode: TileIndexActor.GetNode =>
          context.spawn(
            GetNodeSession(sharding, getNode, tileEntityFn),
            s"getting_node_${UUID.randomString}"
          )
          Behaviors.same
        case metricsCmd: TileIndexActor.GetWay =>
          ???

      }
    }

}
