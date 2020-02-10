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
package com.simplexportal.spatial.index.grid.lookups

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.simplexportal.spatial.index.grid.lookups.LookUpActor._
import com.simplexportal.spatial.index.grid.tile.impl.TileIndex
import com.simplexportal.spatial.model.Location
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class LookUpActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {

  "LookUpActor" must {
    "Put and Get correctly" in {
      val probeResponse = testKit.createTestProbe[LookUpActor.Response]()
      val lookup = testKit.spawn(
        LookUpActor("test-index", "lookup"),
        "lookup-test"
      )

      lookup ! Put(10, "v10", Some(probeResponse.ref))
      probeResponse.expectMessage(Done())

      lookup ! Put(11, "v11", Some(probeResponse.ref))
      probeResponse.expectMessage(Done())

      lookup ! Get(10, probeResponse.ref)
      probeResponse.expectMessage(GetResponse(10, Some("v10")))

      lookup ! Get(1000, probeResponse.ref)
      probeResponse.expectMessage(GetResponse(1000, None))

      lookup ! Gets(Seq(10, 11, 1000), probeResponse.ref)
      probeResponse.expectMessage(
        GetsResponse[Int, String](
          Seq(
            GetResponse(10, Some("v10")),
            GetResponse(11, Some("v11")),
            GetResponse(1000, None)
          )
        )
      )

    }

    "Put and Get correctly with Nodes" in {

      def node(id: Long) = TileIndex.InternalNode(id, Location(id, id))

      val probeResponse = testKit.createTestProbe[LookUpActor.Response]()
      val lookup = testKit.spawn(
        LookUpActor("test-nodes-index", "lookup-nodes"),
        "lookup-nodes-test"
      )

      lookup ! Put(10, node(10), Some(probeResponse.ref))
      probeResponse.expectMessage(Done())

      lookup ! Put(11, node(11), Some(probeResponse.ref))
      probeResponse.expectMessage(Done())

      lookup ! Get(10, probeResponse.ref)
      probeResponse.expectMessage(GetResponse(10, Some(node(10))))

      lookup ! Get(1000, probeResponse.ref)
      probeResponse.expectMessage(GetResponse(1000, None))

      lookup ! Gets(Seq(10, 11, 1000), probeResponse.ref)
      probeResponse.expectMessage(
        GetsResponse[Int, TileIndex.InternalNode](
          Seq(
            GetResponse(10, Some(node(10))),
            GetResponse(11, Some(node(11))),
            GetResponse(1000, None)
          )
        )
      )

    }

  }

}
