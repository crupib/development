/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.bdp.spark.streaming.kafka

import java.io.File
import java.net.InetAddress

import scala.collection.immutable
import scala.util.Try
import kafka.serializer.StringDecoder
import kafka.server.KafkaConfig
import org.joda.time.{DateTimeZone, DateTime}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{StreamingContext, Seconds}
import org.apache.spark.SparkConf
import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.embedded._

object SparkStreamingKafkaDemo extends Settings {
  import SensorEvents._

  /** For running from gradle 'run' where jar may not be built yet, vs spark submit. */
  private val jars = {
    val jar = s"$rootpath/spark-streaming-kafka-demo.jar"
    if(new File(jar).exists) Seq(jar) else Seq.empty
  }

  def main(args: Array[String]): Unit = {

    /** Starts the Kafka broker. */
    lazy val kafka = new EmbeddedKafka()

    /** Represents a separate process but for demo purposes it is start here. */
    val producer = new KafkaStreamingProducer(kafka.kafkaConfig)

    val conf = new SparkConf()
      .setAppName("SparkStreamingKafka")
      .set("spark.cleaner.ttl", "3600")
      .setJars(jars)

    CassandraConnector(conf).withSessionDo { session =>
      session.execute(s"DROP KEYSPACE IF EXISTS $keyspace") // start fresh each run
      session.execute(s"CREATE KEYSPACE $keyspace WITH REPLICATION = { 'class':'SimpleStrategy', 'replication_factor':1}")
      session.execute(
        s"""
           |CREATE TABLE $keyspace.$table (sensor_id int, event_time timestamp, metric double, PRIMARY KEY(sensor_id, event_time))
           |WITH CLUSTERING ORDER BY (event_time DESC)
         """.stripMargin)
    }

    kafka.createTopic(topics.head)

    new Thread("producer") {
      override def run(): Unit = producer.start()
    }.start()

    /** Creates the Spark Streaming context. */
    val ssc = new StreamingContext(conf, Seconds(1))
    ssc.checkpoint(checkpoint)

    /** Creates an input stream that pulls messages from a Kafka Broker. */
    val stream = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
      ssc, kafka.kafkaParams, Map(topics.head -> 1), StorageLevel.MEMORY_ONLY)
      .map { case (_,v) => SensorEvent(v)}

    stream.saveToCassandra(keyspace, table, SomeColumns("sensor_id", "event_time", "metric"))

    val sensorTotals = stream
      .map(e => (e.sensorId, e.metric))
      .reduceByKeyAndWindow(_ + _, _ - _, Seconds(5), Seconds(1))

    sensorTotals.print()

    ssc.start()
    ssc.awaitTermination()

    Runtime.getRuntime.addShutdownHook(new Thread("demo shutdown") {
      override def run() {
        println("Starting shutdown.")
        producer.stop
        ssc.stop(stopSparkContext = true, stopGracefully = false)
        println("Stopping Kafka and Zookeeper.")
        kafka.shutdown()
      }
    })
  }
}

object SensorEvents {

  sealed trait Event extends Serializable
  case class SensorEvent(sensorId: Int, eventTime: String, metric: Double) extends Event

  object SensorEvent {

    def apply(data: String): SensorEvent = {
      val v = data.split(",")
      SensorEvent(v(0).toInt, v(1), v(2).toDouble)
    }
  }
}
/** Produces some random words between 1 and 100 and sends to Kafka.
  * This represents a completely separate process from the consumer, but
  * for ease of demo running only, it is started by the main consumer. */
class KafkaStreamingProducer(config: KafkaConfig) extends Settings {

  private lazy val producer = new KafkaProducer[String, String](config)

  private var isRunning: Boolean = false

  def start(): Unit = {
    isRunning = true

    while (isRunning) {
      val messages = for (sensor <- 0 until numSensors) yield {
        val event_time = new DateTime(DateTimeZone.UTC)
        val metric = (scala.util.Random.nextGaussian() * sigma + xbar).toString
        s"$sensor,$event_time,$metric"
      }

      producer.batchSend(topics.head, group, messages)
      println(s"Sent ${messages.size} to Kafka [${messages.mkString(",")}]")
      Thread.sleep(1000)
    }
  }

  def stop(): Unit = {
    println("Shutting down Kafka producer.")
    isRunning = false
    Try(producer.close)
  }

}

private[streaming] trait Settings extends Serializable {

  /* app */
  val numSensors = 5
  val sigma = 10
  val xbar = 70

  val rootpath = System.getProperty("user.dir")
  val keyspace = "sensors"
  val table = "stream_ts"
  val checkpoint = "checkpoint"

  /* kafka */
  val topics = immutable.Set(table)
  val group = "sensors.group"
  val numThreads = "1"
  val zkConnect = s"${InetAddress.getLocalHost.getHostAddress}:2181"
  val brokers = Set("127.0.0.1:9092")
  val topicMap = for (t <- topics) yield t -> numThreads

}
