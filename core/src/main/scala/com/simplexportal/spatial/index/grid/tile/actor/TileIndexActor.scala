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

package com.simplexportal.spatial.index.grid.tile.actor

import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.RetentionCriteria
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.simplexportal.spatial.index.grid.tile.impl.TileIndex
import scala.concurrent.duration._

object TileIndexActor extends TileIndexQueryHandler with TileIndexActionHandler {

  def apply(indexId: String, tileId: String): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("Starting grid tile [{}]", tileId)

      EventSourcedBehavior[Command, Event, TileIndex](
        persistenceId = PersistenceId(s"Tile_${indexId}", tileId),
        emptyState = TileIndex(),
        commandHandler = (state, command) => onCommand(tileId, state, command),
        eventHandler = (state, event) => applyEvent(state, event)
      ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 1000, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 3.seconds, 0.1))
    }

  def onCommand(
      tileId: String,
      tile: TileIndex,
      command: Command
  ): Effect[Event, TileIndex] = command match {
    case q: Query  => applyQueries(tileId, tile, q)
    case a: Action => applyAction(tileId, tile, a)
  }

}
