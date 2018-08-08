/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */

package com.datastax.bdp.spark.demo

import org.apache.spark.{SparkConf, SparkContext}
import org.joda.time.{DateTime, Period}

import com.datastax.spark.connector._

case class StockHist(symbol: String, date: DateTime, value: Double)
case class Portfolios(key: Long, symbol: String, value: Double)
case class Return(symbol: String, date: DateTime, value: Double)
case class HistLoss(key: String, column1: String, value: String)

object Spark10DayLoss extends App {

  val sc = new SparkContext(new SparkConf().setAppName("PortfolioDemo"))

  val stockHist = sc.cassandraTable("PortfolioDemo", "StockHist").select("key", "column1", "value").as(StockHist)
  val portfolios = sc.cassandraTable("PortfolioDemo", "Portfolios").select("key", "column1", "value").as(Portfolios)

  val a = stockHist.keyBy(r => (r.symbol, r.date.plus(Period.days(10))))
  val b = stockHist.keyBy(r => (r.symbol, r.date))

  val tenDayReturns = a.join(b)
    .map { case ((symbol, date), (aRow, bRow)) => (symbol, Return(symbol, date, bRow.value - aRow.value)) }

  val portfolioReturns = portfolios
    .keyBy(_.symbol)
    .join(tenDayReturns)
    .map { case (_, (portfolio, ret)) => ((portfolio.key, ret.date), ret.value)}
    .reduceByKey(_ + _)

  val histLoss = portfolioReturns
    .map { case ((key, date), ret) => (key, (date, ret)) }
    .reduceByKey((a, b) => if (a._2 < b._2) a else b)

  histLoss
    .flatMap { case (key, (date, ret)) =>
      HistLoss(key = key.toString, column1 = "worst_date", value = date.toString("yyyy-MM-dd")) ::
      HistLoss(key = key.toString, column1 = "loss", value = ret.toString) :: Nil
    }
    .saveToCassandra("PortfolioDemo", "HistLoss")

  sc.stop()
}
