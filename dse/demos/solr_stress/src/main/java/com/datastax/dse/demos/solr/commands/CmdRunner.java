/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.commands;

import com.datastax.dse.demos.solr.stats.StatsRecorder;

import java.util.List;

public abstract class CmdRunner
{
    public volatile long numDocsRead = 0;

    public abstract Type runCommand(List<String> cmd2Run) throws Throwable;

    public abstract void close() throws Throwable;

    public void reportStatistics(StatsRecorder appender)
    {
        // no-op by default
    }

    public enum Type
    {
        READ, WRITE, BOTH
    }
}
