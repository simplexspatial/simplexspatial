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

package com.simplexportal.spatial.index.grid.grpc

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.simplexportal.spatial.api.grpc._
import com.simplexportal.spatial.index.grid.tile
import com.simplexportal.spatial.index.grid.tile.actor
import com.simplexportal.spatial.index.grid.tile.actor.{
  AddBatch,
  AddNode,
  AddWay,
  BatchActions,
  Command,
  GetMetrics
}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class DataServiceImpl(gridIndex: ActorRef[Command])(
    implicit
    executionContext: ExecutionContext,
    scheduler: Scheduler
) extends DataService {

  // FIXME: Temporal timeout for POC
  implicit val timeout = Timeout(1 minutes)

  implicit def responseAdapter(response: actor.ACK): ACK = response match {
    case actor.Done()         => ACK().withDone(Done())
    case actor.NotDone(error) => ACK().withNotDone(NotDone(error))
  }

  override def addNode(in: AddNodeCmd): Future[ACK] =
    gridIndex
      .ask[actor.ACK](ref =>
        AddNode(in.id, in.lat, in.lon, in.attributes, Some(ref))
      )
      .map(responseAdapter)

  override def addWay(in: AddWayCmd): Future[ACK] =
    gridIndex
      .ask[actor.ACK](ref => AddWay(in.id, in.nodeIds, in.attributes, Some(ref))
      )
      .map(responseAdapter)

  override def getMetrics(in: GetMetricsCmd): Future[Metrics] =
    gridIndex
      .ask[actor.Metrics](GetMetrics(_))
      .map(m => Metrics(ways = m.ways, nodes = m.nodes))

  override def streamBatchCommands(
      in: Source[ExecuteBatchCmd, NotUsed]
  ): Source[ACK, NotUsed] =
    in.map(cmd => toAddBatch(cmd))
      .via(
        ActorFlow.ask(gridIndex)((commands, replyTo: ActorRef[actor.ACK]) =>
          AddBatch(commands, Some(replyTo))
        )
      )
      .map(responseAdapter);

  private def toAddBatch(
      batchCmd: ExecuteBatchCmd
  ): Seq[BatchActions] =
    batchCmd.commands.flatMap(executeCmd =>
      executeCmd.command match {
        case ExecuteCmd.Command.Way(way) =>
          Some(actor.AddWay(way.id, way.nodeIds, way.attributes))
        case ExecuteCmd.Command.Node(node) =>
          Some(actor.AddNode(node.id, node.lat, node.lon, node.attributes))
        case ExecuteCmd.Command.Empty => None
      }
    )
}
