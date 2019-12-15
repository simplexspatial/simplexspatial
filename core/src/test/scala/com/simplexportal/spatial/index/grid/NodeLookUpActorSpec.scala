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

// scalastyle:off magic.number
package com.simplexportal.spatial.index.grid

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.simplexportal.spatial.index.grid.NodeLookUpActor._
import org.scalatest.{Matchers, WordSpecLike}

class NodeLookUpActorSpec
    extends ScalaTestWithActorTestKit
    with WordSpecLike
    with Matchers {

  "NodeLookUpActor" must {
    "Put and Get correctly" in {
      val probeResponse = testKit.createTestProbe[NodeLookUpActor.Response]()
      val lookup = testKit.spawn(
        NodeLookUpActor("test-index", "add-lookup"),
        "add-lookup-test"
      )
      lookup ! Put(10, NodeEntityId(10, 10), Some(probeResponse.ref))
      probeResponse.expectMessage(Done())

      lookup ! Get(10, probeResponse.ref)
      probeResponse.expectMessage(GetResponse(Some(NodeEntityId(10, 10))))

      lookup ! Get(1000, probeResponse.ref)
      probeResponse.expectMessage(GetResponse(None))

    }
  }

}
