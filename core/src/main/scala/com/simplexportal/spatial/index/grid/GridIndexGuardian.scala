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

package com.simplexportal.spatial.index.grid

import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding

object GridIndexGuardian {
  def apply(indexId: String)(implicit system: ActorSystem[Nothing]): GridIndexGuardian = new GridIndexGuardian(indexId)
}

class GridIndexGuardian(indexId: String)(
    implicit system: ActorSystem[Nothing]
) {

  private val sharding = ClusterSharding(system) // TODO: Maybe it is possible to create it in the setup and don't implement any class

  /* TBC What is necessary to calculate the shardId. **/
  private def sharded( /** TCB **/) = {
    sharding.entityRefFor(TileActor.TypeKey, calculateShardId())
  }

  /**
    * Calculate the ShardId in function of the Index configuration, etc....
    *
    * @return
    */
  private def calculateShardId(): String = ???

  def activate(): Behavior[TileActor.Command] = ???
}
