/*
 * Copyright 2020 SimplexPortal Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simplexportal.spatial.utils

import com.simplexportal.spatial.model.{LineSegment, Location}
import com.simplexportal.spatial.utils.ModelEnrichers._
import org.locationtech.jts.geom.{Coordinate, LineSegment => JTSLineSegment}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

// scalastyle:off magic.number
class ModelEnrichersSpec extends AnyWordSpecLike with Matchers {

  "ModelEnrichers" should {

    "enrich Location" when {
      "transform into a JTS Coordinate" in {
        Location(10, 20).toJTS() shouldBe new Coordinate(20, 10)
      }
    }

    "enrich LineSegment" when {
      "transform into a JTS LineSegment" in {
        LineSegment(Location(10, 20), Location(15, 25))
          .toJTS() shouldBe new JTSLineSegment(
          new Coordinate(20, 10),
          new Coordinate(25, 15)
        )
      }
      "transform into a list of JTS coordinates" in {
        LineSegment(Location(10, 20), Location(15, 25))
          .toJTSArrayCoords() shouldBe Seq(
          new Coordinate(20, 10),
          new Coordinate(25, 15)
        )
      }
    }

  }
}
