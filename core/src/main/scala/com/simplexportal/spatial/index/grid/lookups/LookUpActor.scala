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

package com.simplexportal.spatial.index.grid.lookups

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

@deprecated(
  "It is more complex that expected. Use a specially implementation per case.",
  "Simplexspatial Core 0.0.1"
)
object LookUpActor {

  sealed trait LookUpMessage

  sealed trait Response extends LookUpMessage
  final case class Done() extends Response
  final case class GetResponse[K, V](key: K, maybeValue: Option[V])
      extends Response
  final case class GetsResponse[K, V](gets: Seq[GetResponse[K, V]])
      extends Response

  trait Command extends LookUpMessage
  final case class Put[K, V](key: K, value: V, replyTo: Option[ActorRef[Done]])
      extends Command
  final case class Get[K, V](key: K, replyTo: ActorRef[GetResponse[K, V]])
      extends Command
  final case class Gets[K, V](
      keys: Seq[K],
      replyTo: ActorRef[GetsResponse[K, V]]
  ) extends Command

  trait Event extends LookUpMessage
  final case class Putted[K, V](key: K, value: V) extends Event

  def apply[K, V](indexId: String, entityId: String): Behavior[Command] =
    Behaviors.setup { _ =>
      EventSourcedBehavior[Command, Event, Map[K, V]](
        persistenceId = PersistenceId(indexId, entityId),
        emptyState = Map.empty,
        commandHandler = (state, command) => onCommand(state, command),
        eventHandler = (state, event) => applyEvent(state, event)
      )
    }

  private def onCommand[K, V](
      table: Map[K, V],
      command: Command
  ): Effect[Event, Map[K, V]] = {
    command match {
      case cmd: Get[K @unchecked, V @unchecked] =>
        cmd.replyTo ! GetResponse(cmd.key, table.get(cmd.key))
        Effect.none
      case cmd: Gets[K @unchecked, V @unchecked] =>
        cmd.replyTo ! GetsResponse(
          cmd.keys.map(key => GetResponse(key, table.get(key)))
        )
        Effect.none
      case cmd: Put[K @unchecked, V @unchecked] =>
        Effect.persist(Putted[K, V](cmd.key, cmd.value)).thenRun { _ =>
          cmd.replyTo.foreach(_ ! Done())
        }
    }
  }

  private def applyEvent[K, V](table: Map[K, V], event: Event): Map[K, V] =
    event match {
      case put: Putted[K @unchecked, V @unchecked] =>
        table + (put.key -> put.value)
    }

}
