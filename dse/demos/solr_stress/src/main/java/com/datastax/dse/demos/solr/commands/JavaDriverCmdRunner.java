/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.commands;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.dse.demos.solr.Utils;
import com.datastax.dse.demos.solr.CommandLine;

public class JavaDriverCmdRunner extends CmdRunner
{
    public final static String CMD_STRING_ID = "CQL";
    private final Cluster javaDriverCluster;
    private final Session javaDriverSession;
    private final CommandLine.Params params;

    JavaDriverCmdRunner(String fullUrl, CommandLine.Params params) throws URISyntaxException
    {
        this.params = params;
        this.javaDriverCluster = Cluster.builder().addContactPoint((new URI(fullUrl)).getHost()).build();
        this.javaDriverSession = javaDriverCluster.connect();
    }

    @Override
    public Type runCommand(List<String> cmd2Run) throws Throwable
    {
        String cqlCmd = Utils.runCmdNotationSubstitutions(cmd2Run.get(1), params.random);
        ResultSet result = javaDriverSession.execute(new SimpleStatement(cqlCmd).setFetchSize(params.fetchSize));

        // Return number of reads and writes
        if (cqlCmd.toUpperCase().startsWith("SELECT"))
        {
            int reads = 0;
            for (Row current : result)
            {
                numDocsRead++;
                if (++reads == params.fetchSize)
                {
                    break;
                }
            }
            return Type.READ;
        }
        else
        {
            return Type.WRITE;
        }
    }

    @Override
    public void close() throws Throwable
    {
        javaDriverSession.close();
        javaDriverCluster.close();
    }
}
