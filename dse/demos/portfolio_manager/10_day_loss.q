--Access the data in cassandra

DROP TABLE IF EXISTS Portfolios;
CREATE EXTERNAL TABLE Portfolios(row_key bigint, column_name string, value bigint)
ROW FORMAT SERDE
  "org.apache.hadoop.hive.cassandra.cql3.serde.CqlColumnSerDe"
STORED BY
  "org.apache.hadoop.hive.cassandra.cql3.CqlStorageHandler"
WITH SERDEPROPERTIES (
  "cassandra.columns.mapping" = "key,column1,value",
  "cassandra.input.split.size" = "64000",
  "cassandra.page.size" = "1000"
  )
TBLPROPERTIES (
  "cassandra.ks.name" = "PortfolioDemo",
  "cassandra.cf.name" = "Portfolios" ,
  "cassandra.partitioner" = "org.apache.cassandra.dht.Murmur3Partitioner",
  "cql3.partition.key"="key"
);

DROP TABLE IF EXISTS StockHist;
CREATE EXTERNAL TABLE StockHist(row_key string, column_name string, value double)
ROW FORMAT SERDE
  "org.apache.hadoop.hive.cassandra.cql3.serde.CqlColumnSerDe"
STORED BY
  "org.apache.hadoop.hive.cassandra.cql3.CqlStorageHandler"
WITH SERDEPROPERTIES (
  "cassandra.columns.mapping" = "key,column1,value")
TBLPROPERTIES(
  "cassandra.ks.name" = "PortfolioDemo",
  "cassandra.cf.name" = "StockHist" ,
  "cassandra.partitioner" = "org.apache.cassandra.dht.Murmur3Partitioner",
  "cql3.partition.key"="key"
);

--first calculate returns
DROP TABLE IF EXISTS 10dayreturns;
CREATE TABLE 10dayreturns(ticker string, rdate string, return double)
STORED AS SEQUENCEFILE;

INSERT OVERWRITE TABLE 10dayreturns
select a.row_key ticker, b.column_name rdate, (b.value - a.value) ret
from StockHist a JOIN StockHist b on
(a.row_key = b.row_key AND date_add(a.column_name,10) = b.column_name);


--CALCULATE PORTFOLIO RETURNS
DROP TABLE IF EXISTS portfolio_returns;
CREATE TABLE portfolio_returns(portfolio bigint, rdate string, preturn double)
STORED AS SEQUENCEFILE;


INSERT OVERWRITE TABLE portfolio_returns
select row_key portfolio, rdate, SUM(b.return)
from Portfolios a JOIN 10dayreturns b ON
    (a.column_name = b.ticker)
group by row_key, rdate;


--Next find worst returns and save them back to cassandra
DROP TABLE IF EXISTS HistLoss;
create external table HistLoss(row_key string, column_name string, value string)
STORED BY
  "org.apache.hadoop.hive.cassandra.cql3.CqlStorageHandler"
WITH SERDEPROPERTIES (
  "cassandra.columns.mapping" = "key,column1,value")
TBLPROPERTIES (
  "cassandra.ks.name" = "PortfolioDemo",
  "cassandra.cf.name" = "HistLoss" ,
  "cassandra.partitioner" = "org.apache.cassandra.dht.Murmur3Partitioner",
  "cql3.partition.key"="key"
);

INSERT OVERWRITE TABLE HistLoss
select cast(portfolio as string), "loss", cast(MIN(preturn) as string)
  FROM portfolio_returns
  group by portfolio;

INSERT OVERWRITE TABLE HistLoss
select cast(a.portfolio as string), "worst_date", rdate
FROM (
  select portfolio, MIN(preturn) as minp
  FROM portfolio_returns
  group by portfolio
) a JOIN portfolio_returns b ON (a.portfolio = b.portfolio and a.minp = b.preturn);
