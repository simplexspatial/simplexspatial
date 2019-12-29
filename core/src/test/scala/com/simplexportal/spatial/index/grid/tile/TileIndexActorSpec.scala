/*
 * Copyright 2019 SimplexPortal Ltd
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

// scalastyle:off magic.number

package com.simplexportal.spatial.index.grid.tile

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.simplexportal.spatial.index.grid.tile
import com.simplexportal.spatial.model._
import org.scalatest.{Matchers, WordSpecLike}

class TileIndexActorSpec extends ScalaTestWithActorTestKit
    with WordSpecLike
    with Matchers
    with TileIndexActorDataset {

  "Tile Actor" should {

    "add the nodes" in {
      val probeDone = testKit.createTestProbe[TileIndexActor.Done]()
      val probeNode = testKit.createTestProbe[TileIndexActor.GetInternalNodeResponse]()
      val probeMetrics = testKit.createTestProbe[TileIndexActor.Metrics]()

      val tileActor = testKit.spawn(TileIndexActor("add-nodes-test", "add-nodes-test"), "add-nodes-test")
      tileActor ! TileIndexActor.AddNode(10, 5, 5, Map("nodeAttrKey" -> "nodeAttrValue"), Some(probeDone.ref))

      tileActor ! TileIndexActor.GetInternalNode(10, probeNode.ref)
      tileActor ! TileIndexActor.GetMetrics(probeMetrics.ref)

      probeNode.expectMessage(
        TileIndexActor.GetInternalNodeResponse(10, Some(TileIndex.InternalNode(10, Location(5, 5), Map(128826956 -> "nodeAttrValue"))))
      )
      probeMetrics.expectMessage(TileIndexActor.Metrics(0, 1))
    }

    "connect nodes using ways" in {
      val probeWay = testKit.createTestProbe[TileIndexActor.GetInternalWayResponse]()
      val probeMetrics = testKit.createTestProbe[TileIndexActor.Metrics]()

      val tileActor = testKit.spawn(tile.TileIndexActor("connect-nodes-using-ways-test", "connect-nodes-using-ways-test"), "connect-nodes-using-ways-test")

      exampleTileCommands foreach (command => tileActor ! command)

      tileActor ! TileIndexActor.GetMetrics(probeMetrics.ref)
      tileActor ! TileIndexActor.GetInternalWay(100, probeWay.ref)
      probeMetrics.expectMessage(TileIndexActor.Metrics(2, 6))
      probeWay.expectMessage(TileIndexActor.GetInternalWayResponse(100, Some(TileIndex.InternalWay(100, 5, Map(276737215 -> "wayAttrValue")))))
    }

    "create network using blocks" in {
      val probeWay = testKit.createTestProbe[TileIndexActor.GetInternalWayResponse]()
      val probeMetrics = testKit.createTestProbe[TileIndexActor.Metrics]()

      val tileActor = testKit.spawn(tile.TileIndexActor("create-network-using-blocks-test", "create-network-using-blocks-test"), "create-network-using-blocks-test")

      tileActor ! TileIndexActor.AddBatch(exampleTileCommands)

      tileActor ! TileIndexActor.GetMetrics(probeMetrics.ref)
      tileActor ! TileIndexActor.GetInternalWay(100, probeWay.ref)
      probeWay.expectMessage(TileIndexActor.GetInternalWayResponse(100, Some(TileIndex.InternalWay(100, 5, Map(276737215 -> "wayAttrValue")))))

    }

  }

}
