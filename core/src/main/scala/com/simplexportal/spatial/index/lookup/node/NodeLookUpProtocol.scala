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

package com.simplexportal.spatial.index.lookup.node

import akka.actor.typed.ActorRef
import com.simplexportal.spatial.index.CommonInternalSerializer
import com.simplexportal.spatial.index.grid.tile.actor.TileIdx

object NodeLookUpProtocol {
  sealed trait Message extends CommonInternalSerializer

  trait Response extends Message

  trait ACK extends Response

  case class Done() extends ACK

  case class NotDone(error: String) extends ACK

  case class GetResponse(id: Long, maybeNodeEntityId: Option[TileIdx]) extends Response

  case class GetsResponse(gets: Set[GetResponse]) extends Response

  trait Command extends Message

  case class Put(
      id: Long,
      nodeEntityId: TileIdx,
      replyTo: Option[ActorRef[ACK]]
  ) extends Command

  case class PutBatch(puts: Seq[Put], replyTo: Option[ActorRef[ACK]]) extends Command

  case class Get(id: Long, replyTo: ActorRef[GetResponse]) extends Command

  case class Gets(ids: Set[Long], replyTo: ActorRef[GetsResponse]) extends Command

  trait Event extends Message

  case class Putted(id: Long, nodeEntityId: TileIdx) extends Event

  case class PuttedBatch(puts: Seq[Putted]) extends Event
}
