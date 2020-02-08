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

package com.simplexportal.spatial.index.grid.tile.actor

import com.simplexportal.spatial.model.{BoundingBox, Location}

import scala.util.Try

object TileIdx {
  def apply(entityId: String): Either[String, TileIdx] =
    entityId.split("_") match {
      case Array(latIdx, lonIdx) =>
        Try(TileIdx(latIdx.toInt, lonIdx.toInt)).toEither.left.map(ex => s"Error parsing ${entityId} => ${ex.getMessage}")
      case _ => Left(s"[${entityId}] is not a valid format for a TileIdx")
    }
}

case class TileIdx(latIdx: Int, lonIdx: Int) {
  def entityId: String = s"${latIdx}_${lonIdx}"

  def normalize()(implicit tileIdxGen: TileIndexEntityIdGen): TileIdx =
    TileIdx(
      normalize(latIdx, tileIdxGen.latPartitions),
      normalize(lonIdx, tileIdxGen.lonPartitions)
    )

  @inline private def normalize(idx: Int, partitions: Int)(implicit tileIdxGen: TileIndexEntityIdGen): Int =
    idx % partitions match {
      case i if i < 0 => i + partitions
      case i          => i
    }

  def layer(layer: Int)(
      implicit tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Set[TileIdx] = {
    val minLat = latIdx - layer
    val maxLat = latIdx + layer
    val minLon = lonIdx - layer
    val maxLon = lonIdx + layer
    ((minLat to maxLat flatMap (
        lat => Seq(TileIdx(lat, minLon).normalize(), TileIdx(lat, maxLon).normalize())
    )) ++ (minLon to maxLon flatMap (
        lon => Seq(TileIdx(minLat, lon).normalize(), TileIdx(maxLat, lon).normalize())
    ))).toSet
  }
}

object TileIndexEntityIdGen {
  val defaultRoundingDecimal: Byte = 6
}

/**
  * Shaping the cluster as a grid of latPartitions x lonPartitions.
  * All nearest points are going to be located in the same shard.
  *
  *
  * @param latPartitions Partitions in the latitude axe
  * @param lonPartitions Partitions in the longitude axe
  * @param roundingDecimals 6 decimals precision means that the smallest shard possible will be around 100 square millimeters per shard.
  * @return A function that calculate Entity Id info for tiles.
  */
case class TileIndexEntityIdGen(
    latPartitions: Int,
    lonPartitions: Int,
    roundingDecimals: Byte = TileIndexEntityIdGen.defaultRoundingDecimal
) {
  // scalastyle:off magic.number
  val PRECISION_ROUNDING: Int = Math.pow(10, roundingDecimals).toInt
  val maxLatIdx = latPartitions - 1
  val maxLonIdx = lonPartitions - 1

  require(
    latPartitions <= PRECISION_ROUNDING,
    s"latitude partitions could not be higher than ${PRECISION_ROUNDING} "
  )
  require(
    lonPartitions <= PRECISION_ROUNDING,
    s"longitude partitions could not be higher than ${PRECISION_ROUNDING} "
  )

  def tileIdx(lat: Double, lon: Double): TileIdx =
    TileIdx(latPartition(lat), lonPartition(lon))

  def latPartition(lat: Double): Int =
    ((lat + 90) * PRECISION_ROUNDING).toInt / ((180 * PRECISION_ROUNDING) / latPartitions)

  def lonPartition(lon: Double): Int =
    ((lon + 180) * PRECISION_ROUNDING).toInt / ((360 * PRECISION_ROUNDING) / lonPartitions)

  def boundingBox(tileIdx: TileIdx): BoundingBox = BoundingBox(
    min = Location(
      -90 + ((180 / latPartitions) * tileIdx.latIdx),
      -180 + ((360 / lonPartitions) * tileIdx.lonIdx)
    ),
    max = Location(
      -90 + ((180 / latPartitions) * (tileIdx.latIdx + 1)),
      -180 + ((360 / lonPartitions) * (tileIdx.lonIdx + 1))
    )
  )

  // TODO: Think about move all neighbour calculation into TileIdx passing TileIndexEntityIdGen as implicit.

  def clockNeighbours(tileIdx: TileIdx): Seq[TileIdx] = Seq(
    northTileIdx(tileIdx),
    northEastTileIdx(tileIdx),
    eastTileIdx(tileIdx),
    southEastTileIdx(tileIdx),
    southTileIdx(tileIdx),
    southWestTileIdx(tileIdx),
    westTileIdx(tileIdx),
    northWestTileIdx(tileIdx)
  )

  def northTileIdx(tileIdx: TileIdx): TileIdx = TileIdx(
    incTileCoord(tileIdx.latIdx, maxLatIdx),
    tileIdx.lonIdx
  )

  @inline private def incTileCoord(current: Int, max: Int): Int =
    if (current >= max) 0 else current + 1

  def northEastTileIdx(tileIdx: TileIdx): TileIdx = TileIdx(
    incTileCoord(tileIdx.latIdx, maxLatIdx),
    incTileCoord(tileIdx.lonIdx, maxLonIdx)
  )

  def eastTileIdx(tileIdx: TileIdx): TileIdx = TileIdx(
    tileIdx.latIdx,
    incTileCoord(tileIdx.lonIdx, maxLonIdx)
  )

  def southEastTileIdx(tileIdx: TileIdx): TileIdx = TileIdx(
    if (tileIdx.latIdx == 0) maxLatIdx else tileIdx.latIdx - 1,
    incTileCoord(tileIdx.lonIdx, maxLonIdx)
  )

  def southTileIdx(tileIdx: TileIdx): TileIdx = TileIdx(
    decTileCoord(tileIdx.latIdx, maxLatIdx),
    tileIdx.lonIdx
  )

  def southWestTileIdx(tileIdx: TileIdx): TileIdx = TileIdx(
    decTileCoord(tileIdx.latIdx, maxLatIdx),
    decTileCoord(tileIdx.lonIdx, maxLonIdx)
  )

  @inline private def decTileCoord(current: Int, max: Int): Int =
    if (current == 0) max else current - 1

  def westTileIdx(tileIdx: TileIdx): TileIdx = TileIdx(
    tileIdx.latIdx,
    decTileCoord(tileIdx.lonIdx, maxLonIdx)
  )

  def northWestTileIdx(tileIdx: TileIdx): TileIdx = TileIdx(
    decTileCoord(tileIdx.latIdx, maxLatIdx),
    decTileCoord(tileIdx.lonIdx, maxLonIdx)
  )
  // scalastyle:on magic.number
}
