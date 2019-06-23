package com.simplexportal.spatial

import com.simplexportal.spatial.Tile.{Node, Way}
import com.simplexportal.spatial.model._
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.immutable.LongMap
import scala.util.Random

// scalastyle:off magic.number

class TileSpec extends WordSpecLike with Matchers {

  "Tile" should {

    "create network" in {

      val expected = Tile(
        nodes = LongMap(
          1L -> Node(1, Location(7, 3), Map("traffic_light" -> "true"), Set(101), Set(2), Set(2)),
          2L -> Node(2, Location(7, 10), Map.empty,  Set(101), Set(1,3), Set(1,3)),
          3L -> Node(3, Location(3, 10), Map.empty,  Set(101, 100), Set(2,6,4), Set(2,6,4)),
          4L -> Node(4, Location(3, 16), Map.empty,  Set(101), Set(3), Set(3)),
          5L -> Node(5, Location(4,5), Map.empty, Set(100), Set(6), Set(6)),
          6L -> Node(6, Location(2,5), Map.empty, Set(100), Set(3,5), Set(3,5))
        ),
        ways = LongMap(
          100L -> Way(100, 5, Map("name" -> "Street Name")),
          101L -> Way(101, 1)
        )
      )

      val current = Tile()
        .addNode(2, 7, 10, Map.empty)
        .addNode(1, 7, 3, Map("traffic_light" -> "true"))
        .addNode(3, 3, 10, Map.empty)
        .addNode(4, 3,16, Map.empty)
        .addNode(5, 4, 5, Map.empty)
        .addNode(6, 2,5, Map.empty)
        .addWay(100, Seq(5,6,3), Map("name" -> "Street Name"))
        .addWay(101, Seq(1,2,3,4), Map.empty)

      current should be(expected)
      current.nodes.size should be(6)
      current.ways.size should be(2)
    }

  }
}
