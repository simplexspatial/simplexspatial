/*
 * Copyright (C) 2019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
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
  }

  "RTree Actor" should {

    "add the nodes" in {
      val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,10))))
      rTreeActor ! AddNode(Node(10, Location(5,5), Map(10L -> "17 Redwood Avenue")))

      rTreeActor ! GetNode(10)
      rTreeActor ! GetMetrics

      expectMsg(Some(Node(10, Location(5,5), Map(10L -> "17 Redwood Avenue"))))
      expectMsg(Metrics(1, 0))
    }

    "connect nodes adding edge information" when {
      "source and target are present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,10))))
        rTreeActor ! AddNode(Node(10, Location(5,5), Map(10L -> "source node")))
        rTreeActor ! AddNode(Node(11, Location(5,6), Map(10L -> "target node")))
        rTreeActor ! ConnectNodes(Edge(1L, 10L, 11L, Map(1L -> "bridge") ))

        rTreeActor ! GetNode(10)
        rTreeActor ! GetNode(11)
        rTreeActor ! GetMetrics

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))
        expectMsg(100.millis, Some(Node(10, Location(5,5), Map(10L -> "source node"), Set(expectedEdge))))
        expectMsg(100.millis, Some(Node(11, Location(5,6), Map(10L -> "target node"), Set(expectedEdge))))
        expectMsg(Metrics(2, 1))
      }

      "only source is present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,10))))
        rTreeActor ! AddNode(Node(10, Location(5,5), Map(10L -> "source node")))
        rTreeActor ! ConnectNodes(Edge(1L, 10L, 11L, Map(1L -> "bridge") ))

        rTreeActor ! GetNode(10)
        rTreeActor ! GetMetrics

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))
        expectMsg(100.millis, Some(Node(10, Location(5,5), Map(10L -> "source node"), Set(expectedEdge))))
        expectMsg(Metrics(1, 1))
      }

      "only target is present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,10))))
        rTreeActor ! AddNode(Node(11, Location(5,6), Map(10L -> "target node")))
        rTreeActor ! ConnectNodes(Edge(1L, 10L, 11L, Map(1L -> "bridge") ))

        rTreeActor ! GetNode(11)
        rTreeActor ! GetMetrics

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))
        expectMsg(100.millis, Some(Node(11, Location(5,6), Map(10L -> "target node"), Set(expectedEdge))))
        expectMsg(Metrics(1, 1))
      }

      "neither target nor source are present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,10))))
        rTreeActor ! ConnectNodes(Edge(1L, 10L, 11L, Map(1L -> "bridge") ))

        rTreeActor ! GetMetrics

        expectMsg(Metrics(0, 0))
      }

    }

  }

}
