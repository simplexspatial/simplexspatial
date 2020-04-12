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

package com.simplexportal.spatial.index.lookup.way

import akka.actor.typed.ActorRef
import com.simplexportal.spatial.index.CommonInternalSerializer
import com.simplexportal.spatial.index.grid.tile.actor.TileIdx

object WayLookUpProtocol {
  sealed trait Message extends CommonInternalSerializer

  trait Response extends Message

  trait ACK extends Response

  case class Done() extends ACK

  case class NotDone(error: String) extends ACK

  case class GetResponse(id: Long, maybeWayEntityIds: Option[Set[TileIdx]]) extends Response

  case class GetsResponse(gets: Seq[GetResponse]) extends Response

  trait Command extends Message

  case class Put(
      id: Long,
      wayEntityId: TileIdx,
      replyTo: Option[ActorRef[ACK]]
  ) extends Command

  case class PutBatch(puts: Seq[Put], replyTo: Option[ActorRef[ACK]]) extends Command

  case class Get(id: Long, replyTo: ActorRef[GetResponse]) extends Command

  case class Gets(ids: Seq[Long], replyTo: ActorRef[GetsResponse]) extends Command

  trait Event extends Message

  case class Putted(id: Long, wayEntityId: TileIdx) extends Event

  case class PuttedBatch(puts: Seq[Putted]) extends Event
}
