/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.weather;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;


public class Station {

    private Session session;

    // I want the same weather every time we run this, so use a constant seed
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
            "CREATE TABLE IF NOT EXISTS station (\n" +
            "    stationid text,\n" +
            "    location text,\n" +
            "    PRIMARY KEY (stationid)\n" +
            ")";

    public void createTable(Cluster cluster)
    {
        session = cluster.connect(WeatherKeyspace.keyspaceName);

        System.out.println("Creating station table...");
        session.execute(CREATE_TABLE);

        System.out.println("Truncating station table...");
        session.execute("TRUNCATE station");
    }

    public List<String> generate(int numStations)
    {

        Set<String> stations = new HashSet<String>();

        // Create PreparedStatment for repeated use
        PreparedStatement stmt = session.prepare("INSERT INTO station (stationid, location) VALUES (?, ?)");

        System.out.println("Writing station information...");
        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader("resources/airports.dat"));
            String [] nextLine;
            while ((nextLine = reader.readNext()) != null && stations.size() < numStations) {
                stations.add(nextLine[4]);
                session.execute(stmt.bind(nextLine[4], nextLine[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<String>(stations);
    }

}
