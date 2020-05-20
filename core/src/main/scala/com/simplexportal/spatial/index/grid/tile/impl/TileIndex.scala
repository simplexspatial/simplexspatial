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

package com.simplexportal.spatial.index.grid.tile.impl

import com.simplexportal.spatial.index.grid.tile.impl.{TileIndex => internal}
import com.simplexportal.spatial.model

import scala.annotation.tailrec

// FIXME: Search a better Set structure to short list of Long numbers (for InternalNode and InternalWay node lists).
// FIXME: Search a better Map structure to store huge amount of data (for nodes and ways state)
object TileIndex {

  sealed trait InternalModel

  final case class Node(
      id: Long,
      location: model.Location,
      attributes: Map[Int, String] = Map.empty,
      ways: Set[Long] = Set.empty,
      outs: Set[Long] = Set.empty,
      ins: Set[Long] = Set.empty
  ) extends InternalModel {

    /**
      * Create a new Node:
      * - Taking id, location and attributes from the parameter b
      * - joining ways, outs and ins set
      * @param b Object to merge.
      * @return new Object with merged values.
      */
    def merge(b: Node): Node = b.copy(
      ways = ways ++ b.ways,
      outs = outs ++ b.outs,
      ins = ins ++ b.ins
    )
  }

  final case class Way(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[Int, String] = Map.empty
  ) extends InternalModel

}

case class TileIndex(
    nodes: Map[Long, internal.Node] = Map.empty,
    ways: Map[Long, internal.Way] = Map.empty,
    tagsDic: Map[Int, String] = Map.empty
) extends NearestNodeSearch
    with AttribsDictionary {

  private def searchAndMerge(iNode: internal.Node, nodes: Map[Long, internal.Node]): Map[Long, internal.Node] =
    nodes + (iNode.id -> {
      nodes.get(iNode.id) match {
        case None        => iNode
        case Some(older) => older.merge(iNode)
      }
    })

  def addNode(node: model.Node): TileIndex = {
    val (dic, attrs) = attributesToDictionary(node.attributes)

    // If exist, merge with the new one.
    copy(
      nodes = searchAndMerge(internal.Node(node.id, node.location, attrs), nodes),
      tagsDic = tagsDic ++ dic
    )
  }

  @tailrec
  private def processWayNodes(
      wayId: Long,
      prev: Option[model.Node],
      current: model.Node,
      wayNodes: Seq[model.Node],
      updatedNodes: Map[Long, internal.Node],
      updatedTagsDic: Map[Int, String]
  ): TileIndex = {
    val (dic, attrs) = attributesToDictionary(current.attributes)
    wayNodes match {
      case Seq() =>
        val connections = prev.map(_.id).toSet
        val iNode = internal.Node( // TODO: Calculate directions. Now, all bidirectional.
          current.id,
          current.location,
          attrs,
          ways = Set(wayId),
          outs = connections,
          ins = connections
        )

        copy(
          nodes = searchAndMerge(iNode, updatedNodes),
          tagsDic = updatedTagsDic ++ dic
        )
      case Seq(next, tail @ _*) => {
        val connections = prev.map(_.id).toSet + next.id // TODO: Calculate directions. Now, all bidirectional.
        val iNode = internal.Node(
          current.id,
          current.location,
          attrs,
          ways = Set(wayId),
          outs = connections,
          ins = connections
        )

        processWayNodes(
          wayId,
          Some(current),
          next,
          tail,
          searchAndMerge(iNode, updatedNodes),
          updatedTagsDic ++ dic
        )
      }
    }
  }

  def addWay(way: model.Way): TileIndex = {
    val (dic, attrs) = attributesToDictionary(way.attributes)

    val newWaysSet = ways + (way.id -> internal.Way(way.id, way.nodes.map(_.id), attrs))

    copy(
      ways = newWaysSet
    ).processWayNodes(
      way.id,
      None,
      way.nodes.head,
      way.nodes.tail,
      nodes,
      tagsDic ++ dic
    )
  }

  def getNode(id: Long): Option[model.Node] =
    nodes
      .get(id)
      .map(iNode =>
        model.Node(
          iNode.id,
          iNode.location,
          dictionaryToAttributes(iNode.attributes)
        )
      )

  def getWay(id: Long): Option[model.Way] = ways.get(id).map { iWay =>
    model.Way(
      id,
      iWay.nodeIds
        .map { nodeId =>
          val iNode = nodes(nodeId)
          model.Node(
            iNode.id,
            iNode.location,
            iNode.attributes.map(attr => tagsDic(attr._1) -> attr._2)
          )
        },
      iWay.attributes.map(attr => tagsDic(attr._1) -> attr._2)
    )
  }

}
