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

    val gridIndex = system.spawn(Grid("GridIndexTest", 10000, 10000, 10000), "GridIndex")

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

    "be able to retrieve nodes" in {
      val probe = TestProbe[TileIndexActor.GetNodeResponse]()

      gridIndex ! TileIndexActor.GetNode(999, probe.ref)
      gridIndex ! TileIndexActor.GetNode(0, probe.ref)
      gridIndex ! TileIndexActor.GetNode(1, probe.ref)
      gridIndex ! TileIndexActor.GetNode(2, probe.ref)

      probe.receiveMessages(4, 1.minutes).toSet shouldBe Set(
        TileIndexActor.GetNodeResponse(None),
        TileIndexActor.GetNodeResponse(Some(TileIndex.Node(0, Location(-23, -90), Map.empty))),
        TileIndexActor.GetNodeResponse(Some(TileIndex.Node(1, Location(60, 130), Map.empty))),
        TileIndexActor.GetNodeResponse(Some(TileIndex.Node(2, Location(-23.3, -90), Map.empty)))
      )

      enterBarrier("nodes retrieved")
    }




    //    "be able to add a ways" in within(10.seconds)  {
//      val probe = TestProbe[AnyRef]()
//      runOn(node0) {
//        gridIndex ! TileActor.AddWay(1, Seq(0, 1, 2), Map.empty, Some(probe.ref))
//        probe.receiveMessages(1)
//      }
//      enterBarrier("ways added")
//    }

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
