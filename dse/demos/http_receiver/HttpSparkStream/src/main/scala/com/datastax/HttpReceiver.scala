/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax


import java.net.{URI, InetSocketAddress}
import java.io.{InputStreamReader, BufferedReader}
import java.util.UUID

import scala.collection.JavaConversions._

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

import org.apache.spark.streaming.receiver.Receiver
import org.apache.spark.storage.StorageLevel
import org.apache.spark.Logging

import com.datastax.driver.core.utils.UUIDs

import HttpReceiverCases._


/**
 * Each HttpReceiver will start up a lightweight HTTP Server on which
 * simply takes requests and serializes them to a DStream which can be
 * accessed by Spark
 * @param port TCP Port to bind for the server
 */
class HttpReceiver(port: Int)
  extends Receiver[HttpRequest](StorageLevel.MEMORY_AND_DISK_2) with Logging {

  var server: Option[HttpServer] = None
  var running: Boolean = true

  def onStart(): Unit = {
    // Start the Http Server that comes built in with the OracleJDK
    val s = HttpServer.create(new InetSocketAddress(port), 0)
    // Bind all URI's to the same handler
    s.createContext("/", new StreamHandler())
    s.start()
    //Save Handle to the server to close later
    server = Some(s)
  }

  class StreamHandler extends HttpHandler {
    /**
     * This handler will be used for all incoming Http Requests. It will handle Http Exchanges
     * by reading in the entire request, serializing it to the DStream and close the response with
     * a 200 Request Code.
     * @param transaction
     */
    override def handle(transaction: HttpExchange): Unit = {
      val dataReader = new BufferedReader(new InputStreamReader(transaction.getRequestBody))
      val data = Stream.continually(dataReader.readLine).takeWhile(_ != null).mkString("\n")
      //Explict conversion needed for nested collections
      val headers: Map[String, List[String]] = transaction.getRequestHeaders.toMap.map { case (k, v) => (k, v.toList)}
      store(HttpRequest(
        UUIDs.timeBased(),
        transaction.getRequestMethod,
        headers,
        transaction.getRequestURI,
        data))
      transaction.sendResponseHeaders(200, 0)
      val response = transaction.getResponseBody
      response.close() // Empty response body
      transaction.close() // Finish Transaction
    }
  }

  def onStop(): Unit = server map(_.stop(0))
}



