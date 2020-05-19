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

// scalastyle:off magic.number

package com.simplexportal.spatial.index.grid.tile.impl

import com.simplexportal.spatial.model
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class NearestNodeSearchSpec extends AnyWordSpecLike with Matchers with Inside {

  "TileGISSearch" should {
    val index = TileIndex()
      .addNode(1, 7, 10, Map.empty)
      .addNode(2, 8, 10, Map.empty)
      .addNode(3, 9, 10, Map.empty)
      .addNode(4, -7, -10, Map.empty)

    "fine the nearest node" when {

      "there is only one result" in {
        inside(index.nearestNode(model.Location(6, 9))) {
          case Some(NearestNode(nodes, distance)) =>
            nodes shouldBe Set(model.Node(1, model.Location(7, 10), Map.empty))
            distance shouldBe 1.4 +- 0.1
        }
      }

      "there are more than one result" in {
        inside(index.nearestNode(model.Location(0, 0))) {
          case Some(NearestNode(nodes, distance)) =>
            nodes shouldBe Set(
              model.Node(1, model.Location(7, 10), Map.empty),
              model.Node(4, model.Location(-7, -10), Map.empty)
            )
            distance shouldBe 12.2 +- 0.1
        }
      }

      "there are not results" in {
        TileIndex().nearestNode(model.Location(6, 9)) shouldBe None
      }
    }

  }
}
