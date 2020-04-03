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
import com.simplexportal.spatial.index.grid.GridProtocol._
import com.simplexportal.spatial.index.grid.{Grid, GridConfig}
import com.simplexportal.spatial.model.{Location, Node}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

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

  commonConfig(
    ConfigFactory
      .parseString(s"""
      akka.cluster.seed-nodes = [ "akka://GetNearestNodeSessionSpec@localhost:2551" ]
      akka.persistence.snapshot-store.local.dir = "target/snapshots/GetNearestNodeSessionSpec"
    """)
      .withFallback(ConfigFactory.load("application-default-multijvm.conf"))
  )

}

abstract class GetNearestNodeSessionSpec
    extends MultiNodeSpec(GetNearestNodeSessionSpecConfig)
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  implicit val typedSystem = system.toTyped

  import GetNearestNodeSessionSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int = roles.size

  "GetNearestNodeSession" must {
    val gridIndex = system.spawn(Grid(GridConfig("GetNearestNodeSessionTest", 100, 100, 4, 4)), "GetNearestNodeSession")

    Cluster(system).subscribe(testActor, classOf[MemberUp])
    expectMsgClass(classOf[CurrentClusterState])

    Cluster(system) join node(node1).address

    receiveN(3).collect { case MemberUp(m) => m.address }.toSet should be(
      Set(node(node0).address, node(node1).address, node(node2).address)
    )

    Cluster(system).unsubscribe(testActor)

    enterBarrier("all-up")

    runOn(node1) {
      val probeACK = TestProbe[GridACK]()
      gridIndex ! GridAddNode(1, 0, 0, Map.empty, Some(probeACK.ref))
      gridIndex ! GridAddNode(2, 1, 1, Map.empty, Some(probeACK.ref))
      gridIndex ! GridAddNode(3, 46, 91, Map.empty, Some(probeACK.ref))
      gridIndex ! GridAddNode(4, 46, 91, Map.empty, Some(probeACK.ref))
      gridIndex ! GridAddNode(5, 89, -179, Map.empty, Some(probeACK.ref))
      probeACK.receiveMessages(5) shouldBe Seq.fill(5)(GridDone())
    }

    enterBarrier("nodes added and all ready for the test")

    "be able to find nearest nodes in the cluster" when {

      "ask for exact position in the edge" in {
        val probe = TestProbe[GridNearestNodeReply]()
        gridIndex ! GridNearestNode(Location(0, 0), probe.ref)
        probe.expectMessage(
          GridNearestNodeReply(Right(Set(Node(1, Location(0, 0)))))
        )
      }

      "ask for exact position" in {
        val probe = TestProbe[GridNearestNodeReply]()
        gridIndex ! GridNearestNode(Location(1, 1), probe.ref)
        probe.expectMessage(
          GridNearestNodeReply(Right(Set(Node(2, Location(1, 1)))))
        )
      }

      "found multiple nodes" in {
        val probe = TestProbe[GridNearestNodeReply]()
        gridIndex ! GridNearestNode(Location(47, 92), probe.ref)
        probe.expectMessage(
          GridNearestNodeReply(
            Right(
              Set(
                Node(3, Location(46, 91)),
                Node(4, Location(46, 91))
              )
            )
          )
        )
      }

      "from an empty tile" in {
        val probe = TestProbe[GridNearestNodeReply]()
        gridIndex ! GridNearestNode(Location(-1, -1), probe.ref)
        probe.expectMessage(GridNearestNodeReply(Right(Set(Node(1, Location(0, 0))))))
      }

//      "jump from 180 to -180" in {
//        runOn(node2) {
//          val probe = TestProbe[GridNearestNodeReply]()
//          gridIndex ! GridNearestNode(Location(89, 179), probe.ref)
//          probe.expectMessage(
//            GridNearestNodeReply(
//              Right(
//                Set(
//                  Node(5, Location(89, -179))
//                )
//              )
//            )
//          )
//
//        }
//      }

    }
  }

}

class GetNearestNodeSessionSpecMultiJvmNode0 extends GetNearestNodeSessionSpec
class GetNearestNodeSessionSpecMultiJvmNode1 extends GetNearestNodeSessionSpec
class GetNearestNodeSessionSpecMultiJvmNode2 extends GetNearestNodeSessionSpec
