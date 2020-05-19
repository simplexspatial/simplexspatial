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

package com.simplexportal.spatial.index.grid.tile.impl

/**
  * The set of attributed per Way and Node are stored in a  Map where the key is an Int.
  * This id is generated and related to a String key.
  *
  * This trait is implementing all stuff related to this id: Int -> id: String dictionary.
  */
protected trait AttribsDictionary {
  this: TileIndex =>

  // Generate a tuple a map with all tagsIds and another with the value indexed by tagId.
  def attributesToDictionary(
      attributes: Map[String, String]
  ): (Map[Int, String], Map[Int, String]) =
    attributes.foldLeft((Map.empty[Int, String], Map.empty[Int, String])) {
      case ((dic, attrs), attr) => {
        val hash = attr._1.hashCode
        (dic + (hash -> attr._1), attrs + (hash -> attr._2))
      }
    }

  def dictionaryToAttributes(
      attrs: Map[Int, String]
  ): Map[String, String] =
    attrs.map { case (k, v) => tagsDic(k) -> v }
}
