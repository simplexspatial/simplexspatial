/*
 * Copyright (C) 2019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

import com.simplexportal.spatial.Tile.{Node, Way}
import com.simplexportal.spatial.model._

import scala.annotation.tailrec
import scala.collection.immutable.LongMap

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
    nodes: LongMap[Node] = LongMap.empty,
    ways: LongMap[Way] = LongMap.empty
) {

  def addNode(id: Long,
              lat: Double,
              lon: Double,
              attributes: Attributes): Tile =
    copy(nodes = nodes + (id -> Node(id, Location(lat, lon), attributes)))

  def addWay(wayId: Long,
             nodeIds: Seq[Long],
             attributes: Map[String, String]): Tile = {

    // Manage generated list in private scope as List because performance is not bad!
    @tailrec
    def updateConnections(prev: Option[Long],
                          current: Long,
                          nodeIds: Seq[Long],
                          updated: List[(Long, Node)]): List[(Long, Node)] =
      nodeIds match {
        case Nil => (current, buildNewNode(prev, current, None)) :: updated
        case next :: tail => {
          updateConnections(
            Some(current),
            next,
            tail,
            (current, buildNewNode(prev, current, Some(next))) :: updated)
        }
      }

    def buildNewNode(prev: Option[Long], current: Long, next: Option[Long]) = {
      val node = nodes
        .getOrElse(
          current,
          throw new NotImplementedError(
            "Node not found in the Tile is still not implemented.")
        )

      node.copy( // TODO: Calculate directions. Now, all bidirectional.
                ways = node.ways + wayId,
                outs = (node.outs ++ next) ++ prev,
                ins = (node.ins ++ next) ++ prev)
    }

    copy(
      ways = ways + (wayId -> Way(wayId, nodeIds.head, attributes)),
      nodes = nodes ++ updateConnections(None,
                                         nodeIds.head,
                                         nodeIds.tail,
                                         List.empty)
    )
  }

}
