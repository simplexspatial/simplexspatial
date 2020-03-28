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
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.Materializer
import com.simplexportal.spatial.index.protocol.GridRequest
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

object GRPCServer {
  def start(gridIndex: ActorRef[GridRequest], config: Config)(
      implicit executionContext: ExecutionContext,
      scheduler: Scheduler,
      mat: Materializer,
      system: ActorSystem
  ): Future[Http.ServerBinding] = {

    val interface = config.getString("simplexportal.spatial.entrypoint.grpc.interface")
    val port = config.getInt("simplexportal.spatial.entrypoint.grpc.port")

    val grpcHandler = GRPCEntryPointHandler.partial(new GRPCEntryPointImpl(gridIndex))
    // val algorithmServiceHandler = ....

    val serviceHandlers: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(
        grpcHandler
        /*, algorithmServiceHandler*/
      )

    Http()
      .bindAndHandleAsync(
        serviceHandlers,
        interface = interface,
        port = port,
        connectionContext = HttpConnectionContext()
      )
  }
}
