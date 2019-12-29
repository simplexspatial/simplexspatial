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

import com.simplexportal.spatial.index.grid.tile.{TileIndex, TileIndexActor}
import com.simplexportal.spatial.model.Location
import org.scalatest.{Matchers, WordSpecLike}

import scala.util.Random

// scalastyle:off magic.number
class GetNodesSessionSpec extends WordSpecLike with Matchers {

  "GetNodesSession" should {

    "order the response" in {
      val ids = Seq[Long](0, 1, 2, 30, 4, 30, 5 )
      val expectedResponse = TileIndexActor.GetInternalNodesResponse(
        Seq(
          TileIndexActor.GetInternalNodeResponse(0, Some(TileIndex.InternalNode(0, Location(0,0)))),
          TileIndexActor.GetInternalNodeResponse(1, None),
          TileIndexActor.GetInternalNodeResponse(2, Some(TileIndex.InternalNode(2, Location(2,2)))),
          TileIndexActor.GetInternalNodeResponse(30, Some(TileIndex.InternalNode(30, Location(30,30)))),
          TileIndexActor.GetInternalNodeResponse(4, Some(TileIndex.InternalNode(4, Location(4,4)))),
          TileIndexActor.GetInternalNodeResponse(30, Some(TileIndex.InternalNode(30, Location(30,30)))),
          TileIndexActor.GetInternalNodeResponse(5, Some(TileIndex.InternalNode(5, Location(5,5)))),
        )
      )

      val unorderedResponse = TileIndexActor.GetInternalNodesResponse(Random.shuffle(expectedResponse.nodes))

      GetNodesSession.sortResponse(ids, unorderedResponse) shouldBe expectedResponse
    }

  }
}
