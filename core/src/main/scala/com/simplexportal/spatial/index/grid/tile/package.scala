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

package com.simplexportal.spatial.index.grid

import akka.actor.typed.ActorRef
import com.simplexportal.spatial.model.Way

package tile {

  sealed trait Message
  sealed trait Reply extends Message
  sealed trait Command extends Message
  sealed trait Query extends Command
  sealed trait Action extends Command
  protected[tile] sealed trait Event extends Message

  case class Metrics(ways: Long, nodes: Long) extends Reply

  case class Done() extends Reply

  case class NotDone() extends Reply

  case class GetInternalNodeResponse(
      id: Long,
      node: Option[TileIndex.InternalNode]
  ) extends Reply

  case class GetInternalNodesResponse(nodes: Seq[GetInternalNodeResponse])
      extends Reply

  case class GetInternalWayResponse(
      id: Long,
      way: Option[TileIndex.InternalWay]
  ) extends Reply

  case class GetWayResponse(
      id: Long,
      way: Option[Way]
  ) extends Reply

  final case class GetInternalNode(
      id: Long,
      replyTo: ActorRef[GetInternalNodeResponse]
  ) extends Query

  final case class GetInternalNodes(
      ids: Seq[Long],
      replyTo: ActorRef[GetInternalNodesResponse]
  ) extends Query

  final case class GetInternalWay(
      id: Long,
      replyTo: ActorRef[GetInternalWayResponse]
  ) extends Query

  final case class GetWay(
      id: Long,
      replyTo: ActorRef[GetWayResponse]
  ) extends Query

  final case class GetMetrics(replyTo: ActorRef[Metrics]) extends Query

  sealed trait BatchActions extends Action

  final case class AddNode(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String],
      replyTo: Option[ActorRef[Done]] = None
  ) extends BatchActions

  final case class AddWay(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String],
      replyTo: Option[ActorRef[Done]] = None
  ) extends BatchActions

  final case class AddBatch(
      cmds: Seq[BatchActions],
      replyTo: Option[ActorRef[Done]] = None
  ) extends Action

  protected[tile] sealed trait AtomicEvent extends Event

  protected[tile] final case class NodeAdded(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String]
  ) extends AtomicEvent

  protected[tile] final case class WayAdded(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String]
  ) extends AtomicEvent

  protected[tile] final case class BatchAdded(events: Seq[AtomicEvent])
      extends Event

}
