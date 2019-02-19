/*
 * Copyright (C) 2019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

import akka.actor.{ActorRef, ActorSystem, Kill, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.simplexportal.spatial.Model.{BoundingBox, Edge, Location, Node}
import com.simplexportal.spatial.RTreeActor._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class RTreeActorSpec extends TestKit(ActorSystem("RTreeActorSpec"))
    with ImplicitSender
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
    File("target/journal").delete(true)
  }

  "RTree Actor" should {

    "add the nodes" in {
      val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,11))))
      rTreeActor ! AddNodeCommand(10, Location(5,5), Map(10L -> "17 Redwood Avenue"))

      rTreeActor ! GetNode(10)
      rTreeActor ! GetMetrics

      expectMsg(akka.Done)
      expectMsg(Some(Node(10, Location(5,5), Map(10L -> "17 Redwood Avenue"))))
      expectMsg(Metrics(1, 0))
    }

    "connect nodes adding edge information" when {
      "source and target are present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,12))))
        rTreeActor ! AddNodeCommand(10, Location(5,5), Map(10L -> "source node"))
        rTreeActor ! AddNodeCommand(11, Location(5,6), Map(10L -> "target node"), Set(ConnectNodesCommand(1L, 10L, 11L, Map(1L -> "bridge"))) )

        rTreeActor ! GetNode(10)
        rTreeActor ! GetNode(11)
        rTreeActor ! GetMetrics

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))

        expectMsg(akka.Done)
        expectMsg(akka.Done)
        expectMsg(akka.Done)
        expectMsg(100.millis, Some(Node(10, Location(5,5), Map(10L -> "source node"), Set(expectedEdge))))
        expectMsg(100.millis, Some(Node(11, Location(5,6), Map(10L -> "target node"), Set(expectedEdge))))
        expectMsg(Metrics(2, 1))
      }

      "only source is present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,13))))
        rTreeActor ! AddNodeCommand(10, Location(5,5), Map(10L -> "source node"), Set(ConnectNodesCommand(1L, 10L, 11L, Map(1L -> "bridge"))))

        rTreeActor ! GetNode(10)
        rTreeActor ! GetMetrics

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))

        expectMsg(akka.Done)
        expectMsg(akka.Done)
        expectMsg(100.millis, Some(Node(10, Location(5,5), Map(10L -> "source node"), Set(expectedEdge))))
        expectMsg(Metrics(1, 1))
      }

      "only target is present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,14))))
        rTreeActor ! AddNodeCommand(11, Location(5,6), Map(10L -> "target node"), Set(ConnectNodesCommand(1L, 10L, 11L, Map(1L -> "bridge"))))

        rTreeActor ! GetNode(11)
        rTreeActor ! GetMetrics

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))

        expectMsg(akka.Done)
        expectMsg(akka.Done)
        expectMsg(100.millis, Some(Node(11, Location(5,6), Map(10L -> "target node"), Set(expectedEdge))))
        expectMsg(Metrics(1, 1))
      }

      "neither target nor source are present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,15))))
        rTreeActor ! ConnectNodesCommand(1L, 10L, 11L, Map(1L -> "bridge") )

        rTreeActor ! GetMetrics

        expectMsg(akka.Done)
        expectMsg(Metrics(0, 0))
      }

    }

    "recover properly" when {

      def createActorAndKillIt(bbox: BoundingBox, kill: ActorRef => Unit) = {
        val rTreeActor = system.actorOf(Props(new RTreeActor(bbox)))
        rTreeActor ! AddNodeCommand(11, Location(5,6), Map(10L -> "target node"), Set(ConnectNodesCommand(1L, 10L, 11L, Map(1L -> "bridge"))))
        receiveN(2)
        kill(rTreeActor)
      }

      "the actor restart" in {
        val bbox = BoundingBox(Location(1,1), Location(10,15))
        createActorAndKillIt(bbox, actor => actor ! PoisonPill)

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))

        val rTreeActorRecovered = system.actorOf(RTreeActor.props(bbox))
        rTreeActorRecovered ! GetNode(11)
        rTreeActorRecovered ! GetMetrics

        expectMsg(100.millis, Some(Node(11, Location(5,6), Map(10L -> "target node"), Set(expectedEdge))))
        expectMsg(Metrics(1, 1))
      }

      "the actor died because Exception" in {
        val bbox = BoundingBox(Location(1,1), Location(10,16))
        createActorAndKillIt(bbox, actor => actor ! Kill)

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))

        val rTreeActorRecovered = system.actorOf(RTreeActor.props(bbox))
        rTreeActorRecovered ! GetNode(11)
        rTreeActorRecovered ! GetMetrics

        expectMsg(100.millis, Some(Node(11, Location(5,6), Map(10L -> "target node"), Set(expectedEdge))))
        expectMsg(Metrics(1, 1))
      }
    }
  }

}
