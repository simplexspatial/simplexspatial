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

import com.simplexportal.spatial.RTreeActor.{AddNode, AddWay, RTreeCommands}
import com.simplexportal.spatial.model.{BoundingBox, Location}

trait RTreeActorDataset {
  val bbox = BoundingBox(Location(1,1), Location(10,10))

  val exampleTileCommands: Seq[RTreeCommands] = Seq(
    AddNode(1, 7,3, Map("nodeAttrKey" -> "nodeAttrValue")),
    AddNode(2, 7,10, Map.empty),
    AddNode(3, 3, 10, Map.empty),
    AddNode(4, 3, 16, Map.empty),
    AddNode(5, 4, 5, Map.empty),
    AddNode(6, 2, 5, Map.empty),
    AddWay(100, Seq(5,6,3), Map("wayAttrKey" -> "wayAttrValue") ),
    AddWay(101, Seq(1,2,3,4), Map.empty ),
  )
}
