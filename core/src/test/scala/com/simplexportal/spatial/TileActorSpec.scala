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

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.simplexportal.spatial.TileActor._
import com.simplexportal.spatial.Tile.{Node, Way}
import com.simplexportal.spatial.model._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class TileActorSpec
    extends TestKit(ActorSystem("TileActorSpec"))
    with ImplicitSender
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll
    with RTreeActorDataset {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
    File("target/journal").delete(true)
  }

  "RTree Actor" should {

    "add the nodes" in {
      val rTreeActor = system.actorOf(TileActor.props("add-nodes-test", bbox))
      rTreeActor ! AddNode(10, 5, 5, Map("nodeAttrKey" -> "nodeAttrValue"))

      rTreeActor ! GetNode(10)
      rTreeActor ! GetMetrics

      expectMsg(akka.Done)
      expectMsg(
        Some(Node(10, Location(5, 5), Map(10 -> "nodeAttrValue")))
      )
      expectMsg(Metrics(0, 1))
    }

    "connect nodes using ways" in {
      val rTreeActor =
        system.actorOf(TileActor.props("connect-nodes-using-ways-test", bbox))

      exampleTileCommands foreach (command => rTreeActor ! command)

      ignoreMsg { case msg => msg == akka.Done }

      rTreeActor ! GetMetrics
      rTreeActor ! GetWay(100)
      expectMsg(Metrics(2, 6))
      expectMsg(Some(Way(100, 5, Map(10 -> "wayAttrValue"))))
    }

  }

}
