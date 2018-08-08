/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.clearspring.analytics.util.Lists;
import com.datastax.dse.demos.solr.readers.InputReaderStrategy;
import com.datastax.dse.demos.solr.readers.InputReaderStrategySelector;
import com.datastax.dse.demos.solr.stats.DefaultStatsRecorder;
import com.datastax.dse.demos.solr.stats.StatsRecorder;
import org.HdrHistogram.AtomicHistogram;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import com.datastax.dse.demos.solr.commands.CmdRunner;
import com.datastax.dse.demos.solr.commands.CmdRunnerFactory;
import com.google.common.util.concurrent.RateLimiter;

public class SolrStress
{
    private static final long MAX_USEC_VALUE_IN_HISTOGRAM = 300000000;
    private static final AtomicInteger threadId = new AtomicInteger(0);
    private static final AtomicLong totalCommandsExecuted = new AtomicLong(0);
    private static final AtomicHistogram queriesHistogram = new AtomicHistogram(MAX_USEC_VALUE_IN_HISTOGRAM, 3);
    private static final AtomicLong queriesNotInHistogram = new AtomicLong(0);
    private static volatile boolean testRunHadExceptions = false;


    public static void main(String[] args) throws Exception
    {
        Utils.init();

        System.out.println("Reading command line...");
        CommandLine.Params params = CommandLine.parse(args);

        InputReaderStrategy inputReader = new InputReaderStrategySelector(params.random, params.loops)
                .createInputReader(new File(params.testDataFileName));
        DefaultStatsRecorder statsRecorder = new DefaultStatsRecorder();

        System.out.println("Starting Benchmark...\n");
        ExecutorService threadPool = Executors.newFixedThreadPool(params.clients);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < params.clients; i++)
        {
            threadPool.submit(new StressRunner(params, inputReader, statsRecorder));
        }
        threadPool.shutdown();

        long endTime = 0;
        try
        {
            boolean terminated = false;
            while (!terminated)
            {
                terminated = threadPool.awaitTermination(1, TimeUnit.MINUTES);
                System.out.println("\nTest running, " + totalCommandsExecuted.get() + " commands executed so far.");
            }
            endTime = System.currentTimeMillis();
        }
        catch (InterruptedException e)
        {
            threadPool.shutdownNow();
            System.out.println("\nBenchmark manually stopped. Random seed: " + params.randomSeed);
            System.exit(1);
        }

        if (testRunHadExceptions)
        {
            System.out.println("\n\n***************************************************************************************");
            System.out.println("*** There were exceptions during the test run. Please check the exceptions.log file ***");
            System.out.println("***************************************************************************************\n\n");
        }
        
        StatsService.getStatsReady();

        System.out.println("\nFinished with clients: " + params.clients + ", loops: " + params.loops + ", rate (ops/sec): "
                + Math.floor((double) (totalCommandsExecuted.get() / ((endTime - startTime) / (double) 1000)))
                + ", random seed: " + params.randomSeed + ", commands executed: " + totalCommandsExecuted.get()
                + ", total time: " + DurationFormatUtils.formatDurationWords(endTime - startTime, true, true));

        System.out.println("\n" + queriesNotInHistogram.get() + " queries exceeded the " + MAX_USEC_VALUE_IN_HISTOGRAM + " (usec) limit.");

