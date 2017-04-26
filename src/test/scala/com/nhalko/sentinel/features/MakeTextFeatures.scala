package com.nhalko.sentinel.features

import org.scalatest._

/**
  * Created by nhalko on 4/13/17.
  */

class MakeTextFeatures extends FlatSpec {

  "Features" should "run" in {
    TextFeature.createTextFeatures()
    assert(true)
  }

}
