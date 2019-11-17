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
import com.simplexportal.spatial.model.BoundingBox

import scala.collection.breakOut
import scala.concurrent.duration._

// TODO: Force Reply with https://doc.akka.io/docs/akka/current/typed/persistence.html#replies

object TileActor {

  // From here all possible replies.
  sealed trait Reply
  case class Metrics(ways: Long, nodes: Long) extends Reply
  case class Done() extends Reply

  // From here all possible commands accepted.
  sealed trait Command
  sealed trait BatchCommand extends Command

  final case class AddNode(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String],
      replyTo: Option[ActorRef[TileActor.Done]] = None
  ) extends BatchCommand

  final case class AddWay(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String],
      replyTo: Option[ActorRef[TileActor.Done]] = None
  ) extends BatchCommand

  final case class AddBatch(cmds: Seq[BatchCommand], replyTo: Option[ActorRef[TileActor.Done]] = None) extends Command

  final case class GetNode(id: Long, replyTo: ActorRef[Option[Node]]) extends Command

  final case class GetWay(id: Long, replyTo: ActorRef[Option[Way]]) extends Command

  final case class GetMetrics(replyTo: ActorRef[Metrics]) extends Command

  // From here, all possible events generated.
  sealed trait Event
  sealed trait AtomicEvent extends Event

  final case class NodeAdded(
                        id: Long,
                        lat: Double,
                        lon: Double,
                        attributes: Map[String, String]
                      ) extends AtomicEvent

  final case class WayAdded(
                       id: Long,
                       nodeIds: Seq[Long],
                       attributes: Map[String, String]
                     ) extends AtomicEvent

  final case class BatchAdded(events: Seq[AtomicEvent]) extends Event



  def apply(indexId: String, bbox: BoundingBox): Behavior[Command] =
    EventSourcedBehavior[Command, Event, Tile](
      persistenceId = PersistenceId("TileActor", s"${indexId}_[(${bbox.min.lon},${bbox.min.lat}),(${bbox.max.lon},${bbox.max.lat})]"),
      emptyState = Tile(),
      commandHandler = (state, command) => onCommand(state, command),
      eventHandler = (state, event) => applyEvent(state, event)
    )
    .onPersistFailure(SupervisorStrategy.restartWithBackoff(1.second, 30.seconds, 0.2))

  private def onCommand(tile: Tile, command: Command): Effect[Event, Tile] =
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

      case AddNode(id, lat, lon, attributes, replyTo) =>
        Effect.persist(NodeAdded(id, lat, lon, attributes)).thenRun { _  =>
          replyTo.foreach(_ ! TileActor.Done() )
        }

      case AddWay(id, nodeIds, attributes, replyTo) =>
        Effect.persist(WayAdded(id, nodeIds, attributes)).thenRun { _ =>
          replyTo.foreach( _ ! TileActor.Done() )
        }

      case AddBatch(cmds, replyTo) =>
        Effect.persist(BatchAdded(cmds.map {
          case AddNode(id, lat, lon, attributes, _) => NodeAdded(id, lat, lon, attributes)
          case AddWay(id, nodeIds, attributes, _) => WayAdded(id, nodeIds, attributes)
        }(breakOut))).thenRun { _ =>
          replyTo.foreach( _ ! TileActor.Done() )
        }
    }

  private def applyEvent(tile: Tile, event: Event): Tile =
    event match {
      case atomicEvent: AtomicEvent => applyAtomicEvent(tile, atomicEvent)
      case BatchAdded(events) => events.foldLeft(tile)( (tile, event) => applyAtomicEvent(tile, event))
    }

  private def applyAtomicEvent(tile: Tile, event: AtomicEvent): Tile =
    event match {
      case NodeAdded(id, lat, lon, attributes) => tile.addNode(id, lat, lon, attributes)
      case WayAdded(id, nodeIds, attributes) => tile.addWay(id, nodeIds, attributes)
    }

}
