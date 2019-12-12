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
import com.simplexportal.spatial.index.grid.NodeLookUpActor.Done
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


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
        withSettings (overwriteNumOfShards(nodeLookUpShards, system))
    )

    sharding
  }

  def apply(indexId: String, nodeLookUpPartitions: Int, latPartitions: Int, lonPartitions: Int): Behavior[TileIndexActor.Command] =
    Behaviors.setup { context =>
      logInfo(context, indexId, nodeLookUpPartitions, latPartitions, lonPartitions)


      val nodeLookUpHashFn = (id: Long) => id.toString
      val tileHashFn = TileIndexHashFunction(lonPartitions, latPartitions)

      val sharding = initSharding(indexId, nodeLookUpPartitions, latPartitions * lonPartitions, context.system)
      implicit val ctx = context
      implicit val timeout: Timeout = 6.seconds
      implicit val scheduler = context.system.executionContext

      Behaviors.receiveMessage {
        case cmd: TileIndexActor.AddNode =>
          val result = addNode(nodeLookUpHashFn(cmd.id), tileHashFn(cmd.lat, cmd.lon), sharding, cmd)
          cmd.replyTo.map { clientRef =>
            result.onComplete {
              case Success(_) => clientRef ! TileIndexActor.Done()
              case Failure(ex) =>
                context.log.error(s"Error Adding node (${cmd}).", ex)
              // TODO: Should be able to response with Done and NotDone, or ACK and NACK.
            }
          }
          // FIXME: Following code should be executed after the execution of both previous calls?
          // In other case, it is going to consume the next message and downstream. So we can fill create hundreds of ask Actors.
          Behaviors.same

        case addWayCmd: TileIndexActor.AddWay =>
//          sharding.entityRefFor(TileActor.TypeKey, partitionId(addWayCmd, lonPartitions, latPartitions)) ! addWayCmd
//          Behaviors.same
          ???
        case addBatchCmd: TileIndexActor.AddBatch =>
//          sharding.entityRefFor(TileActor.TypeKey, partitionId(addBatchCmd, lonPartitions, latPartitions)) ! addBatchCmd
//          Behaviors.same
          ???
        case metricsCmd: TileIndexActor.GetMetrics =>
//          sharding.entityRefFor(TileActor.TypeKey, partitionId(metricsCmd, lonPartitions, latPartitions)) ! metricsCmd
//          Behaviors.same
          ???
        case metricsCmd: TileIndexActor.GetNode =>
          ???
        case metricsCmd: TileIndexActor.GetWay =>
          ???

      }
    }

  private def addNode
    (nodeHash: String, tileHash: TileIndexHashFunction.TileHashInfo, sharding: ClusterSharding, cmd: TileIndexActor.AddNode)
    (implicit context: ActorContext[_], timeout: Timeout, executionContext: ExecutionContext)
  : Future[TileIndexActor.Done] = {

    val nodeLookUpActor = sharding.entityRefFor(NodeLookUpTypeKey, nodeHash)
    val tileIndexActor = sharding.entityRefFor(TileTypeKey, tileHash.tileHash)

    println(s">>>>>>>>>>>>>>>>>>>>>. Sending to ${nodeLookUpActor}")
    val nodeLookUpFuture = nodeLookUpActor.ask[Done](ref =>
      NodeLookUpActor.Put(
        cmd.id,
        NodeLookUpActor.Hash(tileHash.latIdx, tileHash.lonIdx),
        cmd.replyTo.map(_ => ref)
      )
    )

    nodeLookUpFuture.map(r => println(s">>>>>>>>>>>>>> Responding with ${r}"))

    val tileIndexFuture = tileIndexActor.ask[TileIndexActor.Done](ref =>
      cmd.copy(replyTo = cmd.replyTo.map(_ => ref))
    )

    for (
      _ <- nodeLookUpFuture;
      _ <- tileIndexFuture
    ) yield TileIndexActor.Done()
  }
}
