/*
 * Copyright (C) 2019 SimplexPortal Ltd. <https://www.simplexportal.com>
 */

package com.simplexportal.spatial

import akka.actor.{Actor, ActorLogging, Props}
import com.simplexportal.spatial.RTreeActor._
import akka.persistence._
import com.simplexportal.spatial.Model.{Attributes, Edge, Location}
import collection.immutable.Seq

object RTreeActor {

  def props(boundingBox: Model.BoundingBox): Props = Props(new RTreeActor(boundingBox))

  // TODO: Parameters of the commands should be not other Messages. Try to use parameters directly.
  //       For example: AddNode(id: Long, location: Location)
  sealed trait  RTreeCommands

  case class AddNodeCommand(
    id: Long,
    location: Location,
    attributes: Attributes,
    edges: Set[ConnectNodesCommand] = Set.empty
  ) extends RTreeCommands

  case class ConnectNodesCommand(
    id: Long,
    source: Long,
    target: Long,
    attributes: Attributes
  ) extends RTreeCommands

  case class GetNode(id: Long) extends RTreeCommands

  object GetMetrics extends RTreeCommands

  sealed trait RTreeDataTransfer
  case class Metrics(nodes: Long, edges: Long) extends RTreeDataTransfer

  // Internal representation of Nodes and Edges.

  protected case class NodeIntRepr(
    location: Model.Location,
    attributes: Model.Attributes,
    outs: Set[Long] = Set.empty,
    ins: Set[Long] = Set.empty
  )

  protected case class EdgeIntRepr(
    source: Long,
    target: Long,
    attributes: Model.Attributes
  )

  // Persisted messages.

  sealed trait  RTreeEvents
  case class AddNodeEvent(id: Long, node:NodeIntRepr) extends RTreeEvents
  case class AddEdgeEvent(id: Long, edge: EdgeIntRepr) extends RTreeEvents

}

class RTreeActor(boundingBox: Model.BoundingBox) extends PersistentActor with ActorLogging {

  override def persistenceId: String = s"rtree-status-[(${boundingBox.min.lon},${boundingBox.min.lat}),(${boundingBox.max.lon},${boundingBox.max.lat})]"



  // TODO: For performance, should be replaced by a binary tree or mutable Map ??

  // Table with Nodes by Id.
  var nodes: Map[Long, NodeIntRepr] = Map.empty

  // Table with Edges by Id.
  var edges: Map[Long, EdgeIntRepr] = Map.empty


  override def receiveCommand: Receive = {

    case GetNode(id) =>
      sender ! nodes.get(id).map(node => reconstructNode(id, node))

    case GetMetrics =>
      sender ! Metrics(nodes.size, edges.size)

    case AddNodeCommand(id, location, attributes, edges) =>
      persistAll( buildAddNodeEvents(id, location, attributes, edges) ) { event =>
        event match {
          case e: AddNodeEvent => processAddNodeEvent(e)
          case e: AddEdgeEvent => processAddEdgeEvent(e)
        }
        sender ! akka.Done
      }

    case ConnectNodesCommand(id, source, target, attributes) =>
      persist( buildAddEdgeEvent(id, source, target, attributes) ) { event =>
        processAddEdgeEvent(event)
        sender ! akka.Done
      }

  }

  def buildEvents(): scala.collection.immutable.Seq[AddNodeEvent] = ???

  override def receiveRecover: Receive = {
    case event: AddNodeEvent => processAddNodeEvent(event)
    case event: AddEdgeEvent => processAddEdgeEvent(event)
  }

  def processEvents: Receive = {
    case event: AddNodeEvent => processAddNodeEvent(event)
    case event: AddEdgeEvent => processAddEdgeEvent(event)
  }

  private def buildAddNodeEvents(id: Long, location: Location, attributes: Attributes, edges: Set[ConnectNodesCommand]) =
    Seq(AddNodeEvent(id, NodeIntRepr(location, attributes))) ++ edges.map(e => buildAddEdgeEvent(e.id, e.source, e.target, e.attributes))

  private def buildAddEdgeEvent(id: Long, source: Long, target: Long, attributes: Attributes): AddEdgeEvent = AddEdgeEvent(id, EdgeIntRepr(source, target, attributes))

  private def processAddNodeEvent(event: AddNodeEvent) = addNodeIntRepr(event.id, event.node)

  private def processAddEdgeEvent(event: AddEdgeEvent) = {
    // Add "out" connection.
    val addedOut = nodes.get(event.edge.source).map(n => addNodeIntRepr(event.edge.source, n.copy(outs = n.outs + event.id))).isDefined
    // Add "in" connection.
    val addedIn = nodes.get(event.edge.target).map(n => addNodeIntRepr(event.edge.target, n.copy(ins = n.ins + event.id))).isDefined
    // Add edge data only if the node is present as In or Out.
    if(addedOut || addedIn) addEdgeIntRepr(event.id, EdgeIntRepr(event.edge.source, event.edge.target, event.edge.attributes))
  }

  private def addNodeIntRepr(id: Long, node: NodeIntRepr) = nodes = nodes + ( id -> node )

  private def addEdgeIntRepr(id: Long, edge: EdgeIntRepr) = edges = edges + ( id -> edge )

  private def reconstructEdge(id: Long, edge: EdgeIntRepr) =
    Model.Edge(id, edge.source, edge.target, edge.attributes)

  private def reconstructNode(id: Long, node: NodeIntRepr) =
    Model.Node(
      id,
      node.location,
      node.attributes,
      reconstructEdgeList(node.ins) ++ reconstructEdgeList(node.outs)
    )

  private def reconstructEdgeList(ids: Set[Long]) =
    ids.flatMap(edgeId => edges.get(edgeId).map(reconstructEdge(edgeId, _)))

}
