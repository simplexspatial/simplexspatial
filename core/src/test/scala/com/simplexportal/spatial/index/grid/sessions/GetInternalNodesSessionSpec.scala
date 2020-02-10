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

import com.simplexportal.spatial.index.grid.tile.actor
import com.simplexportal.spatial.index.grid.tile.actor.{GetInternalNodeResponse, GetInternalNodesResponse}
import com.simplexportal.spatial.index.grid.tile.impl.TileIndex
import com.simplexportal.spatial.model.Location
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Random

// scalastyle:off magic.number
class GetInternalNodesSessionSpec extends AnyWordSpecLike with Matchers {

  "GetNodesSession" should {

    "order the response" in {
      val ids = Seq[Long](0, 1, 2, 30, 4, 30, 5)
      val expectedResponse = GetInternalNodesResponse(
        Seq(
          GetInternalNodeResponse(0, Some(TileIndex.InternalNode(0, Location(0, 0)))),
          actor.GetInternalNodeResponse(1, None),
          actor.GetInternalNodeResponse(2, Some(TileIndex.InternalNode(2, Location(2, 2)))),
          actor.GetInternalNodeResponse(30, Some(TileIndex.InternalNode(30, Location(30, 30)))),
          actor.GetInternalNodeResponse(4, Some(TileIndex.InternalNode(4, Location(4, 4)))),
          actor.GetInternalNodeResponse(30, Some(TileIndex.InternalNode(30, Location(30, 30)))),
          actor.GetInternalNodeResponse(5, Some(TileIndex.InternalNode(5, Location(5, 5))))
        )
      )

      val unorderedResponse = actor.GetInternalNodesResponse(Random.shuffle(expectedResponse.nodes))

      GetInternalNodesSession.sortResponse(ids, unorderedResponse) shouldBe expectedResponse
    }

  }
}