        if (totalCommandsExecuted.get() > 0)
        {
            ConcurrentHashMap<Long,Number> staticStats = StatsService.getStatsHolder().get(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING);
            long totalReads = ((AtomicLong)staticStats.get(StatsService.TOTAL_READS_STATS_INDEX)).get();
            if (totalReads > 0)
            {
                System.out.println("\nRead doc stats: Min docs read: " + ((AtomicLong)staticStats.get(StatsService.MIN_READ_DOCS_STATS_INDEX)).get() 
                        + " Med docs read: " + ((AtomicLong)staticStats.get(StatsService.MED_READ_DOCS_STATS_INDEX)).get() 
                        + " Max docs read: " + ((AtomicLong)staticStats.get(StatsService.MAX_READ_DOCS_STATS_INDEX)).get()
                        + " Total reads: " + ((AtomicLong)staticStats.get(StatsService.TOTAL_READS_STATS_INDEX)).get() 
                        + " Total docs read:" + ((AtomicLong)staticStats.get(StatsService.TOTAL_READ_DOCS_STATS_INDEX)).get());
            }
            
            System.out.println("\nPercentiles (usec):");
            for (int i = 0; i < 10; i++)
            {
                System.out.println("\t" + (i + 1) * 10 + "%: \t" + queriesHistogram.getHistogramData().getValueAtPercentile((i + 1) * 10));
            }
            
            System.out.println("\nCommitting");
            for (String url : params.urls)
            {
                try
                {
                    (new HttpSolrServer(url)).commit(true, true);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
    
            if (params.gatherStats)
            {
                // Output statistics to csv file
                File statsFile = new File("stats/stressTestStats" + System.currentTimeMillis() + ".stats");
                System.out.println("Writing statistics file " + statsFile.getAbsolutePath());
                statsFile.getParentFile().mkdirs();
                PrintWriter writer = new PrintWriter(statsFile, "UTF-8");
                StatsService.writeStatsToFile(writer);
                writer.close();
            }
        }

        statsRecorder.printRecordedStatistics(System.out);

        System.out.println("Done. Check the file exceptions.log for any warnings.");

        System.exit(0);
    }
    
    private static class StatsService
    {
        // stats format is nano_timestamp, value
        private final static Map<String, ConcurrentHashMap<Long, Number>> stats = new ConcurrentHashMap<>();
        //
        public final static String QUERY_LATENCIES_STATS_STRING = "Query latencies";
        public final static String MAX_MIN_MED_DOCS_READ_STATS_STRING = "Min Med Max Total_Reads Total_Docs";
        public final static long MAX_READ_DOCS_STATS_INDEX = 2 * 1_000_000;
        public final static long MIN_READ_DOCS_STATS_INDEX= 0 * 1_000_000;
        public final static long MED_READ_DOCS_STATS_INDEX = 1 * 1_000_000;
        public final static long TOTAL_READS_STATS_INDEX = 3 * 1_000_000;
        public final static long TOTAL_READ_DOCS_STATS_INDEX = 4 *1_000_000;
        
        static
        {
            // Initialize stats
            initStaticStatistics();
            getStatsHolder().put(QUERY_LATENCIES_STATS_STRING, new ConcurrentHashMap<Long, Number>());
        }
                
        public static Map<String, ConcurrentHashMap<Long, Number>> getStatsHolder()
        {
            return stats;
        }
        
        private static void initStaticStatistics()
        {
            getStatsHolder().put(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING, new ConcurrentHashMap<Long, Number>());
            getStatsHolder().get(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING).put(MIN_READ_DOCS_STATS_INDEX, new AtomicLong(Long.MAX_VALUE));
            getStatsHolder().get(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING).put(MED_READ_DOCS_STATS_INDEX, new AtomicLong(0));
            getStatsHolder().get(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING).put(MAX_READ_DOCS_STATS_INDEX, new AtomicLong(0));
            getStatsHolder().get(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING).put(TOTAL_READS_STATS_INDEX, new AtomicLong(0));
            getStatsHolder().get(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING).put(TOTAL_READ_DOCS_STATS_INDEX, new AtomicLong(0));
        }
        
        public static void getStatsReady()
        {
            ConcurrentHashMap<Long,Number> staticStats = StatsService.getStatsHolder().get(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING);
            long totalReads = ((AtomicLong)staticStats.get(StatsService.TOTAL_READS_STATS_INDEX)).get();
            if (totalReads > 0)
            {
            // Calculate average
            ((AtomicLong)staticStats.get(StatsService.MED_READ_DOCS_STATS_INDEX)).set(
                    ((AtomicLong)staticStats.get(StatsService.TOTAL_READ_DOCS_STATS_INDEX)).get() / totalReads);
            }
        }
        
        public static void writeStatsToFile(PrintWriter writer)
        {
            for(String currentStat : stats.keySet())
            {
                writer.print(currentStat);
                long minTimeStamp = Collections.min(stats.get(currentStat).keySet());
                for (Entry<Long, Number> current : stats.get(currentStat).entrySet())
                {
                    // Timestamp + value
                    writer.print("," + (current.getKey() - minTimeStamp) / 1_000_000 + "," + current.getValue());
                }
                writer.println();
            }
        }
    }

    private static class StressRunner implements Runnable
    {
        private final int myThreadId = SolrStress.threadId.getAndIncrement();
        private final CommandLine.Params params;
        private final InputReaderStrategy inputReader;
        private final StatsRecorder statsRecorder;

        public StressRunner(CommandLine.Params params, InputReaderStrategy inputReader, StatsRecorder statsRecorder)
        {
            this.params = params;
            this.inputReader = inputReader;
            this.statsRecorder = statsRecorder;
        }

        @Override
        public void run()
        {
            HashMap<String, CmdRunner> cmdRunners = new HashMap<>();

            // First line of testData specifies random or sequential behavior
            long readsNum = 0;
            long writesNum = 0;
            long exceptionsNum = 0;
            RateLimiter rateLimiter = params.qps == -1 ? null : RateLimiter.create(((double)params.qps)/((double)params.clients));
            String fullUrl = params.urls.toArray()[myThreadId % params.urls.size()] + "/solr";
            if (!params.indexName.equals(""))
            {
                fullUrl += "/" + params.indexName;
            }

            // In random Mode loops = number of commands to run.
            // In seq Mode loops = number of times to loop the whole testData

            List<String> cmd2Run;
            while (!(cmd2Run = inputReader.getNextCommand()).isEmpty())
            {
                try
                {
                    String cmdType = CmdRunnerFactory.getCmdType(cmd2Run);
                    CmdRunner cmdRunner = null;
                    if ((cmdRunner = cmdRunners.get(cmdType)) == null)
                    {
                        cmdRunner = CmdRunnerFactory.getInstance(cmd2Run, fullUrl, params);
                        cmdRunners.put(cmdType, cmdRunner);
                    }

                    String repeatsStr = cmd2Run.get(0).replaceAll("[^\\d]", "");
                    long repeats = repeatsStr.equals("") ? 1 : Integer.parseInt(repeatsStr);
                    while (repeats > 0)
                    {
                        if (rateLimiter != null)
                        {
                            rateLimiter.acquire();
                        }
                        long queryStartTime = System.nanoTime();
                        CmdRunner.Type opType = cmdRunner.runCommand(cmd2Run);
                        long queryElapsedTime = (System.nanoTime() - queryStartTime) / 1000;
                        try
                        {
                            queriesHistogram.recordValue(queryElapsedTime);
                        }
                        catch (ArrayIndexOutOfBoundsException e)
                        {
                            queriesNotInHistogram.incrementAndGet();
                        }
                        StatsService.getStatsHolder().get(StatsService.QUERY_LATENCIES_STATS_STRING).put(queryStartTime, queryElapsedTime);
                        if (opType == CmdRunner.Type.READ)
                        {
                            readsNum++;
                        }
                        else if (opType == CmdRunner.Type.WRITE)
                        {
                            writesNum++;
                        }
                        else if (opType == CmdRunner.Type.BOTH)
                        {
                            readsNum++;
                            writesNum++;
                        }
                        
                        updateStaticStats(cmdRunner);
                        repeats--;
                        totalCommandsExecuted.incrementAndGet();
                                                
                    }
                }
                catch (Throwable e)
                {
                    testRunHadExceptions = true;
                    exceptionsNum++;
                    System.err.println("Exception processing this command: " + Arrays.toString(cmd2Run.toArray()));
                    e.printStackTrace();
                }
            }

            // Closing, clean-up...
            for (CmdRunner cmdRunner : cmdRunners.values())
            {
                try
                {
                    cmdRunner.reportStatistics(statsRecorder);
                    cmdRunner.close();
                }
                catch (Throwable e)
                {
                    testRunHadExceptions = true;
                    exceptionsNum++;
                    System.err.println("Exception committing/closing test command runners");
                    e.printStackTrace();
                }
            }

            System.out.println("Terminated thread: " + myThreadId + ", writes: " + writesNum + ", reads: " + readsNum + ", exceptions: " + exceptionsNum);
        }
        
        private void updateStaticStats(CmdRunner cmdRunner)
        {
            // Update static stats
            ConcurrentHashMap<Long, Number> staticStats = StatsService.getStatsHolder().get(StatsService.MAX_MIN_MED_DOCS_READ_STATS_STRING);
            // Update read docs, reads num and static stats
            if (cmdRunner.numDocsRead > 0)
            {
                ((AtomicLong)staticStats.get(StatsService.TOTAL_READS_STATS_INDEX)).addAndGet(1);
                ((AtomicLong)staticStats.get(StatsService.TOTAL_READ_DOCS_STATS_INDEX)).addAndGet(cmdRunner.numDocsRead);
            }
            
            // Update Min and Max
            AtomicLong currentStat = (AtomicLong) staticStats.get(StatsService.MIN_READ_DOCS_STATS_INDEX);
            long currentStatLongVal = currentStat.get();
            while (cmdRunner.numDocsRead < (currentStatLongVal = currentStat.get()))
            {
                currentStat.compareAndSet(currentStatLongVal, cmdRunner.numDocsRead);
            }
            
            currentStat = (AtomicLong) staticStats.get(StatsService.MAX_READ_DOCS_STATS_INDEX);
            while (cmdRunner.numDocsRead > (currentStatLongVal = currentStat.get()))
            {
                currentStat.compareAndSet(currentStatLongVal, cmdRunner.numDocsRead);
            }
        }
    }
}
