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

package com.simplexportal.spatial.api.data

import akka.NotUsed
import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.simplexportal.spatial.TileActor
import com.simplexportal.spatial.TileActor._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class DataServiceImpl(tile: ActorRef)(
    implicit executionContext: ExecutionContext,
    materializer: Materializer
) extends DataService {

  // FIXME: Temporal timeout for POC
  implicit val timeout = Timeout(15 minutes)

  override def addNode(in: AddNodeCmd): Future[Done] = {
    tile ! AddNode(in.id, in.lat, in.lon, in.attributes)
    Future.successful(Done())
  }

  override def addWay(in: AddWayCmd): Future[Done] = {
    tile ! AddWay(in.id, in.nodeIds, in.attributes)
    Future.successful(Done())
  }

  override def getMetrics(in: GetMetricsCmd): Future[Metrics] =
    (tile ask GetMetrics)
      .mapTo[TileActor.Metrics]
      .map(m => Metrics(ways = m.ways, nodes = m.nodes))

  override def streamBatchCommands(in: Source[ExecuteBatchCmd, NotUsed]): Source[Done, NotUsed] =
    in
      .map( executeBatchCmd => executeBatchCmd.commands.flatMap(
        executeCmd =>
          executeCmd.command match {
            case ExecuteCmd.Command.Way(way) => Some(AddWay(way.id, way.nodeIds, way.attributes))
            case ExecuteCmd.Command.Node(node) => Some(AddNode(node.id, node.lat, node.lon, node.attributes))
            case ExecuteCmd.Command.Empty => None
          }
      ))
      .map(AddBatch(_))
      .ask[Done](tile) // TODO: Maybe better performance with a actorRefWithAck

}
