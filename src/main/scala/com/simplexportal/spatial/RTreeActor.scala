/*
 * Copyright (C) 20019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

import akka.actor.{Actor, ActorLogging, Props}
import com.simplexportal.spatial.RTreeActor.{AddNode, ConnectNodes, GetNode}

object RTreeActor {

  def props(boundingBox: Model.BoundingBox): Props = Props(new RTreeActor(boundingBox))

  sealed  trait  RTreeActions
  case class AddNode(node: Model.Node) extends RTreeActions
  case class GetNode(id: Long) extends RTreeActions
  case class ConnectNodes(edge: Model.Edge) extends RTreeActions

}

class RTreeActor(boundingBox: Model.BoundingBox) extends Actor with ActorLogging {

  // Internal representation of Nodes and Edges.

  protected case class Node(
    location: Model.Location,
    attributes: Model.Attributes,
    outs: Set[Long] = Set.empty,
    ins: Set[Long] = Set.empty
  )

  protected case class Edge(attributes: Model.Attributes)

  // TODO: For performance, should be replaced by a binary tree or mutable Map ??

  // Table with Nodes by Id.
  var nodes: Map[Long, Node] = Map.empty

  // Table with Edges by Id.
  var edges: Map[Long, Edge] = Map.empty

  override def receive: Receive = {

    case GetNode(id) =>
      sender ! nodes.get(id).map(node => reconstructNode(id, node))

    case AddNode(modelNode) =>
      addNode(modelNode.id, Node(modelNode.location, modelNode.attributes))

    case ConnectNodes(modelEdge) =>
      // Add "out" connection.
      def addOut = nodes.get(modelEdge.source).map(n => addNode(modelEdge.id, n.copy(outs = n.outs + modelEdge.id)))
      // Add "in" connection.
      def addIn = nodes.get(modelEdge.target).map(n => addNode(modelEdge.id, n.copy(ins = n.ins + modelEdge.id)))
      // Add edge data only if the node is present as In or Out.
      if(addOut.isDefined || addIn.isDefined) addEdge(modelEdge.id, Edge(modelEdge.attributes))

  }

  private def addNode(id: Long, node: Node) = nodes = nodes + ( id -> node )

  private def addEdge(id: Long, edge: Edge) = edges = edges + ( id -> edge )

  private def reconstructNode(id: Long, node: Node) = Model.Node(id, node.location, node.attributes) // TODO: Fill with Edges data.


}
