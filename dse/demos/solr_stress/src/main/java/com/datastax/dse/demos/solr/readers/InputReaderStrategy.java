/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.readers;

import com.datastax.dse.demos.solr.commands.Constants;
import com.datastax.dse.demos.solr.commands.LucenePerfCmdRunner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.datastax.dse.demos.solr.commands.Constants.TEST_DATA_VALUES_DELIMITER;

public abstract class InputReaderStrategy implements Closeable
{
    protected final BufferedReader reader;
    private final Constants.TestDataHeader header;

    protected InputReaderStrategy(Constants.TestDataHeader header, BufferedReader reader)
    {
        this.reader = reader;
        this.header = header;
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.closeQuietly(reader);
    }

    public List<String> getNextCommand()
    {
        while (true)
        {
            Optional<String> line = getNextLine();
            if (line.isPresent())
            {
                List<String> analyzedLine = analyzeSingleLine(line.get());
                if (analyzedLine.isEmpty())
                {
                    // nothing here, try another line
                    continue;
                }
                return analyzedLine;
            }
            return Collections.emptyList();
        }
    }

    public Constants.TestDataHeader getHeader()
    {
        return header;
    }

    protected abstract Optional<String> getNextLine();

    protected List<String> readAllRemainingLines()
    {
        try
        {
            return ImmutableList.copyOf(IOUtils.readLines(reader));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private List<String> analyzeSingleLine(String line)
    {
        if (line.startsWith("#"))
        {
            return Collections.emptyList();
        }
        else if (!line.startsWith(LucenePerfCmdRunner.CMD_STRING_ID))
        {
            line = line.replaceAll("#.*", "");
        }

        if (!line.isEmpty())
        {
            // Take escaping (character doubling) into account
            List<String> cmdSplitArray = Arrays.asList(
                    line.split(
                            "(?<![" + TEST_DATA_VALUES_DELIMITER + "])[" + TEST_DATA_VALUES_DELIMITER + "](?![" + TEST_DATA_VALUES_DELIMITER + "])"));
            for (int i = 0; i < cmdSplitArray.size(); i++)
            {
                cmdSplitArray.set(i, cmdSplitArray.get(i).replaceAll("[|][|]", "|"));
            }
            return ImmutableList.copyOf(cmdSplitArray);
        }

        return Collections.emptyList();
    }
}
