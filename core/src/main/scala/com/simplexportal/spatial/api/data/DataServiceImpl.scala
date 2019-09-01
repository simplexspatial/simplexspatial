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
import akka.stream.scaladsl.{Sink, Source}
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

//  // sent from actor to stream to "ack" processing of given element
//  val AckMessage = Done
//
//  // sent from stream to actor to indicate start, end or failure of stream:
//  val InitMessage = TileActor.StreamInitialized
//  val OnCompleteMessage = TileActor.StreamCompleted
//  val onErrorMessage = (ex: Throwable) => TileActor.StreamFailure(ex)
//
//  val sink = Sink.actorRefWithAck(
//    tile,
//    onInitMessage = InitMessage,
//    ackMessage = AckMessage,
//    onCompleteMessage = OnCompleteMessage,
//    onFailureMessage = onErrorMessage)

  override def streamCommands(in: Source[ExecuteCmd, NotUsed]): Future[Done] =
    in
      .mapAsync(4)( // TODO: Maybe better mapAsync ???
        executeCmd =>
          executeCmd.command match {
            case ExecuteCmd.Command.Way(way) => Future(AddWay(way.id, way.nodeIds, way.attributes))
            case ExecuteCmd.Command.Node(node) => Future(AddNode(node.id, node.lat, node.lon, node.attributes))
            case ExecuteCmd.Command.Empty => Future(null)
          }
      )
      .groupedWithin(300, 1 second)
//      .map(tile ! AddBatch(_))
//      .runWith(sink)
      .runForeach( cmds => {
        tile ! AddBatch(cmds)
      })
      .map(_ => Done())

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
        .map( cmds => { // TODO: Replace with a actorRefWithAck
          tile ? AddBatch(cmds)
          Done()
          }
        )

  override def executeBatch(in: ExecuteBatchCmd): Future[Done] = {
    val commands: Seq[TileCommands] = in.commands.flatMap( executeCmd =>
        executeCmd.command match {
          case ExecuteCmd.Command.Way(way) => Some(AddWay(way.id, way.nodeIds, way.attributes))
          case ExecuteCmd.Command.Node(node) => Some(AddNode(node.id, node.lat, node.lon, node.attributes))
          case ExecuteCmd.Command.Empty => None
        }
    )
    tile ! AddBatch(commands)
    Future.successful(Done())
  }

  override def getMetrics(in: GetMetricsCmd): Future[Metrics] = {
    (tile ask GetMetrics)
      .mapTo[TileActor.Metrics]
      .map(m => Metrics(ways = m.ways, nodes = m.nodes))
  }


}
