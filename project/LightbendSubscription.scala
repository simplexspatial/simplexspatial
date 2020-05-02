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

import com.lightbend.cinnamon.sbt.Cinnamon
import com.lightbend.cinnamon.sbt.Cinnamon.CinnamonKeys.cinnamon
import sbt.Keys._
import sbt._

object LightbendSubscription {
  private val lightbendPrivateRepo = "./lightbend.sbt"

  private def doesReposExists = file(lightbendPrivateRepo).exists()

  def enablePlugin: Plugins = if (LightbendSubscription.doesReposExists) Cinnamon else Plugins.empty

  def addSettings: Seq[Def.SettingsDefinition] =
    if (doesReposExists) {
      println(s"Lightbend private repo config [${lightbendPrivateRepo}] exist so will be used.")
      Seq(
        cinnamon in run := true,
        cinnamon in test := true,
        libraryDependencies ++= Seq(
          Cinnamon.library.cinnamonCHMetrics,
          Cinnamon.library.cinnamonAkka,
          Cinnamon.library.cinnamonAkkaTyped,
          Cinnamon.library.cinnamonAkkaHttp,
          Cinnamon.library.cinnamonJvmMetricsProducer,
          Cinnamon.library.cinnamonPrometheus,
          Cinnamon.library.cinnamonPrometheusHttpServer,
          Cinnamon.library.cinnamonAkkaPersistence,
          Cinnamon.library.cinnamonAkkaStream,
          Cinnamon.library.jmxImporter,
          Cinnamon.library.cinnamonSlf4jEvents
        )
      )
    } else {
      println(s"Lightbend private repo config [${lightbendPrivateRepo}] does not exist so ignoring Cinnamon.")
      Seq(
        cinnamon := false
      )
    }
}
