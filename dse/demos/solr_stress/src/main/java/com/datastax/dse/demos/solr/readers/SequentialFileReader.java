/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.readers;

import com.datastax.dse.demos.solr.commands.Constants;
import com.google.common.base.Optional;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class SequentialFileReader extends InputReaderStrategy
{
    private final ReentrantLock lock = new ReentrantLock();

    protected SequentialFileReader(Constants.TestDataHeader header, BufferedReader input)
    {
        super(header, input);
    }

    @Override
    public Optional<String> getNextLine()
    {
        try
        {
            lock.lock();
            return Optional.fromNullable(reader.readLine());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            lock.unlock();
        }
    }
}
