package com.nhalko.sentinel.util

/**
  * Created by nhalko on 4/13/17.
  */

trait Logger {

  lazy val name = this.getClass.getName
//  lazy val logger = org.slf4j.LoggerFactory.getLogger(this.getClass.getName)
  lazy val logger = new PrintLnLogger(name)

}

class PrintLnLogger(name: String) {

  def info(msg: String) = println(s"$name INFO: $msg")
  def warn(msg: String) = println(s"$name WARN: $msg")
  def error(msg: String) = println(s"$name ERROR: $msg")
}
