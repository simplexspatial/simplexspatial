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

package com.simplexportal.spatial.index.grid.sessions

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.simplexportal.spatial.index.grid.{Grid, GridConfig}
import com.simplexportal.spatial.index.grid.tile.impl.TileIndex
import com.simplexportal.spatial.index.protocol.{GridAddNode, GridGetNodeReply}
import com.simplexportal.spatial.model.{Location, Node, Way}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.implicitConversions

// scalastyle:off magic.number
object GetNearestNodeSessionSpecConfig extends MultiNodeConfig {

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
      akka.cluster.seed-nodes = [ "akka://GetNearestNodeSessionSpec@localhost:2551" ]
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
    """).withFallback(ConfigFactory.load()))

}

abstract class GetNearestNodeSessionSpec
  extends MultiNodeSpec(GetNearestNodeSessionSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  implicit val typedSystem = system.toTyped

  import GetNearestNodeSessionSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int =  roles.size

  "Search the nearest node" must {
    val gridIndex = system.spawn(Grid(GridConfig("GridIndexTest", 10000, 10000, 10000, 10000)), "GridIndex")

    "prepare the cluster for testing" in within(10.seconds) {

      Cluster(system).subscribe(testActor, classOf[MemberUp])
      expectMsgClass(classOf[CurrentClusterState])

      Cluster(system) join node(node1).address

      receiveN(3).collect { case MemberUp(m) => m.address }.toSet should be(
        Set(node(node0).address, node(node1).address, node(node2).address)
      )

      Cluster(system).unsubscribe(testActor)

      enterBarrier("all-up")

      runOn(node1) {
        val probe = TestProbe[GridGetNodeReply]()

        gridIndex ! GridAddNode(10, 1, 1, Map.empty, None)
        gridIndex ! GridAddNode(11, 1.000001, 1.000001, Map.empty, None)
        gridIndex ! GridAddNode(12, 1.000002, 1.000002, Map.empty, None)
        gridIndex ! GridAddNode(13, 1.000002, 1.000002, Map.empty, None)
        gridIndex ! GridGetNodes(Seq(999, 10, 11, 12, 13), probe.ref)

        Seq(
          actor.GetInternalNodeResponse(999, None),
          actor.GetInternalNodeResponse(10, Some(TileIndex.InternalNode(10, Location(1, 1), Map.empty))),
          actor.GetInternalNodeResponse(11, Some(TileIndex.InternalNode(11, Location(1.000001, 1.000001), Map.empty))),
          actor.GetInternalNodeResponse(12, Some(TileIndex.InternalNode(12, Location(1.000002, 1.000002), Map.empty))),
          actor.GetInternalNodeResponse(13, Some(TileIndex.InternalNode(12, Location(1.000002, 1.000002), Map.empty)))
        ) should be (probe.receiveMessage().nodes)
      }

      enterBarrier("nodes added")
    }


    "return the node in the same location that the origin" in {

      runOn(node1) {

      }

      enterBarrier("nodes retrieved in group")

    }

//    "be able to add a ways in different shards" in {
//      val probe = TestProbe[ACK]()
//      runOn(node0) {
//        gridIndex ! AddWay(1, Seq(0, 1, 2, 10, 11, 12), Map.empty, Some(probe.ref))
//        probe.receiveMessage()
//      }
//      enterBarrier("way added")
//    }
//
//    "retrieve way from multiple shards" in {
//      val probe = TestProbe[GetWayResponse]()
//      runOn(node0) {
//        gridIndex ! GetWay(1, probe.ref)
//        GetWayResponse(1,Some(
//          Way(1, Seq(
//            Node(0,Location(-23.0,-90.0),Map()),
//            Node(1,Location(60.0,130.0),Map()),
//            Node(2,Location(-23.3,-90.0),Map()),
//            Node(10,Location(1.0,1.0),Map()), Node(11,Location(1.000001,1.000001),Map()), Node(12,Location(1.000002,1.000002),Map())
//          ), Map.empty)
//        )) shouldBe probe.receiveMessage()
//      }
//      enterBarrier("way retrieved from different shards")
//    }

    "return None if data is not there" in {
      val probe = TestProbe[GetWayResponse]()
      runOn(node1) {
        gridIndex ! actor.GetWay(999, probe.ref)
        GetWayResponse(999,None) shouldBe probe.receiveMessage()
      }
      enterBarrier("no data found")
    }

  }
}

class GetNearestNodeSessionSpecMultiJvmNode0 extends GetNearestNodeSessionSpec
class GetNearestNodeSessionSpecMultiJvmNode1 extends GetNearestNodeSessionSpec
class GetNearestNodeSessionSpecMultiJvmNode2 extends GetNearestNodeSessionSpec
