/*
 * Copyright 2019 SimplexPortal Ltd
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

package com.simplexportal.spatial

import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.ActorMaterializer
import com.simplexportal.spatial.api.grpc.{DataServiceHandler, DataServiceImpl}
import com.simplexportal.spatial.index.grid.Grid
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

// TODO: Create the RootBehavior in a separate object.
object Main extends App {

  val config = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.load())

  val interface = config.getString("simplexportal.spatial.api.http.interface")
  val port = config.getInt("simplexportal.spatial.api.http.port")

  // Akka Classic implicits
  implicit val system = akka.actor.ActorSystem("CounterServerSystem", config)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Akka Typed implicits
  implicit val typedSystem = system.toTyped
  implicit val scheduler: Scheduler = typedSystem.scheduler

// TODO: Add roles to http entrypoint and grid shards.
//  cluster.selfMember match {
//    case member if member.roles.contains("http") => ???
//    case member if member.roles.contains("grid-index") => ???
//  }

  val gridIndex = system.spawn(
    Grid(
      "GridIndex",
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
    ),
    "GridIndex"
  );

  val dataServiceHandler =
    DataServiceHandler.partial(new DataServiceImpl(gridIndex))
  // val algorithmServiceHandler = ....

  val serviceHandlers: HttpRequest => Future[HttpResponse] =
    ServiceHandler.concatOrNotFound(
      dataServiceHandler
      /*, algorithmServiceHandler*/
    )

  val serverBinding: Future[Http.ServerBinding] = Http()
    .bindAndHandleAsync(
      serviceHandlers,
      interface = interface,
      port = port,
      connectionContext = HttpConnectionContext()
    )

  serverBinding.onComplete {
    case Success(bound) =>
      println(
        s"SimplexSpatial online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/"
      )
    case Failure(e) =>
      Console.err.println("SimplexSpatial server can not start!")
      system.log.error(e, "SimplexSpatial server can not start!")
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)

}
