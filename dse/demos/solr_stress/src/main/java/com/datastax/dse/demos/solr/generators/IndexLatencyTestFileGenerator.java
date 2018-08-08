/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.generators;

import com.clearspring.analytics.util.Lists;
import com.datastax.bdp.shade.com.google.common.base.Throwables;
import com.datastax.dse.demos.solr.Geonames;
import com.datastax.dse.demos.solr.commands.Constants;
import com.datastax.dse.demos.solr.commands.HttpWriteCmdRunner;
import com.datastax.dse.demos.solr.commands.IndexLatencyCmdRunner;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import org.slf4j.helpers.MessageFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class IndexLatencyTestFileGenerator
{
    private static final Geonames.GeonamesLineParser GEONAMES_LINE_PARSER = new Geonames.GeonamesLineParser();
    private static final Joiner COMMAND_PARTS_JOINER = Joiner.on(Constants.TEST_DATA_VALUES_DELIMITER).skipNulls();

    private final SecureRandom secureRandom = new SecureRandom();

    public List<String> generateStressCommands(File rawDataFile, long queryCount)
    {
        Stopwatch stopwatch = Stopwatch.createStarted();

        ImmutableList.Builder<String> commands = ImmutableList.builder();
        try (BufferedReader reader = new BufferedReader(new FileReader(rawDataFile)))
        {
            long documentCount = 0L;
            while (documentCount < queryCount)
            {
                Map<String, String> fields = GEONAMES_LINE_PARSER.parseLine(reader.readLine());
                if (performOnlyWrite())
                {
                    commands.add(createWriteCommand(fields));
                }
                else
                {
                    commands.add(createProbeCommand(fields));
                }
                documentCount++;
                printStatus(documentCount, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }

            printStatus(documentCount, stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
            return commands.build();
        }
        catch (IOException e)
        {
            Throwables.propagate(e);
            return Collections.emptyList();
        }
    }

    private String createWriteCommand(Map<String, String> fields)
    {
        return getFieldsAsWriteCommand(HttpWriteCmdRunner.CMD_STRING_ID, fields);
    }

    private String createProbeCommand(Map<String, String> fields)
    {
        return getFieldsAsWriteCommand(IndexLatencyCmdRunner.CMD_STRING_ID, fields);
    }

    private String getFieldsAsWriteCommand(String commandType, Map<String, String> fields)
    {
        List<String> result = Lists.newArrayList();
        result.add(commandType);
        for (Map.Entry<String, String> field : fields.entrySet())
        {
            result.add(field.getKey() + "=" + field.getValue());
        }
        return COMMAND_PARTS_JOINER.join(result);
    }


    private boolean performOnlyWrite()
    {
        // 85 % of the time
        return secureRandom.nextInt(100) < 85;
    }

    private void printStatus(long documentCount, long timeElapsedInSeconds)
    {
        if (documentCount % 500 == 0)
        {
            System.out.println(
                    MessageFormatter.arrayFormat("Generated {} commands in {} ms",
                            new Object[]{documentCount, timeElapsedInSeconds}).getMessage());

        }
    }
}