/*
 * Copyright 2020 SimplexPortal Ltd
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

import akka.persistence.typed.scaladsl.Effect
import com.simplexportal.spatial.index.grid.tile.impl.TileIndex
import com.simplexportal.spatial.index.grid.tile.actor.TileIndexProtocol._

trait TileIndexQueryHandler {
  def applyQueries(
      tile: TileIndex,
      query: Query
  ): Effect[Event, TileIndex] = query match {

    case GetMetrics(replyTo) =>
      replyTo ! Metrics(tile.ways.size, tile.nodes.size)
      Effect.none

    case GetNode(id, replyTo) =>
      replyTo ! GetNodeResponse(id, tile.getNode(id))
      Effect.none

    case GetWay(id, replyTo) =>
      replyTo ! GetWayResponse(id, tile.getWay(id))
      Effect.none

    case GetNearestNode(origin, replyTo) =>
      replyTo ! GetNearestNodeResponse(
        origin,
        tile.nearestNode(origin)
      )
      Effect.none
  }
}
