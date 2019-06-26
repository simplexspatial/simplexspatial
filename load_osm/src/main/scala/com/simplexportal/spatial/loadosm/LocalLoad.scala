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

package com.simplexportal.spatial.loadosm

import java.util.Date

import com.acervera.osm4scala.model.{NodeEntity, WayEntity}
import com.simplexportal.spatial.TileActor.Metrics
import com.simplexportal.spatial.Tile

object LocalLoad extends Load {

  var tile = Tile()
  var lastMetric = Metrics(0, 0)

  override def addNode(node: NodeEntity): Unit =
    tile = tile.addNode(node.id, node.latitude, node.longitude, node.tags)

  override def addWay(way: WayEntity): Unit =
    tile = tile.addWay(way.id, way.nodes, way.tags)

  override def printPartials(time: Long): Unit = {
    val newMetric = Metrics(tile.ways.size, tile.nodes.size)
    val deltaMetrics = (newMetric.nodes + newMetric.ways) - (lastMetric.nodes + lastMetric.ways)
    println(
      s"Metrics at [${new Date()}] => nodes = ${tile.nodes.size}, ways = ${tile.ways.size} so ${deltaMetrics / (time / 1000)} entities per second"
    )
    lastMetric = newMetric
  }

  override def printTotals(time: Long): Unit =
    println(
      f"${tile.nodes.size} nodes and ${tile.ways.size} ways loaded in ${(time / 1000)}%,2.2f seconds"
    )

}
