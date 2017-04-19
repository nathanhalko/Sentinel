package com.nhalko.sentinel.lib

import scala.util.Try
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import java.util.concurrent.TimeUnit

import org.mongodb.scala._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model._

import org.json4s._
import org.json4s.jackson.JsonMethods._

import twitter4j._

import com.nhalko.sentinel.lib.TwitterFactory._

/**
  * Created by nhalko on 4/13/17.
  *
  * The DAO for the sentinel database
  * Assumes local mongo at default port 27017
  * just start locally with
  *     $>> mongod
  */

class MongoDB {


  val mongoClient = MongoClient()
  val db = mongoClient.getDatabase("sentinel")

  val usersColl = db.getCollection("users")
  val statsColl = db.getCollection("statuses")

  implicit val formats = DefaultFormats

  // this method needed to subscribe to the oberservable and await the result
  def results[A](obs: Observable[A]): Seq[A] = Await.result(obs.toFuture(), Duration(30, TimeUnit.SECONDS))

  def user2Doc(user: User): Document = Document(
    "_id" -> user.getId, "user" -> user2Json(user)
  )
  def status2Doc(status: Status): Document = Document(
    "_id" -> status.getId, "user" -> status.getUser.getId, "status" -> status2Json(status)
  )

  /**
    * Crazy stuff here::
    *
    *   Document -> JsonString
    *   JsonString -> json4s , extract data element with "\"
    *   reduced json4s -> JsonString
    *   JsonString -> twitter4j model
    */
  def userFromDoc(doc: Document): User = userFromJson((parse(doc.toJson()) \ "user").extract[String])
  def statusFromDoc(doc: Document): Status = statusFromJson((parse(doc.toJson()) \ "status").extract[String])


  // set
  def insertUser(user: User) = Try {
    results(usersColl.insertOne(user2Doc(user)))
  }
  def insertManyUsers(users: Seq[User]) = Try {
    results(usersColl.insertMany(users.map(user2Doc)))
  }

  def insertStatus(status: Status) = Try {
    results(statsColl.insertOne(status2Doc(status)))
  }
  def insertManyStatuses(statuses: Seq[Status]) = Try {
    results(statsColl.insertMany(statuses.map(status2Doc)))
  }



  // get
  def getUser(id: Long): Option[User] = {
    results(usersColl.find(equal("_id", id)))
      .headOption.map(userFromDoc)

  }

  def getAllUsers(): Seq[User] = {
    results(usersColl.find()).map(userFromDoc)
  }

  def getUserStatuses(id: Long): Seq[Status] = {
    results(statsColl.find(equal("user", id)))
      .map(statusFromDoc)
  }

  def getAllStatuses(): Seq[Status] = {
    results(statsColl.find()).map(statusFromDoc)
  }


  def dropUsersBEVERYCAREFUL() = results(usersColl.drop())
  def dropStatusesBEVERYCAREFUL() = results(statsColl.drop())
  def close() = mongoClient.close()
}
