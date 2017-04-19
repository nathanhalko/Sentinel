package com.nhalko.sentinel.models

import scala.collection.JavaConverters._
import org.apache.spark.sql.{DataFrame, SparkSession, Row}
import org.apache.spark.sql.types.StructType
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.{HashingTF, Tokenizer, VectorAssembler, VectorIndexer}
import org.apache.spark.ml.classification.{LogisticRegression, RandomForestClassifier}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel, ParamGridBuilder}
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics


import com.nhalko.sentinel.lib.TwitterFactory
import com.nhalko.sentinel.features.TwitterFeature
import com.nhalko.sentinel.util.{Logger, MySparkSession}

/**
  * Created by nhalko on 4/13/17.
  *
  * Extend the basic Model1 with some nice features.
  */

object RandomForest extends Logger {

  val savedModel = "data/models/twitter-feature-decision-tree-model"

  def train(spark: SparkSession) = {

    /**
      * Load the raw csv data and pass through prep method
      */
    val data = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("data/cleaned/twitter_features.csv")

    data.show()
    data.printSchema()

    // Split the data into training and test sets (30% held out for testing).
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    /**
      * Create features
      */

    val vectorAssembler = new VectorAssembler()
      .setOutputCol("features")
      .setInputCols(TwitterFeature.getFeatureVectorCols())

    val featureIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(4)

    /**
      * Choose the classifier
      */
    // Train a RandomForest model.
    val randomForestClassifier = new RandomForestClassifier()
      .setLabelCol("label")
      .setFeaturesCol("indexedFeatures")
//      .setThresholds(Array(1.0, 0.0))


    /**
      * Create the Pipeline and call fit to trigger the transformer chain
      */
    val pipeline = new Pipeline()
      .setStages(Array(vectorAssembler, featureIndexer, randomForestClassifier))

    val evaluator = new BinaryClassificationEvaluator()

    // Run cross-validation, and choose the best set of parameters.
    val model = pipeline.fit(trainingData)

    /**
      * Save for later, we'll want to predict some unknown stuff with it
      */
    model.write.overwrite().save(savedModel)

    /**
      * Make predictions on test data
      */

    val predictions = model.transform(testData)
    val eval = evaluator.evaluate(predictions)

    // ToDo: not sure how to map probability into raw score
    val metrics = new BinaryClassificationMetrics(
                       // score      , label
      predictions.select("probability", "label").rdd.map(r => (r.getAs[DenseVector](0).toArray(0), r.getDouble(1))),
      numBins = 10
    )

    metrics.precisionByThreshold.join(metrics.recallByThreshold())
      .sortBy(_._1)
      .foreach { case (t, (p, r)) =>
        println(f"Threshold: $t%.4f, Precision: $p%.4f, Recall: $r%.4f")
      }

    predictions.show()
    predictions.select("id", "handle", "probability", "prediction", "label").show(truncate = false)
//    logger.info(s"Area under the ROC: ${cvModel.avgMetrics.max}")
    logger.info(s"Evaluator results: ${evaluator.getMetricName} -> $eval")
  }


  lazy implicit val ss = MySparkSession()
  // make this lazy because it might not exist if we haven't called .train above
  lazy val model = PipelineModel.load(savedModel)

  def predict(handle: String)(implicit spark: SparkSession) = {

    val featuresFile = TwitterFeature.writeSingeFeatureFile(TwitterFeature.handle2Features(handle))
    val data = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(featuresFile)

    data.show()

    // ToDo: VectorIndexer unhappy with a single row i think
    //Cause: org.apache.spark.SparkException: Failed to execute user defined function($anonfun$11: (vector) => vector)
    //Cause: java.util.NoSuchElementException: key not found: 1.0

    model.transform(data)
      .select("id", "handle", "probability", "prediction", "label")
      .show(truncate = false)
  }

}
