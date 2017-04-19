package com.nhalko.sentinel.lib

import scala.util.{Try, Failure}

import twitter4j.{
TwitterFactory => T4JFactory,
TwitterException,
TwitterObjectFactory => TOF,
User,
Status
}

import com.nhalko.sentinel.util.Logger
/**
  * Created by nhalko on 4/13/17.
  */

object TwitterFactory extends Logger {

  lazy val twitter = T4JFactory.getSingleton

  def get() = twitter

  /**
    * Safely call the twitter API, waiting and retrying if rate limit is used up.
    */
  def callTwitterAndWait[A](call: => A, maxTry: Int = 3): Option[A] = {

    def callAndWait() = Try {
      call
    } match {
      case Failure(te: TwitterException) =>
        val rls = te.getRateLimitStatus
        if (rls.getRemaining == 0) {
          val t = rls.getSecondsUntilReset
          logger.info(s"Sleeping $t seconds for rate limit.")
          Thread.sleep(t * 1010L max 0) // use 1.01 * rl seconds for buffer
          logger.info("Resuming call.")
        }
        Failure(te)
      case s => s
    }

    // try up to 3 times for Some result, else return None
    Iterator.continually(callAndWait()).take(maxTry).find(_.isSuccess).map(_.get)
  }

  // conversion methods to/from JSON strings
  def user2Json(user: User): String = TOF.getRawJSON(user)
  def userFromJson(str: String): User = TOF.createUser(str)
  def status2Json(status: Status): String = TOF.getRawJSON(status)
  def statusFromJson(str: String): Status = TOF.createStatus(str)

}
