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

package com.simplexportal.spatial.api

import akka.actor.ActorSystem
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.UseHttp2.Always
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.{ActorMaterializer, Materializer}
import com.simplexportal.spatial.TileActor
import com.simplexportal.spatial.model.{BoundingBox, Location}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContext, Future}

object HttpServer {

  def main(args: Array[String]): Unit = {
    // Important: enable HTTP/2 in ActorSystem's config
    // We do it here programmatically, but you can also set it in the application.conf
    implicit val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem("SimplexSpatial", conf)
    new HttpServer(system).run()
    // ActorSystem threads will keep the app alive until `system.terminate()` is called
  }

}

class HttpServer(system: ActorSystem)(implicit config: Config) {
  def run(): Future[Http.ServerBinding] = {

    // Akka boot up code
    implicit val sys: ActorSystem = system
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    val tileActor = sys.actorOf(TileActor.props("tileRoot", BoundingBox(
      Location(Double.MinValue, Double.MinValue),
      Location(Double.MaxValue, Double.MaxValue)
    )))

    // Define all service handlers.
    val dataServiceHandler = SimplexSpatialServiceHandler.partial(new SimplexSpatialServiceImpl(tileActor))
    // val algorithmServiceHandler = ....

    // Create service handlers
    val serviceHandlers: HttpRequest => Future[HttpResponse] = ServiceHandler.concatOrNotFound(dataServiceHandler /*, algorithmServiceHandler*/)


    // Bind service handler servers to localhost:8080/8081
    val binding = Http().bindAndHandleAsync(
      serviceHandlers,
      interface = config.getString("simplexportal.spatial.api.interface"),
      port = config.getInt("simplexportal.spatial.api.port"),
      connectionContext = HttpConnectionContext(http2 = Always))

    // report successful binding
    binding.foreach { binding =>
      println(s"gRPC server bound to: ${binding.localAddress}")
    }

    binding
  }
}
