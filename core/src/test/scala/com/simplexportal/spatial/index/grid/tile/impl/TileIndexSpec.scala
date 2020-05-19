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

// scalastyle:off magic.number

package com.simplexportal.spatial.index.grid.tile.impl

import com.simplexportal.spatial.model
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.simplexportal.spatial.index.grid.tile.impl.{TileIndex => internal}

class TileIndexSpec extends AnyWordSpecLike with Matchers {

  "Tile" should {

    // format: off
    val exampleNetwork = TileIndex(
      nodes = Map(
        1L -> internal.Node(1, model.Location(7, 3), Map(206407764 -> "true"), Set(101), Set(2), Set(2)),
        2L -> internal.Node(2, model.Location(7, 10), Map.empty, Set(101), Set(1, 3), Set(1, 3)),
        3L -> internal.Node(3, model.Location(3, 10), Map.empty, Set(101, 100), Set(2, 6, 4), Set(2, 6, 4)),
        4L -> internal.Node(4, model.Location(3, 16), Map.empty, Set(101), Set(3), Set(3)),
        5L -> internal.Node(5, model.Location(4, 5), Map.empty, Set(100), Set(6), Set(6)),
        6L -> internal.Node(6, model.Location(2, 5), Map.empty, Set(100), Set(3, 5), Set(3, 5))
      ),
      ways = Map(
        100L -> internal.Way(100,Seq(5, 6, 3), Map(3373707 -> "Street Name")),
        101L -> internal.Way(101, Seq(1, 2, 3, 4))
      ),
      Map(206407764 -> "traffic_light", 3373707 -> "name")
    )
    // format: on

    "create a network" when {

      "is simple definition" in {
        val current = TileIndex()
          .addNode(2, 7, 10, Map.empty)
          .addNode(1, 7, 3, Map("traffic_light" -> "true"))
          .addNode(3, 3, 10, Map.empty)
          .addNode(4, 3, 16, Map.empty)
          .addNode(5, 4, 5, Map.empty)
          .addNode(6, 2, 5, Map.empty)
          .addWay(100, Seq(5, 6, 3), Map("name" -> "Street Name"))
          .addWay(101, Seq(1, 2, 3, 4), Map.empty)

        current should be(exampleNetwork)
        current.nodes.size should be(6)
        current.ways.size should be(2)
      }

      "Vectors are used as list of nodes" in {
        val current = TileIndex()
          .addNode(2, 7, 10, Map.empty)
          .addNode(1, 7, 3, Map("traffic_light" -> "true"))
          .addNode(3, 3, 10, Map.empty)
          .addNode(4, 3, 16, Map.empty)
          .addNode(5, 4, 5, Map.empty)
          .addNode(6, 2, 5, Map.empty)
          .addWay(100, Vector(5, 6, 3), Map("name" -> "Street Name"))
          .addWay(101, Vector(1, 2, 3, 4), Map.empty)

        current should be(exampleNetwork)
        current.nodes.size should be(6)
        current.ways.size should be(2)
      }
    }

  }
}
