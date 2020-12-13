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

package com.simplexportal.spatial.index.grid.sessions.addbatch

import akka.actor.typed.{ActorTags, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.index.CommonInternalSerializer
import com.simplexportal.spatial.index.grid.GridProtocol._
import com.simplexportal.spatial.model
import com.simplexportal.spatial.index.grid.tile.actor.{TileIdx, TileIndexEntityIdGen}
import io.jvm.uuid.UUID

object AddBatchSession extends DataDistribution {

  protected sealed trait ForeignResponse extends CommonInternalSerializer
  protected case class DoneWrapper() extends ForeignResponse
  protected case class NotDoneWrapper(msg: String) extends ForeignResponse

  /**
    * Will generate a new actor to process the request.
    * This actor will be stopped after execute all commands.
    *
    * @param cmd
    * @param context
    * @param sharding
    * @param tileIndexEntityIdGen
    */
  def processRequest(
      cmd: GridAddBatch,
      context: ActorContext[GridRequest]
  )(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Unit =
    context.spawn(
      AddBatchSession(),
      s"adding_batch_${UUID.randomString}",
      ActorTags("session", "session-add-batch")
    ) ! cmd

  /**
    * Split ways actions per in tiles, group ways and nodes actions per tile and send the list of commands to the right set
    * of actors.
    *
    * @return
    */
  def apply()(
      implicit sharding: ClusterSharding,
      entityIdGen: TileIndexEntityIdGen
  ): Behavior[GridAddBatch] = Behaviors.receiveMessage { msg =>
    val commandsPerTile: Map[TileIdx, Seq[model.Entity]] = msg.commands
      .foldRight(Map.empty[TileIdx, Seq[model.Entity]]) {
        case (GridAddNode(node, _), mPerTile) =>
          val tileIdx = entityIdGen.tileIdx(node.location.lat, node.location.lon)
          mPerTile place
            ++(tileIdx -> mPerTile.getOrElse(tileIdx, Seq()) :+ node)
        case (GridAddWay(way, _), mPerTile) => ???
      }
//      .flatMap {
//        _ match {
//          case GridAddNode(node, _)     => Seq((entityIdGen.tileIdx(node.location.lat, node.location.lon) -> node))
//          case GridAddWay(way, replyTo) => splitWay(way)
//        }
//      }
//      .groupBy(_._1)

    Behaviors.same
  }

  def splitWay(way: model.Way): Seq[(TileIdx, model.Way)] = ???

}
