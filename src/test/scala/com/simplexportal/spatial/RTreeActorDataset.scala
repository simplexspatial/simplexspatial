package com.simplexportal.spatial

import com.simplexportal.spatial.RTreeActor.{AddNode, AddWay, RTreeCommands}
import com.simplexportal.spatial.model.{BoundingBox, Location}

trait RTreeActorDataset {
  val bbox = BoundingBox(Location(1,1), Location(10,10))

  val exampleTileCommands: Seq[RTreeCommands] = Seq(
    AddNode(1, 7,3, Map("nodeAttrKey" -> "nodeAttrValue")),
    AddNode(2, 7,10, Map.empty),
    AddNode(3, 3, 10, Map.empty),
    AddNode(4, 3, 16, Map.empty),
    AddNode(5, 4, 5, Map.empty),
    AddNode(6, 2, 5, Map.empty),
    AddWay(100, Seq(5,6,3), Map("wayAttrKey" -> "wayAttrValue") ),
    AddWay(101, Seq(1,2,3,4), Map.empty ),
  )
}
