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

import com.simplexportal.spatial.model.{Location, Node, Way}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

// scalastyle:off magic.number

class APISpec extends AnyWordSpecLike with Matchers {

  "Using the API" should {
    val exampleNetwork = TileIndex(
      nodes = Map(
        1L -> TileIndex.InternalNode(1, Location(7, 3), Map(206407764 -> "true"), Set(101), Set(2), Set(2)),
        2L -> TileIndex.InternalNode(2, Location(7, 10), Map.empty, Set(101), Set(1, 3), Set(1, 3)),
        3L -> TileIndex.InternalNode(3, Location(3, 10), Map.empty, Set(101, 100), Set(2, 6, 4), Set(2, 6, 4)),
        4L -> TileIndex.InternalNode(4, Location(3, 16), Map.empty, Set(101), Set(3), Set(3)),
        5L -> TileIndex.InternalNode(5, Location(4, 5), Map.empty, Set(100), Set(6), Set(6)),
        6L -> TileIndex.InternalNode(6, Location(2, 5), Map.empty, Set(100), Set(3, 5), Set(3, 5))
      ),
      ways = Map(
        100L -> TileIndex.InternalWay(100, Seq(5, 6, 3), Map(3373707 -> "Street Name")),
        101L -> TileIndex.InternalWay(101, Seq(1, 2, 3, 4))
      ),
      Map(206407764 -> "traffic_light", 3373707 -> "name")
    )

    "retrieve a way" in {
      exampleNetwork.getWay(100) shouldBe Some(
        Way(
          100,
          Seq(
            Node(5, Location(4, 5)),
            Node(6, Location(2, 5)),
            Node(3, Location(3, 10))
          ),
          Map("name" -> "Street Name")
        )
      )

      exampleNetwork.getWay(101) shouldBe Some(
        Way(
          101,
          Seq(
            Node(1, Location(7, 3), Map("traffic_light" -> "true")),
            Node(2, Location(7, 10)),
            Node(3, Location(3, 10)),
            Node(4, Location(3, 16))
          ),
          Map.empty
        )
      )

      exampleNetwork.getWay(10) shouldBe None
    }

    "retrieve a node" in {
      exampleNetwork.getNode(100000) shouldBe None
      exampleNetwork.getNode(1) shouldBe Some(Node(1, Location(7, 3), Map("traffic_light" -> "true")))
    }

    "add a Node" in {
      val node = Node(10, Location(90, 90), Map.empty)
      exampleNetwork.addNode(node).getNode(10) shouldBe Some(node)
    }

    "add a Way" in {
      val way =
        Way(
          1000,
          Seq(
            Node(1001, Location(90, 90), Map.empty),
            Node(1002, Location(90, 90), Map.empty),
            Node(1003, Location(90, 90), Map.empty)
          ),
          Map("key" -> "value")
        )

      val tileIdx = exampleNetwork.addWay(way)

      tileIdx.getNode(1001) shouldBe Some(Node(1001, Location(90, 90), Map.empty))
      tileIdx.getWay((1000)) shouldBe Some(way)
    }

  }
}
