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

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import com.simplexportal.spatial.index.grid.CommonInternalSerializer
import com.simplexportal.spatial.index.grid.lookups.{NodeLookUpActor, WayLookUpActor}
import com.simplexportal.spatial.index.grid.sessions.GetNodeLocationsSession
import com.simplexportal.spatial.index.grid.tile.{actor => tile}
import com.simplexportal.spatial.index.protocol.{GridAddNode, GridAddWay, GridBatchCommand}

protected trait Adapter {

  protected sealed trait ForeignResponse extends CommonInternalSerializer

  protected case class LocationsWrapper(
      locs: GetNodeLocationsSession.NodeLocations
  ) extends ForeignResponse

  protected case class DoneWrapper() extends ForeignResponse
  protected case class NotDoneWrapper(msg: String) extends ForeignResponse

  protected def adapters(
      context: ActorContext[ForeignResponse]
  ): ActorRef[AnyRef] =
    context.messageAdapter {
      case tile.Done()                  => DoneWrapper()
      case tile.NotDone(msg)            => NotDoneWrapper(msg)
      case NodeLookUpActor.Done()       => DoneWrapper()
      case NodeLookUpActor.NotDone(msg) => NotDoneWrapper(msg)
      case WayLookUpActor.Done()        => DoneWrapper()
      case WayLookUpActor.NotDone(msg)  => NotDoneWrapper(msg)
    }

  implicit class GridAddBatchEnricher(gridAddBatch: GridBatchCommand) {
    def toTileProtocol(): tile.BatchActions = gridAddBatch match {
      case GridAddNode(id, lat, lon, attributes, _) =>
        tile.AddNode(id, lat, lon, attributes)
      case GridAddWay(id, nodeIds, attributes, _) =>
        tile.AddWay(id, nodeIds, attributes)
    }
  }

}
