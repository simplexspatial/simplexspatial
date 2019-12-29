/*
 * Copyright 2019 SimplexPortal Ltd
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

package com.simplexportal.spatial.index.grid.sessions

import com.simplexportal.spatial.index.grid.tile.{GetInternalNodeResponse, TileIdx, TileIndex, TileIndexEntityIdGen}
import com.simplexportal.spatial.index.grid.tile
import com.simplexportal.spatial.model.Location
import org.scalatest.{Matchers, TryValues, WordSpecLike}

import scala.util.Success

// scalastyle:off magic.number
class AddWaySessionSpec extends WordSpecLike with Matchers with TryValues {

  def node(id: Long, lat: Double, lon: Double): TileIndex.InternalNode =
    TileIndex.InternalNode(id, Location(lat, lon))

  "AddWaySessionSpec" when {

    "split list of nodes in shards" should {

      "retrieve splits in order 2x2" in {
        val nodes: Seq[TileIndex.InternalNode] = Seq(
          node(1, 30, -150), node(2, 50, -100), node(3, 30, -30),
          node(4, 50, 30),
          node(5, -30, 30), node(6, -30, 100),
          node(7, 30, 100), node(8, 30, 150)
        )
        val expectedShardedNodes: Seq[(TileIdx, Seq[TileIndex.InternalNode])] = Seq(
          (TileIdx(1,0), Seq( node(1, 30, -150), node(2, 50, -100), node(3, 30, -30), node(4, 50, 30))),
          (TileIdx(1,1), Seq( node(3, 30, -30), node(4, 50, 30), node(5, -30, 30))),
          (TileIdx(0,1), Seq( node(4, 50, 30), node(5, -30, 30), node(6, -30, 100), node(7, 30, 100))),
          (TileIdx(1,1), Seq( node(6, -30, 100), node(7, 30, 100), node(8, 30, 150)))
        )
        val tileEntityFn = new TileIndexEntityIdGen(2, 2)
        AddWaySession.splitNodesInShards(nodes, tileEntityFn) shouldBe expectedShardedNodes
      }

      "retrieve splits in order 2x4" in {
        val nodes: Seq[TileIndex.InternalNode] = Seq(
          node(1, 30, -150), node(2, 50, -100),
          node(3, 30, -30),
          node(4, 50, 30),
          node(5, -30, 30),
          node(6, -30, 100),
          node(7, 30, 100), node(8, 30, 150)
        )
        val expectedShardedNodes: Seq[(TileIdx, Seq[TileIndex.InternalNode])] = Seq(
          (TileIdx(1,0), Seq(node(1, 30, -150), node(2, 50, -100), node(3, 30, -30))),
          (TileIdx(1,1), Seq(node(2, 50, -100), node(3, 30, -30), node(4, 50, 30))),
          (TileIdx(1,2), Seq(node(3, 30, -30), node(4, 50, 30), node(5, -30, 30))),
          (TileIdx(0,2), Seq(node(4, 50, 30), node(5, -30, 30), node(6, -30, 100))),
          (TileIdx(0,3), Seq(node(5, -30, 30), node(6, -30, 100), node(7, 30, 100))),
          (TileIdx(1,3), Seq(node(6, -30, 100), node(7, 30, 100), node(8, 30, 150)))
        )
        val tileEntityFn = new TileIndexEntityIdGen(2, 4)
        AddWaySession.splitNodesInShards(nodes, tileEntityFn) shouldBe expectedShardedNodes
      }


      "retrieve splits in order 4x4" in {
        val nodes: Seq[TileIndex.InternalNode] = Seq(
          node(1, 30, -150), node(2, 50, -100),
          node(3, 30, -30),
          node(4, 50, 30),
          node(5, -30, 30),
          node(6, -30, 100),
          node(7, 30, 100), node(8, 30, 150)
        )
        val expectedShardedNodes: Seq[(TileIdx, Seq[TileIndex.InternalNode])] = Seq(
          (TileIdx(1,0), Seq(node(1, 30, -150), node(2, 50, -100), node(3, 30, -30))),
          (TileIdx(1,1), Seq(node(2, 50, -100), node(3, 30, -30), node(4, 50, 30))),
          (TileIdx(1,2), Seq(node(3, 30, -30), node(4, 50, 30), node(5, -30, 30))),
          (TileIdx(0,2), Seq(node(4, 50, 30), node(5, -30, 30), node(6, -30, 100))),
          (TileIdx(0,3), Seq(node(5, -30, 30), node(6, -30, 100), node(7, 30, 100))),
          (TileIdx(1,3), Seq(node(6, -30, 100), node(7, 30, 100), node(8, 30, 150)))
        )

        val tileEntityFn = new TileIndexEntityIdGen(2, 4)
        AddWaySession.splitNodesInShards(nodes, tileEntityFn) shouldBe expectedShardedNodes
      }

      "retrieve splits in order even if all is in one shard" in {
        val nodes: Seq[TileIndex.InternalNode] = Seq(
            node(1, 30, -150),
            node(2, 50, -100),
            node(3, 30, -30),
            node(4, 31, -31),
            node(5, 32, -32)
        )
        val expectedShardedNodes: Seq[(TileIdx, Seq[TileIndex.InternalNode])] = Seq(
          (TileIdx(1,0), Seq(
            node(1, 30, -150),
            node(2, 50, -100),
            node(3, 30, -30),
            node(4, 31, -31),
            node(5, 32, -32)
          ))
        )
        val tileEntityFn = new TileIndexEntityIdGen(2, 2)
        AddWaySession.splitNodesInShards(nodes, tileEntityFn) shouldBe expectedShardedNodes
      }

    }

    "validate nodes" should {

      "return a list of validated nodes, unpacked from Option" in {
        AddWaySession.validateNodes(
          Seq(
            GetInternalNodeResponse(
              10,
              Some(TileIndex.InternalNode(10, Location(10, 10)))
            ),
            GetInternalNodeResponse(
              11,
              Some(TileIndex.InternalNode(11, Location(11, 11)))
            ),
            tile.GetInternalNodeResponse(12, Some(TileIndex.InternalNode(12, Location(12, 12))))
          )
        ) shouldBe Success(
          Seq(
            TileIndex.InternalNode(10, Location(10, 10)),
            TileIndex.InternalNode(11, Location(11, 11)),
            TileIndex.InternalNode(12, Location(12, 12))
          )
        )
      }

      "throw an error if there is None node" in {
        AddWaySession
          .validateNodes(
            Seq(
              GetInternalNodeResponse(
                10,
                Some(TileIndex.InternalNode(10, Location(10, 10)))
              ),
              GetInternalNodeResponse(
                11,
                None
              ),
              tile.GetInternalNodeResponse(12, Some(TileIndex.InternalNode(12, Location(12, 12))))
            )
          )
          .failure
          .exception should have message "Node [11] not found."
      }

    }

  }
}
