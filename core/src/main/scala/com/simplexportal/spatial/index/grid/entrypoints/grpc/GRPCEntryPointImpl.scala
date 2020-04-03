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

package com.simplexportal.spatial.index.grid.entrypoints.grpc

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.simplexportal.spatial.index.grid.GridProtocol._
import com.simplexportal.spatial.index.grid.entrypoints.grpc
import com.simplexportal.spatial.model.Location

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class GRPCEntryPointImpl(gridIndex: ActorRef[GridRequest])(
    implicit
    executionContext: ExecutionContext,
    scheduler: Scheduler
) extends GRPCEntryPoint {

  // FIXME: Temporal timeout for POC
  implicit val timeout = Timeout(1.minutes)

  def ackAdapter(response: GridACK): grpc.ACK = response match {
    case GridDone()         => grpc.ACK().withDone(grpc.Done())
    case GridNotDone(error) => grpc.ACK().withNotDone(grpc.NotDone(error))
  }

  override def addNode(in: grpc.AddNodeCmd): Future[grpc.ACK] =
    gridIndex
      .ask[GridACK](ref => GridAddNode(in.id, in.lat, in.lon, in.attributes, Some(ref)))
      .map(ackAdapter)

  override def addWay(in: grpc.AddWayCmd): Future[grpc.ACK] =
    gridIndex
      .ask[GridACK](ref => GridAddWay(in.id, in.nodeIds, in.attributes, Some(ref)))
      .map(ackAdapter)

  // TODO: Implement metrics for the cluster.
  override def getMetrics(in: grpc.GetMetricsCmd): Future[grpc.Metrics] = ???
//    gridIndex
//      .ask[actor.Metrics](actor.GetMetrics(_))
//      .map(m => grpc.Metrics(ways = m.ways, nodes = m.nodes))

  override def streamBatchCommands(
      in: Source[grpc.ExecuteBatchCmd, NotUsed]
  ): Source[grpc.ACK, NotUsed] =
    in.map(cmd => toAddBatch(cmd))
      .via(
        ActorFlow.ask(gridIndex)((commands, replyTo: ActorRef[GridACK]) => GridAddBatch(commands, Some(replyTo)))
      )
      .map(ackAdapter);

  private def toAddBatch(
      batchCmd: grpc.ExecuteBatchCmd
  ): Seq[GridBatchCommand] =
    batchCmd.commands.flatMap(executeCmd =>
      executeCmd.command match {
        case grpc.ExecuteCmd.Command.Way(way) =>
          Some(GridAddWay(way.id, way.nodeIds, way.attributes))
        case grpc.ExecuteCmd.Command.Node(node) =>
          Some(GridAddNode(node.id, node.lat, node.lon, node.attributes))
        case grpc.ExecuteCmd.Command.Empty => None
      }
    )

  override def searchNearestNode(in: SearchNearestNodeCmd): Future[NearestNodeReply] =
    gridIndex
      .ask[GridNearestNodeReply](ref => GridNearestNode(Location(in.lat, in.lon), ref))
      .map {
        case GridNearestNodeReply(Right(nodes)) =>
          grpc
            .NearestNodeReply()
            .withDone(
              grpc.NearestNode(nodes.map(n => grpc.Node(n.id, n.location.lat, n.location.lat, n.attributes)).toSeq)
            )
        case GridNearestNodeReply(Left(error)) => grpc.NearestNodeReply().withNotDone(grpc.NotDone(error))
      }
}
