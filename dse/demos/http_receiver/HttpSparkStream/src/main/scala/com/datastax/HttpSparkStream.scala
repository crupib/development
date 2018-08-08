/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax

import org.apache.spark._
import org.apache.spark.streaming._

import com.datastax.HttpReceiverCases._
import com.datastax.driver.core.utils.UUIDs
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._

object HttpSparkStream {

  def main(args: Array[String]) {
    if (sys.env.get("SPARK_HOME").isEmpty) {
      println(
        """SPARK_HOME not set.
          |Run
          |./sbt/sbt assembly
          |dse spark-submit --class com.datastax.HttpSparkStream target/HttpSparkStream.jar""".stripMargin)
      sys.exit()
    }

    val parser = new scopt.OptionParser[Config]("HttpSparkStreaming") {
      head("HttpSparkStreaming Example", "1.0")
      opt[Int]('d', "numDstreams") optional() action { (arg, config) => config.copy(numDstreams = arg)} text {
        s"""Number of HttpServing Dstreams to Start. Specifying more than n > cluster size will cause port collisions.
           Default: $DEFAULT_RECEIVERS
        """.stripMargin
      }
      opt[Int]('p', "port") optional() action { (arg, config) => config.copy(port = arg)} text {
        s"Port to listen on for HTTP Requests. Default: $DEFAULT_PORT"
      }
      help("help") text {
        "CLI Help"
      }
    }

    parser.parse(args, Config()) map { config =>
      startHttpStream(config)
    } getOrElse {
      System.exit(1)
    }
  }

  def startHttpStream(config: Config) {
    val conf = new SparkConf()
      .setAppName("HttpSparkStreamer")
      .set("spark.cleaner.ttl", "3600")
      .set("spark.cassandra.connection.keep_alive_ms", "120000")
      .setJars(Seq(System.getProperty("user.dir") + "/target/HttpSparkStream.jar"))

    CassandraConnector(conf).withSessionDo { session =>
      session.execute( """CREATE KEYSPACE IF NOT EXISTS requests_ks with replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1 }""")
      session.execute( """use requests_ks""")

      session.execute(
        """CREATE TABLE IF NOT EXISTS timeline (
          |timesegment bigint ,
          |url text,
          |t_uuid timeuuid ,
          |method text,
          |headers map <text, text>,
          |body text ,
          |PRIMARY KEY ((url, timesegment) , t_uuid )
          |)""".stripMargin)

      session.execute(
        """CREATE TABLE IF NOT EXISTS method_agg(
          |url text,
          |method text,
          |time timestamp,
          |count bigint,
          |PRIMARY KEY ((url,method), time)
          |)
        """.stripMargin
      )

      session.execute(
        """CREATE TABLE IF NOT EXISTS sorted_urls(
          |url text,
          |time timestamp,
          |count bigint,
          |PRIMARY KEY (time, count)
          |)
        """.stripMargin
      )
    }

    val ssc = new StreamingContext(conf, Seconds(5))
    val multipleStreams = (1 to config.numDstreams).map { i => ssc.receiverStream[HttpRequest](new HttpReceiver(config.port))}
    val requests = ssc.union(multipleStreams)
    requests.print()

    /**
     * Persist all incoming events to C*. This way we'll have a record of every single event in the order 
     * in which it happened on a per URL basis. The timeuuid protects us from similtaneous events 
     * over-writing one another and the segment keeps us from writing unbounded partitions.
     */
    requests.map {
      request =>
        timelineRow(
          timesegment = UUIDs.unixTimestamp(request.timeuuid) / 10000L,
          url = request.uri.toString,
          t_uuid = request.timeuuid,
          method = request.method,
          headers = request.headers.map { case (k, v) => (k, v.mkString("#"))},
          body = request.body)
    }.saveToCassandra("requests_ks", "timeline")

    /**
     * Record the amount of hits to each URI and which HTTP request method was used
     * in the query. The backing C* table will allow for quick lookup of the popularity
     * of a particular url and method over time.
     */
    requests.map(request => (request.method, request.uri.toString))
      .countByValue()
      .transform((rdd, time) => rdd.map { case ((m, u), c) => ((m, u), c, time.milliseconds)})
      .map { case ((m, u), c, t) => methodAggRow(time = t, url = u, method = m, count = c)}
      .saveToCassandra("requests_ks", "method_agg")

    /**
     * Save the amount of hits to each URL during each batch into a C* table. This table structure
     * will allow quick lookups of the most popular url in a given batch. Since the
     * Cassandra table has a clustering key on count, these will be automatically sorted
     * when they are insterted into the C* database and no sorting is required on Spark.
     */
    requests.map(request => (request.uri.toString))
      .countByValue()
      .transform((rdd, time) => rdd.map { case (u, c) => (u, c, time.milliseconds)})
      .map { case (u, c, t) => sortedUrlRow(time = t, url = u, count = c)}
      .saveToCassandra("requests_ks", "sorted_urls")

    println("Streaming receivers starting now")
    ssc.start()
    ssc.awaitTermination()
  }


}
