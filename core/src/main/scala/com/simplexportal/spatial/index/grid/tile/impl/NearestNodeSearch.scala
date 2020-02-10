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

package com.simplexportal.spatial.index.grid.tile.impl

import com.simplexportal.spatial.index.grid.tile.impl.TileIndex.InternalNode
import com.simplexportal.spatial.model.{Location, Node}
import com.simplexportal.spatial.utils.ModelEnrichers._
import org.locationtech.jts.geom.Coordinate

case class NearestNode(
    nodes: Set[Node],
    distance: Double
) {

  def chooseNearest(b: NearestNode): NearestNode = if (b.distance < distance) b else this
}

trait NearestNodeSearch {
  this: TileIndex =>

  def nearestNode(origin: Location): Option[NearestNode] =
    nearestInternalNode(origin.toJTS()).map {
      case (internalNodes, distance) =>
        NearestNode(
          internalNodes.map(iNode =>
            Node(
              iNode.id,
              iNode.location,
              dictionaryToAttributes(iNode.attributes)
            )
          ),
          distance
        )
    }

  /**
    * Return nearest points and the distance in degrees.
    * TODO: It will iterate all nodes in the tile so it is a really bad approach. https://github.com/angelcervera/simplexspatial/issues/41
    *
    * @param origin Point of origin. It is a JTS Coordinate(lon, lat)
    * @return A tuple with the set of nodes found and the distance in degrees.
    */
  private def nearestInternalNode(
      origin: Coordinate
  ): Option[(Set[InternalNode], Double)] =
    nodes.foldLeft(Option.empty[(Set[InternalNode], Double)]) {
      case (current, (_, node)) => {
        val d = origin.distance(node.location.toJTS())
        current match {
          case None => Some(Set(node), d)
          case Some((nearestNodes, nearestDistance)) if d == nearestDistance =>
            Some((nearestNodes + node, nearestDistance))
          case Some((_, nearestDistance)) if d < nearestDistance =>
            Some(Set(node), d)
          case _ => current
        }
      }
    }
}
