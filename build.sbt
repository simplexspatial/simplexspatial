import com.typesafe.sbt.MultiJvmPlugin.multiJvmSettings
import sbt.Keys.{description, startYear}

Global / onChangedBuildSource := ReloadOnSourceChanges

scapegoatVersion in ThisBuild := "1.3.11"

lazy val commonSettings = Seq(
  scapegoatIgnoredFiles := Seq(".*/src_managed/.*"),
  organization := "com.simplexportal.spatial",
  organizationHomepage := Some(url("http://www.simplexportal.com")),
  organizationName := "SimplexPortal Ltd",
  maintainer := "angelcervera@simplexportal.com",
  developers := List(
    Developer(
      "angelcervera",
      "Angel Cervera Claudio",
      "angelcervera@simplexportal.com",
      url("http://github.com/angelcervera")
    )
  ),
  startYear := Some(2019),
  licenses += ("Apache-2.0", new URL(
    "https://www.apache.org/licenses/LICENSE-2.0.txt"
  )),
  version := "0.0.1-SNAPSHOT",
  fork := true,
  resolvers += "osm4scala repo" at "https://dl.bintray.com/angelcervera/maven",
  scalaVersion := "2.12.10",
  scalacOptions ++= Seq("-deprecation", "-feature")
//  Compile / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
//  Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
//  run / javaOptions ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
  /*  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding",
    "utf8",
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:_",
    "-Xlint",
    "-Xlog-reflective-calls"
  ),
  javacOptions ++= Seq(
    "-Xlint:all",
    "-source",
    "1.8",
    "-target",
    "1.8",
    "-parameters"
  ),*/
)

lazy val akkaVersion = "2.6.3"
lazy val scalatestVersion = "3.1.0"
lazy val leveldbVersion = "1.8"
lazy val betterFilesVersion = "3.8.0"
lazy val akkaPersistenceNowhereVersion = "1.0.2"
lazy val akkaKryoSerializationVersion = "1.1.0"
lazy val scalaUUIDVersion = "0.3.1"
lazy val jtsVersion = "1.16.1"

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "SimplexSpatial",
    description := "Geospatial distributed server"
  )
  .aggregate(protobufApi, core, loadOSM)

lazy val protobufApi = (project in file("protobuf-api"))
  .settings(
    name := "protobuf-api",
    description := "Protobuf API definition"
  )

lazy val grpcClientScala = (project in file("grpc-client-scala"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    PB.protoSources in Compile += (resourceDirectory in (protobufApi, Compile)).value,
    akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client)
  )
  .settings(
    commonSettings,
    name := "grpc-client-scala",
    description := "gRPC Client for Scala"
  )
  .dependsOn(protobufApi)

lazy val core = (project in file("core"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(AkkaGrpcPlugin)
  .enablePlugins(JavaAgent) // ALPN agent
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .settings(multiJvmSettings: _*)
  .settings(parallelExecution in Test := false)
  .settings(
    PB.protoSources in Compile += (resourceDirectory in (protobufApi, Compile)).value,
    akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Scala),
    akkaGrpcGeneratedSources := Seq(AkkaGrpc.Server)
  )
  .settings(
    commonSettings,
    name := "simplexspatial-core",
    description := "Core",
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test",
    mainClass in (Compile, packageBin) := Some(
      "com.simplexportal.spatial.Main"
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "io.altoo" %% "akka-kryo-serialization" % akkaKryoSerializationVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion, // FIXME: Remove after update sbt-akka-grpc
      "org.fusesource.leveldbjni" % "leveldbjni-all" % leveldbVersion,
      "com.acervera.akka" %% "akka-persistence-nowhere" % akkaPersistenceNowhereVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.jvm.uuid" %% "scala-uuid" % scalaUUIDVersion,
      "org.locationtech.jts" % "jts-core" % jtsVersion
    ) ++ Seq(
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion,
      "org.scalactic" %% "scalactic" % scalatestVersion,
      "com.github.pathikrit" %% "better-files" % betterFilesVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion
    ).map(_ % Test)
  )
  .enablePlugins(UniversalPlugin)
  .enablePlugins(BashStartScriptPlugin)
  .enablePlugins(LauncherJarPlugin)
  .settings(
    packageDescription := "SimplexSpatial Server"
  )
  .dependsOn(protobufApi, grpcClientScala % "test->compile")

lazy val loadOSM = (project in file("load_osm"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonSettings,
    name := "simplexspatial-osm-loader",
    description := "OSM Loader",
    mainClass in (Compile, packageBin) := Some(
      "com.simplexportal.spatial.loadosm.Main"
    ),
    libraryDependencies ++= Seq(
      "com.acervera.osm4scala" %% "osm4scala-core" % "1.0.1",
      "org.backuity.clist" %% "clist-core" % "3.5.1",
      "org.backuity.clist" %% "clist-macros" % "3.5.1" % "provided",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
  .dependsOn(grpcClientScala)
