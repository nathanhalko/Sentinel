

val sparkVer = "2.1.0"

lazy val root = (project in file(".")).
  settings(
    name := "sentinel",
    version := "0.1",
    scalaVersion := "2.11.8",

    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-mllib" % sparkVer,

      //
      "org.twitter4j" % "twitter4j-core" % "4.0.4",
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.0.0",

      // test
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  )