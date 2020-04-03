/*
 * Copyright 2020 SimplexPortal Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simplexportal.spatial.utils

import com.simplexportal.spatial.model.{Location, Line => ModelLine, LineSegment => ModelLineSegment}
import org.locationtech.jts.geom.{Coordinate, LineSegment => JTSLineSegment}

/**
  * Useful enrichers to operate with JTS objects.
  */
object ModelEnrichers {
  implicit class ModelLocationEnricher(location: Location) {
    def toJTS(): Coordinate = new Coordinate(location.lon, location.lat)
  }

  implicit class ModelLineSegmentEnricher(line: ModelLineSegment) {
    def toJTS(): JTSLineSegment =
      new JTSLineSegment(line.start.toJTS(), line.end.toJTS())

    def toJTSArrayCoords(): Array[Coordinate] = Array(
      new Coordinate(line.start.lon, line.start.lat),
      new Coordinate(line.end.lon, line.end.lat)
    )
  }

  implicit class ModelLineEnricher(line: ModelLine) {
    def toJTSArrayCoords(): Array[Coordinate] = line.segments.map(_.toJTS()).toArray
  }
}
