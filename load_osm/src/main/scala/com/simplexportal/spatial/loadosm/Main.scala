/*
 * Copyright 2019 SimplexPortal Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simplexportal.spatial.loadosm

import org.backuity.clist.Cli

object Main extends App {

  Cli.parse(args).withCommand(new Parameters) {
    case params if params.loadType == "akka" => AKKALoad.load(params.osmFile)
    case params if params.loadType == "akka-blocks" => AKKABlocksLoad.load(params.osmFile, params.blockSize)
    case params if params.loadType == "local" => LocalLoad.load(params.osmFile)
    case params => println(s"type ${params} not uspported. Only akka, local supported.")
  }

}
