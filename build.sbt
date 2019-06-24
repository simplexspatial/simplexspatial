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
  resolvers += "osm4scala repo" at "http://dl.bintray.com/angelcervera/maven",
  scalaVersion := "2.12.6",
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

lazy val akkaVersion = "2.5.21"
lazy val scalatestVersion = "3.0.5"
lazy val leveldbVersion = "1.8"
lazy val betterFilesVersion = "3.7.0"

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "org.fusesource.leveldbjni" % "leveldbjni-all" % leveldbVersion
    ) ++ Seq(
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion,
      "com.github.pathikrit" %% "better-files" % betterFilesVersion
    ).map(_ % "test")
  )

lazy val loadOSM = (project in file("load_osm"))
  .settings(
    commonSettings,
    mainClass in assembly := Some("com.simplexportal.spatial.loadosm.Main"),
    
    libraryDependencies ++= Seq(
      "com.acervera.osm4scala" %% "osm4scala-core" % "1.0.1",
      "org.backuity.clist" %% "clist-core" % "3.5.1",
      "org.backuity.clist" %% "clist-macros" % "3.5.1" % "provided"
    )
  )
  .dependsOn(core)
