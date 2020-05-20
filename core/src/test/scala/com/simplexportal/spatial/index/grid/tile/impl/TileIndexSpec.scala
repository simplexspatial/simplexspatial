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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.simplexportal.spatial.model

class TileIndexSpec extends AnyWordSpecLike with Matchers {
  "TileIdx.Node" should {
    "Merge two object" in {
      TileIndex.Node(1, model.Location(1, 1), Map.empty, Set(0), Set(1), Set(2)) merge (
        TileIndex.Node(
          3,
          model.Location(3, 3),
          Map.empty,
          Set(1),
          Set(2),
          Set(3)
        )
      ) shouldBe (TileIndex.Node(
        3,
        model.Location(3, 3),
        Map.empty,
        Set(0, 1),
        Set(2, 1),
        Set(3, 2)
      ))
    }
  }
}
