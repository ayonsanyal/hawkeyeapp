name := "common"

version := "1.0"

scalaVersion := "2.12.6"
val akkaVersion = "2.5.12"
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "org.json4s" %% "json4s-ext" % "3.2.11",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.11"
)
