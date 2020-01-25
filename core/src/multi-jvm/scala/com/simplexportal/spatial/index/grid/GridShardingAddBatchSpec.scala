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
import com.simplexportal.spatial.index.grid.tile.GetWayResponse
import com.simplexportal.spatial.model.{Location, Node, Way}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.implicitConversions

// scalastyle:off magic.number
object GridShardingAddBatchSpecConfig extends MultiNodeConfig {

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
    ConfigFactory.parseString("""
      akka.loglevel=INFO
      akka.cluster.seed-nodes = [ "akka://GridShardingAddBatchSpec@localhost:2551" ]
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
    """).withFallback(ConfigFactory.load())
  )

}

abstract class GridShardingAddBatchSpec
    extends MultiNodeSpec(GridShardingAddBatchSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  implicit val typedSystem = system.toTyped

  import GridShardingAddBatchSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int = roles.size

  "The batch command in the tile index" must {
    println(s"Running System [${system.name}]")

    val gridIndex = system.spawn(
      Grid("GridAddBatchIndexTest", 10000, 10000, 10000, 10000),
      "GridAddBatchIndex"
    )

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

    "be able to add nodes and ways" in {
      val probe = TestProbe[tile.ACK]()
      runOn(node0) {

        gridIndex ! tile.AddBatch(
          Seq(
            tile.AddNode(100, -23, -90, Map.empty),
            tile.AddNode(101, 60, 130, Map.empty),
            tile.AddNode(102, -23.3, -90, Map.empty),
            tile.AddNode(110, 1, 1, Map.empty, None),
            tile.AddNode(111, 1.000001, 1.000001, Map.empty, None),
            tile.AddNode(112, 1.000002, 1.000002, Map.empty, None),
            tile.AddWay(101, Seq(100, 101, 102, 110, 111, 112), Map.empty, None)
          ),
          Some(probe.ref)
        )
        probe.receiveMessages(1, 20.seconds)
      }
      enterBarrier("nodes added")
    }

    "return None if way is not there" in {
      val probe = TestProbe[tile.GetWayResponse]()
      runOn(node1) {
        gridIndex ! tile.GetWay(999, probe.ref)
        GetWayResponse(999, None) shouldBe probe.receiveMessage()
      }
      enterBarrier("no data found")
    }

    "return the way if it is there" in {
      val probe = TestProbe[tile.GetWayResponse]()
      runOn(node1) {
        gridIndex ! tile.GetWay(101, probe.ref)
        GetWayResponse(
          101,
          Some(
            Way(
              101,
              Seq(
                Node(100, Location(-23.0, -90.0), Map()),
                Node(101, Location(60.0, 130.0), Map()),
                Node(102, Location(-23.3, -90.0), Map()),
                Node(110, Location(1.0, 1.0), Map()),
                Node(111, Location(1.000001, 1.000001), Map()),
                Node(112, Location(1.000002, 1.000002), Map())
              ),
              Map()
            )
          )
        ) shouldBe probe.receiveMessage()
      }
      enterBarrier("way returned")
    }

  }
}

class GridShardingAddBatchSpecMultiJvmNode0 extends GridShardingAddBatchSpec
class GridShardingAddBatchSpecMultiJvmNode1 extends GridShardingAddBatchSpec
class GridShardingAddBatchSpecMultiJvmNode2 extends GridShardingAddBatchSpec
