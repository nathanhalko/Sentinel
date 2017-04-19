package com.nhalko.sentinel.models


import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.ml.classification.LogisticRegression

import com.nhalko.sentinel.util.Logger

/**
  * Created by nhalko on 4/13/17.
  */

object LogisticRegressionExa extends Logger {

  def run(spark: SparkSession) = {

    /**
      * Load the raw csv data
      */
    val dataRaw = spark.read.csv("data/cleaned/text_features.csv")
      .toDF("idStr", "handle", "labelStr", "text")

    // transform strings to required types, fill in missing fields
    val data = dataRaw
      .withColumn("id", dataRaw("idStr").cast("long"))
      .withColumn("label", dataRaw("labelStr").cast("double"))
      .na.fill(Map("text" -> "empty"))
      .na.drop(Array("id", "label"))

    data.show()


    // Split the data into training and test sets (30% held out for testing).
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    /**
      * Create features
      *
      * 1. A tokenizer that converts the input string to
      *    lowercase and then splits it by white spaces.
      *
      * 2. Maps a sequence of terms to their term frequencies
      *    using the hashing trick.
      */
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("tokens")

    val hashingTF = new HashingTF()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
      .setNumFeatures(1000)

    /**
      * Choose the classifier
      */
    val logisticRegression = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.001)

    /**
      * Create the Pipeline and call fit to trigger the transformer chain
      */
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, logisticRegression))

    val model = pipeline.fit(trainingData)

    /**
      * Make predictions on test data
      */
    val predictions = model.transform(testData)

    predictions.show()
    predictions.select("id", "handle", "text", "label", "prediction", "probability").show()
  }

}
