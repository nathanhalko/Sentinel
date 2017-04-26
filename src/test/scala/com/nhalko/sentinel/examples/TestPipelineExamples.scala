package com.nhalko.sentinel.examples


import org.scalatest._

import org.apache.spark.sql.SparkSession

/**
  * Created by nhalko on 4/12/17.
  */

class TestPipelineExamples extends FlatSpec {

  "PipelineExample" should "run" in {

    val spark = SparkSession.builder().master("local[4]").getOrCreate()

    PipelineExample.run(spark)

    assert(true)
  }

}
