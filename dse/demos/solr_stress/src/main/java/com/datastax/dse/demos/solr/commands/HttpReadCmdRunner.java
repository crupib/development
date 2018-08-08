/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.commands;

import java.util.Arrays;
import java.util.List;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocumentList;
import com.datastax.dse.demos.solr.CommandLine;

public class HttpReadCmdRunner extends CmdRunner
{
    public final static String CMD_STRING_ID = "HTTPREAD";
    private final CommandLine.Params params;
    private final SolrServer readSolrClient;
    
    public HttpReadCmdRunner(String fullUrl, CommandLine.Params params)
    {
        this.params = params;
        this.readSolrClient = new HttpSolrServer(fullUrl);
    }
    
    @Override
    public Type runCommand(List<String> cmd2Run) throws Throwable
    {
        SolrQuery query = new SolrQuery();

        // Take escaping into account
        for (String nameValuePair : cmd2Run.get(1).split("(?<!&)&(?!&)"))
        {
            nameValuePair = nameValuePair.replace("&&", "&");
            String[] nameValuePairArray = nameValuePair.split("(?<!=)=(?!=)");
            query.set(nameValuePairArray[0], nameValuePairArray[1].replace("==", "="));
        }
        
        SolrDocumentList results = readSolrClient.query(query).getResults();
        numDocsRead = results.getNumFound();

        // Do we have a number of expected results to be parsed and verified?
        Long expectedNumResults = cmd2Run.size() == 3 ? Long.parseLong(cmd2Run.get(2)) : null;
        if (expectedNumResults != null && results != null && results.getNumFound() != expectedNumResults)
        {
            throw new Exception("Number of results not matched for command: " + Arrays.asList(cmd2Run) + " expected "
                    + expectedNumResults + " but found: " + results.getNumFound() + " instead.");
        }

        return Type.READ;
    }

    @Override
    public void close() throws Throwable
    {
        readSolrClient.shutdown();
    }
}
