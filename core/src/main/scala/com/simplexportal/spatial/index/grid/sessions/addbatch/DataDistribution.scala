/*
 * Copyright 2020 SimplexPortal Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.simplexportal.spatial.index.grid.sessions.addbatch

import com.simplexportal.spatial.index.grid.lookups.{LookUpNodeEntityIdGen, LookUpWayEntityIdGen}
import com.simplexportal.spatial.index.grid.tile.actor.{TileIdx, TileIndexEntityIdGen}
import com.simplexportal.spatial.index.grid.tile.{actor => tile}
import com.simplexportal.spatial.index.protocol._
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

protected trait DataDistribution {

  private val logger =
    LoggerFactory.getLogger("com.simplexportal.spatial.index.grid.sessions.addbatch.DataDistribution")

  /**
    * Generate a map of NodeId -> TileIdx from the definition of nodes.
    *
    * @param commands
    * @param tileIndexEntityIdGen
    * @return Map with nodeId -> TileIdx
    */
  def extractNodesTileIdxs(commands: Seq[GridBatchCommand])(
      implicit tileIndexEntityIdGen: TileIndexEntityIdGen
  ): Map[Long, tile.TileIdx] = commands.foldLeft(Map.empty[Long, tile.TileIdx]) {
    case (nodesIdxs, cmd) =>
      cmd match {
        case n: GridAddNode =>
          (
            nodesIdxs + (n.id -> tileIndexEntityIdGen.tileIdx(n.lat, n.lon))
          )
        case _ => nodesIdxs
      }
  }

  def groupByTileIdx(
      cmds: Seq[GridBatchCommand],
      locationsIdx: Map[Long, tile.TileIdx]
  ): Map[tile.TileIdx, Seq[GridBatchCommand]] =
    cmds.foldLeft(Map.empty[tile.TileIdx, Seq[GridBatchCommand]]) {
      case (acc, n: GridAddNode) =>
        val tileIdx = locationsIdx(n.id)
        acc + (tileIdx -> (acc.getOrElse(tileIdx, Seq()) :+ n))
      case (acc, w: GridAddWay) =>
        splitWayByTile(w, locationsIdx).foldLeft(acc) {
          case (acc, (tileIdx, way)) =>
            acc + (tileIdx -> (acc.getOrElse(tileIdx, Seq()) :+ way))
        }
    }

  def splitWayByTile(
      way: GridAddWay,
      nodeLocs: Map[Long, TileIdx]
  ): Seq[(tile.TileIdx, GridAddWay)] =
    splitWayNodesPerTile(way.nodeIds, nodeLocs).map {
      case (tileIdx, nodeIds) => (tileIdx, way.copy(nodeIds = nodeIds))
    }

  /**
    * It will split a way into a set of ways bounded to a tile.
    * It will add connector as well.
    *
    * @param nodeIds Original sequence of node ids that define the way.
    * @param nodeLocs index of nodesId -> tileIdx
    * @return Pairs tileIdx, Way fragment.
    */
  def splitWayNodesPerTile(
      nodeIds: Seq[Long],
      nodeLocs: Map[Long, tile.TileIdx]
  ): Seq[(tile.TileIdx, Seq[Long])] = {

    @tailrec
    def rec(
        nodes: Seq[Long],
        acc: Seq[(tile.TileIdx, Seq[Long])],
        currentShard: (tile.TileIdx, Seq[Long])
    ): Seq[(tile.TileIdx, Seq[Long])] = {
      nodes match {
        case Nil => acc :+ currentShard
        case nodeId :: tail =>
          val entityId = nodeLocs(nodeId)
          val updated_shard = (currentShard._1, currentShard._2 :+ nodeId)
          if (entityId == currentShard._1) {
            rec(tail, acc, updated_shard)
          } else {
            rec(
              tail,
              acc :+ updated_shard,
              (entityId, currentShard._2.last +: Seq(nodeId))
            )
          }
      }
    }

    rec(
      nodeIds.tail,
      Seq.empty,
      (nodeLocs(nodeIds.head), Seq(nodeIds.head))
    )
  }

  protected def splitLookUps(
      cmdsPerTileIdx: Map[TileIdx, Seq[GridBatchCommand]]
  ): (Map[String, Seq[(Long, TileIdx)]], Map[String, Seq[(Long, TileIdx)]]) = {

    @tailrec
    def rec(
        remaining: Seq[(TileIdx, GridBatchCommand)],
        nodes: Map[String, Seq[(Long, TileIdx)]],
        ways: Map[String, Seq[(Long, TileIdx)]]
    ): (Map[String, Seq[(Long, TileIdx)]], Map[String, Seq[(Long, TileIdx)]]) =
      remaining match {
        case Seq() => (nodes, ways)
        case head +: tail =>
          head match {
            case (tileIdx, GridAddNode(id, _, _, _, _)) =>
              val shard = LookUpNodeEntityIdGen.entityId(id)
              val itemsPerShard = nodes.getOrElse(shard, Seq.empty) :+ (id, tileIdx)
              rec(
                tail,
                nodes + (shard -> itemsPerShard),
                ways
              )
            case (tileIdx, GridAddWay(id, _, _, _)) =>
              val shard = LookUpWayEntityIdGen.entityId(id)
              val itemsPerShard = ways.getOrElse(shard, Seq.empty) :+ (id, tileIdx)
              rec(
                tail,
                nodes,
                ways + (shard -> itemsPerShard)
              )
            case (_, cmd) =>
              logger.error(s"Type not supported: ${cmd}")
              throw new Exception(s"Type not supported: ${cmd}")
          }
      }

    val actions: Seq[(TileIdx, GridBatchCommand)] = cmdsPerTileIdx.toSeq.flatMap {
      case (tileIdx, actions) => actions.map(action => (tileIdx, action))
    }

    rec(actions, Map.empty, Map.empty)
  }
}
