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
import com.simplexportal.spatial.index.grid.lookups.NodeLookUpActor._
import com.simplexportal.spatial.index.grid.tile.TileIdx
import org.scalatest.{Matchers, WordSpecLike}

class NodeLookUpActorSpec
    extends ScalaTestWithActorTestKit
    with WordSpecLike
    with Matchers {

  "NodeLookUpActor" must {
    "Put and Get correctly" when {
      "do it one by one" in {
        val probeResponse = testKit.createTestProbe[NodeLookUpActor.Response]()
        val lookup = testKit.spawn(
          NodeLookUpActor("test-one-by-one-index", "add-one-by-one-lookup"),
          "add-lookup-one-by-one-test"
        )

        lookup ! Put(10, TileIdx(10, 10), Some(probeResponse.ref))
        probeResponse.expectMessage(Done())

        lookup ! Put(11, TileIdx(11, 11), Some(probeResponse.ref))
        probeResponse.expectMessage(Done())

        lookup ! Get(10, probeResponse.ref)
        probeResponse.expectMessage(GetResponse(10, Some(TileIdx(10, 10))))

        lookup ! Get(1000, probeResponse.ref)
        probeResponse.expectMessage(GetResponse(1000, None))

        lookup ! Gets(Seq(10, 11, 1000), probeResponse.ref)
        probeResponse.expectMessage(
          GetsResponse(
            Seq(
              GetResponse(10, Some(TileIdx(10, 10))),
              GetResponse(11, Some(TileIdx(11, 11))),
              GetResponse(1000, None)
            )
          )
        )
      }

      "do it in blocks" in {
        val probeResponse = testKit.createTestProbe[NodeLookUpActor.Response]()
        val lookup = testKit.spawn(
          NodeLookUpActor("test-batch-index", "add-batch-lookup"),
          "add-lookup-batch-test"
        )
        lookup ! PutBatch(
          Seq(
            Put(10, TileIdx(10, 10), Some(probeResponse.ref)),
            Put(11, TileIdx(11, 11), Some(probeResponse.ref)),
            Put(12, TileIdx(12, 12), Some(probeResponse.ref)),
            Put(13, TileIdx(13, 13), Some(probeResponse.ref))
          ),
          Some(probeResponse.ref)
        )
        probeResponse.expectMessage(Done())

        lookup ! Gets(Seq(10, 11, 1000), probeResponse.ref)
        probeResponse.expectMessage(
          GetsResponse(
            Seq(
              GetResponse(10, Some(TileIdx(10, 10))),
              GetResponse(11, Some(TileIdx(11, 11))),
              GetResponse(1000, None)
            )
          )
        )
      }

    }
  }

}
