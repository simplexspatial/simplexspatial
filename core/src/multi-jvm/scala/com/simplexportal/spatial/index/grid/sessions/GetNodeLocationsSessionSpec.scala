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
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.simplexportal.spatial.index.grid.Grid.NodeLookUpTypeKey
import com.simplexportal.spatial.index.grid.lookups.{
  LookUpNodeEntityIdGen,
  NodeLookUpActor
}
import com.simplexportal.spatial.index.grid.tile
import com.simplexportal.spatial.index.grid.tile.{TileIdx, TileIndexEntityIdGen}
import com.typesafe.config.ConfigFactory
import io.jvm.uuid.UUID
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.implicitConversions

// scalastyle:off magic.number
object GetNodeLocationsSessionSpecConfig extends MultiNodeConfig {

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
        """
      akka.loglevel=INFO
      akka.cluster.seed-nodes = [ "akka://GridShardingSpec@localhost:2551" ]
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
    """
      )
      .withFallback(ConfigFactory.load())
  )

}

abstract class GetNodeLocationsSessionSpec
    extends MultiNodeSpec(GetNodeLocationsSessionSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  import GetNodeLocationsSessionSpecConfig._

  implicit val typedSystem = system.toTyped

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int = roles.size

  "The tile index" must {
    println(s"Running System [${system.name}]")

    val tileEntityFn = new TileIndexEntityIdGen(2, 2)
    val sharding = ClusterSharding(system.toTyped)
    sharding.init(
      Entity(NodeLookUpTypeKey) { entityContext =>
        NodeLookUpActor("LookUpNodesIndexTest", entityContext.entityId)
      }
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

    "Ask for locations when is empty" when {

      "ask a list of not found nodes return a map on none in the value" in {
        val probe = TestProbe[GetNodeLocationsSession.NodeLocations]()
        runOn(node1) {
          system.spawn(
            GetNodeLocationsSession(sharding, Seq(9910, 9911), probe.ref),
            s"get_not_found_node_locations_${UUID.randomString}"
          )

          probe.expectMessage(
            GetNodeLocationsSession.NodeLocations(
              Map(
                9910L -> None,
                9911L -> None
              )
            )
          )
        }
      }

      "ask an empty list return an empty map" in {
        val probe = TestProbe[GetNodeLocationsSession.NodeLocations]()
        runOn(node1) {
          system.spawn(
            GetNodeLocationsSession(sharding, Seq.empty, probe.ref),
            s"get_empty_request_node_locations_${UUID.randomString}"
          )

          probe.expectMessage(
            GetNodeLocationsSession.NodeLocations(
              Map.empty
            )
          )
        }
      }
    }

    "Add nodes in different " in {
      val probe = TestProbe[NodeLookUpActor.ACK]()
      runOn(node0) {
        val nodes = Seq(
          tile.AddNode(0, -23, -90, Map.empty, None),
          tile.AddNode(1, 60, 130, Map.empty, None),
          tile.AddNode(2, -23.3, -90, Map.empty, None)
        )

        nodes.foreach { node =>
          val tileIdx = tileEntityFn.tileIdx(node.lat, node.lat)
          sharding.entityRefFor(
            NodeLookUpTypeKey,
            LookUpNodeEntityIdGen.entityId(node.id)
          ) ! NodeLookUpActor
            .Put(node.id, tileIdx, Some(probe.ref))
        }

        probe.receiveMessages(3)
      }
      enterBarrier("nodes added")
    }

    "Get locations" in {
      val probe = TestProbe[GetNodeLocationsSession.NodeLocations]()
      runOn(node1) {
        system.spawn(
          GetNodeLocationsSession(sharding, Seq(0, 1, 2, 3), probe.ref),
          s"get_node_locations${UUID.randomString}"
        )

        probe.expectMessage(
          GetNodeLocationsSession.NodeLocations(
            Map(
              0L -> Some(TileIdx(0, 0)),
              1L -> Some(TileIdx(1, 1)),
              2L -> Some(TileIdx(0, 0)),
              3L -> None
            )
          )
        )
      }

      enterBarrier("nodes added")
    }

  }
}

class GetNodeLocationsSessionSpecMultiJvmNode0
    extends GetNodeLocationsSessionSpec

class GetNodeLocationsSessionSpecMultiJvmNode1
    extends GetNodeLocationsSessionSpec

class GetNodeLocationsSessionSpecMultiJvmNode2
    extends GetNodeLocationsSessionSpec
