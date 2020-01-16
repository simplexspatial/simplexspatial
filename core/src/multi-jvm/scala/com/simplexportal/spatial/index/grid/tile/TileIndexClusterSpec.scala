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

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.implicitConversions

import com.simplexportal.spatial.index.grid.tile

/**
 * Testing a single tile in a cluster. On this way, it is possible to verify aspects like serialization.
 */
object TileIndexClusterSpecConfig extends MultiNodeConfig {

  val node0 = role("node0")
  val node1 = role("node1")
  val node2 = role("node2")

  nodeConfig(node0)(
    ConfigFactory.parseString("""
    akka.remote.artery.canonical.port = 2551
    """)
  )

  nodeConfig(node1)(
    ConfigFactory.parseString("""
    akka.remote.artery.canonical.port = 2552
    """)
  )

  nodeConfig(node2)(
    ConfigFactory.parseString("""
    akka.remote.artery.canonical.port = 2553
    """)
  )

  commonConfig(ConfigFactory.parseString("""
      akka.loglevel=ERROR
      akka.cluster.seed-nodes = [ "akka://TileIndexClusterSpec@localhost:2551" ]
    """).withFallback(ConfigFactory.load()))

}

abstract class TileIndexClusterSpec
  extends MultiNodeSpec(TileIndexClusterSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  implicit val typedSystem = system.toTyped

  import TileIndexClusterSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int =  roles.size

  "The tile index" must {
    println(s"Running System [${system.name}]")

    "wait until all nodes are ready" in within(10.seconds) {

      Cluster(system).subscribe(testActor, classOf[MemberUp])
      expectMsgClass(classOf[CurrentClusterState])

      Cluster(system) join node(node1).address

      receiveN(3).collect { case MemberUp(m) => m.address }.toSet should be(
        Set(node(node0).address, node(node1).address, node(node2).address)
      )

      Cluster(system).unsubscribe(testActor)

      enterBarrier("all-up")
    }

    "be able to add a entities in the a local tile" in within(10.seconds)  {
      runOn(node0) {
        val probe = TestProbe[AnyRef]()
        val localTileActor = system.spawn(TileIndexActor("IndexTestTile", "FIXED_INDEX_TEST_NODE0"), "TileActorNode0")
        localTileActor ! tile.AddNode(0, 0, 0, Map.empty, Some(probe.ref))
        localTileActor ! tile.AddNode(1, 1, 1, Map.empty, Some(probe.ref))
        localTileActor ! tile.AddNode(2, 2, 2, Map.empty, Some(probe.ref))
        localTileActor ! tile.AddWay(1, Seq(0, 1, 2), Map.empty, Some(probe.ref))

        probe.receiveMessages(4)
      }
      enterBarrier("added locally")
    }

    "be able to add a entities in the a remote tile" in within(10.seconds)  {
      runOn(node2) {
        val probe = TestProbe[AnyRef]()
        val remoteTileActor = system.actorSelection(node(node0) / "user" / "TileActorNode0")

        remoteTileActor ! tile.AddNode(10, 10, 10, Map.empty, Some(probe.ref))
        remoteTileActor ! tile.AddNode(11, 11, 11, Map.empty, Some(probe.ref))
        remoteTileActor ! tile.AddNode(12, 12, 12, Map.empty, Some(probe.ref))
        remoteTileActor ! tile.AddWay(11, Seq(10, 11, 12), Map.empty, Some(probe.ref))

        probe.receiveMessages(4)
      }

      enterBarrier("added remotely")
    }

    "retrieve metrics from the remote actor" in {
      runOn(node2) {
        val probe = TestProbe[AnyRef]()
        val remoteTileActor = system.actorSelection(node(node0) / "user" / "TileActorNode0")
        remoteTileActor ! tile.GetMetrics(probe.ref)

        probe.expectMessage(tile.Metrics(2,6))

      }
    }

  }
}

class TileIndexClusterSpecMultiJvmNode0 extends TileIndexClusterSpec
class TileIndexClusterSpecMultiJvmNode1 extends TileIndexClusterSpec
class TileIndexClusterSpecMultiJvmNode2 extends TileIndexClusterSpec
