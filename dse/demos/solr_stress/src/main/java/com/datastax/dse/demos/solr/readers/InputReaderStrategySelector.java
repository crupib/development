/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.readers;

import com.datastax.bdp.shade.com.google.common.collect.ImmutableMap;
import com.datastax.dse.demos.solr.commands.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import static com.datastax.dse.demos.solr.commands.Constants.TestDataHeader.EXCLUSIVE_RANDOM;
import static com.datastax.dse.demos.solr.commands.Constants.TestDataHeader.EXCLUSIVE_SEQUENTIAL;
import static com.datastax.dse.demos.solr.commands.Constants.TestDataHeader.SHARED_SEQUENTIAL;

public class InputReaderStrategySelector
{
    private final Map<Constants.TestDataHeader, InputReaderStrategySupplier> strategies;

    public InputReaderStrategySelector(Random random, int loopCount)
    {
        this.strategies = getAvailableStrategies(random, loopCount);
    }

    public InputReaderStrategy createInputReader(File input)
    {
        try
        {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(input));
            Constants.TestDataHeader header = peekDataHeader(bufferedReader);
            return strategies.get(header).get(bufferedReader, header);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Map<Constants.TestDataHeader, InputReaderStrategySupplier> getAvailableStrategies(final Random random, final int loopCount)
    {
        return ImmutableMap.of(
                EXCLUSIVE_SEQUENTIAL, new InputReaderStrategySupplier()
                {
                    @Override
                    public InputReaderStrategy get(BufferedReader input, Constants.TestDataHeader header)
                    {
                        return new SequentialInMemoryReader(header, input, loopCount);
                    }
                },
                EXCLUSIVE_RANDOM, new InputReaderStrategySupplier()
                {
                    @Override
                    protected InputReaderStrategy get(BufferedReader input, Constants.TestDataHeader header)
                    {
                        return new RandomInMemoryReader(header, input, random, loopCount);
                    }
                },
                SHARED_SEQUENTIAL, new InputReaderStrategySupplier()
                {
                    @Override
                    protected InputReaderStrategy get(BufferedReader input, Constants.TestDataHeader header)
                    {
                        return new SequentialFileReader(header, input);
                    }
                });
    }

    private Constants.TestDataHeader peekDataHeader(BufferedReader reader) throws IOException
    {
        String headerLine = reader.readLine();
        return Constants.TestDataHeader.byName(headerLine);
    }

    private abstract static class InputReaderStrategySupplier
    {
        protected abstract InputReaderStrategy get(BufferedReader input, Constants.TestDataHeader header);
    }
}
