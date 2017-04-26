package com.nhalko.sentinel.models

import scala.collection.JavaConverters._
import org.apache.spark.sql.{DataFrame, SparkSession, Row}
import org.apache.spark.sql.types.StructType
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.{HashingTF, Tokenizer, VectorAssembler, VectorIndexer}
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
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

object KMeansClustering extends Logger {

  val savedModel = "data/models/twitter-feature-kmeans-model"

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
    val kmeans = new KMeans()
      .setFeaturesCol("indexedFeatures")
      .setSeed(1L)
      .setK(10)

    /**
      *
      */
    val dataPrepPipeline = new Pipeline()
      .setStages(Array(vectorAssembler, featureIndexer))

    val dataPrepModel = dataPrepPipeline.fit(data)

    val pipeline = new Pipeline()
      .setStages(dataPrepModel.stages ++ Array(kmeans))

    val pipelineModel = pipeline.fit(data)
    val predictions = pipelineModel.transform(data)

    val kmeansModel = pipelineModel.stages(2).asInstanceOf[KMeansModel]

    val wssse = kmeansModel.computeCost(dataPrepModel.transform(data))
    println(s"Within Set Sum of Squared Errors = $wssse")

    // Shows the result.
    println("Cluster Centers: ")
    kmeansModel.clusterCenters.foreach(println)

    pipelineModel
      .transform(data.sample(withReplacement = false, 0.025))
      .select("id", "handle", "prediction", "label").show(100)
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
