/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;

public class CommandLine
{
    public static Params parse(String[] args)
    {
        Params params = new Params();
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith("--"))
            {
                int eq = args[i].indexOf("=");
                if (eq < 0)
                {
                    usage();
                }
                String arg = args[i].substring(2, eq).trim().toLowerCase();
                String value = args[i].substring(eq + 1);
                try
                {
                    switch (arg)
                    {
                        case "clients":
                            params.clients = Integer.valueOf(value);
                            break;
                        case "loops":
                            params.loops = Integer.valueOf(value);
                            break;
                        case "fetch":
                            params.fetchSize = Integer.valueOf(value);
                            break;
                        case "url":
                            params.urls = new CopyOnWriteArraySet<String>(Arrays.asList(value.split(",")));
                            break;
                        case "solrcore":
                            params.indexName = value;
                            break;
                        case "testdata":
                            params.testDataFileName = value;
                            break;
                        case "seed":
                            params.randomSeed = Long.parseLong(value);
                            params.random = new Random(params.randomSeed);
                            break;
                        case "qps":
                            params.qps = Integer.parseInt(value);
                            break;
                        case "stats":
                            params.gatherStats = Boolean.parseBoolean(value);
                            break;
                        default:
                            System.out.println("Invalid argument: " + arg);
                            usage();
                            System.exit(1);
                            break;
                    }
                }
                catch (Throwable t)
                {
                    usage();
                    System.exit(1);
                }
            }
            else
            {
                System.out.println("Invalid argument: " + args[i]);
                usage();
                System.exit(1);
            }
        }
        return params;
    }

    private static void usage()
    {
        System.out.print("Allowed command line arguments:\n"
                + "[--clients=<clients count>]"
                + " [--loops=<loops count>]"
                + " [--fetch=<fetch size for cql pagination (disabled by default). Only the first page is retrieved.>]"
                + " [--solrCore=<solr core>]"
                + " [--testData=<test data file>]"
                + " [--url=<url1,url2,url3,...>]"
                + " [--qps=<max queries per second>]"
                + " [--stats=<true|false>]"
                + " [--seed=<seed value>]\n");
        System.exit(0);
    }

    public static class Params
    {
        public volatile int clients = 1;
        public volatile int loops = 1;
        public volatile int fetchSize = Integer.MAX_VALUE;
        public volatile String indexName = "demo.solr";
        public volatile CopyOnWriteArraySet<String> urls = new CopyOnWriteArraySet<>(Arrays.asList("http://localhost:8983"));
        public volatile String testDataFileName = "testMixed.txt";
        public volatile long randomSeed = System.currentTimeMillis();
        public volatile Random random = new Random(randomSeed);
        public volatile int qps = -1;
        public volatile boolean gatherStats = false;
    }
}
