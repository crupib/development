-- Access the data in cassandra
-- use automapping

use weathercql;

-- Write aggregates into a daily table
INSERT OVERWRITE TABLE daily
SELECT h.stationid AS stationid,
       '${hiveconf:METRIC}' AS metric,
       unix_timestamp(to_date(h.time), 'yyyy-MM-dd') * 1000 AS date,
       s.location AS location,
       max(h.${hiveconf:METRIC}) AS max,
       avg(h.${hiveconf:METRIC}) AS mean,
       percentile_approx(h.${hiveconf:METRIC}, 0.5) AS median,
       min(h.${hiveconf:METRIC}) AS min,
       percentile_approx(h.${hiveconf:METRIC}, 0.01) AS percentile1,
       percentile_approx(h.${hiveconf:METRIC}, 0.05) AS percentile5,
       percentile_approx(h.${hiveconf:METRIC}, 0.95) AS percentile95,
       percentile_approx(h.${hiveconf:METRIC}, 0.99) AS percentile99,
       sum(h.${hiveconf:METRIC}) AS total
FROM historical h
JOIN station s
ON (h.stationid = s.stationid)
GROUP BY h.stationid, s.location, to_date(h.time);


-- Write aggregates into a monthly table
INSERT OVERWRITE TABLE monthly
SELECT h.stationid AS stationid,
       '${hiveconf:METRIC}' AS metric,
       unix_timestamp(concat(year(h.time), '-', lpad(month(h.time), 2, '0')), 'yyyy-MM') * 1000 AS date,
       s.location AS location,
       max(h.${hiveconf:METRIC}) AS max,
       avg(h.${hiveconf:METRIC}) AS mean,
       percentile_approx(h.${hiveconf:METRIC}, 0.5) AS median,
       min(h.${hiveconf:METRIC}) AS min,
       percentile_approx(h.${hiveconf:METRIC}, 0.01) AS percentile1,
       percentile_approx(h.${hiveconf:METRIC}, 0.05) AS percentile5,
       percentile_approx(h.${hiveconf:METRIC}, 0.95) AS percentile95,
       percentile_approx(h.${hiveconf:METRIC}, 0.99) AS percentile99,
       sum(h.${hiveconf:METRIC}) AS total
FROM historical h
JOIN station s
ON (h.stationid = s.stationid)
GROUP BY h.stationid, s.location, unix_timestamp(concat(year(h.time), '-', lpad(month(h.time), 2, '0')), 'yyyy-MM') * 1000;
