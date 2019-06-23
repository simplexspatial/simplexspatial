/*
 * Copyright (C) 2019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

package object model {

  type Attributes = Map[String, String]

  case class Location(lon: Double, lat: Double, alt: Double = 0)

  case class BoundingBox(min: Location, max: Location)

}
