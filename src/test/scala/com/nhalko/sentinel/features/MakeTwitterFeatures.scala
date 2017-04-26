package com.nhalko.sentinel.features

import org.scalatest._

/**
  * Created by nhalko on 4/13/17.
  */

class MakeTwitterFeatures extends FlatSpec {

  "Features" should "run" in {
    TwitterFeature.createTwitterFeatures()
    assert(true)
  }

}
