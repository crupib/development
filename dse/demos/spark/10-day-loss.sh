#!/bin/sh

BASEDIR=`cd "$(dirname $0)"; pwd`

dse spark-submit --class com.datastax.bdp.spark.demo.Spark10DayLoss "$BASEDIR/spark-10-day-loss.jar"
