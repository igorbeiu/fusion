name := "fusion"
organization := "net.asynchorswim"

scalaVersion := "2.12.2"
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseIgnoreUntrackedFiles := true

publishTo := Some("Sonatype Nexus Repository Manager"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2/")

sonatypeProfileName := "org.asynchorswim"

publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://(asynchorswim.net"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/asynchorswim/fusion"),
    "scm:git@github.com:asynchorswim/fusion.git"
  )
)

val akkaVersion = "2.5.3"
val json4sVersion = "3.5.2"
val kamonVersion = "0.6.7"
val quillVersion = "1.2.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-agent" % akkaVersion,
  "com.typesafe.akka" %% "akka-camel" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,
  "com.typesafe.akka" %% "akka-osgi" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-tck" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
  "com.typesafe" % "config" % "1.3.1",
  "org.json4s" %% "json4s-core" % json4sVersion,
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "io.kamon" %% "kamon-core" % kamonVersion,
  "io.getquill" %% "quill" % quillVersion,
  "io.getquill" %% "quill-cassandra" % quillVersion,

  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.0.0" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
