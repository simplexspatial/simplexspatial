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

import akka.actor.typed.{ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.ActorMaterializer
import akka.{Done, actor}
import com.simplexportal.spatial.api.data.{DataServiceHandler, DataServiceImpl}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())

  val interface = config.getString("simplexportal.spatial.api.http.interface")
  val port = config.getInt("simplexportal.spatial.api.http.port")

  val system = ActorSystem[Done](Behaviors.setup[Done] { ctx =>

    // http doesn't know about akka typed so create untyped system/materializer
    implicit val untypedSystem: actor.ActorSystem = ctx.system.toClassic
    implicit val materializer: ActorMaterializer = ActorMaterializer()(ctx.system.toClassic)
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext
    implicit val scheduler: Scheduler = ctx.system.scheduler

    val tileActor = ctx.spawn(TileActor(???), "TileActor")

    val dataServiceHandler = DataServiceHandler.partial(new DataServiceImpl(tileActor))
    // val algorithmServiceHandler = ....

    val serviceHandlers: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(
        dataServiceHandler
        /*, algorithmServiceHandler*/
      )

    val serverBinding: Future[Http.ServerBinding] = Http()(untypedSystem)
      .bindAndHandleAsync(
        serviceHandlers,
        interface = interface,
        port = port,
        connectionContext = HttpConnectionContext())

    serverBinding.onComplete {
      case Success(bound) =>
        println(
          s"SimplexSpatial online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/"
        )
      case Failure(e) =>
        Console.err.println(s"SimplexSpatial server can not start!")
        e.printStackTrace()
        ctx.self ! Done
    }

    Behaviors.receiveMessage {
      case Done =>
        Behaviors.stopped
    }

  }, "SimplexSpatialServer")

}
