/*
 * Copyright (C) 20019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

import akka.actor.{Actor, ActorLogging, Props}
import com.simplexportal.spatial.RTreeActor.AddNode

object RTreeActor {

  def props(boundingBox: Model.BoundingBox): Props = Props(new RTreeActor(boundingBox))

  sealed  trait  RTreeActions
  case class AddNode(node: Model.Node) extends RTreeActions
  case class ConnectNodes(edge: Model.Edge) extends RTreeActions

}

class RTreeActor(boundingBox: Model.BoundingBox) extends Actor with ActorLogging {

  // Internal representation of Nodes and Edges.

  case class Node(location: Model.Location,attributes: Model.Attributes)
  case class Edge(attributes: Model.Attributes)

  // TODO: For performance, should be replaced by a binary tree or mutable Map

  // Tables containing entities data.

  // Table with Nodes by Id.
  var nodes: Map[Long, Node] = Map.empty

  // Table with Edges by Id.
  var edges: Map[Long, Edge] = Map.empty


  // Tables defining relations between Nodes and Edges.
  // This is the representation of the network.
  // Every "In Edge" of a Node is going to be always the "Out Edge" of another node.
  // In Edge -> Node -> Out Edge.

  // Table with all outs edges in a node. Node id -> Set[Edge Ids]
  var ins: Map[Long, Set[Long]] = Map.empty

  // Table with all ins edges in a node. Node id -> Set[Edge Outs]
  var outs: Map[Long, Set[Long]] = Map.empty

  override def receive: Receive = {
    case AddNode(modelNode) => ???
    case Edge(modelEdge) => ???
  }



}
