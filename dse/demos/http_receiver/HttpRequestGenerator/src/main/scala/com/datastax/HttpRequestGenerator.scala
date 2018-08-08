/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax

import java.util.concurrent.{Executors, ExecutorService}
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

import scala.util.Random

import org.apache.http.{HttpStatus}
import org.apache.http.client.methods._
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.config.SocketConfig
import org.apache.http.util.EntityUtils

import com.datastax.Http._

object HttpRequestGenerator {

  val METHODS = Seq("POST", "GET", "DELETE", "PUT")

  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[Config]("HttpSparkStreaming") {
      head("HttpSparkStreaming Example", "1.0")
      opt[Int]('t', "numThreads") optional() action { (arg, config) => config.copy(numThreads = arg)} text {
        s"""Number of Threads to Run Reqeusts from, Default $DEFAULT_NUM_THREADS.
            If you are seeing a lot of connection exceptions try lowering this parameter to give the system more room to breath.
            If you have a beefier system or are running against remote hosts try increasing the number for increased load.""".stripMargin
      }
      opt[Int]('p', "port") optional() action { (arg, config) => config.copy(port = arg)} text {
        s"Port to connect on, Default $DEFAULT_PORT_NUM"
      }
      opt[String]('i', "ips") optional() action { (arg, config) => config.copy(ips = arg.split(","))} text {
        s"Comma seperated list of IPs to send HTTP Requests. Default ${DEFAULT_IPS.mkString(",")}"
      }
      help("help") text {
        "CLI Help"
      }
    }

    parser.parse(args, Config()) map { config =>
      startHttpRequestGenerator(config)
    } getOrElse {
      System.exit(1)
    }
  }

  def startHttpRequestGenerator(config: Config) {

    val count = new AtomicInteger(0)
    val target_ips = config.ips
    val target_port = config.port
    val cores = config.numThreads

    val httpConnectionManager = new PoolingHttpClientConnectionManager()
    //The client will be allowed one connection to each server per thread max
    httpConnectionManager.setMaxTotal(config.ips.length * config.numThreads)
    httpConnectionManager.setDefaultMaxPerRoute(config.numThreads)

    val socketConfig = SocketConfig.custom()
      .setSoReuseAddress(false)
      .setSoKeepAlive(false)
      .setTcpNoDelay(true)
      .setSoTimeout(20)
      .build()

    httpConnectionManager.setDefaultSocketConfig(socketConfig)

    val pool = Executors.newFixedThreadPool(cores + 1)
    val queue = new LinkedBlockingQueue[String](5000)
    // Submit one consumer per core.
    pool.submit(new HttpUrlGenerator(queue, target_ips, target_port))
    for (i <- 1 to cores + 1) {
      pool.submit(new HttpRequester(httpConnectionManager, queue, count))
    }
  }

  class HttpUrlGenerator(queue: LinkedBlockingQueue[String], ips: Seq[String], port: Int) extends Runnable {
    def run() {
      println("Generator Starting")
      val r = new Random()
      val toplevelDomains = Seq("home", "about", "island", "mirror", "store", "images", "gallery", "products", "streams")
      val words = Seq("apple", "banana", "orange", "kiwi", "lemon")
      while (true) {
        val ip = ips(r.nextInt(ips.length))
        val tld = toplevelDomains(r.nextInt(toplevelDomains.length))
        val sub1 = words(r.nextInt(words.length))
        val sub2 = words(r.nextInt(words.length))
        val uri = s"/$tld/$sub1/$sub2"
        queue.put(s"http://$ip:$port$uri")
      }
    }
  }

  class HttpRequester(cm: HttpClientConnectionManager, queue: LinkedBlockingQueue[String], count: AtomicInteger) extends Runnable {
    def run() {
      println("Requester Starting")
      val r = new Random()
      val httpclient = HttpClients.custom().setConnectionManager(cm).build()
      println("ClientMade")
      while (true) {
        val method = METHODS(r.nextInt(METHODS.length))
        val url = queue.take()
        val request = method match {
          case "POST" => new HttpPost(url)
          case "GET" => new HttpGet(url)
          case "DELETE" => new HttpDelete(url)
          case "PUT" => new HttpPut(url)
        }
        try {
          val resp = httpclient.execute(request)
          val entity = resp.getEntity
          EntityUtils.consume(entity)
          val status = resp.getStatusLine().getStatusCode;
          resp.close()
          if (status == HttpStatus.SC_OK) {
            val c = count.addAndGet(1)
            if (c % 500 == 0) {
              println(s"Requests: $c Executed")
            }
          }
        } catch {
          case to: SocketTimeoutException =>
            Thread sleep 20
          case e: Exception => print(e.getMessage)
            println(s"Exception on Request ${count.get()}")
            println(e.printStackTrace())
            throw e
        } finally {
          request.releaseConnection()
        }
      }
    }
  }


}
