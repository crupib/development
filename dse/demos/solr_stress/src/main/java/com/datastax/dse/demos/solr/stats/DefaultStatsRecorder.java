/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.stats;

import com.datastax.bdp.shade.com.google.common.collect.Maps;
import org.HdrHistogram.AtomicHistogram;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DefaultStatsRecorder implements StatsRecorder
{
    private final Map<String, AtomicHistogram> histograms = Maps.newHashMap();
    private final List<Integer> reportedPercentiles = Arrays.asList(50, 60, 70, 80, 90, 95, 96, 97, 98, 99);

    @Override
    public void saveHistogram(String statName, AtomicHistogram histogram)
    {
        if (histograms.containsKey(statName))
        {
            histograms.get(statName).add(histogram);
        }
        else
        {
            histograms.put(statName, histogram);
        }
    }

    public void printRecordedStatistics(PrintStream out)
    {
        for (Map.Entry<String, AtomicHistogram> histogramEntry : histograms.entrySet())
        {
            out.println(String.format("%s (%d probes)", histogramEntry.getKey(), histogramEntry.getValue().getTotalCount()));
            for (Integer percentile : reportedPercentiles)
            {
                out.println(
                        MessageFormatter.arrayFormat("\t{} %\t=>\t{}",
                                new Object[]{
                                        percentile,
                                        histogramEntry.getValue().getValueAtPercentile(percentile)}).getMessage()
                );

            }
        }
    }
}
