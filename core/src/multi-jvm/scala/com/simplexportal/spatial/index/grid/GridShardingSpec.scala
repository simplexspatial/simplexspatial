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
import com.simplexportal.spatial.index.grid.GridProtocol._
import com.simplexportal.spatial.model.{Location, Node, Way}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

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

  commonConfig(
    ConfigFactory
      .parseString(
        s"""
      akka.cluster.seed-nodes = [ "akka://GridShardingSpec@localhost:2551" ]
      akka.persistence.snapshot-store.local.dir = "target/snapshots/GridShardingSpec"
    """
      )
      .withFallback(ConfigFactory.load("application-default-multijvm.conf"))
  )

}

abstract class GridShardingSpec
    extends MultiNodeSpec(GridShardingSpecConfig)
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  implicit val typedSystem = system.toTyped

  import GridShardingSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int = roles.size

  "The tile index" must {
    println(s"Running System [${system.name}]")

    val gridIndex = system.spawn(
      Grid(GridConfig("GridIndexTest", 10000, 10000, 10000, 10000)),
      "GridIndex"
    )

    "wait until all nodes are ready" in {

      Cluster(system).subscribe(testActor, classOf[MemberUp])
      expectMsgClass(classOf[CurrentClusterState])

      Cluster(system) join node(node1).address

      receiveN(3).collect { case MemberUp(m) => m.address }.toSet should be(
        Set(node(node0).address, node(node1).address, node(node2).address)
      )

      Cluster(system).unsubscribe(testActor)

      enterBarrier("all-up")
    }

//    "be able to add nodes" in {
//      val probe = TestProbe[GridACK]()
//      runOn(node0) {
//        gridIndex ! GridAddNode(0, -23, -90, Map.empty, Some(probe.ref))
//        gridIndex ! GridAddNode(1, 60, 130, Map.empty, Some(probe.ref))
//        gridIndex ! GridAddNode(2, -23.3, -90, Map.empty, Some(probe.ref))
//        probe.receiveMessages(3, 20.seconds)
//      }
//      enterBarrier("nodes added")
//    }

    "be able to retrieve nodes one per one" in {
      val probe = TestProbe[GridGetNodeReply]()

      gridIndex ! GridGetNode(999, probe.ref)
      gridIndex ! GridGetNode(0, probe.ref)
      gridIndex ! GridGetNode(1, probe.ref)
      gridIndex ! GridGetNode(2, probe.ref)

      probe.receiveMessages(4).toSet shouldBe Set(
        GridGetNodeReply(Right(None)),
        GridGetNodeReply(Right(Some(Node(0, Location(-23, -90), Map.empty)))),
        GridGetNodeReply(Right(Some(Node(1, Location(60, 130), Map.empty)))),
        GridGetNodeReply(Right(Some(Node(2, Location(-23.3, -90), Map.empty))))
      )

      enterBarrier("nodes retrieved")
    }

//    "be able to add a ways in different shards" in {
//      val probe = TestProbe[GridACK]()
//      runOn(node0) {
//        // Adding extra nodes to build larger way.
//        gridIndex ! GridAddNode(10, 1, 1, Map.empty, Some(probe.ref))
//        gridIndex ! GridAddNode(11, 1.000001, 1.000001, Map.empty, Some(probe.ref))
//        gridIndex ! GridAddNode(12, 1.000002, 1.000002, Map.empty, Some(probe.ref))
//        probe.receiveMessages(3)
//
//        gridIndex ! GridAddWay(
//          1,
//          Seq(0, 1, 2, 10, 11, 12),
//          Map.empty,
//          Some(probe.ref)
//        )
//        probe.expectMessage(GridDone())
//
//      }
//      enterBarrier("way added")
//    }

    "retrieve way from multiple shards" in {
      val probe = TestProbe[GridGetWayReply]()

      gridIndex ! GridGetWay(1, probe.ref)
      probe.expectMessage(
        GridGetWayReply(
          Right(
            Some(
              Way(
                1,
                Seq(
                  Node(0, Location(-23.0, -90.0), Map()),
                  Node(1, Location(60.0, 130.0), Map()),
                  Node(2, Location(-23.3, -90.0), Map()),
                  Node(10, Location(1.0, 1.0), Map()),
                  Node(11, Location(1.000001, 1.000001), Map()),
                  Node(12, Location(1.000002, 1.000002), Map())
                ),
                Map.empty
              )
            )
          )
        )
      )

      enterBarrier("way retrieved from different shards")
    }

    "return None if data is not there" in {
      val probe = TestProbe[GridGetWayReply]()
      runOn(node1) {
        gridIndex ! GridGetWay(999, probe.ref)
        GridGetWayReply(Right(None)) shouldBe probe.receiveMessage()
      }
      enterBarrier("no data found")
    }

//    "be able to add nodes and ways in different shards using batched commands" in {
//      val probe = TestProbe[GridACK]()
//      runOn(node0) {
//        gridIndex ! GridAddBatch(
//          Seq(
//            GridAddNode(130, -23, -90, Map.empty),
//            GridAddNode(140, 60, 130, Map.empty),
//            GridAddNode(150, -23.3, -90, Map.empty),
//            GridAddWay(2, Seq(11, 130, 140, 150), Map.empty)
//          ),
//          Some(probe.ref)
//        )
//
//        probe.expectMessage(GridDone())
//
//        val probeGetWay = TestProbe[GridGetWayReply]
//        gridIndex ! GridGetWay(2, probeGetWay.ref)
//        probeGetWay.expectMessage(
//          GridGetWayReply(
//            Right(
//              Some(
//                Way(
//                  2,
//                  Seq(
//                    Node(11, Location(1.000001, 1.000001), Map()),
//                    Node(130, Location(-23, -90), Map()),
//                    Node(140, Location(60, 130), Map()),
//                    Node(150, Location(-23.3, -90), Map())
//                  ),
//                  Map.empty
//                )
//              )
//            )
//          )
//        )
//
//      }
//      enterBarrier("batch commands executed")
//    }

//    "get right metrics" in within(10.seconds)  {
//      val probe = TestProbe[AnyRef]()
//      println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>. Asking for metrics")
//      gridIndex ! tile.GetMetrics(probe.ref)
//      probe.expectMessage(tile.Metrics(1,3))
//      enterBarrier("tested metrics in all nodes")
//    }

  }
}

class GridShardingSpecMultiJvmNode0 extends GridShardingSpec
class GridShardingSpecMultiJvmNode1 extends GridShardingSpec
class GridShardingSpecMultiJvmNode2 extends GridShardingSpec
