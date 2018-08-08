/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.weather;

import java.util.List;

import org.apache.commons.cli.*;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;

public class DataLoader
{
    private static Cluster cluster;
    private static int startYear = 2014;
    private static int numYears = 3;
    private static int numStations = 10;
    private static String cassNode = "localhost";

    private static void parseCommandLineArguments(String [] args)
    {

        Options options = new Options();
        options.addOption("h", "help", false,
                "prints this message");
        options.addOption("e", "startyear", true,
                String.format("year to start generating data. [default: %d]", startYear));
        options.addOption("y", "years", true,
                String.format("number of years worth of data to generate. [default: %d]", numYears));
        options.addOption("s", "stations", true,
                String.format("number of stations worth of data to generate. [default: %d]", numStations));
        options.addOption("d", "node", true,
                String.format("Cassandra node. [default: %s]", cassNode));

        CommandLine line = null;
        CommandLineParser parser = new GnuParser();
        try {
            line = parser.parse( options, args );
        } catch( ParseException exp ) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("weather-sensors", options);
            System.exit(0);
        }
        if (line.hasOption("startyear")) {
            startYear = Integer.parseInt(line.getOptionValue("startyear"));
        }
        if (line.hasOption("years")) {
            numYears = Integer.parseInt(line.getOptionValue("years"));
        }
        if (line.hasOption("stations")) {
            numStations = Integer.parseInt(line.getOptionValue("stations"));
        }
        if (line.hasOption("node")) {
            cassNode = line.getOptionValue("node");
        }

        System.out.printf("\nGenerating %d years, %d stations, Cassandra node %s.\n\n",
                numYears, numStations, cassNode);
    }

    public static void main(String [] args)
    {
        parseCommandLineArguments(args);

        connect(cassNode);

        try {
            WeatherKeyspace keyspace = new WeatherKeyspace();
            Weather weather = new Weather();
            Station station = new Station();

            // Create Cassandra schema dynamically
            // An anti-pattern in production, but using this for a smooth flowing demo
            keyspace.doCreateKeyspace(cluster);
            weather.createTable(cluster);
            station.createTable(cluster);

            // Generate station and weather data
            List<String> stations = station.generate(numStations);
            weather.generate(stations, startYear, numYears);
        } finally {
            cluster.close();
        }
    }

    public static void connect(String node) {
        // Connect to a Cassandra cluster
        cluster = Cluster.builder()
                .addContactPoint(node)
                .build();
        Metadata metadata = cluster.getMetadata();

        // Display data about the connection
        System.out.printf("Connected to cluster: %s\n",
                metadata.getClusterName());
        for ( Host host : metadata.getAllHosts() ) {
            System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n",
                    host.getDatacenter(), host.getAddress(), host.getRack());
        }
    }
}
