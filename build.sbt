name := "tweet-grabber"

version := "0.1"

scalaVersion := "2.13.4"

val akkaHttpVersion = "10.2.3"
val alpakkaMqttVersion = "2.0.2"

val AkkaVersion = "2.6.12"

assemblyMergeStrategy in assembly := {
  case "application.conf"            => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.1.3",
  "com.typesafe.play" %% "play-ws-standalone-json" % "2.1.3",

  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

  "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % alpakkaMqttVersion,

  "io.prometheus" % "simpleclient" % "0.10.0",
  "io.prometheus" % "simpleclient_hotspot" % "0.10.0",
  "io.prometheus" % "simpleclient_httpserver" % "0.10.0",

  "com.typesafe" % "config" % "1.4.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "org.scalatest" %% "scalatest" % "3.2.7" % "test",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.7.0" % "test"
)

