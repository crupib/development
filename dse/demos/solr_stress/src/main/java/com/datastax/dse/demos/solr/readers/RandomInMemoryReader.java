/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.readers;

import com.datastax.dse.demos.solr.commands.Constants;
import com.google.common.base.Optional;

import java.io.BufferedReader;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomInMemoryReader extends InputReaderStrategy
{
    private final List<String> lines;
    private final AtomicInteger repeatCount;
    private final Random random;

    protected RandomInMemoryReader(Constants.TestDataHeader header, BufferedReader input, Random random, int expectedLinesCount)
    {
        super(header, input);
        this.random = random;
        this.lines = readAllRemainingLines();
        this.repeatCount = new AtomicInteger(expectedLinesCount);
    }

    @Override
    public Optional<String> getNextLine()
    {
        if (repeatCount.getAndDecrement() > 0)
        {
            int index = random.nextInt(lines.size());
            return Optional.of(lines.get(index));
        }
        return Optional.absent();
    }
}
