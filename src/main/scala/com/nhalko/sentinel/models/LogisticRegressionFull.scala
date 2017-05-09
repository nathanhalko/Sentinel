package com.nhalko.sentinel.models

import scala.collection.JavaConverters._

import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder, CrossValidatorModel}


import com.nhalko.sentinel.lib.TwitterFactory
import com.nhalko.sentinel.features.TextFeature
import com.nhalko.sentinel.util.{Logger, MySparkSession}

/**
  * Created by nhalko on 4/13/17.
  *
  * Extend the basic LogisticRegressionExa with some nice features.
  */

object LogisticRegressionFull extends Logger {

  val savedModel = "data/models/text-feature-logistic-regression-model"

  def train(spark: SparkSession) = {

    /**
      * Load the raw csv data and pass through prep method
      */
    val data = prepData(spark.read.csv("data/cleaned/text_features.csv")
      .toDF("idStr", "handle", "labelStr", "text"))

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
     // .setNumFeatures(1000)  <-- DONT SET THIS

    /**
      * Choose the classifier
      */
    val logisticRegression = new LogisticRegression()
      .setMaxIter(10)
      //.setRegParam(0.001)  <-- DONT SET THIS

    /**
      * Create the Pipeline and call fit to trigger the transformer chain
      */
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, logisticRegression))


    // the whole pipeline now is an estimator!
    //val model = pipeline.fit(trainingData)

    /**
      *  We use a ParamGridBuilder to construct a grid of parameters to search over.
      *   With 4 values for hashingTF.numFeatures and 5 values for lr.regParam,
      *   this grid will have 4 x 5 = 20 parameter settings for CrossValidator to choose from.
      */
    val paramGrid = new ParamGridBuilder()
      .addGrid(hashingTF.numFeatures, Array(10, 100, 1000, 10000))
      .addGrid(logisticRegression.regParam, Array(0.1, 0.01, 0.1, 0.25, 0.5))
      .build()

    /**
      * We now treat the Pipeline as an Estimator, wrapping it in a CrossValidator instance.
      * This will allow us to jointly choose parameters for all Pipeline stages.
      * A CrossValidator requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.
      * Note that the evaluator here is a BinaryClassificationEvaluator and its default metric
      * is areaUnderROC.
      */
    val evaluator = new BinaryClassificationEvaluator()

    val crossValidator = new CrossValidator()
      .setEstimator(pipeline)
      .setEvaluator(evaluator)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(5)

    // Run cross-validation, and choose the best set of parameters.
    val cvModel = crossValidator.fit(trainingData)

    /**
    17/04/14 12:52:13 INFO CrossValidator: Average cross-validation metrics: WrappedArray(0.6394785856234795, 0.6386537105321648, 0.7037028651893089, 0.6931414510695715, 0.7186931396017183, 0.6891542048658938)
    17/04/14 12:52:13 INFO CrossValidator: Best set of parameters:
        {
	        hashingTF_890194e6e9b3-numFeatures: 1000,
	        logreg_37a628c76799-regParam: 0.1
        }
    17/04/14 12:52:13 INFO CrossValidator: Best cross-validation metric: 0.7186931396017183.
      */

    /**
      * Save for later, we'll want to predict some labels with it
      */
    cvModel.write.overwrite().save(savedModel)

    /**
      * Make predictions on test data
      */
    val predictions = cvModel.transform(testData)

    predictions.show()
    predictions.select("id", "handle", "text", "label", "prediction", "probability").show()
    logger.info(s"Area under the ROC: ${cvModel.avgMetrics.max}")
    logger.info(s"Evaluator results: ${evaluator.evaluate(predictions)}")
  }

  /**
    * Prep and sanitize incoming raw data
    * transform strings to required types, fill in / drop missing fields
    */
  def prepData(dataRaw: DataFrame): DataFrame = {
    val data = dataRaw
      .withColumn("id", dataRaw("idStr").cast("long"))
      .withColumn("label", dataRaw("labelStr").cast("double"))
      .na.fill(Map("text" -> "empty"))
      .na.drop(Array("id", "label"))

    data.show()
    data
  }

  lazy implicit val ss = MySparkSession()
  // make this lazy because it might not exist if we haven't called .train above
//  lazy val model = PipelineModel.load(savedModel)
  lazy val model = CrossValidatorModel.load(savedModel)
  lazy val twitApi = TwitterFactory.get()

  /**
    * Predict labels for given twitter handles using the model we trained above.
    */
  def predict(handle: String)(implicit spark: SparkSession) = {

    val normHandle = handle.stripPrefix("@").toLowerCase.trim
    val statuses = twitApi.getUserTimeline(normHandle).asScala
    val text = TextFeature.status2Feature(statuses)

    // need placeholders for prepData method
    val data = prepData(spark.createDataFrame(Seq((normHandle, text, "0", "0")))
      .toDF("handle", "text", "idStr", "labelStr"))

    model.transform(data)
      .select("handle", "text", "prediction", "probability")
      .show()
  }

}
