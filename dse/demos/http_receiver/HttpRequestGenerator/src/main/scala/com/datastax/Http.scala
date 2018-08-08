/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax

object Http {

  val DEFAULT_NUM_THREADS = 4
  val DEFAULT_PORT_NUM = 9999
  val DEFAULT_IPS = Seq("127.0.0.1")

  sealed trait HttpRequestConfig extends Serializable

  case class Config(numThreads: Int = DEFAULT_NUM_THREADS,
                    port: Int = DEFAULT_PORT_NUM,
                    ips: Seq[String] = DEFAULT_IPS
                     ) extends HttpRequestConfig

}
