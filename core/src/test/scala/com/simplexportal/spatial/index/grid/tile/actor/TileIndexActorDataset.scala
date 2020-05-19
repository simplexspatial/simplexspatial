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

package com.simplexportal.spatial.index.grid.tile.actor

import com.simplexportal.spatial.index.grid.tile.actor.{TileIndexProtocol => protocol}
import com.simplexportal.spatial.model

// scalastyle:off magic.number
trait TileIndexActorDataset {
  val bbox = model.BoundingBox(model.Location(1, 1), model.Location(10, 10))

  val node1 = model.Node(1, model.Location(7, 3), Map("nodeAttrKey" -> "nodeAttrValue"))
  val node2 = model.Node(2, model.Location(7, 10), Map.empty)
  val node3 = model.Node(3, model.Location(3, 10), Map.empty)
  val node4 = model.Node(4, model.Location(3, 16), Map.empty)
  val node5 = model.Node(5, model.Location(4, 5), Map.empty)
  val node6 = model.Node(6, model.Location(2, 5), Map.empty)
  val way100 = model.Way(100, Seq(node5, node6, node3), Map("wayAttrKey" -> "wayAttrValue"))
  val way101 = model.Way(101, Seq(node1, node2, node3, node4), Map.empty)

  val exampleTileCommands: Seq[protocol.BatchActions] = Seq(
    protocol.AddNode(node1),
    protocol.AddNode(node2),
    protocol.AddNode(node3),
    protocol.AddNode(node4),
    protocol.AddNode(node5),
    protocol.AddNode(node6),
    protocol.AddWay(way100),
    protocol.AddWay(way101)
  )
}
