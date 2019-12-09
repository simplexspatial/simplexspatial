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

package com.simplexportal.spatial.index.grid

// scalastyle:off magic.number

/**
  * Shaping the cluster as a grid of latPartitions x lonPartitions.
  * All nearest points are going to be located in the same shard.
  *
  */
object TileIndexHashFunction {

  case class TileHashInfo(latIdx: Int, lonIdx: Int, tileHash: String)

  /**
   *  Constructor of new Tile Hash functions.
   *
   * @param latPartitions Partitions in the latitude axe
   * @param lonPartitions Partitions in the longitude axe
   * @param roundingDecimals 6 decimals precision means that the smallest shard possible will be around 100 square millimeters per shard.
   * @return A function that calculate hash info for tiles.
   */
  def apply(
             latPartitions: Int,
             lonPartitions: Int,
             roundingDecimals: Byte = 6
           ): (Double, Double) => TileHashInfo = {

    val PRECISION_ROUNDING: Int = Math.pow(10, roundingDecimals).toInt

    require(
      latPartitions <= PRECISION_ROUNDING,
      s"latitude partitions could not be higher than ${PRECISION_ROUNDING} "
    )
    require(
      lonPartitions <= PRECISION_ROUNDING,
      s"longitude partitions could not be higher than ${PRECISION_ROUNDING} "
    )

    def latPartition(lat: Double): Int =
      ((lat + 90) * PRECISION_ROUNDING).toInt / ((180 * PRECISION_ROUNDING) / latPartitions)

    def lonPartition(lon: Double): Int =
      ((lon + 180) * PRECISION_ROUNDING).toInt / ((360 * PRECISION_ROUNDING) / lonPartitions)

    def indicesToString(latIdx: Int, lonIdx: Int): String =
      s"${latIdx}_${lonIdx}"

    (lat: Double, lon: Double) => {
      val latIdx = latPartition(lat)
      val lonIdx = lonPartition(lon)
      TileHashInfo(latIdx, lonIdx, indicesToString(latIdx, lonIdx))
    }
  }

}
