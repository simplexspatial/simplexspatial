/*
 * Copyright (C) 2019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

// scalastyle:off magic.number

package com.simplexportal.spatial

import akka.actor.{ActorRef, ActorSystem, Kill, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.simplexportal.spatial.RTreeActor._
import com.simplexportal.spatial.Tile.{Node, Way}
import com.simplexportal.spatial.model._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class RTreeActorSpec
    extends TestKit(ActorSystem("RTreeActorSpec"))
    with ImplicitSender
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll
    with RTreeActorDataset {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
    File("target/journal").delete(true)
  }

  "RTree Actor" should {

    "add the nodes" in {
      val rTreeActor = system.actorOf(RTreeActor.props("add-nodes-test", bbox))
      rTreeActor ! AddNode(10, 5, 5, Map("nodeAttrKey" -> "nodeAttrValue"))

      rTreeActor ! GetNode(10)
      rTreeActor ! GetMetrics

      expectMsg(akka.Done)
      expectMsg(
        Some(Node(10, Location(5, 5), Map("nodeAttrKey" -> "nodeAttrValue"))))
      expectMsg(Metrics(0, 1))
    }

    "connect nodes using ways" in {
      val rTreeActor =
        system.actorOf(RTreeActor.props("connect-nodes-using-ways-test", bbox))
      exampleTileCommands foreach (command => rTreeActor ! command)

      ignoreMsg { case msg => msg == akka.Done }

      rTreeActor ! GetMetrics
      rTreeActor ! GetWay(100)
      expectMsg(Metrics(2, 6))
      expectMsg(Some(Way(100, 5, Map("wayAttrKey" -> "wayAttrValue"))))
    }

  }

}
