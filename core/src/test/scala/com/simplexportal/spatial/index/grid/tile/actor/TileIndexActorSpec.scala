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
import com.simplexportal.spatial.index.grid.tile.impl.TileIndex
import com.simplexportal.spatial.model
import com.simplexportal.spatial.index.grid.tile.actor.{TileIndexProtocol => protocol}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class TileIndexActorSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with TileIndexActorDataset {

  "Tile Actor" should {

    "add the nodes" in {
      val probeDone = testKit.createTestProbe[protocol.ACK]()
      val probeInternalNode = testKit.createTestProbe[protocol.GetInternalNodeResponse]()
      val probeNode = testKit.createTestProbe[protocol.GetNodeResponse]()
      val probeMetrics = testKit.createTestProbe[protocol.Metrics]()

      val tileActor = testKit.spawn(
        TileIndexActor("add-nodes-test", "add-nodes-test"),
        "add-nodes-test"
      )
      tileActor ! protocol.AddNode(
        10,
        5,
        5,
        Map("nodeAttrKey" -> "nodeAttrValue"),
        Some(probeDone.ref)
      )

      tileActor ! protocol.GetInternalNode(10, probeInternalNode.ref)
      tileActor ! protocol.GetNode(10, probeNode.ref)
      tileActor ! protocol.GetMetrics(probeMetrics.ref)

      probeInternalNode.expectMessage(
        protocol.GetInternalNodeResponse(
          10,
          Some(
            TileIndex.InternalNode(
              10,
              model.Location(5, 5),
              Map(128826956 -> "nodeAttrValue")
            )
          )
        )
      )
      probeNode.expectMessage(
        protocol.GetNodeResponse(
          10,
          Some(
            model.Node(
              10,
              model.Location(5, 5),
              Map("nodeAttrKey" -> "nodeAttrValue")
            )
          )
        )
      )

      probeMetrics.expectMessage(protocol.Metrics(0, 1))
    }

    "connect nodes using ways" in {
      val probeWay = testKit.createTestProbe[protocol.GetInternalWayResponse]()
      val probeMetrics = testKit.createTestProbe[protocol.Metrics]()

      val tileActor = testKit.spawn(
        TileIndexActor(
          "connect-nodes-using-ways-test",
          "connect-nodes-using-ways-test"
        ),
        "connect-nodes-using-ways-test"
      )

      exampleTileCommands foreach (command => tileActor ! command)

      tileActor ! protocol.GetMetrics(probeMetrics.ref)
      tileActor ! protocol.GetInternalWay(100, probeWay.ref)
      probeMetrics.expectMessage(protocol.Metrics(2, 6))
      probeWay.expectMessage(
        protocol.GetInternalWayResponse(
          100,
          Some(
            TileIndex
              .InternalWay(100, Seq(5, 6, 3), Map(276737215 -> "wayAttrValue"))
          )
        )
      )
    }

    "create network using blocks" in {
      val probeWay = testKit.createTestProbe[protocol.GetInternalWayResponse]()
      val probeMetrics = testKit.createTestProbe[protocol.Metrics]()

      val tileActor = testKit.spawn(
        TileIndexActor(
          "create-network-using-blocks-test",
          "create-network-using-blocks-test"
        ),
        "create-network-using-blocks-test"
      )

      tileActor ! protocol.AddBatch(exampleTileCommands)

      tileActor ! protocol.GetMetrics(probeMetrics.ref)
      tileActor ! protocol.GetInternalWay(100, probeWay.ref)
      probeWay.expectMessage(
        protocol.GetInternalWayResponse(
          100,
          Some(
            TileIndex
              .InternalWay(100, Seq(5, 6, 3), Map(276737215 -> "wayAttrValue"))
          )
        )
      )

    }

    "retrieve a way from the network" in {
      val probeWay = testKit.createTestProbe[protocol.GetWayResponse]()

      val tileActor = testKit.spawn(
        TileIndexActor(
          "retrieve-way-from-network-test",
          "retrieve-way-from-network-test"
        ),
        "retrieve-way-from-network-test"
      )

      tileActor ! protocol.AddBatch(exampleTileCommands)

      tileActor ! protocol.GetWay(100, probeWay.ref)
      probeWay.expectMessage(
        protocol.GetWayResponse(
          100,
          Some(
            model.Way(
              100,
              Seq(
                model.Node(5, model.Location(4, 5)),
                model.Node(6, model.Location(2, 5)),
                model.Node(3, model.Location(3, 10))
              ),
              Map("wayAttrKey" -> "wayAttrValue")
            )
          )
        )
      )

      tileActor ! protocol.GetWay(10, probeWay.ref)
      probeWay.expectMessage(protocol.GetWayResponse(10, None))

    }

  }

}
