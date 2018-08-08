/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.weather;


import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

import org.joda.time.DateTime;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

public class Weather {

    private Session session;

    private static SecureRandom random;

    static {
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(new byte[]{0x00});
        }catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS historical (\n" +
            "    stationid text,\n" +
            "    time timestamp,\n" +
            "    temperature int,\n" +
            "    humidity int,\n" +
            "    dewpoint int,\n" +
            "    winddirection int,\n" +
            "    windspeed int,\n" +
            "    barometricpressure float,\n" +
            "    precipitation float,\n" +
            "    PRIMARY KEY (stationid, time)\n" +
            ")";

    private static String CREATE_DAILY_TABLE =
            "CREATE TABLE IF NOT EXISTS daily (\n" +
            "    stationid text,\n" +
            "    date timestamp,\n" +
            "    metric text,\n" +
            "    location text,\n" +
            "    min int,\n" +
            "    max int,\n" +
            "    mean int,\n" +
            "    median int,\n" +
            "    percentile1 int,\n" +
            "    percentile5 int,\n" +
            "    percentile95 int,\n" +
            "    percentile99 int,\n" +
            "    total int,\n" +
            "    PRIMARY KEY (stationid, metric, date)\n" +
            ")";

    private static String CREATE_MONTHLY_TABLE =
            "CREATE TABLE IF NOT EXISTS monthly (\n" +
            "    stationid text,\n" +
            "    date timestamp,\n" +
            "    metric text,\n" +
            "    location text,\n" +
            "    min int,\n" +
            "    max int,\n" +
            "    mean int,\n" +
            "    median int,\n" +
            "    percentile1 int,\n" +
            "    percentile5 int,\n" +
            "    percentile95 int,\n" +
            "    percentile99 int,\n" +
            "    total int,\n" +
            "    PRIMARY KEY (stationid, metric, date)\n" +
            ")";

    public void createTable(Cluster cluster)
    {
        session = cluster.connect(WeatherKeyspace.keyspaceName);

        System.out.println("Creating historical table...");
        session.execute(CREATE_TABLE);

        System.out.println("Creating daily table...");
        session.execute(CREATE_DAILY_TABLE);

        System.out.println("Creating monthly table...");
        session.execute(CREATE_MONTHLY_TABLE);

        System.out.println("Truncating historical table...");
        session.execute("TRUNCATE historical");

        System.out.println("Truncating daily table...");
        session.execute("TRUNCATE daily");

        System.out.println("Truncating monthly table...");
        session.execute("TRUNCATE monthly");
    }

    public void generate(List<String> stations, int startYear, int numYears) {
        // Create a PreparedStatement for repeated use
        PreparedStatement stmt = session.prepare(
                "INSERT INTO historical (" +
                    "stationid, time, temperature, humidity, dewpoint, " +
                    "winddirection, windspeed, barometricpressure, precipitation" +
                " ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        );

        DateTime start = new DateTime(startYear, 1, 1, 0, 0);
        DateTime end = new DateTime(startYear + numYears, 1, 1, 0, 0);

        Map<String, Map<String, Float>> lastMetrics = new HashMap<String, Map<String, Float>>();
        for (String station : stations) {
            // Find decent starting values
            Map<String, Float> thisStation = new HashMap<String, Float>();

            thisStation.put("temperature", generateStartingDouble(50, 80));
            thisStation.put("humidity", generateStartingDouble(40, 60));
            thisStation.put("dewPoint", generateStartingDouble(50, 70));
            thisStation.put("windDirection", generateStartingDouble(0, 360));
            thisStation.put("windSpeed", generateStartingDouble(0, 10));
            thisStation.put("barometricPressure", generateStartingDouble(900, 1000));
            thisStation.put("precipitation", generateStartingDouble(0, 0));

            lastMetrics.put(station, thisStation);
        }

        System.out.println("Generating weather sensor data...");

        for (DateTime date : new DateTimeRange(start, end)) {
            for (String station : stations) {

                Map<String, Float> thisStation = lastMetrics.get(station);

                // Create a batch statement object
                // Batches are only acceptable when:
                // a) requiring atomicity
                // b) grouping statements of the same primary key
                // This case falls into scenario b.
                BatchStatement batch = new BatchStatement();

                for (int h = 0; h < 24; ++h) {
                    for (int m = 0; m < 60; ++m) {
                        // Generate new values
                        thisStation.put("temperature",
                                        generateNextDouble(thisStation.get("temperature"), -100, 134, 1, 0.10, 0.20, false));
                        thisStation.put("humidity",
                                        generateNextDouble(thisStation.get("humidity"), 0, 100, 1, 0.05, 0.10, false));
                        thisStation.put("dewPoint",
                                        generateNextDouble(thisStation.get("dewPoint"), 30, 90, 1, 0.05, 0.10, false));
                        thisStation.put("windDirection",
                                        generateNextDouble(thisStation.get("windDirection"), 0, 360, 1, 0.15, 0.30, true));
                        thisStation.put("windSpeed",
                                        generateNextDouble(thisStation.get("windSpeed"), 0, 108, 10, 0.15, 0.50, true));
                        thisStation.put("barometricPressure",
                                        generateNextDouble(thisStation.get("barometricPressure"), 870, 1085, 1, 0.05, 0.10, false));
                        thisStation.put("precipitation",
                                        generateNextDouble(thisStation.get("precipitation"), 0, 71, 1, 0.20, 0.80, false));

                        // Add new values to a batch statement for later processing
                        batch.add(stmt.bind(
                                station,
                                date.plusHours(h).plusMinutes(m).toDate(),
                                (int) thisStation.get("temperature").floatValue(),
                                (int) thisStation.get("humidity").floatValue(),
                                (int) thisStation.get("dewPoint").floatValue(),
                                (int) thisStation.get("windDirection").floatValue(),
                                (int) thisStation.get("windSpeed").floatValue(),
                                thisStation.get("barometricPressure"),
                                thisStation.get("precipitation")
                        ));
                    }
                }

                // Execute batch write
                session.execute(batch);
            }
        }

        System.out.printf("Done\n");
    }

    public float generateStartingDouble(int min, int max) {
        return random.nextFloat() * (max - min) + min;
    }

    public float generateNextDouble(float current, int min, int max, float maxChange,
                                    double positiveChange, double negativeChange,
                                    boolean cyclic) {
        float nextValue;

        float changeDirection = random.nextFloat();
        if (changeDirection < positiveChange)
            nextValue = current + random.nextFloat() * maxChange;
        else if (changeDirection < negativeChange)
            nextValue = current - random.nextFloat() * maxChange;
        else
            nextValue = current;

        if (cyclic) {
            if (nextValue > max)
                nextValue = nextValue - max + min;
            else if (nextValue < min)
                nextValue = nextValue - min + max;
        } else {
            if (nextValue > max)
                nextValue = max;
            else if (nextValue < min)
                nextValue = min;
        }
        return nextValue;
    }
}


class DateTimeRange implements Iterable<DateTime>
{
    private final DateTime start;
    private final DateTime end;

    public DateTimeRange(DateTime start,
                         DateTime end)
    {
        this.start = start;
        this.end = end;
    }

    public Iterator<DateTime> iterator()
    {
        return new DateTimeRangeIterator(start, end);
    }

    private static class DateTimeRangeIterator implements Iterator<DateTime>
    {
        private DateTime current;
        private final DateTime end;

        private DateTimeRangeIterator(DateTime start,
                                      DateTime end)
        {
            this.current = start;
            this.end = end;
        }

        public boolean hasNext()
        {
            return current != null;
        }

        public DateTime next()
        {
            if (current == null)
            {
                throw new NoSuchElementException();
            }
            DateTime ret = current;
            current = current.plusDays(1);
            if (current.compareTo(end) > 0)
            {
                current = null;
            }
            return ret;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
