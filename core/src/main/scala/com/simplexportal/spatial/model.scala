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

// FIXME: Don't define case classes in a package
package model {

// TODO: Review Geometry definition to add distance and other functions.
//  sealed trait Geometry {
//    def distance(geom: Geometry): Double
//  }

  sealed trait Geometry
  sealed trait AdjacentRepresentation

  object Location {
    val MAX_LONGITUDE = 180
    val MAX_LATITUDE = 90
    val MIN_LONGITUDE = -180
    val MIN_LATITUDE = -90
    val NO_LATITUDE = 91
    val NO_LONGITUDE = 181
    val MAX: Location = Location(MAX_LATITUDE, MAX_LONGITUDE)
    val MIN: Location = Location(MIN_LATITUDE, MIN_LONGITUDE)
    val NaL: Location = Location(NO_LATITUDE, NO_LONGITUDE)
  }

  final case class Line(segments: Seq[Location]) extends Geometry

  final case class LineSegment(start: Location, end: Location) extends Geometry with AdjacentRepresentation

  final case class Location(lat: Double, lon: Double) extends Geometry with AdjacentRepresentation

  object BoundingBox {
    val MAX: BoundingBox = BoundingBox(Location.MIN, Location.MAX)
  }

  final case class BoundingBox(min: Location, max: Location) extends Geometry {

    def toLine(): Line =
      Line(
        Seq(
          Location(max.lat, min.lon),
          Location(max.lat, max.lon),
          Location(min.lat, max.lon),
          Location(min.lat, min.lon),
          Location(max.lat, min.lon)
        )
      )

    def northEdge(): LineSegment =
      LineSegment(Location(max.lat, min.lon), Location(max.lat, max.lon))

    def northEastVertex(): Location = max

    def eastEdge(): LineSegment =
      LineSegment(Location(max.lat, max.lon), Location(min.lat, max.lon))

    def southEastVertex(): Location = Location(min.lat, max.lon)

    def southEdge(): LineSegment =
      LineSegment(Location(min.lat, min.lon), Location(min.lat, max.lon))

    def southWestVertex(): Location = min

    def westEdge(): LineSegment =
      LineSegment(Location(max.lat, min.lon), Location(min.lat, min.lon))

    def northWestVertex(): Location = Location(max.lat, min.lon)

    def clockNeighbours(): Seq[AdjacentRepresentation] = Seq(
      northEdge(),
      northEastVertex(),
      eastEdge(),
      southEastVertex(),
      southEdge(),
      southWestVertex(),
      westEdge(),
      northWestVertex()
    )
  }

  sealed trait Entity

  final case class Node(
      id: Long,
      location: Location,
      attributes: Map[String, String] = Map.empty
  ) extends Entity

  final case class Way(
      id: Long,
      nodes: Seq[Node],
      attributes: Map[String, String] = Map.empty
  ) extends Entity

}
