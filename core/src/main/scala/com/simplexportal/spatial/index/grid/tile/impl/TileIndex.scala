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

import com.simplexportal.spatial.index.grid.tile.impl.TileIndex._
import com.simplexportal.spatial.model._

import scala.annotation.tailrec

object TileIndex {

  case class InternalNode(
      id: Long,
      location: Location,
      attributes: Map[Int, String] = Map.empty,
      ways: Set[Long] = Set.empty,
      outs: Set[Long] = Set.empty,
      ins: Set[Long] = Set.empty
  )

  case class InternalWay(
      id: Long,
      nodeIds: Seq[Long],
      attributes: Map[Int, String] = Map.empty
  )

}

case class TileIndex(
    nodes: Map[Long, InternalNode] = Map.empty,
    ways: Map[Long, InternalWay] = Map.empty,
    tagsDic: Map[Int, String] = Map.empty
) extends NearestNodeSearch
    with API {

  // Generate a tuple a map with all tagsIds and another with the value indexed by tagId.
  private def attributesToDictionary(
      attributes: Map[String, String]
  ): (Map[Int, String], Map[Int, String]) =
    attributes.foldLeft((Map.empty[Int, String], Map.empty[Int, String])) {
      case ((dic, attrs), attr) => {
        val hash = attr._1.hashCode
        (dic + (hash -> attr._1), attrs + (hash -> attr._2))
      }
    }

  protected def dictionaryToAttributes(
      attrs: Map[Int, String]
  ): Map[String, String] =
    attrs.map { case (k, v) => tagsDic(k) -> v }

  private def buildNewNode(
      wayId: Long,
      prev: Option[Long],
      current: Long,
      next: Option[Long]
  ) =
    nodes.get(current) match {
      case None => // If it is not in the index, it is because it is a connector.
        InternalNode( // TODO: Calculate directions. Now, all bidirectional.
          current,
          Location.NaL,
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
      updated: List[(Long, InternalNode)]
  ): List[(Long, InternalNode)] =
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

  def addNode(
      id: Long,
      lat: Double,
      lon: Double,
      attributes: Map[String, String]
  ): TileIndex = {
    val (dic, attrs) = attributesToDictionary(attributes)
    copy(
      nodes = nodes + (id -> InternalNode(id, Location(lat, lon), attrs)),
      tagsDic = tagsDic ++ dic
    )
  }

  def addWay(
      wayId: Long,
      nodeIds: Seq[Long],
      attributes: Map[String, String]
  ): TileIndex = {
    val (dic, attrs) = attributesToDictionary(attributes)
    copy(
      ways = ways + (wayId -> InternalWay(wayId, nodeIds, attrs)),
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

}
