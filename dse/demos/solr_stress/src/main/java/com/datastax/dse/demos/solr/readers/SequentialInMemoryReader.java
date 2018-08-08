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
import java.util.concurrent.atomic.AtomicInteger;

public class SequentialInMemoryReader extends InputReaderStrategy
{
    private final List<String> lines;
    private final ThreadLocal<AtomicInteger> indexCounter = new ThreadLocal<AtomicInteger>()
    {
        @Override
        protected AtomicInteger initialValue()
        {
            return new AtomicInteger(0);
        }
    };
    private final ThreadLocal<AtomicInteger> repeatsCounter;

    protected SequentialInMemoryReader(Constants.TestDataHeader header, BufferedReader input, final int loopCount)
    {
        super(header, input);
        this.lines = readAllRemainingLines();
        this.repeatsCounter = new ThreadLocal<AtomicInteger>()
        {
            @Override
            protected AtomicInteger initialValue()
            {
                return new AtomicInteger(loopCount);
            }
        };
    }

    @Override
    public Optional<String> getNextLine()
    {
        int index = indexCounter.get().getAndIncrement();
        if (index < lines.size())
        {
            return Optional.of(lines.get(index));
        }
        else if (repeatsCounter.get().decrementAndGet() > 0)
        {
            indexCounter.get().set(0);
            return getNextLine();
        }
        return Optional.absent();
    }
}
