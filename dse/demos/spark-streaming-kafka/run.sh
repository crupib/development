#!/bin/sh

#Make sure we cleanup after ourselves
trap "kill 0" SIGINT SIGTERM EXIT

#Check that dse is on the path
if [ ! hash dse 2>/dev/null ]; then
 echo "dse is not currently on the path. Please add it to the path and try again"
 exit 1
fi

BASEDIR=`cd "$(dirname $0)"; pwd`

echo "Starting Spark Streaming Kafka Demo"
dse spark-submit --class com.datastax.bdp.spark.streaming.kafka.SparkStreamingKafkaDemo "$BASEDIR/spark-streaming-kafka-demo.jar"
