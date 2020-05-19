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

import com.simplexportal.spatial.model

import scala.util.Try

object TileIdx {
  def apply(entityId: String): Either[String, TileIdx] =
    entityId.split("_") match {
      case Array(latIdx, lonIdx) =>
        Try(TileIdx(latIdx.toInt, lonIdx.toInt)).toEither.left.map(ex =>
          s"Error parsing ${entityId} => ${ex.getMessage}"
        )
      case _ => Left(s"[${entityId}] is not a valid format for a TileIdx")
    }

  def apply(location: model.Location)(implicit tileIdxGen: TileIndexEntityIdGen): TileIdx =
    tileIdxGen.tileIdx(location.lat, location.lon)
}

case class TileIdx(latIdx: Int, lonIdx: Int) {
  def entityId: String = s"${latIdx}_${lonIdx}"

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

  def normalize()(implicit tileIdxGen: TileIndexEntityIdGen): TileIdx =
    TileIdx(
      normalize(latIdx, tileIdxGen.latPartitions),
      normalize(lonIdx, tileIdxGen.lonPartitions)
    )

  @inline private def normalize(idx: Int, partitions: Int): Int =
    idx % partitions match {
      case i if i < 0 => i + partitions
      case i          => i
    }

  // scalastyle:off magic.number
  def bbox()(implicit tileIdxGen: TileIndexEntityIdGen): model.BoundingBox = model.BoundingBox(
    min = model.Location(
      -90 + ((180 / tileIdxGen.latPartitions) * latIdx),
      -180 + ((360 / tileIdxGen.lonPartitions) * lonIdx)
    ),
    max = model.Location(
      -90 + ((180 / tileIdxGen.latPartitions) * (latIdx + 1)),
      -180 + ((360 / tileIdxGen.lonPartitions) * (lonIdx + 1))
    )
  )
  // scalastyle:on magic.number
}
