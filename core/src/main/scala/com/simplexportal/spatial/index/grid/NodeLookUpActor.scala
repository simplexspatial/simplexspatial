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

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

// TODO: Generalize LookUp
object NodeLookUpActor {

  sealed trait Message

  // Using ints for size convenient.
  case class NodeEntityId(latIdx: Int, lonIdx: Int) extends Message

  trait Response extends Message
  case class Done() extends Response
  case class GetResponse(maybeNodeEntityId: Option[NodeEntityId]) extends Response

  trait Command extends Message
  case class Put(id: Long, nodeEntityId: NodeEntityId, replyTo: Option[ActorRef[Done]]) extends Command
  case class Get(id: Long, replyTo: ActorRef[GetResponse]) extends Command

  trait Event extends Message
  case class Putted(id: Long, nodeEntityId: NodeEntityId) extends Event

  def apply(indexId: String, partitionId: String): Behavior[Command] =
    Behaviors.setup { context =>
      EventSourcedBehavior[Command, Event, Map[Long, NodeEntityId]](
        persistenceId = PersistenceId(s"NodeLookUp_${indexId}", partitionId),
        emptyState = Map.empty,
        commandHandler = (state, command) => onCommand(state, command),
        eventHandler = (state, event) => applyEvent(state, event)
      )
    }

  private def onCommand(table: Map[Long, NodeEntityId], command: Command): Effect[Event, Map[Long, NodeEntityId]] = {
    command match {
      case Get(id, replyTo) =>
        println(s">>>>>>>>>>>>>>>>>>>>>>>>>> Processing ${command} and found ${table.get(id)}")
        replyTo ! GetResponse(table.get(id))
        Effect.none
      case Put(id, nodeEntityId, replyTo) =>
        println(s">>>>>>>>>>>>>>>>>>>>>>>>>> Processing ${command}")
        Effect.persist(Putted(id, nodeEntityId)).thenRun { _  =>
          replyTo.foreach(_ ! Done() )
        }
    }
  }

  private def applyEvent(table: Map[Long, NodeEntityId], event: Event): Map[Long, NodeEntityId] =
    event match {
      case put: Putted =>
        println(s">>>>>>>>>>>>>>>>>>>>>>>>>> Applying ${event}")
        table + (put.id -> put.nodeEntityId)
    }

}
