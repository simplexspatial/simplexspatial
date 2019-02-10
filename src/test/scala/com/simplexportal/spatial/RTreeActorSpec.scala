package com.simplexportal.spatial

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class RTreeActorSpec(_system: ActorSystem) extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("RTreeActorSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "RTree Actor" should {

    "add the nodes" in {

    }

    "connect nodes adding edge information" in {

    }

  }

}
