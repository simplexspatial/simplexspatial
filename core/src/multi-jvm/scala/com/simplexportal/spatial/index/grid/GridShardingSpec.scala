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
import com.simplexportal.spatial.model.Location
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.implicitConversions

// scalastyle:off magic.number
object GridShardingSpecConfig extends MultiNodeConfig {

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
      akka.loglevel=INFO
      akka.cluster.seed-nodes = [ "akka://GridShardingSpec@localhost:2551" ]
    """).withFallback(ConfigFactory.load()))

}

abstract class GridShardingSpec
  extends MultiNodeSpec(GridShardingSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  implicit val typedSystem = system.toTyped

  import GridShardingSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int =  roles.size

  "The tile index" must {
    println(s"Running System [${system.name}]")

    val gridIndex = system.spawn(Grid("GridIndexTest", 10000, 10000, 10000, 10000), "GridIndex")

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


    "be able to add nodes" in {
      val probe = TestProbe[TileIndexActor.Done]()
      runOn(node0) {
        gridIndex ! TileIndexActor.AddNode(0, -23, -90, Map.empty, Some(probe.ref))
        gridIndex ! TileIndexActor.AddNode(1, 60, 130, Map.empty, Some(probe.ref))
        gridIndex ! TileIndexActor.AddNode(2, -23.3, -90, Map.empty, Some(probe.ref))
        probe.receiveMessages(3, 20.seconds)
      }
      enterBarrier("nodes added")
    }

    "be able to retrieve nodes one per one" in {
      val probe = TestProbe[TileIndexActor.GetInternalNodeResponse]()

      gridIndex ! TileIndexActor.GetInternalNode(999, probe.ref)
      gridIndex ! TileIndexActor.GetInternalNode(0, probe.ref)
      gridIndex ! TileIndexActor.GetInternalNode(1, probe.ref)
      gridIndex ! TileIndexActor.GetInternalNode(2, probe.ref)

      probe.receiveMessages(4, 1.minutes).toSet shouldBe Set(
        TileIndexActor.GetInternalNodeResponse(999, None),
        TileIndexActor.GetInternalNodeResponse(0, Some(TileIndex.InternalNode(0, Location(-23, -90), Map.empty))),
        TileIndexActor.GetInternalNodeResponse(1, Some(TileIndex.InternalNode(1, Location(60, 130), Map.empty))),
        TileIndexActor.GetInternalNodeResponse(2, Some(TileIndex.InternalNode(2, Location(-23.3, -90), Map.empty)))
      )

      enterBarrier("nodes retrieved")
    }

    "be able to retrieve nodes in block" in {
      val probe = TestProbe[TileIndexActor.GetInternalNodesResponse]()

      runOn(node1) {
        gridIndex ! TileIndexActor.AddNode(10, 1, 1, Map.empty, None)
        gridIndex ! TileIndexActor.AddNode(11, 1.000001, 1.000001, Map.empty, None)
        gridIndex ! TileIndexActor.AddNode(12, 1.000002, 1.000002, Map.empty, None)
        gridIndex ! TileIndexActor.GetInternalNodes(Seq(999, 0, 1, 2, 10, 11, 12), probe.ref)

        Seq(
          TileIndexActor.GetInternalNodeResponse(999, None),
          TileIndexActor.GetInternalNodeResponse(0, Some(TileIndex.InternalNode(0, Location(-23, -90), Map.empty))),
          TileIndexActor.GetInternalNodeResponse(1, Some(TileIndex.InternalNode(1, Location(60, 130), Map.empty))),
          TileIndexActor.GetInternalNodeResponse(2, Some(TileIndex.InternalNode(2, Location(-23.3, -90), Map.empty))),
          TileIndexActor.GetInternalNodeResponse(10, Some(TileIndex.InternalNode(10, Location(1, 1), Map.empty))),
          TileIndexActor.GetInternalNodeResponse(11, Some(TileIndex.InternalNode(11, Location(1.000001, 1.000001), Map.empty))),
          TileIndexActor.GetInternalNodeResponse(12, Some(TileIndex.InternalNode(12, Location(1.000002, 1.000002), Map.empty)))
        ) shouldBe(probe.receiveMessage().nodes)
      }

      enterBarrier("nodes retrieved in group")

    }

    "be able to add a ways in different shards" in {
      val probe = TestProbe[TileIndexActor.Done]()
      runOn(node0) {
        gridIndex ! TileIndexActor.AddWay(1, Seq(0, 1, 2, 10, 11, 12), Map.empty, Some(probe.ref))
        probe.receiveMessage()
      }
      enterBarrier("way added")
    }

//    "get right metrics" in within(10.seconds)  {
//      val probe = TestProbe[AnyRef]()
//      println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>. Asking for metrics")
//      gridIndex ! TileIndexActor.GetMetrics(probe.ref)
//      probe.expectMessage(TileIndexActor.Metrics(1,3))
//      enterBarrier("tested metrics in all nodes")
//    }

  }
}

class GridShardingSpecMultiJvmNode0 extends GridShardingSpec
class GridShardingSpecMultiJvmNode1 extends GridShardingSpec
class GridShardingSpecMultiJvmNode2 extends GridShardingSpec
