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

package com.simplexportal.spatial.index.grid.entrypoints.grpc

import akka.actor.typed.scaladsl.adapter._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.scaladsl.Source
import akka.testkit.ImplicitSender
import com.simplexportal.spatial.index.grid.{Grid, GridConfig}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

// scalastyle:off magic.number
object GRPCEntryPointSpecConfig extends MultiNodeConfig {

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
      akka.cluster.seed-nodes = [ "akka://GRPCEntryPointSpec@localhost:2551" ]
      akka.persistence.snapshot-store.local.dir = "target/snapshots/GRPCEntryPointSpec"
    """
      )
      .withFallback(ConfigFactory.load("application-default-multijvm.conf"))
  )

}

abstract class GRPCEntryPointSpec
    extends MultiNodeSpec(GRPCEntryPointSpecConfig)
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with ImplicitSender {

  implicit val typedSystem = system.toTyped
  implicit val scheduler = typedSystem.scheduler
  implicit val executionContext = typedSystem.executionContext

  import GRPCEntryPointSpecConfig._

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override def initialParticipants: Int = roles.size

  "The tile index throu" must {
    println(s"Running System [${system.name}]")

    val gridIndex = system.spawn(
      Grid(GridConfig("GridIndexTest", 10000, 10000, 10000, 10000)),
      "GridGrpcIndex"
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

//    "be able to add entities in batch" in {
//      runOn(node0) {
//        val grpcService = new GRPCEntryPointImpl(gridIndex)
//
//        Await.result(
//          grpcService.streamBatchCommandsOneResponse(Source.empty),
//          500.milli
//        ) shouldBe IngestionMetrics(0, 0, 0)
//
//        Await
//          .result(
//            grpcService.streamBatchCommandsOneResponse(
//              Source.single(
//                ExecuteBatchCmd().withCommands(immutable.Seq())
//              )
//            ),
//            500.milli
//          ) shouldBe IngestionMetrics(0, 0)
//
//        Await
//          .result(
//            grpcService.streamBatchCommandsOneResponse(
//              Source.single(
//                ExecuteBatchCmd().withCommands(
//                  immutable.Seq(
//                    ExecuteCmd().withNode(AddNodeCmd(1, 0.000001, 0.000001)),
//                    ExecuteCmd().withNode(AddNodeCmd(2, 0.000002, 0.000002)),
//                    ExecuteCmd().withWay(AddWayCmd(3, Seq(1, 2)))
//                  )
//                )
//              )
//            ),
//            500.milli
//          ) shouldBe IngestionMetrics(2, 1)
//
//        Await
//          .result(
//            grpcService.streamBatchCommandsOneResponse(
//              Source(
//                immutable.Seq(
//                  ExecuteBatchCmd().withCommands(
//                    immutable.Seq(
//                      ExecuteCmd().withNode(AddNodeCmd(1, 0.000001, 0.000001)),
//                      ExecuteCmd().withNode(AddNodeCmd(2, 0.000002, 0.000002)),
//                      ExecuteCmd().withWay(AddWayCmd(3, Seq(1, 2)))
//                    )
//                  ),
//                  ExecuteBatchCmd().withCommands(
//                    immutable.Seq(
//                      ExecuteCmd().withNode(AddNodeCmd(10, 0.000001, 0.000001)),
//                      ExecuteCmd().withNode(AddNodeCmd(20, 0.000002, 0.000002)),
//                      ExecuteCmd().withNode(AddNodeCmd(30, 0.000003, 0.000003)),
//                      ExecuteCmd().withWay(AddWayCmd(31, Seq(10, 20))),
//                      ExecuteCmd().withWay(AddWayCmd(32, Seq(1, 10, 20, 30)))
//                    )
//                  ),
//                  ExecuteBatchCmd().withCommands(
//                    immutable.Seq(
//                      ExecuteCmd().withWay(AddWayCmd(300, Seq(1, 2, 10, 20)))
//                    )
//                  )
//                )
//              )
//            ),
//            500.milli
//          ) shouldBe IngestionMetrics(5, 4)
//
//      }
//      enterBarrier("entities added in batch")
//    }
  }
}

class GRPCEntryPointSpecMultiJvmNode0 extends GRPCEntryPointSpec
class GRPCEntryPointSpecMultiJvmNode1 extends GRPCEntryPointSpec
class GRPCEntryPointSpecMultiJvmNode2 extends GRPCEntryPointSpec
