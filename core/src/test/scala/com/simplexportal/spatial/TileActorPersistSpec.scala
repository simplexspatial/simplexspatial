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

// scalastyle:off magic.number

package com.simplexportal.spatial

import akka.actor.{ActorRef, ActorSystem, Kill, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.simplexportal.spatial.TileActor.{AddBatch, GetMetrics, GetWay, Metrics}
import com.simplexportal.spatial.Tile.Way
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class TileActorPersistSpec
    extends TestKit(ActorSystem("TileActorPersistSpec"))
    with ImplicitSender
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll
    with TileActorDataset {


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
    File("target/journal").delete(true)
  }

  "Tile Actor Persistence" should {

    "recover properly" when {

      def createActorAndKillIt(networkId: String, kill: ActorRef => Unit): Unit = {
        File("target/journal").delete(true)
        val tileActor = system.actorOf(TileActor.props(networkId, bbox))
        exampleTileCommands foreach (command => tileActor ! command)
        receiveN(exampleTileCommands.size)
        kill(tileActor)
      }

      def createActorInBatchAndKillIt(networkId: String, kill: ActorRef => Unit): Unit = {
        File("target/journal").delete(true)
        val tileActor = system.actorOf(TileActor.props(networkId, bbox))
        tileActor ! AddBatch( exampleTileCommands )
        receiveN(1)
        kill(tileActor)
      }

      "the actor restart" in {
        createActorAndKillIt("recover-after-graceful-kill",
                             actor => actor ! PoisonPill)

        val tileActorRecovered =
          system.actorOf(TileActor.props("recover-after-graceful-kill", bbox))

        tileActorRecovered ! GetMetrics
        tileActorRecovered ! GetWay(100)
        expectMsg(Metrics(2, 6))
        expectMsg(Some(Way(100, 5, Map(276737215 -> "wayAttrValue"))))
      }

      "the actor died because Exception" when {
        "creating using single commands" in {
          createActorAndKillIt("recover-after-failure", actor => actor ! Kill)

          val tileActorRecovered =
            system.actorOf(TileActor.props("recover-after-failure", bbox))

          tileActorRecovered ! GetMetrics
          tileActorRecovered ! GetWay(100)
          expectMsg(Metrics(2, 6))
          expectMsg(Some(Way(100, 5, Map(276737215 -> "wayAttrValue"))))
        }
        "creating using batch command" in {
          createActorInBatchAndKillIt("recover-after-failure", actor => actor ! Kill)

          val tileActorRecovered =
            system.actorOf(TileActor.props("recover-after-failure", bbox))

          tileActorRecovered ! GetMetrics
          tileActorRecovered ! GetWay(100)
          expectMsg(Metrics(2, 6))
          expectMsg(Some(Way(100, 5, Map(276737215 -> "wayAttrValue"))))
        }
      }
    }
  }

}
