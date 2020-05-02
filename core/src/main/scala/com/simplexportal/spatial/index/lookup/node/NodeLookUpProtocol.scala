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

  sealed trait Response extends Message

  sealed trait ACK extends Response

  final case class Done() extends ACK

  final case class NotDone(error: String) extends ACK

  final case class GetResponse(id: Long, maybeNodeEntityId: Option[TileIdx]) extends Response

  final case class GetsResponse(gets: Set[GetResponse]) extends Response

  sealed trait Command extends Message

  final case class Put(
      id: Long,
      nodeEntityId: TileIdx,
      replyTo: Option[ActorRef[ACK]]
  ) extends Command

  final case class PutBatch(puts: Seq[Put], replyTo: Option[ActorRef[ACK]]) extends Command

  final case class Get(id: Long, replyTo: ActorRef[GetResponse]) extends Command

  final case class Gets(ids: Set[Long], replyTo: ActorRef[GetsResponse]) extends Command

  sealed trait Event extends Message

  final case class Putted(id: Long, nodeEntityId: TileIdx) extends Event

  final case class PuttedBatch(puts: Seq[Putted]) extends Event
}
