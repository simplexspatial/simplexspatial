/*
 * Copyright (C) 20019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.simplexportal.spatial.Model.{BoundingBox, Edge, Location, Node}
import com.simplexportal.spatial.RTreeActor.{AddNode, ConnectNodes, GetNode}
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
      expectMsg(100.millis, Some(Node(10, Location(5,5), Map(10L -> "17 Redwood Avenue"))))
    }

    "connect nodes adding edge information" when {
      "source and target are present" in {
        val rTreeActor = system.actorOf(RTreeActor.props(BoundingBox(Location(1,1), Location(10,10))))
        rTreeActor ! AddNode(Node(10, Location(5,5), Map(10L -> "17 Redwood Avenue")))
        rTreeActor ! AddNode(Node(11, Location(5,6), Map(10L -> "19 Redwood Avenue")))
        rTreeActor ! ConnectNodes(Edge(1L, 10L, 11L, Map(1L -> "bridge") ))

        rTreeActor ! GetNode(10)
        rTreeActor ! GetNode(11)

        val expectedEdge = Edge(1, 10, 11, Map(1L -> "bridge"))
        expectMsg(100.millis, Some(Node(10, Location(5,5), Map(10L -> "17 Redwood Avenue"), Set(expectedEdge))))
        expectMsg(100.millis, Some(Node(11, Location(5,6), Map(10L -> "19 Redwood Avenue"), Set(expectedEdge))))
      }

      "only source is present" in {

      }

      "only target is present" in {

      }

      "neither target nor source are present" in {

      }

    }

  }

}
