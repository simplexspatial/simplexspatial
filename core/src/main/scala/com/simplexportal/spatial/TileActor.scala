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

import akka.actor.{ActorLogging, Props}
import akka.persistence._
import com.simplexportal.spatial.TileActor._
import com.simplexportal.spatial.api.data.Done
import com.simplexportal.spatial.model._

object TileActor {

  def props(networkId: String, boundingBox: BoundingBox): Props =
    Props(new TileActor(networkId, boundingBox))

  // Commands
  sealed trait TileCommands

  case class AddNode(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String]
  ) extends TileCommands

  case class AddWay(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String]
  ) extends TileCommands

  case class AddBatch(cmds: Seq[TileCommands]) extends TileCommands

  case class GetNode(id: Long) extends TileCommands

  case class GetWay(id: Long) extends TileCommands

  object GetMetrics extends TileCommands

  sealed trait RTreeDataTransfer

  case class Metrics(ways: Long, nodes: Long) extends RTreeDataTransfer

  // Events.
  sealed trait TileEvents

  case class NodeAdded(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String]
  ) extends TileEvents

  case class WayAdded(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String]
  ) extends TileEvents

  // Other messages
  case object Ack
  case object StreamInitialized
  case object StreamCompleted
  final case class StreamFailure(ex: Throwable)

}

class TileActor(networkId: String, boundingBox: BoundingBox)
    extends PersistentActor
    with ActorLogging {

  override def persistenceId: String =
    s"rtree-${networkId}-[(${boundingBox.min.lon},${boundingBox.min.lat}),(${boundingBox.max.lon},${boundingBox.max.lat})]"

  var tile: Tile = Tile()

  override def receiveCommand: Receive = {

//    case StreamInitialized =>
//      log.info("Stream initialized!")
//      sender ! Ack // ack to allow the stream to proceed sending more elements
//
//    case StreamCompleted =>
//      log.info("Stream completed!")
//
//    case StreamFailure(ex) =>
//      log.error(ex, "Stream failed!")

    case GetNode(id) =>
      sender ! tile.nodes.get(id)

    case GetWay(id) =>
      sender ! tile.ways.get(id)

    case GetMetrics =>
      sender ! Metrics(tile.ways.size, tile.nodes.size)

    case cmd: AddNode =>
      addNodeHandler(cmd)
      sender ! Done

    case cmd: AddWay =>
      addWayHandler(cmd)
      sender ! Done

    case AddBatch(cmds) =>
      addBatchHandler(cmds.flatMap{
        case cmd: AddNode => Some(NodeAdded(cmd.id, cmd.lat, cmd.lon, cmd.attributes))
        case cmd: AddWay => Some(WayAdded(cmd.id, cmd.nodeIds, cmd.attributes))
        case _ => None
      })
      sender ! Done

  }

  private def addNodeHandler(cmd: AddNode) =
    persist(NodeAdded(cmd.id, cmd.lat, cmd.lon, cmd.attributes)) { node =>
      addNode(node)
    }

  private def addWayHandler(cmd: AddWay) =
    persist(WayAdded(cmd.id, cmd.nodeIds, cmd.attributes)) { way =>
      addWay(way)
    }

  private def addBatchHandler(events: Seq[TileEvents]) =
    persist(events) (events => events.foreach{
      case node: NodeAdded => addNode(node)
      case way: WayAdded => addWay(way)
    })


  override def receiveRecover: Receive = {
    case event: NodeAdded => addNode(event)
    case event: WayAdded  => addWay(event)
  }

  private def addNode(node: NodeAdded) =
    tile = tile.addNode(node.id, node.lat, node.lon, node.attributes)

  private def addWay(way: WayAdded) =
    tile = tile.addWay(way.id, way.nodeIds, way.attributes)


}
