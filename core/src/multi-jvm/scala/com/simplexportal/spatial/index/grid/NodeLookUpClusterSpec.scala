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
import com.simplexportal.spatial.index.grid.NodeLookUpActor.Hash
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.implicitConversions

// scalastyle:off magic.number
object NodeLookUpClusterSpecConfig extends MultiNodeConfig {

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
      akka.cluster.seed-nodes = [ "akka://NodeLookUpClusterSpec@localhost:2551" ]
    """).withFallback(ConfigFactory.load()))

}

abstract class NodeLookUpClusterSpec
  extends MultiNodeSpec(NodeLookUpClusterSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  implicit val typedSystem = system.toTyped

  import NodeLookUpClusterSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int =  roles.size

  "The node lookup index" must {
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

    "be able to add a entities in the a local lookup shard" in within(10.seconds)  {
      runOn(node0) {
        val probe = TestProbe[AnyRef]()
        val localNodeLookUpActor = system.spawn(NodeLookUpActor("NodeLookUpActorIndex", "FIXED_LOOKUP_TEST_NODE0"), "NodeLookUpActorNode0")
        localNodeLookUpActor ! NodeLookUpActor.Put(0, Hash(0,0), Some(probe.ref))
        localNodeLookUpActor ! NodeLookUpActor.Put(1, Hash(0,0), Some(probe.ref))
        localNodeLookUpActor ! NodeLookUpActor.Put(2, Hash(0,0), Some(probe.ref))

        probe.receiveMessages(3)
      }
      enterBarrier("added locally")
    }

    "be able to add entities in the a remote lookup node" in within(10.seconds)  {
      runOn(node1) {
        val probe = TestProbe[AnyRef]()
        val remoteNodeLookUpActor = system.actorSelection(node(node0) / "user" / "NodeLookUpActorNode0")

        remoteNodeLookUpActor ! NodeLookUpActor.Put(10, Hash(10, 10), Some(probe.ref))
        remoteNodeLookUpActor ! NodeLookUpActor.Put(11, Hash(11, 11), Some(probe.ref))
        remoteNodeLookUpActor ! NodeLookUpActor.Put(12, Hash(12, 12), Some(probe.ref))

        probe.receiveMessages(3)
      }

      enterBarrier("added remotely")
    }

    "lookup inserted nodes from the remote actor" in {
      runOn(node2) {
        val probe = TestProbe[AnyRef]()
        val remoteNodeLookUpActor = system.actorSelection(node(node0) / "user" / "NodeLookUpActorNode0")
        remoteNodeLookUpActor ! NodeLookUpActor.Get(10, probe.ref)

        probe.expectMessage(NodeLookUpActor.GetResponse(Some(Hash(10,10))))

      }
      enterBarrier("lookup from node2")
    }

  }
}

class NodeLookUpClusterSpecMultiJvmNode0 extends NodeLookUpClusterSpec
class NodeLookUpClusterSpecMultiJvmNode1 extends NodeLookUpClusterSpec
class NodeLookUpClusterSpecMultiJvmNode2 extends NodeLookUpClusterSpec
