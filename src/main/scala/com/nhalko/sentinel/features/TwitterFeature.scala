package com.nhalko.sentinel.features

import scala.collection.JavaConverters._

import twitter4j.{User, Status}

import com.nhalko.sentinel.lib.MongoDB
import com.nhalko.sentinel.lib.TwitterFactory

/**
  * Created by nhalko on 4/17/17.
  *
  * Subset of features described in https://arxiv.org/pdf/1703.03107.pdf
  */

object TwitterFeature {

  lazy val db = new MongoDB
  lazy val twitApi = TwitterFactory.get()

  val features = Array(
    "id",
    "label",
    "handle",
    "screen_name_length",
    "digits_in_screen_name",
    "user_name_length",
    "time_offset",
    "default_profile",
    "default_picture",
    "account_age",
    "friends",
    "followers",
    "favorites",
    "tweets",
    "retweets",
    "mentions",
    "replies",
    "retweeted"
  )

  def getFeatureVectorCols(): Array[String] = features.drop(3)

  val digitRgx = "[0-9]".r

  def createTwitterFeatures() = {
    val fid = new java.io.FileWriter("data/cleaned/twitter_features.csv")

    // write header
    fid.write(features.mkString(",")+"\n")

    scala.io.Source.fromFile("data/annotation-dataset.dat").getLines()
      .flatMap {
        line =>
          // get the user from the db, skip any not found
          val Array(id, label) = line.split(",", 2)
          val user = db.getUser(id.toLong)
          user.map(u => (id, label, u))
      }
      .foreach {
        case (id, label, user) =>

          val features = userStatus2Features(id, label, user, db.getUserStatuses(id.toLong))

          fid.write(
            features.mkString(",")+"\n"
          )
      }

    fid.close()
  }

  def bool2Bin(bool: Boolean) = if (bool) "0" else "1"

  def userStatus2Features(id: String, label: String, user: User, statuses: Seq[Status]): Array[String] = {
    val labelDbl = label.toDouble.toString

    val screenName = user.getScreenName
    val screenNameLength = screenName.length.toString
    val digitsInSN = digitRgx.findAllIn(screenName).toList.length.toString
    val userNameLength = user.getName.length.toString
    val timeOffset = user.getUtcOffset.toString
    val defaultProfile = bool2Bin(user.isDefaultProfile)
    val defaultPicture = bool2Bin(user.isDefaultProfileImage)
    // account was created N days ago
    val accountAge = ((System.currentTimeMillis() - user.getCreatedAt.getTime) / (1000*60*60*24)).toString
    val friends = user.getFriendsCount.toString
    val followers = user.getFollowersCount.toString
    val favorites = user.getFavouritesCount.toString
    val tweets = user.getStatusesCount.toString

    val retweets = statuses.count(_.isRetweet).toString
    val mentions = statuses.map(_.getUserMentionEntities.length).sum.toString
    val replies = statuses.flatMap(s => Option(s.getInReplyToScreenName)).length.toString
    val retweeted = statuses.count(_.isRetweeted).toString

    Array(
      id, labelDbl, screenName,
      screenNameLength,
      digitsInSN,
      userNameLength,
      timeOffset,
      defaultProfile,
      defaultPicture,
      accountAge,
      friends,
      followers,
      favorites,
      tweets,
      retweets,
      mentions,
      replies,
      retweeted
    )
  }

  def handle2Features(handle: String): Array[String] = {
    val normHandle = handle.stripPrefix("@").toLowerCase.trim
    val user = twitApi.showUser(normHandle)
    val id = user.getId
    val statuses = twitApi.getUserTimeline(normHandle).asScala

    userStatus2Features(id.toString, "0", user, statuses)
  }

  def writeSingeFeatureFile(data: Array[String], fName: String = "/tmp/features.csv"): String = {
    val fid = new java.io.FileWriter(fName)
    fid.write(features.mkString(",")+"\n")
    fid.write(data.mkString(",")+"\n")
    fid.close()
    fName
  }

}
