/*
 * Copyright 2019 SimplexPortal Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simplexportal.spatial.loadosm

import java.io.{File, FileInputStream, InputStream}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.acervera.osm4scala.EntityIterator.fromPbf
import com.acervera.osm4scala.model.{NodeEntity, OSMTypes, WayEntity}
import com.simplexportal.spatial.api.data._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class LoadStreaming {

  val startTime = System.currentTimeMillis()

  var ways = 0
  var nodes = 0

  // Boot akka
  implicit val sys = ActorSystem("LoadOSMStreaming")
  implicit val mat = ActorMaterializer()
  implicit val ec = sys.dispatcher

  // Take details how to connect to the service from the config.
  val clientSettings = GrpcClientSettings.fromConfig(DataService.name)

  // Create a client-side stub for the service
  val client: DataService = DataServiceClient(clientSettings)

  def load(osmFile: File): Unit = {

    println(
      s"Loading data from [${osmFile.getAbsolutePath}]"
    )

    val reply = client.streamCommands(createSource(osmFile))
    reply.onComplete {
      case Success(msg) =>
        println(s"got reply for streaming requests: $msg")
        printTotals
        sys.terminate()
      case Failure(e) =>
        println(s"Error streamingRequest: $e")
        sys.terminate()
    }
  }

  def loadBatches(osmFile: File, blockSize: Int): Unit = {

    println(
      s"Loading batches data from [${osmFile.getAbsolutePath}]"
    )

    val reply = client
      .streamBatchCommands(createBatchSource(osmFile, blockSize))
      .runForeach(i =>
        println(
          s"Sent ${nodes} nodes and ${ways} ways in ${(System.currentTimeMillis() - startTime) / 1000} seconds."
        )
      )

    reply.onComplete {
      case Success(msg) =>
        println(s"got last reply for streaming requests as $msg")
        printTotals
        sys.terminate()
      case Failure(e) =>
        println(s"Error streamingRequest: $e")
        sys.terminate()
    }
  }

  def printTotals: Unit = {
    println("Asking for metrics .....")
    val metrics = Await.result(client.getMetrics(GetMetricsCmd()), 1 hour)
    println(
      s"Added ${metrics.nodes}/${nodes} nodes and ${metrics.ways}/${ways} ways in ${(System.currentTimeMillis() - startTime) / 1000} seconds."
    )
  }

  def createSource(osmFile: File): Source[ExecuteCmd, NotUsed] = {
    val pbfIS: InputStream = new FileInputStream(osmFile)

    Source
      .fromIterator(() => fromPbf(pbfIS))
      .filter(osmEntity => osmEntity.osmModel != OSMTypes.Relation)
      .map {
        case nodeEntity: NodeEntity =>
          nodes += 1
          ExecuteCmd().withNode(
            AddNodeCmd(
              nodeEntity.id,
              nodeEntity.longitude,
              nodeEntity.latitude,
              nodeEntity.tags
            )
          )
        case wayEntity: WayEntity =>
          ways += 1
          ExecuteCmd().withWay(
            AddWayCmd(wayEntity.id, wayEntity.nodes, wayEntity.tags)
          )
      }
  }

  def createBatchSource(osmFile: File, blockSize: Int): Source[ExecuteBatchCmd, NotUsed] = {
    val pbfIS: InputStream = new FileInputStream(osmFile)

    Source
      .fromIterator(() => fromPbf(pbfIS))
      .filter(osmEntity => osmEntity.osmModel != OSMTypes.Relation)
      .map {
        case nodeEntity: NodeEntity =>
          nodes += 1
          ExecuteCmd().withNode(
            AddNodeCmd(
              nodeEntity.id,
              nodeEntity.longitude,
              nodeEntity.latitude,
              nodeEntity.tags
            )
          )
        case wayEntity: WayEntity =>
          ways += 1
          ExecuteCmd().withWay(
            AddWayCmd(wayEntity.id, wayEntity.nodes, wayEntity.tags)
          )
      }
      .grouped(blockSize)
      .map(cmds => ExecuteBatchCmd().withCommands(cmds))
  }

}
