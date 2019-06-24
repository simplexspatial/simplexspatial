/*
 * Copyright 2019 SimplexPortal Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simplexportal.spatial

import com.simplexportal.spatial.Tile.{Node, Way}
import com.simplexportal.spatial.model._

import scala.annotation.tailrec

object Tile {

  case class Node(
      id: Long,
      location: Location,
      attributes: Attributes = Map.empty, // TODO: Use a dictionary
      ways: Set[Long] = Set.empty,
      outs: Set[Long] = Set.empty, // TODO: Should be replaced by a set of Node object references??
      ins: Set[Long] = Set.empty // TODO: Should be replaced by a set of Node object references??
  )

  case class Way(
      id: Long,
      startNode: Long,
      attributes: Map[String, String] = Map.empty
  )

}

case class Tile(
    nodes: Map[Long, Node] = Map.empty,
    ways: Map[Long, Way] = Map.empty
) {

  def addNode(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Attributes
  ): Tile =
    copy(nodes = nodes + (id -> Node(id, Location(lat, lon), attributes)))

  private def buildNewNode(
      wayId: Long,
      prev: Option[Long],
      current: Long,
      next: Option[Long]
  ) = {
    val node = nodes
      .getOrElse(
        current,
        throw new NotImplementedError(
          "Node not found in the Tile is still not implemented."
        )
      )

    node.copy( // TODO: Calculate directions. Now, all bidirectional.
      ways = node.ways + wayId,
      outs = (node.outs ++ next) ++ prev,
      ins = (node.ins ++ next) ++ prev
    )
  }

  // Manage generated list in private scope as List because performance is not bad!
  @tailrec
  private def updateConnections(
      wayId: Long,
      prev: Option[Long],
      current: Long,
      nodeIds: Seq[Long],
      updated: List[(Long, Node)]
  ): List[(Long, Node)] =
    nodeIds match {
      case Seq() =>
        (current, buildNewNode(wayId, prev, current, None)) :: updated
      case Seq(next, tail @ _*) => {
        updateConnections(
          wayId,
          Some(current),
          next,
          tail,
          (current, buildNewNode(wayId, prev, current, Some(next))) :: updated
        )
      }
    }

  def addWay(
      wayId: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String]
  ): Tile =
    copy(
      ways = ways + (wayId -> Way(wayId, nodeIds.head, attributes)),
      nodes = nodes ++ updateConnections(
        wayId,
        None,
        nodeIds.head,
        nodeIds.tail,
        List.empty
      )
    )

}
