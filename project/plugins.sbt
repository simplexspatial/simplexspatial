addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")

// gRPC
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "0.6.2")
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.5") // ALPN agent
