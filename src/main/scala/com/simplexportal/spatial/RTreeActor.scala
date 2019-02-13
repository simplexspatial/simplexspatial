/*
 * Copyright (C) 2019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

import akka.actor.{Actor, ActorLogging, Props}
import com.simplexportal.spatial.RTreeActor._

object RTreeActor {

  def props(boundingBox: Model.BoundingBox): Props = Props(new RTreeActor(boundingBox))

  sealed trait  RTreeCommands
  case class AddNode(node: Model.Node) extends RTreeCommands
  case class GetNode(id: Long) extends RTreeCommands
  case class ConnectNodes(edge: Model.Edge) extends RTreeCommands
  object GetMetrics extends RTreeCommands



  sealed trait RTreeDataTransfer
  case class Metrics(nodes: Long, edges: Long) extends RTreeDataTransfer

}

class RTreeActor(boundingBox: Model.BoundingBox) extends Actor with ActorLogging {

  // Internal representation of Nodes and Edges.
  sealed trait RTreeEvents
  protected case class Node(
    location: Model.Location,
    attributes: Model.Attributes,
    outs: Set[Long] = Set.empty,
    ins: Set[Long] = Set.empty
  ) extends RTreeEvents

  protected case class Edge(
    source: Long,
    target: Long,
    attributes: Model.Attributes
  ) extends RTreeEvents

  // TODO: For performance, should be replaced by a binary tree or mutable Map ??

  // Table with Nodes by Id.
  var nodes: Map[Long, Node] = Map.empty

  // Table with Edges by Id.
  var edges: Map[Long, Edge] = Map.empty

  override def receive: Receive = {

    case GetNode(id) =>
      sender ! nodes.get(id).map(node => reconstructNode(id, node))

    case GetMetrics =>
      sender ! Metrics(nodes.size, edges.size)

    case AddNode(modelNode) =>
      addNode(modelNode.id, Node(modelNode.location, modelNode.attributes))
      sender ! akka.Done

    case ConnectNodes(modelEdge) =>
      // Add "out" connection.
      val addedOut = nodes.get(modelEdge.source).map(n => addNode(modelEdge.source, n.copy(outs = n.outs + modelEdge.id))).isDefined
      // Add "in" connection.
      val addedIn = nodes.get(modelEdge.target).map(n => addNode(modelEdge.target, n.copy(ins = n.ins + modelEdge.id))).isDefined
      // Add edge data only if the node is present as In or Out.
      if(addedOut || addedIn) addEdge(modelEdge.id, Edge(modelEdge.source, modelEdge.target, modelEdge.attributes))
      sender ! akka.Done

  }

  private def addNode(id: Long, node: Node) = nodes = nodes + ( id -> node )

  private def addEdge(id: Long, edge: Edge) = edges = edges + ( id -> edge )

  private def reconstructEdge(id: Long, edge: Edge) =
    Model.Edge(id, edge.source, edge.target, edge.attributes)

  private def reconstructNode(id: Long, node: Node) =
    Model.Node(
      id,
      node.location,
      node.attributes,
      reconstructEdgeList(node.ins) ++ reconstructEdgeList(node.outs)
    )

  private def reconstructEdgeList(ids: Set[Long]) =
    ids.flatMap(edgeId => edges.get(edgeId).map(reconstructEdge(edgeId, _)))

}
