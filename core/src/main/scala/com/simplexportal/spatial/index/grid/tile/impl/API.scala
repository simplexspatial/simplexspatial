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

import com.simplexportal.spatial.model.{Node, Way}

trait API {
  this: TileIndex =>

  def getWay(id: Long): Option[Way] = ways.get(id).map { iWay =>
    Way(
      id,
      iWay.nodeIds
        .map { nodeId =>
          val iNode = nodes(nodeId)
          Node(
            iNode.id,
            iNode.location,
            iNode.attributes.map(attr => tagsDic(attr._1) -> attr._2)
          )
        },
      iWay.attributes.map(attr => tagsDic(attr._1) -> attr._2)
    )
  }

  def getNode(id: Long): Option[Node] =
    nodes
      .get(id)
      .map(iNode =>
        Node(
          iNode.id,
          iNode.location,
          dictionaryToAttributes(iNode.attributes)
        )
      )

  def addWay(way: Way): TileIndex =
    way.nodes
      .foldLeft(this)((tileIdx, node) => tileIdx.addNode(node))
      .addWay(
        way.id,
        way.nodes.map(_.id),
        way.attributes
      )

  def addNode(node: Node): TileIndex = addNode(node.id, node.location.lat, node.location.lon, node.attributes)

}
