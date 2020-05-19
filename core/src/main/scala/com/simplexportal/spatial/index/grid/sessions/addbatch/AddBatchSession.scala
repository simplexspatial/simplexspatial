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

package com.simplexportal.spatial.index.grid.sessions.addbatch

import akka.actor.typed.ActorTags
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.simplexportal.spatial.index.grid.GridProtocol._
import com.simplexportal.spatial.index.grid.tile.actor.TileIndexEntityIdGen
import io.jvm.uuid.UUID

object AddBatchSession /*extends CollectNodeInfo*/ {

  def processRequest(
      cmd: GridAddBatch,
      context: ActorContext[GridRequest]
  )(
      implicit sharding: ClusterSharding,
      tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Unit = ???
//    context.spawn(
//      collectNodeIdx(cmd),
//      s"adding_batch_${UUID.randomString}",
//      ActorTags("session", "session-add-batch")
//    )

}
