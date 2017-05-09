


lazy val root = (project in file(".")).
  settings(
    name := "sentinel",
    version := "0.1",
    scalaVersion := "2.11.8",

    test in assembly := {},

    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-mllib" % "2.1.1" % "provided",

      //
      "org.twitter4j" % "twitter4j-core" % "4.0.6",
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.0.0",

      // test
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  )