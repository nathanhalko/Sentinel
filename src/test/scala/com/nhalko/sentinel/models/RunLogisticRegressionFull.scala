package com.nhalko.sentinel.models


import org.scalatest._
import com.nhalko.sentinel.util.MySparkSession

/**
  * Created by nhalko on 4/25/17.
  */

class RunLogisticRegressionFull extends FlatSpec {

  "LogisticRegressionExa" should "run" in {

    val spark = MySparkSession()

    LogisticRegressionFull.train(spark)

    spark.stop()
    assert(true)
  }
}
