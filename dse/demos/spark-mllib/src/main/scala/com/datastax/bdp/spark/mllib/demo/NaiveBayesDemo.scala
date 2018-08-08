/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */

package com.datastax.bdp.spark.mllib.demo


import java.io.File

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint

import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector

object NaiveBayesDemo {
  
  val KeySpaceName = "mllib_ks";
  val TableName = "iris";
  val DefaultInputFileName = "iris.csv";

  def main(args: Array[String]) {

    //  absolute path to iris.csv is expected as a first argument
    // "iris.csv" from current directory is used by default
    val dataSetFile = if (args.nonEmpty) args(0) else new File(DefaultInputFileName).getAbsolutePath()

    //init SparkContext
    val sc = new SparkContext(new SparkConf().setAppName("NaiveBayesDemo"))

    // Prepare and Save the Data Set to Cassandra
    prepareDataSet(sc, dataSetFile)

    // Load data
    val data = sc.cassandraTable[Iris](KeySpaceName, TableName).cache()

    /*
     * The MLlib works with LabeledPoint objects that consists of label (double value) to mark a class
     * and Vector of double features. Define mapping from flower name to index and back.
     * The code select all ‘species’, get distinct values, index them and create map. Then create reverse map.
     */
    val class2id = data.map(_.species).distinct.collect.zipWithIndex.map{case (k,v)=>(k, v.toDouble)}.toMap
    val id2class = class2id.map(_.swap)

    //Map Iris data to LabeledPoint
    val parsedData = data.map { i => LabeledPoint(class2id(i.species), Vectors.dense(i.petal_l, i.petal_w, i.sepal_l, i.sepal_w))}

    // Train NaiveBayes classifier
    val model = NaiveBayes.train(parsedData)

    //We are done with learning and now we can recognise irises by passing 4 measures.
    val sample = Vectors.dense(5, 1.5, 6.4, 3.2)
    val prediction = id2class(model.predict(sample))

    println(s"$sample is a $prediction")

    sc.stop()

  }

  def prepareDataSet(sc: SparkContext, file: String): Unit = {
    // Load data from the file
    val data = sc.textFile("file://" + file)

    //Parse data and generate random id for Iris objects
    val parsed = data.filter(_.nonEmpty).map { row =>
      val splitted = row.split(",")
      val Array(sl, sw, pl, pw) = splitted.slice(0, 4).map(_.toDouble)
      Iris(java.util.UUID.randomUUID(), sl, sw, pl, pw, splitted(4))
    }

    //Cassandra keyspace and table for the data
    CassandraConnector(sc.getConf).withSessionDo { session =>
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $KeySpaceName WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
      session.execute(
          s"""CREATE TABLE IF NOT EXISTS $KeySpaceName.$TableName (
                 id uuid primary key,
                 sepal_l double,
                 sepal_w double,
                 petal_l double,
                 petal_w double,
                 species text
          )"""
      )
    }

    // save the data to Cassandra
    parsed.saveToCassandra(KeySpaceName, TableName)
  }

  /**
   * Class that will wrap the Iris data.
   * The class will be transparently mapped from/to Cassandra rows by connector methods.
   * The “id” field is not in the original data set, but a unique key is needed to store data in Cassandra.
   */
  case class Iris(
                   id:java.util.UUID,
                   sepal_l:Double,
                   sepal_w:Double,
                   petal_l:Double,
                   petal_w:Double,
                   species:String
                   )
}
