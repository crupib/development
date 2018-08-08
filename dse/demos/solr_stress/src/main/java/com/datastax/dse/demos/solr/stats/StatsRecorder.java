/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.stats;

import org.HdrHistogram.AtomicHistogram;

public interface StatsRecorder
{
    void saveHistogram(String statName, AtomicHistogram histogram);
}
