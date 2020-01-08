/*
 * Copyright 2019 SimplexPortal Ltd
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

package com.simplexportal.spatial

package model {

  object Location {
    val MAX_LONGITUDE = 180
    val MAX_LATITUDE = 90
    val MIN_LONGITUDE = -180
    val MIN_LATITUDE = -90
    val NO_LATITUDE = 91
    val NO_LONGITUDE = 181
    val MAX = Location(MAX_LATITUDE, MAX_LONGITUDE)
    val MIN = Location(MIN_LATITUDE, MIN_LONGITUDE)
    val NaL = Location(NO_LATITUDE, NO_LONGITUDE)
  }

  case class Location(lat: Double, lon: Double)

  object BoundingBox {
    val MAX = BoundingBox(Location.MIN, Location.MAX)
  }

  case class BoundingBox(min: Location, max: Location)

  case class Node(
      id: Long,
      location: Location,
      attributes: Map[String, String] = Map.empty
  )

  case class Way(
      id: Long,
      nodes: Seq[Node],
      attributes: Map[String, String] = Map.empty
  )

}
