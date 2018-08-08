/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */

package com.datastax.bdp.spark.demo.java;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import com.datastax.spark.connector.japi.rdd.CassandraJavaRDD;
import org.joda.time.DateTime;
import org.joda.time.Period;
import scala.Tuple2;

import java.io.Serializable;
import java.util.Arrays;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.*;

public class Spark10DayLossJava implements Serializable
{

    private JavaSparkContext createSparkContext()
    {
        SparkConf conf = new SparkConf().setAppName("PortfolioDemo (Java)");
        return new JavaSparkContext(conf);
    }

    private void start()
    {
        JavaSparkContext sc = createSparkContext();

        CassandraJavaRDD<StockHist> stockHist = javaFunctions(sc).cassandraTable("PortfolioDemo", "StockHist",
                mapRowTo(StockHist.class, Pair.of("symbol", "key"), Pair.of("date", "column1")))
                .select("key", "column1", "value");

        CassandraJavaRDD<Portfolios> portfolios = javaFunctions(sc).cassandraTable("PortfolioDemo", "Portfolios",
                mapRowTo(Portfolios.class, Pair.of("symbol", "column1")))
                .select("key", "column1", "value");

        JavaPairRDD<Tuple2<String, DateTime>, StockHist> a = stockHist.keyBy(new Function<StockHist, Tuple2<String, DateTime>>()
        {
            @Override
            public Tuple2<String, DateTime> call(StockHist stockHist) throws Exception
            {
                return new Tuple2<>(stockHist.getSymbol(), stockHist.getDate().plus(Period.days(10)));
            }
        });
        JavaPairRDD<Tuple2<String, DateTime>, StockHist> b = stockHist.keyBy(new Function<StockHist, Tuple2<String, DateTime>>()
        {
            @Override
            public Tuple2<String, DateTime> call(StockHist stockHist) throws Exception
            {
                return new Tuple2<>(stockHist.getSymbol(), stockHist.getDate());
            }
        });

        JavaPairRDD<String, Return> tenDayReturns = a.join(b)
                .mapToPair(new PairFunction<Tuple2<Tuple2<String, DateTime>, Tuple2<StockHist, StockHist>>, String, Return>()
                {
                    @Override
                    public Tuple2<String, Return> call(Tuple2<Tuple2<String, DateTime>, Tuple2<StockHist, StockHist>> input) throws Exception
                    {
                        String symbol = input._1()._1();
                        DateTime date = input._1()._2();
                        StockHist aRow = input._2()._1();
                        StockHist bRow = input._2()._2();

                        return new Tuple2<>(symbol, new Return(symbol, date, bRow.getValue() - aRow.getValue()));
                    }
                });

        JavaPairRDD<Tuple2<Long, DateTime>, Double> portfolioReturns = portfolios.keyBy(new Function<Portfolios, String>()
        {
            @Override
            public String call(Portfolios portfolios) throws Exception
            {
                return portfolios.getSymbol();
            }
        }).join(tenDayReturns)
                .mapToPair(new PairFunction<Tuple2<String, Tuple2<Portfolios, Return>>, Tuple2<Long, DateTime>, Double>()
                {
                    @Override
                    public Tuple2<Tuple2<Long, DateTime>, Double> call(Tuple2<String, Tuple2<Portfolios, Return>> input) throws Exception
                    {
                        Portfolios portfolio = input._2()._1();
                        Return ret = input._2()._2();

                        return new Tuple2<>(new Tuple2<>(portfolio.getKey(), ret.getDate()), ret.getValue());
                    }
                }).reduceByKey(new Function2<Double, Double, Double>()
                {
                    @Override
                    public Double call(Double value1, Double value2) throws Exception
                    {
                        return value1 + value2;
                    }
                });

        JavaPairRDD<Long, Tuple2<DateTime, Double>> histLoss = portfolioReturns.mapToPair(new PairFunction<Tuple2<Tuple2<Long, DateTime>, Double>, Long, Tuple2<DateTime, Double>>()
        {
            @Override
            public Tuple2<Long, Tuple2<DateTime, Double>> call(Tuple2<Tuple2<Long, DateTime>, Double> input) throws Exception
            {
                Long key = input._1()._1();
                DateTime date = input._1()._2();
                Double ret = input._2();

                return new Tuple2<>(key, new Tuple2<>(date, ret));
            }
        }).reduceByKey(new Function2<Tuple2<DateTime, Double>, Tuple2<DateTime, Double>, Tuple2<DateTime, Double>>()
        {
            @Override
            public Tuple2<DateTime, Double> call(Tuple2<DateTime, Double> a, Tuple2<DateTime, Double> b) throws Exception
            {
                return a._2() < b._2() ? a : b;
            }
        });

        javaFunctions(histLoss.flatMap(new FlatMapFunction<Tuple2<Long, Tuple2<DateTime, Double>>, HistLoss>()
        {
            @Override
            public Iterable<HistLoss> call(Tuple2<Long, Tuple2<DateTime, Double>> input) throws Exception
            {
                Long key = input._1();
                DateTime date = input._2()._1();
                Double ret = input._2()._2();

                return Arrays.asList(
                        new HistLoss(key.toString(), "worst_date", date.toString("yyyy-MM-dd")),
                        new HistLoss(key.toString(), "loss", ret.toString())
                );
            }
        })).writerBuilder("PortfolioDemo", "HistLoss", mapToRow(HistLoss.class)).saveToCassandra();

        sc.stop();
    }

    public static void main(String[] args)
    {
        new Spark10DayLossJava().start();
    }
}
