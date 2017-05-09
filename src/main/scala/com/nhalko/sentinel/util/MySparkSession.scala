package com.nhalko.sentinel.util

import org.apache.spark.sql.SparkSession

/**
  * Created by nhalko on 4/13/17.
  *
  * Create a SparkSession for use in the scala repl
  *
  * > val spark = MySparkSession()
  */

object MySparkSession {

  private var ss: SparkSession = _

  def build() = {
    ss = SparkSession.builder()
      .master("local[4]")
      .getOrCreate()
  }

  def reset() = {
    ss.stop()
    build()
  }

  def apply(): SparkSession = {
    build()
    ss
  }

}
