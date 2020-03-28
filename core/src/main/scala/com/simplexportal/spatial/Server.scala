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

package com.simplexportal.spatial

import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.adapter._
import akka.stream.ActorMaterializer
import com.simplexportal.spatial.index.grid.entrypoints.grpc.GRPCServer
import com.simplexportal.spatial.index.grid.entrypoints.restful.RestServer
import com.simplexportal.spatial.index.grid.{Grid, GridConfig}
import com.typesafe.config.Config

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

object Server {
  def run(config: Config): Unit = {

    // Akka Classic implicits
    implicit val system = akka.actor.ActorSystem("SimplexSpatialSystem", config)
    implicit val materializer = ActorMaterializer() // FIXME: How to do change it in 2.6?
    implicit val executionContext: ExecutionContext = system.dispatcher

    // Akka Typed implicits
    implicit val typedSystem = system.toTyped
    implicit val scheduler: Scheduler = typedSystem.scheduler

    // TODO: Add roles to http entrypoint and grid shards.
    //  cluster.selfMember match {
    //    case member if member.roles.contains("http") => ???
    //    case member if member.roles.contains("grid-index") => ???
    //  }

    // TODO: Build the GridConfig here
    val gridIndex = system.spawn(
      Grid(GridConfig("GridIndex", config)),
      "GridIndex"
    );

    GRPCServer.start(gridIndex, config).onComplete {
      case Success(bound) =>
        println(
          s"SimplexSpatial gRPC at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/"
        )
      case Failure(e) =>
        Console.err.println("SimplexSpatial gRPC server can not start!")
        system.log.error(e, "SimplexSpatial gRPC server can not start!")
        system.terminate()
    }

    RestServer.start(gridIndex, config).onComplete {
      case Success(bound) =>
        println(
          s"SimplexSpatial Rest at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/"
        )
      case Failure(e) =>
        Console.err.println("SimplexSpatial Rest server can not start!")
        system.log.error(e, "SimplexSpatial Rest server can not start!")
        system.terminate()
    }

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
