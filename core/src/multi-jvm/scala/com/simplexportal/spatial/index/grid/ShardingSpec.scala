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

package com.simplexportal.spatial.index.grid

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

// scalastyle:off magic.number
object ShardingSpecConfig extends MultiNodeConfig {

  val nodes = Map(
    "node0" -> role("node0"),
    "node1" -> role("node1"),
    "node2" -> role("node2")
  )

  nodeConfig(nodes("node0"))(
    ConfigFactory.parseString("""
    akka.remote.artery.canonical.port = 2551
    """)
  )

  nodeConfig(nodes("node1"))(
    ConfigFactory.parseString("""
    akka.remote.artery.canonical.port = 2552
    """)
  )

  nodeConfig(nodes("node2"))(
    ConfigFactory.parseString("""
    akka.remote.artery.canonical.port = 2553
    """)
  )

  commonConfig(ConfigFactory.parseString("""
      akka.loglevel=ERROR
      akka.actor.provider = cluster
      akka.remote.artery.enabled = on
      akka.cluster.seed-nodes = [ "akka://ClusterSystem@127.0.0.1:2551", "akka://ClusterSystem@127.0.0.1:2552" ]
    """).withFallback(ConfigFactory.load()))

}

abstract class ShardingSpec
  extends MultiNodeSpec(ShardingSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  implicit val typedSystem = system.toTyped

  import ShardingSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int =  roles.size

  "The tile index" must {
    "wait until all nodes are ready" in within(10.seconds) {

      Cluster(system).subscribe(testActor, classOf[MemberUp])
      expectMsgClass(classOf[CurrentClusterState])

      Cluster(system) join node(nodes("node1")).address

      receiveN(3).collect { case MemberUp(m) => m.address }.toSet should be(
        nodes.values.map(node(_).address).toSet
      )

      Cluster(system).unsubscribe(testActor)

      enterBarrier("all-up")
    }

    "be able to add a entities in the a local tile" in within(10.seconds)  {
      runOn(nodes("node0")) {
        val probe = TestProbe[AnyRef]()
        val localTileActor = system.spawn(TileActor("IndexTestTile", "FIXED_INDEX_TEST_NODE0"), "TileActorNode0")
        localTileActor ! TileActor.AddNode(0, 0, 0, Map.empty, Some(probe.ref))
        localTileActor ! TileActor.AddNode(1, 1, 1, Map.empty, Some(probe.ref))
        localTileActor ! TileActor.AddNode(2, 2, 2, Map.empty, Some(probe.ref))
        localTileActor ! TileActor.AddWay(1, Seq(0, 1, 2), Map.empty, Some(probe.ref))

        probe.receiveMessages(4)
      }
      enterBarrier("added locally")
    }

    "be able to add a entities in the a remote tile" in within(10.seconds)  {
      runOn(nodes("node2")) {
        val probe = TestProbe[AnyRef]()
        val remoteTileActor = system.actorSelection(node(nodes("node0")) / "user" / "TileActorNode0")

        remoteTileActor ! TileActor.AddNode(10, 10, 10, Map.empty, Some(probe.ref))
        remoteTileActor ! TileActor.AddNode(11, 11, 11, Map.empty, Some(probe.ref))
        remoteTileActor ! TileActor.AddNode(12, 12, 12, Map.empty, Some(probe.ref))
        remoteTileActor ! TileActor.AddWay(11, Seq(10, 11, 12), Map.empty, Some(probe.ref))

        probe.receiveMessages(4)
      }

      enterBarrier("added remotely")
    }

    "retrieve metrics from the remote actor" in {
      runOn(nodes("node2")) {
        val probe = TestProbe[AnyRef]()
        val remoteTileActor = system.actorSelection(node(nodes("node0")) / "user" / "TileActorNode0")
        remoteTileActor ! TileActor.GetMetrics(probe.ref)

        probe.expectMessage(TileActor.Metrics(2,6))

      }
    }

  }
}

class ShardingSpecMultiJvmNode0 extends ShardingSpec
class ShardingSpecMultiJvmNode1 extends ShardingSpec
class ShardingSpecMultiJvmNode2 extends ShardingSpec
