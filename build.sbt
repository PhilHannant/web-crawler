name := "web-crawler"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-mock" % "4.8.0" % "test",
  "org.specs2" %% "specs2-matcher" % "4.8.0" % "test",
  "org.jsoup" % "jsoup" % "1.12.1",
  "io.circe" %% "circe-parser" % "0.12.2",
  "io.circe" %% "circe-core" % "0.12.2",
  "io.circe" %% "circe-generic" % "0.12.2",
  "io.spray" %% "spray-json" % "1.3.5",
  "io.lemonlabs" %% "scala-uri" % "1.5.1",
  "org.scala-lang.modules" % "scala-jline" % "2.12.1")