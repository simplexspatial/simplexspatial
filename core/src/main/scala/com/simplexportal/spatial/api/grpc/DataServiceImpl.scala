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

package com.simplexportal.spatial.api.grpc

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.simplexportal.spatial.index.grid.tile

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class DataServiceImpl(gridIndex: ActorRef[tile.Command])(
    implicit
    executionContext: ExecutionContext,
    scheduler: Scheduler
) extends DataService {

  // FIXME: Temporal timeout for POC
  implicit val timeout = Timeout(15 minutes)

  implicit def responseAdapter(node: tile.Done): Done = Done()

  override def addNode(in: AddNodeCmd): Future[Done] =
    gridIndex
      .ask[tile.Done](ref =>
        tile.AddNode(in.id, in.lat, in.lon, in.attributes, Some(ref))
      )
      .map(responseAdapter)

  override def addWay(in: AddWayCmd): Future[Done] =
    gridIndex
      .ask[tile.Done](ref =>
        tile.AddWay(in.id, in.nodeIds, in.attributes, Some(ref))
      )
      .map(responseAdapter)

  override def getMetrics(in: GetMetricsCmd): Future[Metrics] =
    gridIndex
      .ask[tile.Metrics](tile.GetMetrics(_))
      .map(m => Metrics(ways = m.ways, nodes = m.nodes))

  override def streamBatchCommands(
      in: Source[ExecuteBatchCmd, NotUsed]
  ): Source[Done, NotUsed] =
    in.map(cmd => toAddBatch(cmd))
      .via(
        ActorFlow.ask(gridIndex)((commands, replyTo: ActorRef[tile.Done]) =>
          tile.AddBatch(commands, Some(replyTo))
        )
      )
      .map(responseAdapter);

  private def toAddBatch(
      batchCmd: ExecuteBatchCmd
  ): Seq[tile.BatchActions] =
    batchCmd.commands.flatMap(executeCmd =>
      executeCmd.command match {
        case ExecuteCmd.Command.Way(way) =>
          Some(tile.AddWay(way.id, way.nodeIds, way.attributes))
        case ExecuteCmd.Command.Node(node) =>
          Some(tile.AddNode(node.id, node.lat, node.lon, node.attributes))
        case ExecuteCmd.Command.Empty => None
      }
    )
}
