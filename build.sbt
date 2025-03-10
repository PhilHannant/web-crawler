name := "web-crawler"

version := "0.1"

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-mock" % "4.20.0" % Test,
  "org.specs2" %% "specs2-matcher" % "4.20.0" % Test,
  "org.jsoup" % "jsoup" % "1.16.1",
  "io.circe" %% "circe-parser" % "0.15.0-M1",
  "io.circe" %% "circe-core" % "0.15.0-M1",
  "io.circe" %% "circe-generic" % "0.15.0-M1",
  "io.spray" %% "spray-json" % "1.3.6",
  "io.lemonlabs" %% "scala-uri" % "4.0.0",
  "com.typesafe.akka" %% "akka-http" % "10.2.10",
  "com.typesafe.akka" %% "akka-stream" % "2.6.20",
  "org.jline" % "jline" % "3.29.0"
)

parallelExecution in Test := false