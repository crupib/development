-- Create schema

CREATE KEYSPACE IF NOT EXISTS weathercql WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': '1'
};

USE weathercql;

CREATE TABLE IF NOT EXISTS historical (
    stationid text,
    time timestamp,
    temperature int,
    humidity int,
    dewpoint int,
    winddirection int,
    windspeed int,
    barometricpressure float,
    precipitation float,
    PRIMARY KEY (stationid, time)
);

CREATE TABLE IF NOT EXISTS station (
    stationid text,
    location text,
    PRIMARY KEY (stationid)
);

CREATE TABLE IF NOT EXISTS daily (
    stationid text,
    date timestamp,
    metric text,
    location text,
    min int,
    max int,
    mean int,
    median int,
    percentile1 int,
    percentile5 int,
    percentile95 int,
    percentile99 int,
    total int,
    PRIMARY KEY (stationid, metric, date)
);

CREATE TABLE IF NOT EXISTS monthly (
    stationid text,
    date timestamp,
    metric text,
    location text,
    min int,
    max int,
    mean int,
    median int,
    percentile1 int,
    percentile5 int,
    percentile95 int,
    percentile99 int,
    total int,
    PRIMARY KEY (stationid, metric, date)
);
