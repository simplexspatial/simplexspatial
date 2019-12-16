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

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.model.Location
import io.jvm.uuid.UUID

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object AddWaySession {
  def apply(
      sharding: ClusterSharding,
      addWay: TileIndexActor.AddWay,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Behavior[NotUsed] =
    Behaviors
      .setup[AnyRef] { context =>
        // Translate nodes ids into nodes.
        context.spawn(
          GetNodesSession(
            sharding,
            TileIndexActor.GetNodes(addWay.nodeIds, context.self),
            tileIndexEntityIdGen
          ),
          s"getting_node_${UUID.randomString}"
        )

        Behaviors.receiveMessage {
          case TileIndexActor.GetNodesResponse(nodes) =>
            validateNodes(nodes) match {
              case Success(nodes) =>
                splitInShards(nodes, tileIndexEntityIdGen)
                ???
              case Failure(exception) =>
                ??? // FIXME Must return NACK or NotDone
            }
          case _ =>
            Behaviors.unhandled
        }
      }
      .narrow[NotUsed]

  /**
    * Validate all nodes in the response, and return a validated sequence of nodes.
    *
    * @param responses The response with possible don't know nodes.
    * @return Return the right sequence of nodes or error.
    */
  def validateNodes(
      responses: Seq[TileIndexActor.GetNodeResponse]
  ): Try[Seq[TileIndex.Node]] = Try {
    responses.map { resp =>
      resp.node.getOrElse(throw new Exception(s"Node [${resp.id}] not found."))
    }
  }

  /**
   * From a list of nodes, create a list of shards, where every element contains the shard Id and the list of nodes in
   * there.
   * Also, it add at the end and at the begining the "connector nodes", that are the connection with the next/previous
   * node in the other shard.
   *
   * Let's suppose the one way is not going to have the same node as connector in the same shard.
   *
   * @param nodes
   * @param entityIdGen
   * @return
   */
  def splitInShards(
      nodes: Seq[TileIndex.Node],
      entityIdGen: TileIndexEntityIdGen
  ): Seq[(String, Seq[TileIndex.Node])] = {

    def entityIdFrom =
      (loc: Location) => entityIdGen.info(loc.lat, loc.lon).entityId

    @tailrec
    def rec(
        nodes: Seq[TileIndex.Node],
        acc: Seq[(String, Seq[TileIndex.Node])],
        currentShard: (String, Seq[TileIndex.Node])
    ): Seq[(String, Seq[TileIndex.Node])] = {
      nodes match {
        case Nil => acc :+ currentShard
        case node :: tail =>
          val entityId = entityIdFrom(node.location)
          if (entityId == currentShard._1) {
            rec(tail, acc, (currentShard._1, currentShard._2 :+ node))
          } else {
            rec(tail, acc :+ currentShard, (entityId, Seq(node)))
          }
      }
    }

    rec(
      nodes.tail,
      Seq.empty,
      (entityIdFrom(nodes.head.location), Seq(nodes.head))
    )
  }

}
