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

import akka.actor.{ActorSystem, PoisonPill}

import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import com.acervera.osm4scala.EntityIterator._
import com.acervera.osm4scala.model.{NodeEntity, WayEntity}
import com.simplexportal.spatial.RTreeActor
import com.simplexportal.spatial.RTreeActor.{AddNode, AddWay, GetMetrics, Metrics}
import com.simplexportal.spatial.model.{BoundingBox, Location}
import com.simplexportal.spatial.utils.Benchmarking

import scala.concurrent.Await

object AKKALoad extends Benchmarking {
  def load(osmFile: File): Unit = {
    println(s"Loading data from [${osmFile.getAbsolutePath}]")

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

    val result = time {
      val pbfIS: InputStream = new FileInputStream(osmFile)


      fromPbf(pbfIS).foreach {
        case node: NodeEntity =>
          rTreeActor ! AddNode(node.id, node.latitude, node.longitude, node.tags)
        case way: WayEntity =>
          rTreeActor ! AddWay(way.id, way.nodes, way.tags)
        case other =>
          println(s"Ignoring ${other.osmModel} ")
      }

      implicit val timeout = Timeout(120 minutes)
      val metrics = rTreeActor ? GetMetrics

      Await.result(metrics, timeout.duration).asInstanceOf[Metrics]
    }

    println(f"Actor loaded in ${(result._1 * 1e-9) / 60}%,2.2f  with metrics ${result._2}")

    rTreeActor ! PoisonPill
  }
}
