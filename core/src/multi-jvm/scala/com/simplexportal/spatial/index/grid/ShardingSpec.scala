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

import akka.remote.testkit._
import akka.testkit.ImplicitSender
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


import scala.concurrent.duration._

// DOCUMENTATION: https://doc.akka.io/docs/akka/current/multi-node-testing.html
object ShardingSpecConfig extends MultiNodeConfig {
  val nodeSeed = role("nodeSeed")
  val node1 = role("node1")
  val node2 = role("node2")
}

class ShardingSpecMultiJvmSeed extends ShardingSpec
class ShardingSpecMultiJvmNode1 extends ShardingSpec
class ShardingSpecMultiJvmNode2 extends ShardingSpec

abstract class ShardingSpec
    extends MultiNodeSpec(ShardingSpecConfig)
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  override def initialParticipants: Int =  roles.size

  override protected def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override protected def afterAll(): Unit = multiNodeSpecAfterAll()

  "The tile index" must {
    "be able to send commands between nodes" in within(15 seconds) {

    }
  }
}


