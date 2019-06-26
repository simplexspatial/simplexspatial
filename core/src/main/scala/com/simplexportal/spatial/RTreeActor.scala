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

import java.util.Date

import akka.actor.{ActorLogging, Props}
import akka.persistence._
import com.simplexportal.spatial.RTreeActor._
import com.simplexportal.spatial.model._
import com.typesafe.config.{Config, ConfigFactory}

object RTreeActor {

  def props(networkId: String, boundingBox: BoundingBox): Props =
    Props(new RTreeActor(networkId, boundingBox))

  sealed trait RTreeCommands

  case class AddNode(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String]
  ) extends RTreeCommands

  case class AddWay(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String]
  ) extends RTreeCommands

  case class GetNode(id: Long) extends RTreeCommands

  case class GetWay(id: Long) extends RTreeCommands

  object GetMetrics extends RTreeCommands

  sealed trait RTreeDataTransfer

  case class Metrics(ways: Long, nodes: Long) extends RTreeDataTransfer

  // Persisted messages.
  sealed trait RTreeEvents

  case class NodeAdded(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String]
  ) extends RTreeEvents

  case class WayAdded(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String]
  ) extends RTreeEvents

}

class RTreeActor(networkId: String, boundingBox: BoundingBox)
    extends PersistentActor
    with ActorLogging {

  override def persistenceId: String =
    s"rtree-${networkId}-[(${boundingBox.min.lon},${boundingBox.min.lat}),(${boundingBox.max.lon},${boundingBox.max.lat})]"

  var tile: Tile = Tile()

  override def receiveCommand: Receive = {

    case GetNode(id) =>
      sender ! tile.nodes.get(id)

    case GetWay(id) =>
      sender ! tile.ways.get(id)

    case GetMetrics =>
      sender ! Metrics(tile.ways.size, tile.nodes.size)

    case AddNode(id, lat, lon, attributes) =>
      persist(NodeAdded(id, lat, lon, attributes)) { node =>
        addNode(node)
        sender ! akka.Done
      }

    case AddWay(id, nodeIds, attributes) =>
      persist(WayAdded(id, nodeIds, attributes)) { way =>
        addWay(way)
        sender ! akka.Done
      }

  }

  override def receiveRecover: Receive = {
    case event: NodeAdded => addNode(event)
    case event: WayAdded  => addWay(event)
  }

  private def addNode(node: NodeAdded) =
    tile = tile.addNode(node.id, node.lat, node.lon, node.attributes)

  private def addWay(way: WayAdded) =
    tile = tile.addWay(way.id, way.nodeIds, way.attributes)

}
