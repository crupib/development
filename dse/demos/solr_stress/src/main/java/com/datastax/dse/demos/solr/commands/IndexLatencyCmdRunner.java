/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.commands;

import com.datastax.dse.demos.solr.CommandLine;
import com.datastax.dse.demos.solr.probes.IndexingLatencyProbe;
import com.datastax.dse.demos.solr.stats.StatsRecorder;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import org.HdrHistogram.AtomicHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.datastax.dse.demos.solr.commands.parsers.CommandArgumentParsers.readArgumentsAsMap;

public class IndexLatencyCmdRunner extends CmdRunner
{
    public static final String CMD_STRING_ID = "INDEX_LATENCY";

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexLatencyCmdRunner.class);

    private final Random random;
    private final IndexingLatencyProbe probe;
    private final AtomicHistogram recordedLatencyInMillis = new AtomicHistogram(1_000_000, 3);

    public IndexLatencyCmdRunner(CommandLine.Params params)
    {
        this.random = params.random;
        this.probe = new IndexingLatencyProbe(Iterables.getFirst(params.urls, "http://localhost:8983") + "/solr/" + params.indexName);
    }

    @Override
    public Type runCommand(List<String> commandArguments) throws Throwable
    {
        Stopwatch stopwatch = Stopwatch.createStarted();
        recordedLatencyInMillis.recordValue(probe.getMillisUntilDocumentBecameSearchable(readDocumentFields(commandArguments)));
        LOGGER.debug("Index latency probe recorded in {} ms", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        return Type.BOTH;
    }

    @Override
    public void close() throws Throwable
    {
        probe.close();
    }

    @Override
    public void reportStatistics(StatsRecorder recorder)
    {
        recorder.saveHistogram("Document visibility latency [ms]", recordedLatencyInMillis);
    }

    private Map<String, String> readDocumentFields(List<String> commandArguments)
    {
        return readArgumentsAsMap(commandArguments.subList(1, commandArguments.size()), random);
    }
}
