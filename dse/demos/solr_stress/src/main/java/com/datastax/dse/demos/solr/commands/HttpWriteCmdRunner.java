/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.commands;

import java.net.MalformedURLException;
import java.util.List;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import com.datastax.dse.demos.solr.CommandLine;
import com.datastax.dse.demos.solr.Utils;

public class HttpWriteCmdRunner extends CmdRunner
{
    public static final String CMD_STRING_ID = "HTTPWRITE";
    private final String fullUrl;
    private final CommandLine.Params params;
    private final SolrServer writeSolrClient;

    HttpWriteCmdRunner(String fullUrl, CommandLine.Params params) throws MalformedURLException
    {
        this.params = params;
        this.fullUrl = fullUrl;
        this.writeSolrClient = new HttpSolrServer(fullUrl);
    }

    @Override
    public Type runCommand(List<String> cmd2Run) throws Throwable
    {
        SolrInputDocument doc = new SolrInputDocument();
        for (String fieldData : cmd2Run.subList(1, cmd2Run.size()))
        {
            // Take character doubling escaping into account
            String[] explodedField = fieldData.replace("&&", "&").split("(?<!=)=(?!=)");
            if (explodedField.length > 1)
            {
                String fieldName = explodedField[0];
                String fieldValue = Utils.runCmdNotationSubstitutions(explodedField[1].replace("==", "="), params.random);
                doc.addField(fieldName, fieldValue);
            }
        }
        writeSolrClient.add(doc);

        return Type.WRITE;
    }

    @Override
    public void close() throws Throwable
    {
        writeSolrClient.shutdown();
    }
}
