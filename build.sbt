name := "fusion"
organization := "asynchorswim"
version := "1.0.0"

scalaVersion := "2.12.2"
crossScalaVersions := Seq("2.11.11", "2.12.2")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.2",
  "com.typesafe.akka" %% "akka-agent" % "2.5.2",
  "com.typesafe.akka" %% "akka-camel" % "2.5.2",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.2",
  "com.typesafe.akka" %% "akka-cluster-metrics" % "2.5.2",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.2",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.2",
  "com.typesafe.akka" %% "akka-distributed-data" % "2.5.2",
  "com.typesafe.akka" %% "akka-osgi" % "2.5.2",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.2",
  "com.typesafe.akka" %% "akka-persistence-query" % "2.5.2",
  "com.typesafe.akka" %% "akka-persistence-tck" % "2.5.2",
  "com.typesafe.akka" %% "akka-remote" % "2.5.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.2",
  "com.typesafe.akka" %% "akka-stream" % "2.5.2",
  "com.typesafe.akka" %% "akka-typed" % "2.5.2",
  "com.typesafe.akka" %% "akka-contrib" % "2.5.2",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-stream-kafka" % "+",
  "com.typesafe" % "config" % "1.3.1",
  "org.json4s" %% "json4s-core" % "+",
  "org.json4s" %% "json4s-jackson" % "+",
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.0.0" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.2" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.2" % "test",
  "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.5.2" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
