name := "simplex-spatial"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.20"
lazy val scalatestVersion = "3.0.5"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion
  ) ++ Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion
  ).map(_ % "test")
