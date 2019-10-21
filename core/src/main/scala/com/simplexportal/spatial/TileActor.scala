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

package com.simplexportal.spatial

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.simplexportal.spatial.Tile.{Node, Way}
import com.simplexportal.spatial.api.data.Done

import scala.concurrent.duration._

// FIXME: Don't use foreign messages in the Actor as the documentation explains.
// TODO: Implement AddBatch.
// TODO: Force Reply with https://doc.akka.io/docs/akka/current/typed/persistence.html#replies

object TileActor {

  // Commands
  sealed trait Command

  final case class AddNode(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String],
      replyTo: ActorRef[Done]
  ) extends Command

  final case class AddWay(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String],
      replyTo: ActorRef[Done]
  ) extends Command

  final case class AddBatch(cmds: Seq[Command]) extends Command

  final case class GetNode(id: Long, replyTo: ActorRef[Option[Node]]) extends Command

  final case class GetWay(id: Long, replyTo: ActorRef[Option[Way]]) extends Command

  final case class GetMetrics(replyTo: ActorRef[Metrics]) extends Command



  // Events.
  sealed trait Event

  final case class NodeAdded(
                        id: Long,
                        lat: Double,
                        lon: Double,
                        attributes: Map[String, String]
                      ) extends Event

  final case class WayAdded(
                       id: Long,
                       nodeIds: Seq[Long],
                       attributes: Map[String, String]
                     ) extends Event



  sealed trait RTreeDataTransfer
  case class Metrics(ways: Long, nodes: Long) extends RTreeDataTransfer


  def apply(persistenceId: PersistenceId): Behavior[Command] =
    EventSourcedBehavior[Command, Event, Tile](
      persistenceId = persistenceId,
      emptyState = Tile(),
      commandHandler = (state, command) => onCommand(state, command),
      eventHandler = (state, event) => applyEvent(state, event))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(1.second, 30.seconds, 0.2))

  private def onCommand(tile: Tile, command: Command): Effect[Event, Tile] = {
    command match {
      case GetMetrics(replyTo) =>
        replyTo ! Metrics(tile.ways.size, tile.nodes.size)
        Effect.none

      case GetNode(id, replyTo) =>
        replyTo ! tile.nodes.get(id)
        Effect.none

      case GetWay(id, replyTo) =>
        replyTo ! tile.ways.get(id)
        Effect.none

      case cmd: AddNode =>
        Effect.persist(NodeAdded(cmd.id, cmd.lat, cmd.lon, cmd.attributes)).thenRun { _ =>
          cmd.replyTo ! Done()
        }

      case cmd: AddWay =>
        Effect.persist(WayAdded(cmd.id, cmd.nodeIds, cmd.attributes)).thenRun { _ =>
          cmd.replyTo ! Done()
        }

      case AddBatch(cmds) =>
        ???
    }
  }

  private def applyEvent(tile: Tile, event: Event): Tile = {
    event match {
      case node: NodeAdded => tile.addNode(node.id, node.lat, node.lon, node.attributes)
      case way: WayAdded => tile.addWay(way.id, way.nodeIds, way.attributes)
      case _ => ??? // TODO: Implement addBatchHandler
    }
  }



//    private def addBatchHandler(events: Seq[Event]) =
//      persist(events) (events => events.foreach{
//        case node: NodeAdded => addNode(node)
//        case way: WayAdded => addWay(way)
//      })


}

