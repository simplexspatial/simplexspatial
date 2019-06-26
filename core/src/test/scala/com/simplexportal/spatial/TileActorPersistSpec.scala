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
import com.simplexportal.spatial.TileActor.{GetMetrics, GetWay, Metrics}
import com.simplexportal.spatial.Tile.Way
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

// TODO: Review if the Test is implemented correctly. If I kill the system, why is not necessary to restart it?

class TileActorPersistSpec
    extends TestKit(ActorSystem("TileActorPersistSpec"))
    with ImplicitSender
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll
    with RTreeActorDataset {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
    File("target/journal").delete(true)
  }

  "RTree Actor Persistence" should {

    "recover properly" when {

      def createActorAndKillIt(networkId: String, kill: ActorRef => Unit): Unit = {
        val rTreeActor = system.actorOf(TileActor.props(networkId, bbox))
        exampleTileCommands foreach (command => rTreeActor ! command)
        receiveN(exampleTileCommands.size)
        kill(rTreeActor)
      }

      "the actor restart" in {
        createActorAndKillIt("recover-after-graceful-kill",
                             actor => actor ! PoisonPill)

        val rTreeActorRecovered =
          system.actorOf(TileActor.props("recover-after-graceful-kill", bbox))

        rTreeActorRecovered ! GetMetrics
        rTreeActorRecovered ! GetWay(100)
        expectMsg(Metrics(2, 6))
        expectMsg(Some(Way(100, 5, Map(10 -> "wayAttrValue"))))
      }

      "the actor died because Exception" in {
        createActorAndKillIt("recover-after-failure", actor => actor ! Kill)

        val rTreeActorRecovered =
          system.actorOf(TileActor.props("recover-after-failure", bbox))

        rTreeActorRecovered ! GetMetrics
        rTreeActorRecovered ! GetWay(100)
        expectMsg(Metrics(2, 6))
        expectMsg(Some(Way(100, 5, Map(10 -> "wayAttrValue"))))
      }
    }
  }

}
