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

package com.simplexportal.spatial.index.grid.tile.actor

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.simplexportal.spatial.index.grid.tile
import com.simplexportal.spatial.index.grid.tile.actor
import com.simplexportal.spatial.index.grid.tile.impl.TileIndex
import com.simplexportal.spatial.model._
import org.scalatest.{Matchers, WordSpecLike}

class TileIndexActorSpec extends ScalaTestWithActorTestKit
    with WordSpecLike
    with Matchers
    with TileIndexActorDataset {

  "Tile Actor" should {

    "add the nodes" in {
      val probeDone = testKit.createTestProbe[ACK]()
      val probeNode = testKit.createTestProbe[GetInternalNodeResponse]()
      val probeMetrics = testKit.createTestProbe[Metrics]()

      val tileActor = testKit.spawn(TileIndexActor("add-nodes-test", "add-nodes-test"), "add-nodes-test")
      tileActor ! AddNode(10, 5, 5, Map("nodeAttrKey" -> "nodeAttrValue"), Some(probeDone.ref))

      tileActor ! GetInternalNode(10, probeNode.ref)
      tileActor ! GetMetrics(probeMetrics.ref)

      probeNode.expectMessage(
        actor.GetInternalNodeResponse(10, Some(TileIndex.InternalNode(10, Location(5, 5), Map(128826956 -> "nodeAttrValue"))))
      )
      probeMetrics.expectMessage(actor.Metrics(0, 1))
    }

    "connect nodes using ways" in {
      val probeWay = testKit.createTestProbe[GetInternalWayResponse]()
      val probeMetrics = testKit.createTestProbe[Metrics]()

      val tileActor = testKit.spawn(actor.TileIndexActor("connect-nodes-using-ways-test", "connect-nodes-using-ways-test"), "connect-nodes-using-ways-test")

      exampleTileCommands foreach (command => tileActor ! command)

      tileActor ! actor.GetMetrics(probeMetrics.ref)
      tileActor ! GetInternalWay(100, probeWay.ref)
      probeMetrics.expectMessage(actor.Metrics(2, 6))
      probeWay.expectMessage(actor.GetInternalWayResponse(100, Some(TileIndex.InternalWay(100, Seq(5, 6, 3), Map(276737215 -> "wayAttrValue")))))
    }

    "create network using blocks" in {
      val probeWay = testKit.createTestProbe[GetInternalWayResponse]()
      val probeMetrics = testKit.createTestProbe[Metrics]()

      val tileActor = testKit.spawn(actor.TileIndexActor("create-network-using-blocks-test", "create-network-using-blocks-test"), "create-network-using-blocks-test")

      tileActor ! AddBatch(exampleTileCommands)

      tileActor ! actor.GetMetrics(probeMetrics.ref)
      tileActor ! actor.GetInternalWay(100, probeWay.ref)
      probeWay.expectMessage(actor.GetInternalWayResponse(100, Some(TileIndex.InternalWay(100, Seq(5, 6, 3), Map(276737215 -> "wayAttrValue")))))

    }

    "retrieve a way from the network" in {
      val probeWay = testKit.createTestProbe[GetWayResponse]()

      val tileActor = testKit.spawn(actor.TileIndexActor("retrieve-way-from-network-test", "retrieve-way-from-network-test"), "retrieve-way-from-network-test")

      tileActor ! actor.AddBatch(exampleTileCommands)

      tileActor ! GetWay(100, probeWay.ref)
      probeWay.expectMessage(actor.GetWayResponse(100, Some(
        Way(
          100,
          Seq(
            Node(5, Location(4, 5)),
            Node(6, Location(2, 5)),
            Node(3, Location(3, 10))
          ),
          Map("wayAttrKey" -> "wayAttrValue")
        )
      )))

      tileActor ! actor.GetWay(10, probeWay.ref)
      probeWay.expectMessage(actor.GetWayResponse(10, None))

    }

  }

}
