/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.commands;

import com.datastax.dse.demos.solr.generators.IndexLatencyTestFileGenerator;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.datastax.dse.demos.solr.commands.parsers.CommandArgumentParsers.checkArgument;
import static com.datastax.dse.demos.solr.commands.parsers.CommandArgumentParsers.readArgumentAsLong;
import static com.datastax.dse.demos.solr.commands.parsers.CommandArgumentParsers.readArgumentValue;
import static com.google.common.collect.ImmutableList.builder;

public class GenerateIndexLatencyTestFileCmdRunner extends CmdRunner
{
    public static final String CMD_STRING_ID = "GENERATE_INDEX_LATENCY_TEST";

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateIndexLatencyTestFileCmdRunner.class);
    private static final String DATA_FILE_LOCATION_ARG_NAME = "geonames-file";
    private static final String OUTPUT_FILE_ARG_NAME = "output";
    private static final String COMMANDS_COUNT_ARG_NAME = "commands-count";
    private static final long DEFAULT_COMMANDS_COUNT = 10_000L;

    @Override
    public Type runCommand(List<String> commandArguments) throws Throwable
    {
        String outputFileLocation = getOutputFileName(commandArguments);
        List<String> commands = new IndexLatencyTestFileGenerator().generateStressCommands(getTestDataFile(commandArguments), getCommandCount(commandArguments));
        Stopwatch stopwatch = Stopwatch.createStarted();
        IOUtils.writeLines(
                builder().add(Constants.TestDataHeader.SHARED_SEQUENTIAL.getName()).addAll(commands).build(),
                "\n",
                new FileOutputStream(outputFileLocation));
        LOGGER.info(
                "Saved commands to a file '{}' in {} ms",
                outputFileLocation,
                stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        return Type.WRITE;
    }

    @Override
    public void close() throws Throwable
    {
        // no-op
    }

    private String getOutputFileName(Iterable<String> commandArguments)
    {
        Optional<String> output = readArgumentValue(commandArguments, OUTPUT_FILE_ARG_NAME);
        checkArgument(output, OUTPUT_FILE_ARG_NAME);
        return output.get();
    }

    private File getTestDataFile(Iterable<String> commandArguments)
    {
        Optional<String> dataFileLocation = readArgumentValue(commandArguments, DATA_FILE_LOCATION_ARG_NAME);
        checkArgument(dataFileLocation, DATA_FILE_LOCATION_ARG_NAME);
        return new File(dataFileLocation.get());
    }

    private long getCommandCount(Iterable<String> commandArguments)
    {
        return readArgumentAsLong(commandArguments, COMMANDS_COUNT_ARG_NAME, DEFAULT_COMMANDS_COUNT);
    }
}
