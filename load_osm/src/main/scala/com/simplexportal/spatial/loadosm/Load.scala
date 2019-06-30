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

import com.acervera.osm4scala.EntityIterator.fromPbf
import com.acervera.osm4scala.model.{NodeEntity, WayEntity}
import com.typesafe.config.ConfigFactory

trait Load {

  def addNode(node: NodeEntity): Unit
  def addWay(way: WayEntity): Unit
  def printTotals(time: Long): Unit
  def printPartials(time: Long): Unit
  def clean(): Unit = {}


  val config = ConfigFactory.load()
  private val printMetricsElapseTime =
    config.getDuration("simplexportal.spatial.metrics.beat").toMillis
  private var lastPrint = System.currentTimeMillis()
  private var nextPrint = lastPrint + printMetricsElapseTime

  val startTime = System.currentTimeMillis()


  def triggerPartials(): Unit = {
    if (System.currentTimeMillis > nextPrint) {
      printPartials(System.currentTimeMillis - lastPrint)
      lastPrint = System.currentTimeMillis()
      nextPrint = lastPrint + printMetricsElapseTime
    }
  }

  def load(osmFile: File): Unit = {
    println(s"Loading data from [${osmFile.getAbsolutePath}]")

    val pbfIS: InputStream = new FileInputStream(osmFile)
    fromPbf(pbfIS).foreach {
      case node: NodeEntity =>
        addNode(node)
        triggerPartials
      case way: WayEntity =>
        addWay(way)
        triggerPartials
      case _ =>
    }
    printTotals(System.currentTimeMillis() - startTime)

    clean()
  }

}