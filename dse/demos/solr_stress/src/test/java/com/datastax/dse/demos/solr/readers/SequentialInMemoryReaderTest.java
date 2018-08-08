/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.readers;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.datastax.dse.demos.solr.commands.Constants.TestDataHeader.EXCLUSIVE_SEQUENTIAL;
import static com.datastax.dse.demos.solr.readers.InputReaderStrategyTestUtils.asBufferedReader;
import static com.datastax.dse.demos.solr.readers.InputReaderStrategyTestUtils.createTestInput;
import static com.datastax.dse.demos.solr.readers.InputReaderStrategyTestUtils.hasItems;
import static com.datastax.dse.demos.solr.readers.InputReaderStrategyTestUtils.readAllLines;
import static com.datastax.dse.demos.solr.readers.InputReaderStrategyTestUtils.readAllLinesConcurrently;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SequentialInMemoryReaderTest
{
    @Test
    public void shouldReadLinesUntilSatisfyingLoopCount()
    {
        // given:
        List<String> lines = createTestInput(1000);
        SequentialInMemoryReader reader = new SequentialInMemoryReader(
                EXCLUSIVE_SEQUENTIAL,
                asBufferedReader(lines),
                10);

        // when:
        List<String> readLines = readAllLines(reader);

        // then:
        assertEquals(10_000, readLines.size());
        assertEquals(1000, Sets.newHashSet(readLines).size());
        assertThat(lines, hasItems(readLines));
    }

    @Test
    public void shouldReadLinesUntilSatisfyingLoopCountUsingMultipleThreads() throws ExecutionException, InterruptedException
    {
        // given:
        List<String> lines = createTestInput(1000);
        SequentialInMemoryReader reader = new SequentialInMemoryReader(
                EXCLUSIVE_SEQUENTIAL,
                asBufferedReader(lines),
                10);

        // when:
        List<String> readLines = readAllLinesConcurrently(8, reader);

        // then:
        assertEquals(80_000, readLines.size());
        assertEquals(1000, Sets.newHashSet(readLines).size());
        assertThat(lines, hasItems(readLines));
    }

    @Test
    public void shouldNotLoop()
    {
        // given:
        List<String> lines = createTestInput(1000);
        SequentialInMemoryReader reader = new SequentialInMemoryReader(
                EXCLUSIVE_SEQUENTIAL,
                asBufferedReader(lines),
                0);

        // when:
        List<String> readLines = readAllLines(reader);

        // then:
        assertEquals(1000, Sets.newHashSet(readLines).size());
        assertThat(lines, hasItems(readLines));
    }

    @Test
    public void shouldReadZeroUsingMultipleThreads() throws ExecutionException, InterruptedException
    {
        // given:
        List<String> lines = createTestInput(1000);
        SequentialInMemoryReader reader = new SequentialInMemoryReader(
                EXCLUSIVE_SEQUENTIAL,
                asBufferedReader(lines),
                0);

        // when:
        List<String> readLines = readAllLinesConcurrently(8, reader);

        // then:
        assertEquals(8_000, readLines.size());
        assertEquals(1000, Sets.newHashSet(readLines).size());
        assertThat(lines, hasItems(readLines));
    }
}