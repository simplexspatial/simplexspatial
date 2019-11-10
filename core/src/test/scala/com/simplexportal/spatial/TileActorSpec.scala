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

// scalastyle:off magic.number

package com.simplexportal.spatial

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.simplexportal.spatial.Tile.{Node, Way}
import com.simplexportal.spatial.model._
import org.scalatest.{Matchers, WordSpecLike}

class TileActorSpec extends ScalaTestWithActorTestKit
    with WordSpecLike
    with Matchers
    with TileActorDataset {

  "Tile Actor" should {

    "add the nodes" in {
      val probeDone = testKit.createTestProbe[TileActor.Done]()
      val probeNode = testKit.createTestProbe[Option[Node]]()
      val probeMetrics = testKit.createTestProbe[TileActor.Metrics]()

      val tileActor = testKit.spawn(TileActor("add-nodes-test", bbox), "add-nodes-test")
      tileActor ! TileActor.AddNode(10, 5, 5, Map("nodeAttrKey" -> "nodeAttrValue"), Some(probeDone.ref))

      tileActor ! TileActor.GetNode(10, probeNode.ref)
      tileActor ! TileActor.GetMetrics(probeMetrics.ref)

      probeNode.expectMessage(
        Some(Node(10, Location(5, 5), Map(128826956 -> "nodeAttrValue")))
      )
      probeMetrics.expectMessage(TileActor.Metrics(0, 1))
    }

    "connect nodes using ways" in {
      val probeWay = testKit.createTestProbe[Option[Way]]()
      val probeMetrics = testKit.createTestProbe[TileActor.Metrics]()

      val tileActor = testKit.spawn(TileActor("connect-nodes-using-ways-test", bbox), "connect-nodes-using-ways-test")

      exampleTileCommands foreach (command => tileActor ! command)

      tileActor ! TileActor.GetMetrics(probeMetrics.ref)
      tileActor ! TileActor.GetWay(100, probeWay.ref)
      probeMetrics.expectMessage(TileActor.Metrics(2, 6))
      probeWay.expectMessage(Some(Way(100, 5, Map(276737215 -> "wayAttrValue"))))
    }

    "create network using blocks" in {
      val probeWay = testKit.createTestProbe[Option[Way]]()
      val probeMetrics = testKit.createTestProbe[TileActor.Metrics]()

      val tileActor = testKit.spawn(TileActor("create-network-using-blocks-test", bbox), "create-network-using-blocks-test")

      tileActor ! TileActor.AddBatch(exampleTileCommands)

      tileActor ! TileActor.GetMetrics(probeMetrics.ref)
      tileActor ! TileActor.GetWay(100, probeWay.ref)
      probeMetrics.expectMessage(TileActor.Metrics(2, 6))
      probeWay.expectMessage(Some(Way(100, 5, Map(276737215 -> "wayAttrValue"))))

    }

  }

}
