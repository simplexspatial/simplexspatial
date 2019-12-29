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

package com.simplexportal.spatial.index.grid.tile

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{Matchers, WordSpecLike}

class TileIndexEntityIdGenSpec extends WordSpecLike with Matchers {

  "GridHashFunctionTest" should {

    "create the right hash" in {
      val coordsHashes = Table(
        ("lat_parts", "lon_parts", "lat", "lon", "Hash"),
        (4, 4, -90, -180, TileIdx(0,0)),
        (4, 4, -60, -120, TileIdx(0,0)),
        (4, 4, -45, -90, TileIdx(1,1)),
        (4, 4, -20, -30, TileIdx(1,1)),
        (4, 4, 0, 0, TileIdx(2,2)),
        (4, 4, 20, 30, TileIdx(2,2)),
        (4, 4, 45, 90, TileIdx(3,3)),
        (4, 4, 60, 120, TileIdx(3,3)),
        (4, 4, 90, 180, TileIdx(4,4)),

        (180, 4, 90, 180, TileIdx(180, 4)),

        (10000, 10000, -90, -180, TileIdx(0,0)),
        (180, 90, 90, 180, TileIdx(180,90)),
        (180, 360, 90, 180, TileIdx(180,360)),
        (181, 361, 90, 180, TileIdx(181,361)),
        (1000, 1000, 90, 180, TileIdx(1000,1000)),
        (1000000, 1000000, 90, 180, TileIdx(1000000,1000000)),
      )
      forAll (coordsHashes) { (latPartitions, lonPartitions, lat, lon, tileIdx) =>
        new TileIndexEntityIdGen(latPartitions, lonPartitions).tileIdx(lat, lon) should be(tileIdx)
      }
    }

    "throw an error when ask for latitude partitions higher than the precision" in {
      val e = intercept[Exception] {
        new TileIndexEntityIdGen(10000000, 1)
      }
      assert(e.getMessage().startsWith("requirement failed: latitude partitions could not be higher"))
    }
    "throw an error when ask for longitude partitions higher than the precision" in {
      val e = intercept[Exception] {
        new TileIndexEntityIdGen(1, 10000000)
      }
      assert(e.getMessage().startsWith("requirement failed: longitude partitions could not be higher"))
    }
  }
}
