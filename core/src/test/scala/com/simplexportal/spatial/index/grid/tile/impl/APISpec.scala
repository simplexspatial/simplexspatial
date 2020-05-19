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

import com.simplexportal.spatial.model
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.simplexportal.spatial.index.grid.tile.impl.{TileIndex => internal}

// scalastyle:off magic.number

class APISpec extends AnyWordSpecLike with Matchers {

  "Using the API" should {
    val exampleNetwork = TileIndex(
      nodes = Map(
        1L -> internal.InternalNode(1, model.Location(7, 3), Map(206407764 -> "true"), Set(101), Set(2), Set(2)),
        2L -> internal.InternalNode(2, model.Location(7, 10), Map.empty, Set(101), Set(1, 3), Set(1, 3)),
        3L -> internal.InternalNode(3, model.Location(3, 10), Map.empty, Set(101, 100), Set(2, 6, 4), Set(2, 6, 4)),
        4L -> internal.InternalNode(4, model.Location(3, 16), Map.empty, Set(101), Set(3), Set(3)),
        5L -> internal.InternalNode(5, model.Location(4, 5), Map.empty, Set(100), Set(6), Set(6)),
        6L -> internal.InternalNode(6, model.Location(2, 5), Map.empty, Set(100), Set(3, 5), Set(3, 5))
      ),
      ways = Map(
        100L -> internal.InternalWay(100, Seq(5, 6, 3), Map(3373707 -> "Street Name")),
        101L -> internal.InternalWay(101, Seq(1, 2, 3, 4))
      ),
      Map(206407764 -> "traffic_light", 3373707 -> "name")
    )

    "retrieve a way" in {
      exampleNetwork.getWay(100) shouldBe Some(
        model.Way(
          100,
          Seq(
            model.Node(5, model.Location(4, 5)),
            model.Node(6, model.Location(2, 5)),
            model.Node(3, model.Location(3, 10))
          ),
          Map("name" -> "Street Name")
        )
      )

      exampleNetwork.getWay(101) shouldBe Some(
        model.Way(
          101,
          Seq(
            model.Node(1, model.Location(7, 3), Map("traffic_light" -> "true")),
            model.Node(2, model.Location(7, 10)),
            model.Node(3, model.Location(3, 10)),
            model.Node(4, model.Location(3, 16))
          ),
          Map.empty
        )
      )

      exampleNetwork.getWay(10) shouldBe None
    }

    "retrieve a node" in {
      exampleNetwork.getNode(100000) shouldBe None
      exampleNetwork.getNode(1) shouldBe Some(model.Node(1, model.Location(7, 3), Map("traffic_light" -> "true")))
    }

    "add a Node" in {
      val node = model.Node(10, model.Location(90, 90), Map.empty)
      exampleNetwork.addNode(node).getNode(10) shouldBe Some(node)
    }

    "add a Way" in {
      val way =
        model.Way(
          1000,
          Seq(
            model.Node(1001, model.Location(90, 90), Map.empty),
            model.Node(1002, model.Location(90, 90), Map.empty),
            model.Node(1003, model.Location(90, 90), Map.empty)
          ),
          Map("key" -> "value")
        )

      val tileIdx = exampleNetwork.addWay(way)

      tileIdx.getNode(1001) shouldBe Some(model.Node(1001, model.Location(90, 90), Map.empty))
      tileIdx.getWay((1000)) shouldBe Some(way)
    }

  }
}
