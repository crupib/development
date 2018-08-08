/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.probes;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class IndexingLatencyProbe implements Closeable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingLatencyProbe.class);
    private static final int DEFAULT_SOLR_CLIENT_CONCURRENT_REQUESTS_COUNT = 50;
    private static final int DEFAULT_SOLR_CLIENT_QUEUE_SIZE = 1_000;
    private static final double MAX_REQUESTS_PER_SEC = 200.0;

    private static final RateLimiter RATE_LIMITER = RateLimiter.create(MAX_REQUESTS_PER_SEC);
    private final ConcurrentUpdateSolrServer solrClient;

    public IndexingLatencyProbe(String solrUrl)
    {
        this.solrClient = new ConcurrentUpdateSolrServer(
                solrUrl,
                DEFAULT_SOLR_CLIENT_QUEUE_SIZE,
                DEFAULT_SOLR_CLIENT_CONCURRENT_REQUESTS_COUNT);
    }

    @Override
    public void close() throws IOException
    {
        solrClient.shutdown();
        solrClient.blockUntilFinished();
    }

    public long getMillisUntilDocumentBecameSearchable(final Map<String, String> fields) throws IOException, SolrServerException
    {
        SolrQuery solrParams = createSolrQuery(fields);
        indexDocument(fields);
        Stopwatch stopwatch = Stopwatch.createUnstarted();

        int repeatsCount = 0;
        do
        {
            RATE_LIMITER.acquire();

            if (!stopwatch.isRunning())
            {
                stopwatch.start();
            }
            try
            {
                QueryResponse response = solrClient.query(solrParams);

                if (isResponseNotEmpty(response))
                {
                    stopwatch.stop();
                    LOGGER.debug("Found {} result(s) satisfying {}'; tried {} time(s); took {} ms",
                            response.getResults().size(), solrParams, repeatsCount + 1, stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    return stopwatch.elapsed(TimeUnit.MILLISECONDS);
                }
                LOGGER.debug("Found 0 result(s) satisfying {}', will repeat; tried {} time(s);",
                        solrParams, ++repeatsCount);
                Thread.yield();
            }
            catch (SolrServerException e)
            {
                LOGGER.trace("Could not complete query", e);
                return 0L;
            }
        } while (true);
    }

    private void indexDocument(Map<String, String> fields) throws SolrServerException, IOException
    {
        solrClient.add(createSolrInputDocument(fields));
    }

    private SolrQuery createSolrQuery(Map<String, String> fields)
    {
        return new SolrQuery("id:" + fields.get("id"));
    }

    private SolrInputDocument createSolrInputDocument(Map<String, String> fields)
    {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        for (Map.Entry<String, String> field : fields.entrySet())
        {
            solrInputDocument.addField(field.getKey(), field.getValue());
        }
        return solrInputDocument;
    }

    private boolean isResponseNotEmpty(QueryResponse response)
    {
        return !response.getResults().isEmpty();
    }
}
