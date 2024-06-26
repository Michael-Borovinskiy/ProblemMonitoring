ThisBuild / version := "1.0"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "analyticalstreams"
  )

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.3.2",
  "org.apache.spark" %% "spark-streaming" % "3.3.2",
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.2.1",
  "org.postgresql" % "postgresql" % "42.3.3",
  "io.delta" %% "delta-core" % "2.3.0"
)