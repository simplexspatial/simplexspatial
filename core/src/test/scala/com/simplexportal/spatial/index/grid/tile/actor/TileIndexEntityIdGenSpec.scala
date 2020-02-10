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

package com.simplexportal.spatial.index.grid.tile.actor

import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.wordspec.AnyWordSpecLike

class TileIndexEntityIdGenSpec extends AnyWordSpecLike with Matchers {

  "TileIdx" should {
    "create TileIdx from String" in {
      TileIdx("1_2") shouldBe Right(TileIdx(1, 2))
      TileIdx("1_2_3") shouldBe Left("[1_2_3] is not a valid format for a TileIdx")
      TileIdx("") shouldBe Left("[] is not a valid format for a TileIdx")
      TileIdx("X_2") shouldBe Left("Error parsing X_2 => For input string: \"X\"")
    }
  }

  "TileIndexEntityGen" should {

    "calculate neighbours TileIdx" when {
      val tileIdxGen = TileIndexEntityIdGen(10, 10)
      "asks for north" in {
        tileIdxGen.northTileIdx(TileIdx(9, 9)) shouldBe TileIdx(0, 9)
        tileIdxGen.northTileIdx(TileIdx(0, 0)) shouldBe TileIdx(1, 0)
      }
      "asks for north-east" in {
        tileIdxGen.northEastTileIdx(TileIdx(9, 9)) shouldBe TileIdx(0, 0)
        tileIdxGen.northEastTileIdx(TileIdx(9, 0)) shouldBe TileIdx(0, 1)
        tileIdxGen.northEastTileIdx(TileIdx(0, 9)) shouldBe TileIdx(1, 0)
        tileIdxGen.northEastTileIdx(TileIdx(0, 0)) shouldBe TileIdx(1, 1)
      }
      "asks for east" in {
        tileIdxGen.eastTileIdx(TileIdx(9, 9)) shouldBe TileIdx(9, 0)
        tileIdxGen.eastTileIdx(TileIdx(9, 0)) shouldBe TileIdx(9, 1)
        tileIdxGen.eastTileIdx(TileIdx(0, 9)) shouldBe TileIdx(0, 0)
        tileIdxGen.eastTileIdx(TileIdx(0, 0)) shouldBe TileIdx(0, 1)
      }
      "asks for south-east" in {
        tileIdxGen.southEastTileIdx(TileIdx(9, 9)) shouldBe TileIdx(8, 0)
        tileIdxGen.southEastTileIdx(TileIdx(9, 0)) shouldBe TileIdx(8, 1)
        tileIdxGen.southEastTileIdx(TileIdx(0, 9)) shouldBe TileIdx(9, 0)
        tileIdxGen.southEastTileIdx(TileIdx(0, 0)) shouldBe TileIdx(9, 1)
      }
      "asks for south" in {
        tileIdxGen.southTileIdx(TileIdx(9, 9)) shouldBe TileIdx(8, 9)
        tileIdxGen.southTileIdx(TileIdx(9, 0)) shouldBe TileIdx(8, 0)
        tileIdxGen.southTileIdx(TileIdx(0, 9)) shouldBe TileIdx(9, 9)
        tileIdxGen.southTileIdx(TileIdx(0, 0)) shouldBe TileIdx(9, 0)
      }
      "asks for south-west" in {
        tileIdxGen.southWestTileIdx(TileIdx(9, 9)) shouldBe TileIdx(8, 8)
        tileIdxGen.southWestTileIdx(TileIdx(9, 0)) shouldBe TileIdx(8, 9)
        tileIdxGen.southWestTileIdx(TileIdx(0, 9)) shouldBe TileIdx(9, 8)
        tileIdxGen.southWestTileIdx(TileIdx(0, 0)) shouldBe TileIdx(9, 9)
      }
      "asks for west" in {
        tileIdxGen.westTileIdx(TileIdx(9, 9)) shouldBe TileIdx(9, 8)
        tileIdxGen.westTileIdx(TileIdx(9, 0)) shouldBe TileIdx(9, 9)
        tileIdxGen.westTileIdx(TileIdx(0, 9)) shouldBe TileIdx(0, 8)
        tileIdxGen.westTileIdx(TileIdx(0, 0)) shouldBe TileIdx(0, 9)
      }
      "asks for north-west" in {
        tileIdxGen.northWestTileIdx(TileIdx(9, 9)) shouldBe TileIdx(8, 8)
        tileIdxGen.northWestTileIdx(TileIdx(9, 0)) shouldBe TileIdx(8, 9)
        tileIdxGen.northWestTileIdx(TileIdx(0, 9)) shouldBe TileIdx(9, 8)
        tileIdxGen.northWestTileIdx(TileIdx(0, 0)) shouldBe TileIdx(9, 9)
      }
      "ask for all neighbours in clock order" in {
        tileIdxGen.clockNeighbours(TileIdx(9, 9)) shouldBe Seq(
          TileIdx(0, 9),
          TileIdx(0, 0),
          TileIdx(9, 0),
          TileIdx(8, 0),
          TileIdx(8, 9),
          TileIdx(8, 8),
          TileIdx(9, 8),
          TileIdx(8, 8)
        )
      }
    }

    "create the right hash" in {
      val coordsHashes = Table(
        ("lat_parts", "lon_parts", "lat", "lon", "Hash"),
        (4, 4, -90, -180, TileIdx(0, 0)),
        (4, 4, -60, -120, TileIdx(0, 0)),
        (4, 4, -45, -90, TileIdx(1, 1)),
        (4, 4, -20, -30, TileIdx(1, 1)),
        (4, 4, 0, 0, TileIdx(2, 2)),
        (4, 4, 20, 30, TileIdx(2, 2)),
        (4, 4, 45, 90, TileIdx(3, 3)),
        (4, 4, 60, 120, TileIdx(3, 3)),
        (4, 4, 90, 180, TileIdx(4, 4)),
        (180, 4, 90, 180, TileIdx(180, 4)),
        (10000, 10000, -90, -180, TileIdx(0, 0)),
        (180, 90, 90, 180, TileIdx(180, 90)),
        (180, 360, 90, 180, TileIdx(180, 360)),
        (181, 361, 90, 180, TileIdx(181, 361)),
        (1000, 1000, 90, 180, TileIdx(1000, 1000)),
        (1000000, 1000000, 90, 180, TileIdx(1000000, 1000000))
      )
      forAll(coordsHashes) { (latPartitions, lonPartitions, lat, lon, tileIdx) =>
        TileIndexEntityIdGen(latPartitions, lonPartitions).tileIdx(lat, lon) should be(tileIdx)
      }
    }

    "throw an error when ask for latitude partitions higher than the precision" in {
      val e = intercept[Exception] {
        TileIndexEntityIdGen(10000000, 1)
      }
      assert(e.getMessage().startsWith("requirement failed: latitude partitions could not be higher"))
    }
    "throw an error when ask for longitude partitions higher than the precision" in {
      val e = intercept[Exception] {
        TileIndexEntityIdGen(1, 10000000)
      }
      assert(e.getMessage().startsWith("requirement failed: longitude partitions could not be higher"))
    }
  }
}
