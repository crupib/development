/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

public class Geonames
{
    private static final GeonamesLineParser GEONAMES_LINE_PARSER = new GeonamesLineParser();

    final File file;
    final ConcurrentUpdateSolrServer client;
    final String index;

    public static void main(String[] args) throws Exception
    {
        File file = new File(args[0]);
        String url = args[1];
        int threadCount = Integer.parseInt(args[2]);
        int max = Integer.parseInt(args[3]);
        String index = args[4];
        boolean commitBeforeShutdown = needsCommitBeforeShutdown(args);

        Geonames geo = new Geonames(url, threadCount, file, index);

        ScheduledExecutorService schedExec = Executors.newSingleThreadScheduledExecutor();

        StatsPoller statsPoller = new StatsPoller(url, index);

        Future future = schedExec.scheduleWithFixedDelay(statsPoller, 100, 50, TimeUnit.MILLISECONDS);

        System.out.println("Starting...");

        long startTime = System.currentTimeMillis();
        geo.load(max, commitBeforeShutdown);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Duration="+duration);

        future.cancel(false);

        schedExec.shutdown();
        schedExec.awaitTermination(5000, TimeUnit.MILLISECONDS);

        System.out.println("Writing statistics file.");
        File statsFile = new File("stats/stressTestStats" + System.currentTimeMillis() + ".stats");
        statsFile.getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter(statsFile, "UTF-8");
        statsPoller.writeStatsToFile(writer);
        writer.close();
    }

    public static boolean needsCommitBeforeShutdown(String[] args)
    {
        if (args.length > 5)
        {
            return Boolean.valueOf(args[5]);
        }
        return false;
    }

    public Geonames(String url, int threadCount, File file, String index)
    {
        this.file = file;
        this.index = index;

        String clientUrl = url+"/solr/"+index;
        System.out.println("client url=" + clientUrl);

        client = new ConcurrentUpdateSolrServer(clientUrl, 1000, threadCount);
    }

    public void load(int max, boolean commitBeforeShutdown) throws Exception
    {
        int counter = 0;
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (counter < max)
        {
            String line = reader.readLine();

            //System.out.println(line);

            line(line);

            counter++;
        }

        reader.close();

        client.blockUntilFinished();

        if (commitBeforeShutdown)
        {
            client.commit();
        }

        client.shutdown();
    }

    public void line(String line) throws Exception
    {
        Map<String, String> fields = GEONAMES_LINE_PARSER.parseLine(line);
        SolrInputDocument doc = new SolrInputDocument();
        for (Map.Entry<String, String> entry : fields.entrySet())
        {
            doc.addField(entry.getKey(), entry.getValue());
        }

        //System.out.println("doc="+doc);

        client.add(doc);
    }

    public static class GeonamesLineParser
    {

        public Map<String, String> parseLine(String line)
        {
            // no more than first 6 fields needed
            String[] parts = line.split("\\t");

            // note: overwrites ID assigned by LineDocSource
            int id = Integer.parseInt(parts[0]);
            String name = parts[1];
            String latitude = parts[4];
            String longitude = parts[5];

            String country = parts[8];
            String timezone = parts[17];
            String date = parts[18];

            Map<String, String> fields = Maps.newHashMap();
            fields.put("id", String.valueOf(id));
            fields.put("name", name);
            fields.put("latitude", latitude);
            fields.put("longitude", longitude);
            fields.put("country", country);
            fields.put("timezone", timezone);
            fields.put("published", date);
            return fields;
        }
    }

    private static class StatsPoller implements Runnable
    {
        public final static String softCommitTimesStringId = "soft_commit_times";
        public final static String updateHandlerStatsURI = "/admin/mbeans?stats=true&cat=UPDATEHANDLER";
        private final String baseURI;
        private final Map<Long, NamedList> stats = new HashMap<Long, NamedList>();
        final HttpSolrServer client;

        public StatsPoller(String url, String index) throws Exception
        {
            URL solrStatsURL = new URL((String) url);
            baseURI = solrStatsURL.getProtocol() + "://" + solrStatsURL.getHost() + ":" + solrStatsURL.getPort()
                    + "/solr/" + index;

            client = new HttpSolrServer(baseURI);
        }

        @Override
        public void run()
        {
            try
            {
                SolrQuery q = new SolrQuery();
                q.set("qt", "/admin/mbeans?key=updateHandler&stats=true");
                QueryResponse rsp = client.query(q);

                NamedList<Object> resp = rsp.getResponse();

                System.out.println("resp="+resp);

                NamedList mbeans = (NamedList) resp.get("solr-mbeans");
                NamedList uhandlerCat = (NamedList) mbeans.get("UPDATEHANDLER");
                NamedList uhandler = (NamedList) uhandlerCat.get("updateHandler");
                NamedList statsNL = (NamedList) uhandler.get("stats");
                NamedList openStats = (NamedList) statsNL.get("openStats");

                // System.out.println("openStats=" + openStats);

                if (openStats != null)
                {
                    long softCommitCompletedTimestamp = (Long) openStats.get("timestamp");
                    long softCommitDuration = (Long) openStats.get("totalDuration");
                    long softCommitStartedTimestamp = softCommitCompletedTimestamp - softCommitDuration;
                    openStats.add("softCommitStarted", softCommitStartedTimestamp);

                    stats.put(softCommitCompletedTimestamp, openStats);
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }

        public void writeStatsToFile(PrintWriter writer)
        {
            writer.print(StatsPoller.softCommitTimesStringId);
            writer.println();

            TreeMap<Long, NamedList> sortedMap = new TreeMap<>(stats);

            boolean printedHeader = false;

            for (Map.Entry<Long, NamedList> entry : sortedMap.entrySet())
            {
                long timestamp = entry.getKey();
                NamedList values = entry.getValue();

                if (!printedHeader)
                {
                    for (int x = 0; x < values.size(); x++)
                    {
                        writer.print(values.getName(x));
                        if (x < values.size() - 1)
                        {
                            writer.print(",");
                        }
                    }
                    writer.println();
                    printedHeader = true;
                }
                for (int x = 0; x < values.size(); x++)
                {
                    writer.print(values.getVal(x));
                    if (x < values.size() - 1)
                    {
                        writer.print(",");
                    }
                }
                writer.println();
            }
        }
    }
}
