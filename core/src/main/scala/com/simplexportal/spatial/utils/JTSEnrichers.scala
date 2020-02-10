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

import com.simplexportal.spatial.model.{LineSegment, Location}
import org.locationtech.jts.geom.{Coordinate, LineSegment => JTSLineSegment}

/**
  * Useful enrichers to operate with JTS objects.
  */
object JTSEnrichers {
  implicit class ModelCoordinateEnricher(coord: Coordinate) {
    def toSpl(): Location = Location(coord.y, coord.x)
  }

  implicit class ModelLineSegmentEnricher(line: JTSLineSegment) {
    def toSpl(): LineSegment = LineSegment(line.p0.toSpl(), line.p1.toSpl())
  }
}
