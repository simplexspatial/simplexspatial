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

package com.simplexportal.spatial.index.grid.tile

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.collection.breakOut

object TileIndexActor {

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
  ): Effect[Event, TileIndex] = command match {
    case q: Query  => applyQueries(tile, q)
    case a: Action => applyAction(tile, a)
  }

  private def applyQueries(
      tile: TileIndex,
      query: Query
  ): Effect[Event, TileIndex] = query match {

    case GetMetrics(replyTo) =>
      replyTo ! Metrics(tile.ways.size, tile.nodes.size)
      Effect.none

    case GetInternalNode(id, replyTo) =>
      replyTo ! GetInternalNodeResponse(id, tile.nodes.get(id))
      Effect.none

    case GetInternalNodes(ids, replyTo) =>
      replyTo ! GetInternalNodesResponse(
        ids.map(id => GetInternalNodeResponse(id, tile.nodes.get(id)))
      )
      Effect.none

    case GetInternalWay(id, replyTo) =>
      replyTo ! GetInternalWayResponse(id, tile.ways.get(id))
      Effect.none

    case GetWay(id, replyTo) =>
      replyTo ! GetWayResponse(id, tile.getWay(id))
      Effect.none
  }

  private def applyAction(
      tile: TileIndex,
      action: Action
  ): Effect[Event, TileIndex] = action match {
    case AddNode(id, lat, lon, attributes, replyTo) =>
      Effect.persist(NodeAdded(id, lat, lon, attributes)).thenRun { _ =>
        replyTo.foreach(_ ! Done())
      }

    case AddWay(id, nodeIds, attributes, replyTo) =>
      Effect.persist(WayAdded(id, nodeIds, attributes)).thenRun { _ =>
        replyTo.foreach(_ ! Done())
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
          replyTo.foreach(_ ! Done())
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
