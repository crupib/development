#!/bin/bash

#Demo Config
#====
#Number of Spark Nodes in the Analytics DC
export NUM_SPARK_NODES=1
#Comma Seperated List of Spark Nodes in Analytics DC
export SPARK_NODE_IPS=127.0.0.1
#====

#Make sure we cleanup after ourselves
trap "kill 0" SIGINT SIGTERM EXIT

#Check that dse is on the path
if ! hash dse 2>/dev/null ; then
 echo "dse is not currently on the path. Please add it to the path and try again"
 exit 1
fi

#Start Streaming Application
echo "Starting Streaming Receiver(s): Logging to http_receiver.log"
cd HttpSparkStream
dse spark-submit --class com.datastax.HttpSparkStream target/HttpSparkStream.jar -d $NUM_SPARK_NODES > ../http_receiver.log 2>&1 &
cd ..

echo "Waiting for 60 Seconds for streaming to come online"
sleep 60

#Start Http Requester
echo "Starting to send requests against streaming receivers: Logging to http_requester.log"
./gradlew -b HttpRequestGenerator/build.gradle run -Pips=$SPARK_NODE_IPS > http_requester.log 2>&1 &

#Monitor Results Via Cqlsh
watch -n 5 './monitor_queries.sh'
