-- Create schema for Cassandra tables

CREATE DATABASE IF NOT EXISTS weathercql;

USE weathercql;

CREATE EXTERNAL TABLE IF NOT EXISTS daily ( 
  stationid string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data',
  metric string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data',
  date timestamp  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.TimestampType from Column Family meta data',
  location string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data',
  max int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  mean int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  median int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  min int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  percentile1 int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  percentile5 int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  percentile95 int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  percentile99 int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  total int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data')
ROW FORMAT SERDE
  'org.apache.hadoop.hive.cassandra.cql3.serde.CqlColumnSerDe' 
STORED BY
  'org.apache.hadoop.hive.cassandra.cql3.CqlStorageHandler' 
WITH SERDEPROPERTIES (
  'serialization.format'='1',
  'cassandra.columns.mapping'='stationid,metric,date,location,max,mean,median,min,percentile1,percentile5,percentile95,percentile99,total')
TBLPROPERTIES (
  'auto_created' = 'true',
  'cassandra.partitioner' = 'org.apache.cassandra.dht.Murmur3Partitioner',
  'cql3.partition.key' = 'stationid',
  'cassandra.ks.name' = 'weathercql',
  'cassandra.cf.name' = 'daily');

CREATE EXTERNAL TABLE IF NOT EXISTS historical ( 
  stationid string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data',
  time timestamp  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.TimestampType from Column Family meta data',
  barometricpressure float  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.FloatType from Column Family meta data',
  dewpoint int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  humidity int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  precipitation float  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.FloatType from Column Family meta data',
  temperature int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  winddirection int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  windspeed int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data')
ROW FORMAT SERDE
  'org.apache.hadoop.hive.cassandra.cql3.serde.CqlColumnSerDe' 
STORED BY
  'org.apache.hadoop.hive.cassandra.cql3.CqlStorageHandler' 
WITH SERDEPROPERTIES (
  'serialization.format'='1',
  'cassandra.columns.mapping'='stationid,time,barometricpressure,dewpoint,humidity,precipitation,temperature,winddirection,windspeed')
TBLPROPERTIES (
  'auto_created' = 'true',
  'cassandra.partitioner' = 'org.apache.cassandra.dht.Murmur3Partitioner',
  'cql3.partition.key' = 'stationid',
  'cassandra.ks.name' = 'weathercql',
  'cassandra.cf.name' = 'historical');

CREATE EXTERNAL TABLE IF NOT EXISTS monthly ( 
  stationid string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data',
  metric string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data',
  date timestamp  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.TimestampType from Column Family meta data',
  location string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data',
  max int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  mean int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  median int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  min int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  percentile1 int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  percentile5 int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  percentile95 int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  percentile99 int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data',
  total int  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.Int32Type from Column Family meta data')
ROW FORMAT SERDE
  'org.apache.hadoop.hive.cassandra.cql3.serde.CqlColumnSerDe' 
STORED BY
  'org.apache.hadoop.hive.cassandra.cql3.CqlStorageHandler' 
WITH SERDEPROPERTIES (
  'serialization.format'='1',
  'cassandra.columns.mapping'='stationid,metric,date,location,max,mean,median,min,percentile1,percentile5,percentile95,percentile99,total')
TBLPROPERTIES (
  'auto_created' = 'true',
  'cassandra.partitioner' = 'org.apache.cassandra.dht.Murmur3Partitioner',
  'cql3.partition.key' = 'stationid',
  'cassandra.ks.name' = 'weathercql',
  'cassandra.cf.name' = 'monthly');

CREATE EXTERNAL TABLE IF NOT EXISTS station ( 
  stationid string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data',
  location string  COMMENT 'Auto-created based on org.apache.cassandra.db.marshal.UTF8Type from Column Family meta data')
ROW FORMAT SERDE
  'org.apache.hadoop.hive.cassandra.cql3.serde.CqlColumnSerDe' 
STORED BY
  'org.apache.hadoop.hive.cassandra.cql3.CqlStorageHandler' 
WITH SERDEPROPERTIES (
  'serialization.format'='1',
  'cassandra.columns.mapping'='stationid,location')
TBLPROPERTIES (
  'auto_created' = 'true',
  'cassandra.partitioner' = 'org.apache.cassandra.dht.Murmur3Partitioner',
  'cql3.partition.key' = 'stationid',
  'cassandra.ks.name' = 'weathercql',
  'cassandra.cf.name' = 'station');



-- Create external tables from csv files

CREATE DATABASE IF NOT EXISTS weatherdfs;

USE weatherdfs;

CREATE EXTERNAL TABLE IF NOT EXISTS station (
    stationid string,
    location string)
ROW FORMAT DELIMITED
    FIELDS TERMINATED BY ','
    LINES TERMINATED BY '\n'
STORED AS TEXTFILE
LOCATION '/datastax/demos/weather_sensors/byoh-station.csv';

CREATE EXTERNAL TABLE IF NOT EXISTS daily (
    stationid string,
    metric string,
    date timestamp,
    location string,
    max int,
    mean int,
    median int,
    min int,
    percentile1 int,
    percentile5 int,
    percentile95 int,
    percentile99 int,
    total int)
ROW FORMAT DELIMITED
    FIELDS TERMINATED BY ','
    LINES TERMINATED BY '\n'
STORED AS TEXTFILE
LOCATION '/datastax/demos/weather_sensors/byoh-daily.csv';

CREATE EXTERNAL TABLE IF NOT EXISTS monthly (
    stationid string,
    metric string,
    date timestamp,
    location string,
    max int,
    mean int,
    median int,
    min int,
    percentile1 int,
    percentile5 int,
    percentile95 int,
    percentile99 int,
    total int)
ROW FORMAT DELIMITED
    FIELDS TERMINATED BY ','
    LINES TERMINATED BY '\n'
STORED AS TEXTFILE
LOCATION '/datastax/demos/weather_sensors/byoh-monthly.csv';
