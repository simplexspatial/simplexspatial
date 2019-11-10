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
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.simplexportal.spatial.TileActor

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class DataServiceImpl(tile: ActorRef[TileActor.Command])(
    implicit
      executionContext: ExecutionContext,
      scheduler: Scheduler
) extends DataService {

  // FIXME: Temporal timeout for POC
  implicit val timeout = Timeout(15 minutes)

  implicit def responseAdapter(node: TileActor.Done): Done = Done()

  override def addNode(in: AddNodeCmd): Future[Done] =
    tile.ask[TileActor.Done]( ref =>TileActor.AddNode(in.id, in.lat, in.lon, in.attributes, Some(ref)) )
    .map(responseAdapter)

  override def addWay(in: AddWayCmd): Future[Done] =
    tile.ask[TileActor.Done](ref => TileActor.AddWay(in.id, in.nodeIds, in.attributes, Some(ref)))
    .map(responseAdapter)

  override def getMetrics(in: GetMetricsCmd): Future[Metrics] =
    tile.ask[TileActor.Metrics](TileActor.GetMetrics(_))
      .map(m => Metrics(ways = m.ways, nodes = m.nodes))

  override def streamBatchCommands(in: Source[ExecuteBatchCmd, NotUsed]): Source[Done, NotUsed] =
    in
      .map(cmd => toAddBatch(cmd))
      .via(ActorFlow.ask(tile)((commands, replyTo: ActorRef[TileActor.Done]) => TileActor.AddBatch(commands, Some(replyTo))))
      .map(responseAdapter);

  private def toAddBatch(batchCmd: ExecuteBatchCmd): Seq[TileActor.BatchCommand] =
    batchCmd.commands.flatMap(
      executeCmd =>
        executeCmd.command match {
          case ExecuteCmd.Command.Way(way) => Some(TileActor.AddWay(way.id, way.nodeIds, way.attributes))
          case ExecuteCmd.Command.Node(node) => Some(TileActor.AddNode(node.id, node.lat, node.lon, node.attributes))
          case ExecuteCmd.Command.Empty => None
        }
    )
}
