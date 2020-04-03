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

package com.simplexportal.spatial.index.grid.entrypoints.grpc

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Scheduler}
import akka.grpc.scaladsl.{ServerReflection, ServiceHandler, WebHandler}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.Materializer
import com.simplexportal.spatial.StartUpServerResult
import com.simplexportal.spatial.index.grid.GridProtocol.GridRequest
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

object GRPCServer {

  def start(gridIndex: ActorRef[GridRequest], config: Config)(
      implicit executionContext: ExecutionContext,
      scheduler: Scheduler,
      system: ActorSystem
  ): Set[StartUpServerResult] = {

    val grpcHandler = GRPCEntryPointHandler.partial(new GRPCEntryPointImpl(gridIndex))
    val reflectionHandler = ServerReflection.partial(List(GRPCEntryPoint))

    Set(
      startGRPC(config, grpcHandler, reflectionHandler),
      startGRPCWeb(config, grpcHandler, reflectionHandler)
    )
  }

  private def startGRPC(
      config: Config,
      handlers: PartialFunction[HttpRequest, Future[HttpResponse]]*
  )(
      implicit mat: Materializer,
      system: ActorSystem
  ): StartUpServerResult = StartUpServerResult(
    "SimplexSpatial gRPC",
    Http()
      .bindAndHandleAsync(
        ServiceHandler.concatOrNotFound(handlers: _*),
        interface = config.getString("simplexportal.spatial.entrypoint.grpc.interface"),
        port = config.getInt("simplexportal.spatial.entrypoint.grpc.port"),
        connectionContext = HttpConnectionContext()
      )
  )

  private def startGRPCWeb(
      config: Config,
      handlers: PartialFunction[HttpRequest, Future[HttpResponse]]*
  )(
      implicit mat: Materializer,
      system: ActorSystem
  ): StartUpServerResult = StartUpServerResult(
    "SimplexSpatial gRPC-Web",
    Http()
      .bindAndHandleAsync(
        WebHandler.grpcWebHandler(handlers: _*),
        interface = config.getString("simplexportal.spatial.entrypoint.grpc-web.interface"),
        port = config.getInt("simplexportal.spatial.entrypoint.grpc-web.port"),
        connectionContext = HttpConnectionContext()
      )
  )

}
