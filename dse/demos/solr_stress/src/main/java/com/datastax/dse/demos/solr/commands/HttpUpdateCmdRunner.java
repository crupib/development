/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.commands;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import com.datastax.dse.demos.solr.CommandLine;
import com.google.common.base.Splitter;

public class HttpUpdateCmdRunner extends CmdRunner
{
    public final static String CMD_STRING_ID = "HTTPUPDATE";
    public final static String UPDATE_ALL_STRING_FIELDS_ID = "ALLSTR";
    private final CommandLine.Params params;
    private final SolrServer updateSolrClient;
    
    HttpUpdateCmdRunner(String fullUrl, CommandLine.Params params)
    {
        this.params = params;
        this.updateSolrClient = new HttpSolrServer(fullUrl);
    }
    
    @Override
    public Type runCommand(List<String> cmd2Run) throws Throwable
    {
        // cmd2Run[0] = update command [1]= csv list of fields to update or 'ALLSTR' [2]=query producing resultset to be updated
        SolrQuery query = new SolrQuery();

        // Take escaping into account
        for (String nameValuePair : cmd2Run.get(2).split("(?<!&)&(?!&)"))
        {
            nameValuePair = nameValuePair.replace("&&", "&");
            String[] nameValuePairArray = nameValuePair.split("(?<!=)=(?!=)");
            query.set(nameValuePairArray[0], nameValuePairArray[1].replace("==", "="));
        }

        // Get only num found and only one doc when querying
        query.setRows(1);
        long numFound = updateSolrClient.query(query).getResults().getNumFound();

        if (numFound == 0)
        {
            System.err.println(this.getClass().getName() + " could not find anything to update for query: " + query);
            return Type.READ;
        }

        query.setStart(ThreadLocalRandom.current().nextInt((int) numFound));
        SolrDocumentList results = updateSolrClient.query(query).getResults();

        List<String> updatableFields = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(cmd2Run.get(1));         
        int updates = 0;
        for (SolrDocument doc : results)
        {
            for (String fieldName : doc.getFieldNames())
            {
                if (updatableFields.contains(fieldName) || updatableFields.contains(UPDATE_ALL_STRING_FIELDS_ID))
                {
                    Object docValue = doc.getFieldValue(fieldName);
                    if (docValue instanceof String)
                    {
                        doc.setField(fieldName, "updated" + ++updates + " " + docValue);
                    }
                }
            }

            updateSolrClient.add(ClientUtils.toSolrInputDocument(doc));
        }

        if (updates == 0)
        {
            System.err.println(this.getClass().getName() + " No string fields to update found for query: " + query + " still docs have been read and written to with their original values.");
        }
        numDocsRead++;
        return Type.BOTH;
    }

    @Override
    public void close() throws Throwable
    {
        updateSolrClient.shutdown();
    }
}
