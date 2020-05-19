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
import com.simplexportal.spatial.index.CommonInternalSerializer
import com.simplexportal.spatial.model

/**
  * Define the protocol used by the Grid.
  */
object GridProtocol {

  sealed trait GridMessage extends Message

  sealed trait GridRequest extends GridMessage

  sealed trait GridCommand extends GridRequest

  sealed trait GridBatchCommand extends GridCommand

  sealed trait GridQuery extends GridRequest

  sealed trait GridReply[T] extends Reply[T] with GridMessage

  // Services that response only with true or false
  sealed trait GridACK extends GridReply[Unit]

  trait Message extends CommonInternalSerializer

  // All Commands

  trait Reply[T] extends Message {
    def payload: Either[String, T]
  }

  final case class GridAddNode(
      node: model.Node,
      replyTo: Option[ActorRef[GridACK]] = None
  ) extends GridBatchCommand

  final case class GridAddWay(
      way: model.Way,
      replyTo: Option[ActorRef[GridACK]] = None
  ) extends GridBatchCommand

  final case class GridAddBatch(
      commands: Seq[GridBatchCommand],
      replyTo: Option[ActorRef[GridACK]] = None
  ) extends GridCommand

  // All Queries

// FIXME: It needs a lookup index.
  final case class GridGetNode(id: Long, replyTo: ActorRef[GridGetNodeReply]) extends GridQuery

  final case class GridGetNodeReply(payload: Either[String, Option[model.Node]]) extends GridReply[Option[model.Node]]

  final case class GridGetWay(id: Long, replyTo: ActorRef[GridGetWayReply]) extends GridQuery

  final case class GridGetWayReply(payload: Either[String, Option[model.Way]]) extends GridReply[Option[model.Way]]

  final case class GridNearestNode(
      location: model.Location,
      replyTo: ActorRef[GridNearestNodeReply]
  ) extends GridQuery

  final case class GridNearestNodeReply(payload: Either[String, Set[model.Node]]) extends GridReply[Set[model.Node]]

  // FIXME: This should be a "final case object GridDone extends GridACK {"
  final case class GridDone() extends GridACK {
    override def payload: Either[String, Unit] = Right(Unit)
  }

  final case class GridNotDone(hint: String) extends GridACK {
    override def payload: Either[String, Unit] = Left(hint)
  }

}
