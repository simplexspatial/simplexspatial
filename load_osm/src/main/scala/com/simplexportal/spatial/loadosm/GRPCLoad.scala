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

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.acervera.osm4scala.model.{NodeEntity, OSMEntity, WayEntity}
import com.simplexportal.spatial.api.data._

import scala.concurrent.Await
import scala.concurrent.duration._

import ExecuteCmd._

object GRPCLoad extends LoadBlocks {

  implicit val sys = ActorSystem("GRPC_Loader")
  implicit val mat = ActorMaterializer()
  implicit val ec = sys.dispatcher

  var nodesCounter = 0L
  var waysCounter = 0L

//  val clientSettings = GrpcClientSettings.fromConfig(SimplexSpatialService.name)
  val clientSettings = GrpcClientSettings
    .connectToServiceAt(
      config.getString("simplexportal.spatial.service.interface"),
      config.getInt("simplexportal.spatial.service.port")
    )
    .withDeadline(1 hour)
    .withConnectionAttempts(1)
    .withTls(false)

  val client: DataService = DataServiceClient(clientSettings)

  def toCmd(node: NodeEntity): AddNodeCmd = AddNodeCmd(
    id = node.id,
    lat = node.latitude,
    lon = node.longitude,
    attributes = node.tags
  )

  def toCmd(way: WayEntity): AddWayCmd = AddWayCmd(
    id = way.id,
    nodeIds = way.nodes,
    attributes = way.tags
  )

  override def addGroup(entities: Seq[OSMEntity]): Unit = {

    val (
      localCommands: Seq[ExecuteCmd],
      nodesLocalCounter: Long,
      waysLocalCounter: Long,
      othersLocalCount: Long
    ) =
      entities.foldLeft(Seq[ExecuteCmd](), 0L, 0L, 0L) {
        case ((commands, nodes, ways, others), entity) =>
          entity match {
            case node: NodeEntity =>
              (
                commands :+ ExecuteCmd(
                  Cmd.Node(
                    AddNodeCmd(
                      node.id,
                      node.longitude,
                      node.latitude,
                      node.tags
                    )
                  )
                ),
                nodes + 1,
                ways,
                others
              )
            case way: WayEntity =>
              (
                commands :+ ExecuteCmd(
                  Cmd.Way(AddWayCmd(way.id, way.nodes, way.tags))
                ),
                nodes,
                ways + 1,
                others
              )
            case _ => (commands, nodes, ways, others + 1)
          }
      }

    nodesCounter += nodesLocalCounter
    waysCounter += waysLocalCounter
    client.executeBatch(ExecuteBatchCmd(localCommands))
  }

  override def printTotals(time: Long): Unit = {
    println("Asking for metrics .....")
    val metrics = Await.result(client.getMetrics(GetMetricsCmd()), 1 hour)
    println(s"Added ${metrics.nodes} nodes and ${metrics.ways} ways in ${(System
      .currentTimeMillis() - startTime) / 1000} seconds.")
  }

  override def printPartials(time: Long): Unit = {
    println(s"sent ${nodesCounter} nodes and ${waysCounter} ways.")
  }

  override def clean(): Unit = sys.terminate()

}
