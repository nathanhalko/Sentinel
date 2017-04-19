package com.nhalko.sentinel.features

import org.scalatest._

/**
  * Created by nhalko on 4/13/17.
  */

class MakeFeatures extends FlatSpec {

  "Features" should "run" in {
//    TextFeature.createTextFeatures()
    TwitterFeature.createTwitterFeatures()
    assert(true)
  }

}
