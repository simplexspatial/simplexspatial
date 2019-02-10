package com.simplexportal.spatial

object Model {

  case class Location(lon: Float, lat: Float)

  case class BoundingBox(min: Location, max: Location)

  type Attributes = Map[Long, Any]

  case class Node(
    id: Long,
    location: Location,
    attributes: Attributes
  )

  case class Edge(
    id: Long,
    attributes: Attributes,
    source: Long,
    target: Long
  )

}


