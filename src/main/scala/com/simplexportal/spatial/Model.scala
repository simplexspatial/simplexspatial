/*
 * Copyright (C) 20019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

object Model {

  case class Location(lon: Float, lat: Float)

  case class BoundingBox(min: Location, max: Location)

  type Attributes = Map[Long, Any]

  case class Node(
    id: Long,
    location: Location,
    attributes: Attributes,
    edges: Set[Edge] = Set.empty
  )

  case class Edge(
    id: Long,
    source: Long,
    target: Long,
    attributes: Attributes
  )

}


