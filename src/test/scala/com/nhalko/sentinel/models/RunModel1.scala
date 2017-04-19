package com.nhalko.sentinel.models

import org.scalatest._

import com.nhalko.sentinel.util.MySparkSession

/**
  * Created by nhalko on 4/13/17.
  */

class RunModel1 extends FlatSpec {

  "Model1" should "run" in {

    val spark = MySparkSession()

//    Model1.run(spark)
//    Model2.train(spark)
//    Model3.train(spark)

    RandomForest.predict("nhalko")(spark)

    spark.stop()
    assert(true)
  }

}
