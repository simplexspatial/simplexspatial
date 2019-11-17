import sbt.Keys.startYear

lazy val commonSettings = Seq(
  organization := "com.simplexportal.spatial",
  organizationHomepage := Some(url("http://www.simplexportal.com")),
  organizationName := "SimplexPortal Ltd",
  developers := List(
    Developer(
      "angelcervera",
      "Angel Cervera Claudio",
      "angelcervera@silyan.com",
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
    "1.8"
  ),*/
  test in assembly := {}
)

lazy val akkaVersion = "2.6.0"
lazy val scalatestVersion = "3.0.8"
lazy val leveldbVersion = "1.8"
lazy val betterFilesVersion = "3.8.0"
lazy val akkaPersistenceNowhereVersion = "1.0.2"

lazy val root = (project in file("."))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings(

  )
  .aggregate(protobufApi, core, loadOSM)

lazy val protobufApi = (project in file("protobuf-api"))
  .settings(
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
  )

lazy val core = (project in file("core"))
  .enablePlugins(AkkaGrpcPlugin)
  .enablePlugins(JavaAgent) // ALPN agent
  .settings(
    PB.protoSources in Compile += (resourceDirectory in (protobufApi, Compile)).value,
    akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Scala),
    akkaGrpcGeneratedSources := Seq(AkkaGrpc.Server)
  )
  .settings(
    commonSettings,
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test",
    mainClass in assembly := Some("com.simplexportal.spatial.Main"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion, // FIXME: Remove after update sbt-akka-grpc
      "org.fusesource.leveldbjni" % "leveldbjni-all" % leveldbVersion,
      "com.acervera.akka" %% "akka-persistence-nowhere" % akkaPersistenceNowhereVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ) ++ Seq(
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion,
      "com.github.pathikrit" %% "better-files" % betterFilesVersion
    ).map(_ % "test")
  )
  .dependsOn(protobufApi)


lazy val loadOSM = (project in file("load_osm"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    PB.protoSources in Compile += (resourceDirectory in (protobufApi, Compile)).value,
    akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client)
  )
  .settings(
    commonSettings,
    mainClass in assembly := Some("com.simplexportal.spatial.loadosm.Main"),
    libraryDependencies ++= Seq(
      "com.acervera.osm4scala" %% "osm4scala-core" % "1.0.1",
      "org.backuity.clist" %% "clist-core" % "3.5.1",
      "org.backuity.clist" %% "clist-macros" % "3.5.1" % "provided",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
  .dependsOn(protobufApi)

