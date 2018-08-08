/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.weather;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class WeatherKeyspace {

    private Session session;

    public static String keyspaceName = "weathercql";

    private static String createKeyspaceStatement =
            "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName +
            " WITH replication = {\n" +
            "  'class': 'SimpleStrategy',\n" +
            "  'replication_factor': '1'\n" +
            "};";

    public void doCreateKeyspace(Cluster cluster)
    {
        session = cluster.connect();
        System.out.println("Creating " + keyspaceName + " keyspace...");
        session.execute(createKeyspaceStatement);
    }
}
