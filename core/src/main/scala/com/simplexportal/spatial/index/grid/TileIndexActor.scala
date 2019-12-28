/*
 * Copyright 2019 SimplexPortal Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.simplexportal.spatial.index.grid

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.collection.breakOut

object TileIndexActor {

  sealed trait Message

  // From here all possible replies to sent.
  sealed trait Reply extends Message
  case class Metrics(ways: Long, nodes: Long) extends Reply
  case class GetNodeResponse(id: Long, node: Option[TileIndex.InternalNode])
      extends Reply
  case class GetNodesResponse(nodes: Seq[GetNodeResponse]) extends Reply
  case class GetWayResponse(id: Long, way: Option[TileIndex.InternalWay]) extends Reply
  case class Done() extends Reply // TODO: Should be able to response with Done or NotDone, or ACK and NACK

  // From here all possible commands to accept.
  sealed trait Command extends Message
  sealed trait BatchCommand extends Command

  final case class AddNode(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String],
      replyTo: Option[ActorRef[TileIndexActor.Done]] = None
  ) extends BatchCommand

  final case class AddWay(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String],
      replyTo: Option[ActorRef[TileIndexActor.Done]] = None
  ) extends BatchCommand

  final case class AddBatch(
      cmds: Seq[BatchCommand],
      replyTo: Option[ActorRef[TileIndexActor.Done]] = None
  ) extends Command

  final case class GetNode(id: Long, replyTo: ActorRef[GetNodeResponse])
      extends Command

  final case class GetNodes(ids: Seq[Long], replyTo: ActorRef[GetNodesResponse])
      extends Command

  final case class GetWay(id: Long, replyTo: ActorRef[GetWayResponse])
      extends Command

  final case class GetMetrics(replyTo: ActorRef[Metrics]) extends Command

  // From here, all possible events generated.
  sealed trait Event extends Message
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

  def apply(indexId: String, tileId: String): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("Starting grid tile [{}]", tileId)

      EventSourcedBehavior[Command, Event, TileIndex](
        persistenceId = PersistenceId(s"Tile_${indexId}", tileId),
        emptyState = TileIndex(),
        commandHandler = (state, command) => onCommand(state, command),
        eventHandler = (state, event) => applyEvent(state, event)
      )
    }

  private def onCommand(
      tile: TileIndex,
      command: Command
  ): Effect[Event, TileIndex] =
    command match {
      case GetMetrics(replyTo) =>
        replyTo ! Metrics(tile.ways.size, tile.nodes.size)
        Effect.none

      case GetNode(id, replyTo) =>
        replyTo ! GetNodeResponse(id, tile.nodes.get(id))
        Effect.none

      case GetNodes(ids, replyTo) =>
        replyTo ! GetNodesResponse(
          ids.map(id => GetNodeResponse(id, tile.nodes.get(id)))
        )
        Effect.none

      case GetWay(id, replyTo) =>
        replyTo ! GetWayResponse(id, tile.ways.get(id))
        Effect.none

      case AddNode(id, lat, lon, attributes, replyTo) =>
        Effect.persist(NodeAdded(id, lat, lon, attributes)).thenRun { _ =>
          replyTo.foreach(_ ! TileIndexActor.Done())
        }

      case AddWay(id, nodeIds, attributes, replyTo) =>
        Effect.persist(WayAdded(id, nodeIds, attributes)).thenRun { _ =>
          replyTo.foreach(_ ! TileIndexActor.Done())
        }

      case AddBatch(cmds, replyTo) =>
        Effect
          .persist(BatchAdded(cmds.map {
            case AddNode(id, lat, lon, attributes, _) =>
              NodeAdded(id, lat, lon, attributes)
            case AddWay(id, nodeIds, attributes, _) =>
              WayAdded(id, nodeIds, attributes)
          }(breakOut)))
          .thenRun { _ =>
            replyTo.foreach(_ ! TileIndexActor.Done())
          }
    }

  private def applyEvent(tile: TileIndex, event: Event): TileIndex =
    event match {
      case atomicEvent: AtomicEvent => applyAtomicEvent(tile, atomicEvent)
      case BatchAdded(events) =>
        events.foldLeft(tile)((tile, event) => applyAtomicEvent(tile, event))
    }

  private def applyAtomicEvent(tile: TileIndex, event: AtomicEvent): TileIndex =
    event match {
      case NodeAdded(id, lat, lon, attributes) =>
        tile.addNode(id, lat, lon, attributes)
      case WayAdded(id, nodeIds, attributes) =>
        tile.addWay(id, nodeIds, attributes)
    }

}
