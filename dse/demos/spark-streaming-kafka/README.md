# Spark Streaming Kafka Demo

This is a standalone Spark application that uses Spark Streaming, Spark Streaming Kafka and Kafka for
a basic Pub-Sub application. Data is published to a Kafka topic at regular intervals. This data is then
received asynchronously as it arrives on the topic and transformed into a custom event type.
The stream of sensor events is stored in the stream to Cassandra as raw data.
The stream then aggregates the event key (ID) and metric through a spark 'reduceByKeyAndWindow' and prints
the data. Typically this aggregation would be stored in a second Cassandra table. For a simple demo, the
Kafka publisher which generates events and sends to the Kafka topic, is started by the main process versus
being a second process to start.

## Run

### Start DSE Cluster
Start a DSE cluster with Analytic DC with Spark enabled

      dse cassandra -k -f
      or
      $DSE_HOME/bin/dse cassandra -k -f


### Run The Application

      cd $DSE_HOME/demos/spark-streaming-kafka
      run.sh
