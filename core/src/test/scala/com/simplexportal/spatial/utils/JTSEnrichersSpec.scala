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
import com.simplexportal.spatial.utils.JTSEnrichers._
import org.locationtech.jts.geom.{Coordinate, LineSegment => JTSLineSegment}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

// scalastyle:off magic.number
class JTSEnrichersSpec extends AnyWordSpecLike with Matchers {

  "JTSEnrichers" should {

    "enrich JTS Coordinate" when {
      "transform into a model Location" in {
        new Coordinate(10, 20).toSpl() shouldBe Location(20, 10)
      }
    }

    "enrich JTS LineSegment" when {
      "transform into a model LineSegment" in {
        new JTSLineSegment(new Coordinate(10, 20), new Coordinate(15, 25))
          .toSpl() shouldBe LineSegment(Location(20, 10), Location(25, 15))
      }

    }

  }
}
