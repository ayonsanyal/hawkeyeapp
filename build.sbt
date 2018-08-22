name := "hawkeyeapp"

lazy val commonSettings = Seq(
  organization := "",
  version := "0.1.0",
  scalaVersion := "2.12.6",
  mainClass in(Compile, run) := Some("com.server.Server")
)
lazy val root = (project in file(".")).settings(commonSettings: _*).
  aggregate(common, retrieveNews, server)
lazy val common = (project in file("common")).
  settings(commonSettings: _*)
lazy val retrieveNews = (project in file("news-streaming-service")).
  settings(commonSettings: _*).
  dependsOn(common)
lazy val server = (project in file("server")).
  settings(commonSettings: _*).dependsOn(common, retrieveNews)
val akkaVersion = "2.5.12"



