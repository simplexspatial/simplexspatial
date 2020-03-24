/*
 * Copyright 2020 SimplexPortal Ltd
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

package com.simplexportal.spatial.index.grid

import com.typesafe.config.Config

object GridConfig {
  def apply(indexId: String, config: Config): GridConfig = new GridConfig(
    indexId,
    config.getInt(
      "simplexportal.spatial.indexes.grid-index.partitions.ways-lookup"
    ),
    config.getInt(
      "simplexportal.spatial.indexes.grid-index.partitions.nodes-lookup"
    ),
    config.getInt(
      "simplexportal.spatial.indexes.grid-index.partitions.latitude"
    ),
    config.getInt(
      "simplexportal.spatial.indexes.grid-index.partitions.longitude"
    )
  )
}

case class GridConfig(
    indexId: String,
    nodeLookUpPartitions: Int,
    wayLookUpPartitions: Int,
    latPartitions: Int,
    lonPartitions: Int
) {
  def description: String = s"""
    | Starting Guardian sharding [${indexId}] with:
    | -> [${nodeLookUpPartitions}] nodes lookup partitions,
    | -> [${wayLookUpPartitions}] ways lookup partitions,
    | -> [${latPartitions}] lat. partitions and [${lonPartitions}] lon. partitions.
    |    So every shard in the index is going to cover a fixed area of [${((40075 / lonPartitions) * (40007 / latPartitions))}] km2 approx. [${(40007 / latPartitions)}] Km. lat. x [${(40075 / lonPartitions)}] Km. lon.
    |""".stripMargin
}
