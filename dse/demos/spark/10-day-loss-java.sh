#!/bin/sh

BASEDIR=`cd "$(dirname $0)"; pwd`

dse spark-submit --class com.datastax.bdp.spark.demo.java.Spark10DayLossJava "$BASEDIR/spark-10-day-loss.jar"
