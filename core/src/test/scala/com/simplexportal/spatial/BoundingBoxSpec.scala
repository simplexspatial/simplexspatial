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

package com.simplexportal.spatial

import com.simplexportal.spatial.model.{BoundingBox, Line, LineSegment, Location}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

// scalastyle:off magic.number

class BoundingBoxSpec extends AnyWordSpecLike with Matchers {
  "BoundingBox" should {

    val bbox = BoundingBox(Location(-10, -20), Location(10, 20))

    "generate line representing edge" in {
      bbox.toLine() shouldBe Line(
        Seq(
          Location(10, -20),
          Location(10, 20),
          Location(-10, 20),
          Location(-10, -20),
          Location(10, -20)
        )
      )
    }

    "calculate neighbours" in {
      bbox.northEdge() shouldBe LineSegment(Location(10, -20), Location(10, 20))
      bbox.northEastVertex() shouldBe Location(10, 20)
      bbox.eastEdge() shouldBe LineSegment(Location(10, 20), Location(-10, 20))
      bbox.southEastVertex() shouldBe Location(-10, 20)
      bbox.southEdge() shouldBe LineSegment(Location(-10, -20), Location(-10, 20))
      bbox.southWestVertex() shouldBe Location(-10, -20)
      bbox.westEdge() shouldBe LineSegment(Location(10, -20), Location(-10, -20))
      bbox.northWestVertex() shouldBe Location(10, -20)
      bbox.clockNeighbours() shouldBe Seq(
        LineSegment(Location(10, -20), Location(10, 20)),
        Location(10, 20),
        LineSegment(Location(10, 20), Location(-10, 20)),
        Location(-10, 20),
        LineSegment(Location(-10, -20), Location(-10, 20)),
        Location(-10, -20),
        LineSegment(Location(10, -20), Location(-10, -20)),
        Location(10, -20)
      )
    }

  }
}
