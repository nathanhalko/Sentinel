package com.nhalko.sentinel.models

import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{VectorAssembler, VectorIndexer}
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics


import com.nhalko.sentinel.features.TwitterFeature
import com.nhalko.sentinel.util.Logger

/**
  * Created by nhalko on 4/13/17.
  *
  * New model with new data.  We now use the data produced by features.TwitterFeature
  * which are a subset of features given in https://arxiv.org/pdf/1703.03107.pdf.
  * The 'data/cleaned/twitter_features.csv' looks something like
  *
  *   3098421349,1.0,cdsimcoecounty,14,0,20,-1,0,1,780,870,233,45,1252,1,1,0,0
  *   554067867,1.0,tammylou01,10,2,11,-1,0,1,1849,1705,42,2974,5811,20,23,0,0
  *   256597786,1.0,quest4Angus,11,1,8,-28800,0,1,2266,472,178,10,1165,1,13,6,0
  *
  * The numeric features starting in column 4 need to be grouped into a single vector
  * which spark can handle for us in the VectorAssembler.
  */

object RandomForest extends Logger {

  val savedModel = "data/models/twitter-feature-decision-tree-model"

  def train(spark: SparkSession) = {

    /**
      * Load the raw csv data.  This time we ask Spark to infer the types of our data
      * automatically so we don't need to explicitly cast ie the id -> long
      * like we did before.
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
      .setInputCol(vectorAssembler.getOutputCol)
      .setOutputCol("indexedFeatures")
      .setMaxCategories(4)

    /**
      * Choose the classifier
      */
    val threshold = 0.5
    val randomForestClassifier = new RandomForestClassifier()
      .setLabelCol("label")
      .setFeaturesCol(featureIndexer.getOutputCol)
      .setThresholds(Array(threshold, 1 - threshold))

    /**
      * Create the Pipeline and call fit to trigger the transformer chain
      */
    val pipeline = new Pipeline()
      .setStages(Array(vectorAssembler, featureIndexer, randomForestClassifier))

    // Fit the pipeline to create the model
    val model = pipeline.fit(trainingData)

    /**
      * Save for later
      */
    model.write.overwrite().save(savedModel)

    /**
      * Make predictions on test data
      */
    val evaluator   = new BinaryClassificationEvaluator()
    val predictions = model.transform(testData)
    val eval        = evaluator.evaluate(predictions)

    val metrics = new BinaryClassificationMetrics(
                       // score      , label
      predictions.select("probability", "label").rdd.map(r => (r.getAs[DenseVector](0).toArray(0), r.getDouble(1))),
      numBins = 10
    )

    /**
      * Threshold defaults to 0.5 but can be tuned to optimize precision or recall
      * depending on your use case.
      */
    metrics.precisionByThreshold.join(metrics.recallByThreshold())
      .sortBy(_._1)
      .foreach { case (t, (p, r)) =>
        println(f"Threshold: $t%.4f, Precision: $p%.4f, Recall: $r%.4f")
      }

    predictions.show()
    predictions.select("id", "handle", "probability", "prediction", "label").show(truncate = false)
    logger.info(s"Evaluator results: ${evaluator.getMetricName} -> $eval")
  }
}