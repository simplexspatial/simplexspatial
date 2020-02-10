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

import com.simplexportal.spatial.model.{Location, Node, Way}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

// scalastyle:off magic.number
class GetWaySessionSpec extends AnyWordSpecLike with Matchers {

  "GetWaySession" should {

    "reconstruct way from parts" in {
      val from = Set(
        Way(1, List(Node(0, Location(-23.0, -90.0), Map()), Node(1, Location.NaL, Map())), Map()),
        Way(
          1,
          List(Node(0, Location.NaL, Map()), Node(1, Location(60.0, 130.0), Map()), Node(2, Location.NaL, Map())),
          Map()
        ),
        Way(
          1,
          List(Node(1, Location.NaL, Map()), Node(2, Location(-23.3, -90.0), Map()), Node(10, Location.NaL, Map())),
          Map()
        ),
        Way(
          1,
          List(
            Node(2, Location.NaL, Map()),
            Node(10, Location(1.0, 1.0), Map()),
            Node(11, Location(1.000001, 1.000001), Map()),
            Node(12, Location(1.000002, 1.000002), Map())
          ),
          Map()
        )
      )

      val expected = Some(
        Way(
          1,
          Seq(
            Node(0, Location(-23.0, -90.0), Map()),
            Node(1, Location(60.0, 130.0), Map()),
            Node(2, Location(-23.3, -90.0), Map()),
            Node(10, Location(1.0, 1.0), Map()),
            Node(11, Location(1.000001, 1.000001), Map()),
            Node(12, Location(1.000002, 1.000002), Map())
          ),
          Map()
        )
      )

      expected shouldBe GetWaySession.joinWayParts(from)
    }

  }
}
