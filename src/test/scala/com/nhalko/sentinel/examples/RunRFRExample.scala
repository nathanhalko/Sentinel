package com.nhalko.sentinel.examples

import org.scalatest._

/**
  * Created by nhalko on 4/12/17.
  */

class RunRFRExample extends FlatSpec {

  "RandomForestRegressorExample" should "run" in {

    RandomForestRegressorExample.main(Array.empty[String])

    assert(true)
  }

}
