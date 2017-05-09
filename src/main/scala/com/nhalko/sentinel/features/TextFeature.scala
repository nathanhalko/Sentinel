package com.nhalko.sentinel.features

import twitter4j.Status

import com.nhalko.sentinel.lib.MongoDB

/**
  * Created by nhalko on 4/13/17.
  */

object TextFeature {

  lazy val db = new MongoDB

  /**
    * create file of
    *   id, handle, label[0,1], text
    *
    * where text is all (<=20) tweets mashed together
    */
  def createTextFeatures() = {
    val fid = new java.io.FileWriter("data/cleaned/text_features.csv")
    scala.io.Source.fromFile("data/annotation-dataset.dat").getLines().foreach { line =>
      val Array(id, label) = line.split(",", 2)
      val statuses = db.getUserStatuses(id.toLong)
      val handle = statuses.headOption.map(_.getUser().getScreenName).getOrElse("")
      val text = status2Feature(statuses)
      fid.write(Array(id, handle, label, text).mkString(",")+"\n")
    }
    fid.close()
  }

  def status2Feature(statuses: Seq[Status]): String = {
    statuses.map {
      // sanitize
      _.getText
        .replaceAll(",", " ")
        .replaceAll("\n", " ")
    }
      .mkString(" ")
  }
}
