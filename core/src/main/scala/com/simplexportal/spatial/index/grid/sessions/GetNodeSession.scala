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

package com.simplexportal.spatial.index.grid.sessions

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.index.grid.CommonInternalSerializer
import com.simplexportal.spatial.index.grid.Grid.{NodeLookUpTypeKey, TileTypeKey}
import com.simplexportal.spatial.index.grid.GridProtocol.{GridGetNode, GridGetNodeReply, GridRequest}
import com.simplexportal.spatial.index.grid.lookups.NodeLookUpActor.{GetResponse => LookUpReply}
import com.simplexportal.spatial.index.grid.lookups.{LookUpNodeEntityIdGen, NodeLookUpActor}
import com.simplexportal.spatial.index.grid.tile.actor.{GetNode, GetNodeResponse => TileReply}
import io.jvm.uuid.UUID

/**
  * Get node in two steps:
  * 1. From the look-up index, retrieve the location of the node.
  * 2. Send the command to the right shard.
  */
object GetNodeSession {

  protected sealed trait ForeignResponse extends CommonInternalSerializer

  protected case class LookUpReplyWrapper(response: LookUpReply) extends ForeignResponse

  protected case class TileReplyWrapper(response: TileReply) extends ForeignResponse

  private def adapters(
      context: ActorContext[ForeignResponse]
  ): ActorRef[AnyRef] =
    context.messageAdapter {
      case msg: LookUpReply => LookUpReplyWrapper(msg)
      case msg: TileReply   => TileReplyWrapper(msg)
    }

  def processRequest(
      cmd: GridGetNode,
      context: ActorContext[GridRequest]
  )(
      implicit sharding: ClusterSharding
  ): Unit =
    context.spawn(
      apply(cmd),
      s"getting_node_${UUID.randomString}"
    )

  def apply(getNode: GridGetNode)(
      implicit sharding: ClusterSharding
  ): Behavior[ForeignResponse] =
    Behaviors
      .setup { context =>
        val adapter = adapters(context)

        // Search the location of the node.
        sharding.entityRefFor(
          NodeLookUpTypeKey,
          LookUpNodeEntityIdGen.entityId(getNode.id)
        ) ! NodeLookUpActor.Get(getNode.id, adapter)

        Behaviors.receiveMessage {
          case LookUpReplyWrapper(LookUpReply(nodeId, Some(tileIdx))) =>
            sharding.entityRefFor(
              TileTypeKey,
              tileIdx.entityId
            ) ! GetNode(nodeId, adapter)
            Behaviors.same
          case LookUpReplyWrapper(LookUpReply(_, None)) =>
            getNode.replyTo ! GridGetNodeReply(Right(None))
            Behaviors.stopped
          case TileReplyWrapper(TileReply(_, node)) =>
            getNode.replyTo ! GridGetNodeReply(Right(node))
            Behaviors.stopped
          case unexpected =>
            context.log.warn(s"Found unexpected message ${unexpected}")
            Behaviors.unhandled
        }
      }

}
