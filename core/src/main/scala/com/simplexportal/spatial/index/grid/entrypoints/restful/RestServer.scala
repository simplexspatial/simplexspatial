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

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext, RouteResult}
import akka.stream.Materializer
import akka.util.Timeout
import com.simplexportal.spatial.index.grid.entrypoints.restful.RestProtocol._
import com.simplexportal.spatial.index.protocol.{GridBatchCommand, _}
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object RestServer extends Directives with RestfulJsonProtocol {

  implicit val timeout: Timeout = 2.second

  private def replyAdapter[T](resp: Future[GridReply[T]]): RequestContext => Future[RouteResult] = {
    onComplete(resp) {
      case Success(GridDone())         => complete(Done())
      case Success(GridNotDone(error)) => complete(StatusCodes.InternalServerError, NotDone(Some(error)))
      case Success(GridGetNodeReply(payload)) =>
        payload.fold(
          error => complete(StatusCodes.InternalServerError, NotDone(Some(error))),
          _ match {
            case None       => complete(StatusCodes.NotFound, "")
            case Some(node) => complete(Node(node.id, node.location.lat, node.location.lon, node.attributes))
          }
        )
      case Success(GridGetWayReply(payload)) =>
        payload.fold(
          error => complete(StatusCodes.InternalServerError, NotDone(Some(error))),
          _ match {
            case None => complete(StatusCodes.NotFound, "")
            case Some(way) =>
              complete(
                Way(
                  way.id,
                  way.nodes.map(n => Node(n.id, n.location.lat, n.location.lon, n.attributes)),
                  way.attributes
                )
              )
          }
        )
      case Failure(error) => complete(NotDone(Some(error.getMessage)))
    }
  }

  def start(gridIndex: ActorRef[GridRequest], config: Config)(
      implicit executionContext: ExecutionContext,
      scheduler: Scheduler,
      mat: Materializer,
      system: ActorSystem
  ): Future[Http.ServerBinding] = {

    val interface = config.getString("simplexportal.spatial.entrypoint.restful.interface")
    val port = config.getInt("simplexportal.spatial.entrypoint.restful.port")

    val route =
      concat(
        nodeRoutes(gridIndex),
        wayRoutes(gridIndex),
        batchRoutes(gridIndex)
      )

    Http().bindAndHandle(route, interface, port)
  }

  private def batchRoutes(gridIndex: ActorRef[GridRequest])(
      implicit executionContext: ExecutionContext,
      scheduler: Scheduler,
      mat: Materializer,
      system: ActorSystem
  ) =
    path("batch") {
      put {
        entity(as[AddBatchBody]) { body =>
          replyAdapter(
            gridIndex
              .ask[GridACK](ref =>
                GridAddBatch(
                  body.nodes.map(n => GridAddNode(n.id, n.lat, n.lon, n.attributes)) ++
                    body.ways.map(n => GridAddWay(n.id, n.nodes, n.attributes)),
                  Some(ref)
                )
              )
          )
        }
      }
    }

  private def nodeRoutes(gridIndex: ActorRef[GridRequest])(
      implicit executionContext: ExecutionContext,
      scheduler: Scheduler,
      mat: Materializer,
      system: ActorSystem
  ) = {
    pathPrefix("node" / LongNumber) { id =>
      concat(
        get {
          replyAdapter(
            gridIndex
              .ask[GridGetNodeReply](ref => GridGetNode(id, ref))
          )
        },
        put {
          entity(as[AddNodeBody]) { body =>
            replyAdapter(
              gridIndex
                .ask[GridACK](ref => GridAddNode(id, body.lat, body.lon, body.attributes, Some(ref)))
            )
          }
        }
      )
    }
  }

  private def wayRoutes(gridIndex: ActorRef[GridRequest])(
      implicit executionContext: ExecutionContext,
      scheduler: Scheduler,
      mat: Materializer,
      system: ActorSystem
  ) = {
    pathPrefix("way" / LongNumber) { id =>
      concat(
        get {
          replyAdapter(
            gridIndex
              .ask[GridGetWayReply](ref => GridGetWay(id, ref))
          )
        },
        put {
          entity(as[AddWayBody]) { body =>
            replyAdapter(
              gridIndex
                .ask[GridACK](ref => GridAddWay(id, body.nodes, body.attributes, Some(ref)))
            )
          }
        }
      )
    }
  }
}
