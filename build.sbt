name := "simplex-spatial"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.6"

fork := true

lazy val akkaVersion = "2.5.21"
lazy val scalatestVersion = "3.0.5"
lazy val leveldbVersion = "1.8"
lazy val betterFilesVersion = "3.7.0"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "org.fusesource.leveldbjni" % "leveldbjni-all" % leveldbVersion
  ) ++ Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion,
    "com.github.pathikrit" %% "better-files" % betterFilesVersion
  ).map(_ % "test")
