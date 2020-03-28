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

package com.simplexportal.spatial.index.grid.entrypoints.restful

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object RestProtocol {
  sealed trait RestfulProtocol
  sealed trait RestfulRequest extends RestfulProtocol
  sealed trait RestfulResponse extends RestfulProtocol {
    def error: Option[String]
  }

  case class AddNodeBody(lon: Double, lat: Double, attributes: Map[String, String]) extends RestfulRequest
  case class AddWayBody(nodes: Seq[Long], attributes: Map[String, String]) extends RestfulRequest

  case class AddNodeBatchBody(id: Long, lon: Double, lat: Double, attributes: Map[String, String])
  case class AddWayBatchBody(id: Long, nodes: Seq[Long], attributes: Map[String, String])
  case class AddBatchBody(nodes: Seq[AddNodeBatchBody], ways: Seq[AddWayBatchBody]) extends RestfulRequest

  case class Node(id: Long, lon: Double, lat: Double, attributes: Map[String, String], error: Option[String] = None)
      extends RestfulResponse
  case class Way(id: Long, nodes: Seq[Node], attributes: Map[String, String], error: Option[String] = None)
      extends RestfulResponse

  sealed trait RestfulACKResponse extends RestfulResponse
  case class Done() extends RestfulACKResponse {
    val error = None
  }
  case class NotDone(error: Option[String]) extends RestfulACKResponse

  trait RestfulJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val addNodeBody = jsonFormat3(AddNodeBody)
    implicit val addWayBody = jsonFormat2(AddWayBody)

    implicit val addNodeBatchFormat = jsonFormat4(AddNodeBatchBody)
    implicit val addWayBatchFormat = jsonFormat3(AddWayBatchBody)
    implicit val addBatchFormat = jsonFormat2(AddBatchBody)

    implicit val nodeFormat = jsonFormat5(Node)
    implicit val wayFormat = jsonFormat4(Way)

    implicit val doneFormat = jsonFormat0(Done)
    implicit val notDoneFormat = jsonFormat1(NotDone)
  }
}
