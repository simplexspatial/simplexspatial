package com.simplexportal.spatial.index.grid

import com.simplexportal.spatial.index.grid.TileIndexEntityFunction.TileEntityInfo
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{Matchers, WordSpecLike}


class TileIndexEntityFunctionSpec extends WordSpecLike with Matchers {

  "GridHashFunctionTest" should {

    "create the right hash" in {
      val coordsHashes = Table(
        ("lat_parts", "lon_parts", "lat", "lon", "Hash"),
        (4, 4, -90, -180, TileEntityInfo(0,0, "0_0")),
        (4, 4, -60, -120, TileEntityInfo(0,0, "0_0")),
        (4, 4, -45, -90, TileEntityInfo(1,1, "1_1")),
        (4, 4, -20, -30, TileEntityInfo(1,1, "1_1")),
        (4, 4, 0, 0, TileEntityInfo(2,2, "2_2")),
        (4, 4, 20, 30, TileEntityInfo(2,2, "2_2")),
        (4, 4, 45, 90, TileEntityInfo(3,3, "3_3")),
        (4, 4, 60, 120, TileEntityInfo(3,3, "3_3")),
        (4, 4, 90, 180, TileEntityInfo(4,4, "4_4")),

        (180, 4, 90, 180, TileEntityInfo(180, 4, "180_4")),

        (10000, 10000, -90, -180, TileEntityInfo(0,0, "0_0")),
        (180, 90, 90, 180, TileEntityInfo(180,90, "180_90")),
        (180, 360, 90, 180, TileEntityInfo(180,360, "180_360")),
        (181, 361, 90, 180, TileEntityInfo(181,361, "181_361")),
        (1000, 1000, 90, 180, TileEntityInfo(1000,1000, "1000_1000")),
        (1000000, 1000000, 90, 180, TileEntityInfo(1000000,1000000, "1000000_1000000")),
      )
      forAll (coordsHashes) { (latPartitions, lonPartitions, lat, lon, id) =>
        TileIndexEntityFunction(latPartitions, lonPartitions)(lat, lon) should be(id)
      }
    }

    "throw an error when ask for latitude partitions higher than the precision" in {
      val e = intercept[Exception] {
        TileIndexEntityFunction(10000000, 1)
      }
      assert(e.getMessage().startsWith("requirement failed: latitude partitions could not be higher"))
    }
    "throw an error when ask for longitude partitions higher than the precision" in {
      val e = intercept[Exception] {
        TileIndexEntityFunction(1, 10000000)
      }
      assert(e.getMessage().startsWith("requirement failed: longitude partitions could not be higher"))
    }
  }
}
