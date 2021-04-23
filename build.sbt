import sbtassembly.MergeStrategy

name := "tweet-grabber"

version := "0.1"

scalaVersion := "2.13.4"

val akkaVersion = "2.6.12"
val akkaHttpVersion = "10.2.3"
val alpakkaMqttVersion = "2.0.2"
val wsStandaloneVersion = "2.1.3"
val logbackVersion = "1.2.3"
val prometheusVersion = "0.10.0"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,

  "com.typesafe.play" %% "play-ahc-ws-standalone" % wsStandaloneVersion,
  "com.typesafe.play" %% "play-ws-standalone-json" % wsStandaloneVersion,

  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

  "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % alpakkaMqttVersion,

  "io.prometheus" % "simpleclient" % prometheusVersion,
  "io.prometheus" % "simpleclient_hotspot" % prometheusVersion,
  "io.prometheus" % "simpleclient_httpserver" % prometheusVersion,

  "com.typesafe" % "config" % "1.4.1",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.6",

  "org.scalatest" %% "scalatest" % "3.2.7" % "test",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.7.0" % "test"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", "LICENSE") => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
