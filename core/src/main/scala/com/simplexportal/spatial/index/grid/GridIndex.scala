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

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import com.simplexportal.spatial.model.BoundingBox

object GridIndex {

  def tileId(indexId: String, bbox: BoundingBox): String =
    s"${indexId}_[(${bbox.min.lon},${bbox.min.lat}),(${bbox.max.lon},${bbox.max.lat})]"

  def logInfo(context: ActorContext[TileActor.Command], indexId: String, lonPartitions: Int, latPartitions: Int): Unit =
    context.log.info(
      "Every shard in the index [{}] is going to cover a fixed area of [{}] km2 approx. ",
      indexId,
      40075 / lonPartitions toString,
      40007 / latPartitions toString,
      ( 40075 / lonPartitions ) * ( 40007 / latPartitions ) toString
    )

  private def partitionId(message: TileActor.Command): String = "FIXED_SHARD"

  private def createNewShardRegion(clusterSharding: ClusterSharding)(indexId: String, lonPartitions: Int, latPartitions: Int) =
    clusterSharding.init(Entity(TileActor.TypeKey) { entityContext =>
        TileActor(indexId, entityContext.entityId)
      }
    )


  def apply(indexId: String, lonPartitions: Int, latPartitions: Int): Behavior[TileActor.Command] =
    Behaviors.setup { context =>
      context.log.info("Starting Guardian sharding [{}]", indexId)
      logInfo(context, indexId, lonPartitions, latPartitions)

      val sharding = ClusterSharding(context.system)
//      val newShardRegion = createNewShardRegion(sharding)

      Behaviors.receiveMessage {
        case addNodeCmd: TileActor.AddNode =>
          sharding.entityRefFor(TileActor.TypeKey, partitionId(addNodeCmd)) ! addNodeCmd
          Behaviors.same
        case addWayCmd: TileActor.AddWay =>
          sharding.entityRefFor(TileActor.TypeKey, partitionId(addWayCmd)) ! addWayCmd
          Behaviors.same
        case addBatchCmd: TileActor.AddBatch =>
          sharding.entityRefFor(TileActor.TypeKey, partitionId(addBatchCmd)) ! addBatchCmd
          Behaviors.same
        case metricsCmd: TileActor.GetMetrics =>
          sharding.entityRefFor(TileActor.TypeKey, partitionId(metricsCmd)) ! metricsCmd
          Behaviors.same
        case metricsCmd: TileActor.GetNode =>
          ???
        case metricsCmd: TileActor.GetWay =>
          ???

      }
    }

}
