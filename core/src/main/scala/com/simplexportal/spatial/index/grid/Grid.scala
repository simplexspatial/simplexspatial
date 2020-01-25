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
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityTypeKey
}
import akka.util.Timeout
import com.simplexportal.spatial.index.grid.lookups.{
  LookUpNodeEntityIdGen,
  NodeLookUpActor,
  WayLookUpActor
}
import com.simplexportal.spatial.index.grid.sessions._
import com.simplexportal.spatial.index.grid.tile._
import com.typesafe.config.ConfigFactory
import io.jvm.uuid._

import scala.concurrent.duration._

// TODO: Every context.spawn(*Session(...), ... ) should be replaced by a spawn in the cluster, to start the session in a free node and not in this one.
object Grid {

  def logInfo(
      context: ActorContext[_],
      indexId: String,
      nodeLookUpPartitions: Int,
      wayLookUpPartitions: Int,
      latPartitions: Int,
      lonPartitions: Int
  ): Unit = {
    context.log.info(
      """
        | Starting Guardian sharding [{}] with:
        | -> [{}] nodes lookup partitions,
        | -> [{}] ways lookup partitions,
        | -> [{}] lat. partitions and [{}] lon. partitions. So every shard in the index is going to cover a fixed area of [{}] km2 approx. [{}] Km. lat. x [{}] Km. lon.
        |""".stripMargin,
      indexId toString,
      nodeLookUpPartitions toString,
      wayLookUpPartitions toString,
      latPartitions toString,
      lonPartitions toString,
      ((40075 / lonPartitions) * (40007 / latPartitions)) toString,
      40007 / latPartitions toString,
      40075 / lonPartitions toString
    )
  }

  val TileTypeKey = EntityTypeKey[Command]("TileEntity")
  val NodeLookUpTypeKey =
    EntityTypeKey[NodeLookUpActor.Command]("NodeLookUpEntity")
  val WayLookUpTypeKey =
    EntityTypeKey[WayLookUpActor.Command]("WayLookUpEntity")

  private def overwriteNumOfShards(numShards: Int, system: ActorSystem[_]) =
    ClusterShardingSettings.fromConfig(
      ConfigFactory
        .parseString(s"number-of-shards = ${numShards}")
        .withFallback(
          system.settings.config.getConfig("akka.cluster.sharding")
        )
    )

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

  // scalastyle:off method.length
  def apply(
      indexId: String,
      wayLookUpPartitions: Int,
      nodeLookUpPartitions: Int,
      latPartitions: Int,
      lonPartitions: Int
  ): Behavior[Command] =
    Behaviors.setup { context =>
      logInfo(
        context,
        indexId,
        nodeLookUpPartitions,
        wayLookUpPartitions,
        latPartitions,
        lonPartitions
      )

      val tileEntityFn = new TileIndexEntityIdGen(lonPartitions, latPartitions)

      val sharding = initSharding(
        indexId,
        wayLookUpPartitions,
        nodeLookUpPartitions,
        latPartitions * lonPartitions,
        context.system
      )

      implicit val ctx = context
      implicit val timeout: Timeout = 6.seconds
      implicit val scheduler = context.system.executionContext

      Behaviors.receiveMessage {

        case cmd: AddNode =>
          context.spawn(
            AddNodeSession(sharding, cmd, tileEntityFn),
            s"adding_node_${UUID.randomString}"
          )
          Behaviors.same

        case cmd: AddWay =>
          context.spawn(
            AddWaySession(sharding, cmd, tileEntityFn),
            s"adding_way_${UUID.randomString}"
          )
          Behaviors.same

        case AddBatch(cmds, maybeReplyTo) =>
          context.spawn(
            AddBatchSession(sharding, cmds, maybeReplyTo, tileEntityFn),
            s"adding_batch_${UUID.randomString}"
          )
          Behaviors.same

        case cmd: GetInternalNode =>
          context.spawn(
            GetInternalNodeSession(sharding, cmd, tileEntityFn),
            s"getting_internal_node_${UUID.randomString}"
          )
          Behaviors.same

        case cmd: GetInternalNodes =>
          context.spawn(
            GetInternalNodesSession(sharding, cmd, tileEntityFn),
            s"getting_internal_nodes_${UUID.randomString}"
          )
          Behaviors.same

        case cmd: GetWay =>
          context.spawn(
            GetWaySession(sharding, cmd.id, cmd.replyTo),
            s"getting_way_${UUID.randomString}"
          )
          Behaviors.same

        case GetMetrics(replyTo) =>
          ???

        case cmd: GetInternalWay =>
          ???
      }
    }
  // scalastyle:on method.length

}
