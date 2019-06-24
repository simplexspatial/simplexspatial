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

import java.io.{File, FileInputStream, InputStream}
import java.util.Date

import com.acervera.osm4scala.EntityIterator.fromPbf
import com.acervera.osm4scala.model.{NodeEntity, WayEntity}
import com.simplexportal.spatial.RTreeActor.Metrics
import com.simplexportal.spatial.Tile
import com.simplexportal.spatial.loadosm.AKKALoad.time
import com.typesafe.config.ConfigFactory

object LocalLoad {

  var tile = Tile()

  val config = ConfigFactory.load()
  var lastPrint = System.currentTimeMillis()
  var lastMetric = Metrics(0, 0)
  val printMetricsElapseTime = config.getDuration("simplexportal.spatial.metrics.print.elapse").toMillis

  private def printMetrics() = {
    val newTimestamp = System.currentTimeMillis()
    if (newTimestamp - lastPrint > printMetricsElapseTime) {
      val newMetric = Metrics(tile.ways.size, tile.nodes.size)
      val deltaMetrics = (newMetric.nodes + newMetric.ways) - (lastMetric.nodes + lastMetric.ways)
      println(
        s"Metrics at [${new Date()}] => ways = ${tile.ways.size}, nodes = ${tile.nodes.size} so ${deltaMetrics / ((newTimestamp - lastPrint) / 1000)} entities per second"
      )
      lastMetric = newMetric
      lastPrint = System.currentTimeMillis()
    }
  }

  def load(osmFile: File): Unit = {
    println(s"Loading data from [${osmFile.getAbsolutePath}]")

    val result = time {
      val pbfIS: InputStream = new FileInputStream(osmFile)

      fromPbf(pbfIS).foreach {
        case node: NodeEntity =>
          printMetrics()
          tile = tile.addNode(node.id, node.latitude, node.longitude, node.tags)
        case way: WayEntity =>
          printMetrics()
          tile = tile.addWay(way.id, way.nodes, way.tags)
        case other =>
          println(s"Ignoring ${other.osmModel} ")
      }
      Metrics(tile.ways.size, tile.nodes.size)
    }

    println(
      f"${result._2.nodes} nodes and ${result._2.ways} ways loaded in ${(result._1 * 1e-9)}%,2.2f seconds"
    )

  }
}
