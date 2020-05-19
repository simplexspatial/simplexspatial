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
  ) extends InternalModel

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

  private def buildNewNode(
      wayId: Long,
      prev: Option[Long],
      current: Long,
      next: Option[Long]
  ) =
    nodes.get(current) match {
      case None => // FIXME: This case will not happen. If it happend, there is an error in the tile!!!!
        // If it is not in the index, it is because it is a connector.
        internal.Node( // TODO: Calculate directions. Now, all bidirectional.
          current,
          model.Location.NaL,
          ways = Set(wayId),
          outs = (Set.empty ++ next) ++ prev,
          ins = (Set.empty ++ next) ++ prev
        )
      case Some(node) =>
        node.copy( // TODO: Calculate directions. Now, all bidirectional.
          ways = node.ways + wayId,
          outs = (node.outs ++ next) ++ prev,
          ins = (node.ins ++ next) ++ prev
        )
    }

  @tailrec
  private def updateConnections(
      wayId: Long,
      prev: Option[Long],
      current: Long,
      nodeIds: Seq[Long],
      updated: List[(Long, internal.Node)]
  ): List[(Long, internal.Node)] =
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

  private def addInternalWay(
      wayId: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String]
  ): TileIndex = {
    val (dic, attrs) = attributesToDictionary(attributes)
    copy(
      ways = ways + (wayId -> internal.Way(wayId, nodeIds, attrs)),
      nodes = nodes ++ updateConnections(
        wayId,
        None,
        nodeIds.head,
        nodeIds.tail,
        List.empty
      ),
      tagsDic = tagsDic ++ dic
    )
  }

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

  def addWay(way: model.Way): TileIndex =
    way.nodes
      .foldLeft(this)((tileIdx, node) => tileIdx.addNode(node))
      .addInternalWay(
        way.id,
        way.nodes.map(_.id),
        way.attributes
      )

  def addNode(node: model.Node): TileIndex = {
    val (dic, attrs) = attributesToDictionary(node.attributes)
    copy(
      nodes = nodes + (node.id -> internal.Node(node.id, node.location, attrs)),
      tagsDic = tagsDic ++ dic
    )
  }

}
