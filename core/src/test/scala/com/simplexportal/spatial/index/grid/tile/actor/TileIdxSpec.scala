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

package com.simplexportal.spatial.index.grid.tile.actor

import org.scalatest.{Matchers, WordSpecLike}

// scalastyle:off magic.number
class TileIdxSpec extends WordSpecLike with Matchers {
  "TileIdx" should {
    "normalize idxs" in {
      implicit val tileIdxGen = TileIndexEntityIdGen(4, 4)
      TileIdx(4, 4).normalize() shouldBe TileIdx(0, 0)
      TileIdx(2, 2).normalize() shouldBe TileIdx(2, 2)
      TileIdx(-1, -1).normalize() shouldBe TileIdx(3, 3)
    }
    "calculate layer surrendering" when {
      implicit val tileIdGen = TileIndexEntityIdGen(10, 10)
      "calculate tiles in the layer" when {
        "it is the layer 0" in {
          TileIdx(5, 5).layer(0) shouldBe Set(TileIdx(5, 5))
        }
        "it is the layer 1" in {
          TileIdx(5, 5).layer(1) shouldBe Set(
            // format: off
            TileIdx(6, 4), TileIdx(6, 5), TileIdx(6, 6),
            TileIdx(5, 4),                TileIdx(5, 6),
            TileIdx(4, 4), TileIdx(4, 5), TileIdx(4, 6),
            // format: on
          )
        }
        "it is the layer 1 in the edge" in {
          TileIdx(9,0).layer(1) shouldBe Set(
            // format: off
            TileIdx(0, 9), TileIdx(0, 0), TileIdx(0, 1),
            TileIdx(9, 9),                TileIdx(9, 1),
            TileIdx(8, 9), TileIdx(8, 0), TileIdx(8, 1),
            // format: on
          )
        }
      }
    }
  }
}
