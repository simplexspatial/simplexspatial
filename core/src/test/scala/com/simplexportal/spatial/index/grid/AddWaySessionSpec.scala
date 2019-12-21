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

package com.simplexportal.spatial.index.grid

import com.simplexportal.spatial.model.Location
import org.scalatest.{Matchers, TryValues, WordSpecLike}

import scala.util.Success

// scalastyle:off magic.number
class AddWaySessionSpec extends WordSpecLike with Matchers with TryValues {

  def node(id: Long, lat: Double, lon: Double): TileIndex.Node =
    TileIndex.Node(id, Location(lat, lon))

  "AddWaySessionSpec" when {

    "split list of nodes in shards" should {

      "retrieve splits in order 2x2" in {
        val nodes: Seq[TileIndex.Node] = Seq(
          node(1, 30, -150), node(2, 50, -100), node(3, 30, -30),
          node(4, 50, 30),
          node(5, -30, 30), node(6, -30, 100),
          node(7, 30, 100), node(8, 30, 150)
        )
        val expectedShardedNodes: Seq[(String, Seq[TileIndex.Node])] = Seq(
          ("1_0", Seq( node(1, 30, -150), node(2, 50, -100), node(3, 30, -30), node(4, 50, 30))),
          ("1_1", Seq( node(3, 30, -30), node(4, 50, 30), node(5, -30, 30))),
          ("0_1", Seq( node(4, 50, 30), node(5, -30, 30), node(6, -30, 100), node(7, 30, 100))),
          ("1_1", Seq( node(6, -30, 100), node(7, 30, 100), node(8, 30, 150)))
        )
        val tileEntityFn = new TileIndexEntityIdGen(2, 2)
        AddWaySession.splitNodesInShards(nodes, tileEntityFn) shouldBe expectedShardedNodes
      }

      "retrieve splits in order 2x4" in {
        val nodes: Seq[TileIndex.Node] = Seq(
          node(1, 30, -150), node(2, 50, -100),
          node(3, 30, -30),
          node(4, 50, 30),
          node(5, -30, 30),
          node(6, -30, 100),
          node(7, 30, 100), node(8, 30, 150)
        )
        val expectedShardedNodes: Seq[(String, Seq[TileIndex.Node])] = Seq(
          ("1_0", Seq(node(1, 30, -150), node(2, 50, -100), node(3, 30, -30))),
          ("1_1", Seq(node(2, 50, -100), node(3, 30, -30), node(4, 50, 30))),
          ("1_2", Seq(node(3, 30, -30), node(4, 50, 30), node(5, -30, 30))),
          ("0_2", Seq(node(4, 50, 30), node(5, -30, 30), node(6, -30, 100))),
          ("0_3", Seq(node(5, -30, 30), node(6, -30, 100), node(7, 30, 100))),
          ("1_3", Seq(node(6, -30, 100), node(7, 30, 100), node(8, 30, 150)))
        )
        val tileEntityFn = new TileIndexEntityIdGen(2, 4)
        AddWaySession.splitNodesInShards(nodes, tileEntityFn) shouldBe expectedShardedNodes
      }


      "retrieve splits in order 4x4" in {
        val nodes: Seq[TileIndex.Node] = Seq(
          node(1, 30, -150), node(2, 50, -100),
          node(3, 30, -30),
          node(4, 50, 30),
          node(5, -30, 30),
          node(6, -30, 100),
          node(7, 30, 100), node(8, 30, 150)
        )
        val expectedShardedNodes: Seq[(String, Seq[TileIndex.Node])] = Seq(
          ("1_0", Seq(node(1, 30, -150), node(2, 50, -100), node(3, 30, -30))),
          ("1_1", Seq(node(2, 50, -100), node(3, 30, -30), node(4, 50, 30))),
          ("1_2", Seq(node(3, 30, -30), node(4, 50, 30), node(5, -30, 30))),
          ("0_2", Seq(node(4, 50, 30), node(5, -30, 30), node(6, -30, 100))),
          ("0_3", Seq(node(5, -30, 30), node(6, -30, 100), node(7, 30, 100))),
          ("1_3", Seq(node(6, -30, 100), node(7, 30, 100), node(8, 30, 150)))
        )

        val tileEntityFn = new TileIndexEntityIdGen(2, 4)
        AddWaySession.splitNodesInShards(nodes, tileEntityFn) shouldBe expectedShardedNodes
      }

      "retrieve splits in order even if all is in one shard" in {
        val nodes: Seq[TileIndex.Node] = Seq(
            node(1, 30, -150),
            node(2, 50, -100),
            node(3, 30, -30),
            node(4, 31, -31),
            node(5, 32, -32)
        )
        val expectedShardedNodes: Seq[(String, Seq[TileIndex.Node])] = Seq(
          ("1_0", Seq(
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
            TileIndexActor.GetNodeResponse(
              10,
              Some(TileIndex.Node(10, Location(10, 10)))
            ),
            TileIndexActor.GetNodeResponse(
              11,
              Some(TileIndex.Node(11, Location(11, 11)))
            ),
            TileIndexActor
              .GetNodeResponse(12, Some(TileIndex.Node(12, Location(12, 12))))
          )
        ) shouldBe Success(
          Seq(
            TileIndex.Node(10, Location(10, 10)),
            TileIndex.Node(11, Location(11, 11)),
            TileIndex.Node(12, Location(12, 12))
          )
        )
      }

      "throw an error if there is None node" in {
        AddWaySession
          .validateNodes(
            Seq(
              TileIndexActor.GetNodeResponse(
                10,
                Some(TileIndex.Node(10, Location(10, 10)))
              ),
              TileIndexActor.GetNodeResponse(
                11,
                None
              ),
              TileIndexActor
                .GetNodeResponse(12, Some(TileIndex.Node(12, Location(12, 12))))
            )
          )
          .failure
          .exception should have message "Node [11] not found."
      }

    }

  }
}
