package com.nhalko.sentinel.lib


import scala.collection.JavaConverters._

import com.nhalko.sentinel.lib.TwitterFactory.callTwitterAndWait
import com.nhalko.sentinel.util.Logger

/**
  * Created by nhalko on 4/13/17.
  *
  * Data collector from the TwitterAPI
  */

object TwitterCrawler extends Logger  {

  lazy val db = new MongoDB
  lazy val twitApi = TwitterFactory.get()

  def collect(ids: Seq[Long]) = {

    var collectedUser = 0
    var collectedStatus = 0

    ids.grouped(100).foreach { idBlk =>

      logger.info(s"Gathering Users for: ${idBlk.take(10).mkString(",")} ...")
      callTwitterAndWait(twitApi.lookupUsers(idBlk:_*)) match {
        case Some(users) =>
          users.asScala.map(u => db.insertUser(u))
          collectedUser += idBlk.length
        case None =>
          logger.error(s"lookupUsers call failed: ${idBlk.mkString(",")}")
      }

      idBlk.foreach { id =>
        logger.info(s"Gathering Status for: $id")
        callTwitterAndWait(twitApi.getUserTimeline(id)) match {
          case Some(statuses) =>
            statuses.asScala.map(s => db.insertStatus(s))
            collectedStatus += statuses.size()
          case None =>
            logger.error(s"Failed getting statuses for: $id")
        }
      }

      logger.info(s"$collectedUser users and $collectedStatus statuses collected.")
    }
  }

  // see if we already have info on these users before collecting
  def filterCollect(ids: Seq[Long]) = {

    collect(
      ids.filter { id =>
        if (db.getUser(id).isDefined) {
          logger.info(s"Already collected: $id")
          false
        } else true
      }
    )
  }

  def parseDataFile(file: String): Seq[Long] = {
    scala.io.Source.fromFile(file).getLines().map(_.split(",")(0).trim.toLong).toList
  }

}
