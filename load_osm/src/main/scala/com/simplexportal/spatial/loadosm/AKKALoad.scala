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

import akka.actor.{ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import com.acervera.osm4scala.model.{NodeEntity, WayEntity}
import com.simplexportal.spatial.RTreeActor
import com.simplexportal.spatial.RTreeActor._
import com.simplexportal.spatial.model.{BoundingBox, Location}

import scala.concurrent.Await
import scala.concurrent.duration._

object AKKALoad extends Load {

  val system = ActorSystem("osm-actor-system")
  val rTreeActor = system.actorOf(
    RTreeActor.props(
      "load_and_shutdown_osm",
      BoundingBox(
        Location(Double.MinValue, Double.MinValue),
        Location(Double.MaxValue, Double.MaxValue)
      )
    )
  )

  var nodes = 0
  var ways = 0

  override def addNode(node: NodeEntity): Unit = {
    rTreeActor ! AddNode(
      node.id,
      node.latitude,
      node.longitude,
      node.tags
    )
    nodes = nodes + 1
  }

  override def addWay(way: WayEntity): Unit = {
    rTreeActor ! AddWay(way.id, way.nodes, way.tags)
    ways = ways + 1
  }

  override def printTotals(time: Long): Unit = {
    implicit val timeout = Timeout(120 minutes)
    val metrics = Await
      .result(rTreeActor ? GetMetrics, timeout.duration)
      .asInstanceOf[Metrics]

    println(
      f"${metrics.nodes} nodes and ${metrics.ways} ways loaded in ${((System.currentTimeMillis() - startTime) / 1000)}%,2.2f seconds"
    )
  }

  override def printPartials(time: Long): Unit = {
    println(
      s"Metrics at [${new Date()}] => Sent nodes = ${nodes}, ways = ${ways}."
    )
  }

  override def clean(): Unit = {
    println("Killing actor system using Poison Pill.")
    system.terminate
  }
}
