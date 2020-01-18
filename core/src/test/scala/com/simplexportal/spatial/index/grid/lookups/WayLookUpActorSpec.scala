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
import com.simplexportal.spatial.index.grid.lookups.WayLookUpActor._
import com.simplexportal.spatial.index.grid.tile.TileIdx
import org.scalatest.{Matchers, WordSpecLike}

class WayLookUpActorSpec
  extends ScalaTestWithActorTestKit
    with WordSpecLike
    with Matchers {

  "WayLookUpActor" must {
    "Put and Get correctly" when {

      "do it one by one" in {
        val probeResponse = testKit.createTestProbe[AnyRef]()
        val lookup = testKit.spawn(
          WayLookUpActor("test-one-by-one-index", "add-one-by-one-lookup"),
          "add-one-by-one-lookup-test"
        )

        lookup ! Put(100, TileIdx(10, 10), Some(probeResponse.ref))
        probeResponse.expectMessage(Done())

        lookup ! Put(100, TileIdx(11, 11), Some(probeResponse.ref))
        probeResponse.expectMessage(Done())

        lookup ! Put(110, TileIdx(11, 11), Some(probeResponse.ref))
        probeResponse.expectMessage(Done())

        lookup ! Get(100, probeResponse.ref)
        probeResponse.expectMessage(GetResponse(100, Some(Set(TileIdx(10, 10), TileIdx(11, 11)))))

        lookup ! Get(110, probeResponse.ref)
        probeResponse.expectMessage(GetResponse(110, Some(Set(TileIdx(11, 11)))))

        lookup ! Get(1000, probeResponse.ref)
        probeResponse.expectMessage(GetResponse(1000, None))

        lookup ! Gets(Seq(100, 110, 1000), probeResponse.ref)
        probeResponse.expectMessage(GetsResponse(
          Seq(
            GetResponse(100, Some(Set(TileIdx(10, 10), TileIdx(11, 11)))),
            GetResponse(110, Some(Set(TileIdx(11, 11)))),
            GetResponse(1000, None)
          )
        ))
      }

      "when do it in block" in {
        val probeResponse = testKit.createTestProbe[AnyRef]()
        val lookup = testKit.spawn(
          WayLookUpActor("test-batch-index", "add-batch-lookup"),
          "add-batch-lookup-test"
        )

        lookup ! PutBatch(
          Seq(
            Put(10, TileIdx(10, 10), Some(probeResponse.ref)),
            Put(10, TileIdx(11, 11), Some(probeResponse.ref)),
            Put(11, TileIdx(11, 11), Some(probeResponse.ref)),
            Put(12, TileIdx(12, 12), Some(probeResponse.ref))
          ),
          Some(probeResponse.ref)
        )
        probeResponse.expectMessage(Done())

        lookup ! Gets(Seq(10, 11, 1000), probeResponse.ref)
        probeResponse.expectMessage(GetsResponse(
          Seq(
            GetResponse(10, Some(Set(TileIdx(11, 11),TileIdx(10, 10)))),
            GetResponse(11, Some(Set(TileIdx(11, 11)))),
            GetResponse(1000, None)
          )
        ))
      }

    }
  }
}
