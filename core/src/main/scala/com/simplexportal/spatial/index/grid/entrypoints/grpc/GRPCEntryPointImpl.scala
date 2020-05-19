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
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.simplexportal.spatial.index.grid.{GridProtocol => grid}
import com.simplexportal.spatial.index.grid.entrypoints.grpc
import com.simplexportal.spatial.model

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class GRPCEntryPointImpl(gridIndex: ActorRef[grid.GridRequest])(
    implicit
    executionContext: ExecutionContext,
    scheduler: Scheduler,
    system: ActorSystem
) extends GRPCEntryPoint {

  // FIXME: Temporal timeout for POC
  implicit val timeout = Timeout(1.minutes)

  implicit def toModel(node: grpc.Node): model.Node =
    model.Node(node.id, model.Location(node.lat, node.lon), node.attributes)

  implicit def toModel(way: grpc.Way): model.Way =
    model.Way(way.id, way.nodes.map(toModel), way.attributes)

  private def ackAdapter(response: grid.GridACK): grpc.AddBatchCmds.ACK = response match {
    case grid.GridDone()         => grpc.AddBatchCmds.ACK().withDone(true)
    case grid.GridNotDone(error) => grpc.AddBatchCmds.ACK().withDone(false).withMessage(error)
  }

  private def toAddBatch(
      batchCmd: grpc.AddBatchCmds.AddCmd.Command
  ): Option[grid.GridBatchCommand] =
    batchCmd match {
      case grpc.AddBatchCmds.AddCmd.Command.Node(node) =>
        Some(grid.GridAddNode(node))
      case grpc.AddBatchCmds.AddCmd.Command.Way(way) =>
        Some(grid.GridAddWay(way))
      case grpc.AddBatchCmds.AddCmd.Command.Empty => None
    }

  private def toAddBatches(
      batchCmd: grpc.AddBatchCmds
  ): Seq[grid.GridBatchCommand] =
    batchCmd.commands.flatMap(executeCmd => toAddBatch(executeCmd.command))

  override def addBatch(in: AddBatchCmds): Future[AddBatchCmds.ACK] =
    gridIndex
      .ask[grid.GridACK](ref => grid.GridAddBatch(toAddBatches(in), Some(ref)))
      .map(ackAdapter)

  // TODO: Implement metrics for the cluster.
  override def getMetrics(in: GetMetricsCmd): Future[GetMetricsCmd.Metrics] = ???
//    gridIndex
//      .ask[actor.Metrics](actor.GetMetrics(_))
//      .map(m => grpc.Metrics(ways = m.ways, nodes = m.nodes))

  override def addStreamBatches(in: Source[AddBatchCmds, NotUsed]): Source[AddBatchCmds.ACK, NotUsed] =
    in.map(cmd => toAddBatches(cmd))
      .via(
        ActorFlow.ask(gridIndex)((commands, replyTo: ActorRef[grid.GridACK]) =>
          grid.GridAddBatch(commands, Some(replyTo))
        )
      )
      .map(ackAdapter)

  override def searchNearestNode(in: SearchNearestNodeCmd): Future[SearchNearestNodeCmd.NearestNodeReply] =
    gridIndex
      .ask[grid.GridNearestNodeReply](ref => grid.GridNearestNode(model.Location(in.lat, in.lon), ref))
      .map {
        case grid.GridNearestNodeReply(Right(nodes)) =>
          grpc.SearchNearestNodeCmd
            .NearestNodeReply()
            .withDone(true)
            .withNodes(nodes.map(n => grpc.Node(n.id, n.location.lat, n.location.lat, n.attributes)).toSeq)
        case grid.GridNearestNodeReply(Left(error)) =>
          grpc.SearchNearestNodeCmd
            .NearestNodeReply()
            .withDone(false)
            .withMessage(error)
      }

}
