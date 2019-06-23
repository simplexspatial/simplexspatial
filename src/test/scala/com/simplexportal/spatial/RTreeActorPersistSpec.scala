/*
 * Copyright (C) 2019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

// scalastyle:off magic.number

package com.simplexportal.spatial

import akka.actor.{ActorRef, ActorSystem, Kill, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.simplexportal.spatial.RTreeActor.{GetMetrics, GetWay, Metrics}
import com.simplexportal.spatial.Tile.Way
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

// TODO: Review if the Test is implemented correctly. If I kill the system, why is not necessary to restart it?

class RTreeActorPersistSpec
    extends TestKit(ActorSystem("RTreeActorPersistSpec"))
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
        val rTreeActor = system.actorOf(RTreeActor.props(networkId, bbox))
        exampleTileCommands foreach (command => rTreeActor ! command)
        receiveN(exampleTileCommands.size)
        kill(rTreeActor)
      }

      "the actor restart" in {
        createActorAndKillIt("recover-after-graceful-kill",
                             actor => actor ! PoisonPill)

        val rTreeActorRecovered =
          system.actorOf(RTreeActor.props("recover-after-graceful-kill", bbox))

        rTreeActorRecovered ! GetMetrics
        rTreeActorRecovered ! GetWay(100)
        expectMsg(Metrics(2, 6))
        expectMsg(Some(Way(100, 5, Map("wayAttrKey" -> "wayAttrValue"))))
      }

      "the actor died because Exception" in {
        createActorAndKillIt("recover-after-failure", actor => actor ! Kill)

        val rTreeActorRecovered =
          system.actorOf(RTreeActor.props("recover-after-failure", bbox))

        rTreeActorRecovered ! GetMetrics
        rTreeActorRecovered ! GetWay(100)
        expectMsg(Metrics(2, 6))
        expectMsg(Some(Way(100, 5, Map("wayAttrKey" -> "wayAttrValue"))))
      }
    }
  }

}
