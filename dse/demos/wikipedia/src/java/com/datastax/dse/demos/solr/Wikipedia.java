/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.EnwikiContentSource;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.datastax.bdp.config.DseConfig;
import com.datastax.bdp.shade.org.apache.http.Header;
import com.datastax.bdp.shade.org.apache.http.HttpException;
import com.datastax.bdp.shade.org.apache.http.HttpRequest;
import com.datastax.bdp.shade.org.apache.http.HttpRequestInterceptor;
import com.datastax.bdp.shade.org.apache.http.auth.UsernamePasswordCredentials;
import com.datastax.bdp.shade.org.apache.http.impl.auth.BasicScheme;
import com.datastax.bdp.shade.org.apache.http.impl.client.AbstractHttpClient;
import com.datastax.bdp.shade.org.apache.http.protocol.HttpContext;
import com.datastax.bdp.shade.com.datastax.solr.client.solrj.auth.SolrHttpClientInitializer;
import com.datastax.bdp.shade.com.datastax.solr.client.solrj.auth.SolrHttpClientInitializer.AuthenticationOptions;
import com.datastax.bdp.shade.com.datastax.solr.client.solrj.auth.SolrHttpClientInitializer.EncryptionOptions;
import com.datastax.bdp.shade.org.apache.http.conn.ssl.SSLSocketFactory;

public class Wikipedia
{
    static String wikifile;
    static String host = "localhost";
    static String scheme = "http";
    static String urlTemplate = "%s://%s:8983/solr/wiki.solr";
    static String url;
    static String user;
    static String password;
    static EnwikiContentSource source;
    static int limit = Integer.MAX_VALUE;

    public static void indexWikipedia()
    {

        HttpSolrServer solrClient = null;
        try
        {
            Properties p = new Properties();
            p.setProperty("keep.image.only.docs", "false");
            p.setProperty("docs.file", wikifile);

            Config config = new Config(p);

            source = new EnwikiContentSource();
            source.setConfig(config);
            source.resetInputs();
            solrClient = new HttpSolrServer(url);
            
            if (null != user && null != password)
            {
                AbstractHttpClient httpClient = (AbstractHttpClient)solrClient.getHttpClient(); 
                httpClient.addRequestInterceptor(new PreEmptiveBasicAuthenticator(user, password));
            }
            
            DocData docData = new DocData();
            String firstName = null;
            SolrInputDocument doc = new SolrInputDocument();
            int i = 0;
            for (int x = 0; x < limit; x++)
            {
                if (i > 0 && i % 1000 == 0)
                    System.out.println("Indexed " + i++);
              
                docData = source.getNextDocData(docData);
                
                if (firstName == null)
                    firstName = docData.getName();
                else if (firstName.equals(docData.getName()))
                    break; //looped
                                    
                if (addDoc(doc, docData))
                {                  
                    solrClient.add(doc);
                    i++;
                }
            }
        }
        catch (NoMoreDataException e)
        {
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {

            try
            {
                if (solrClient != null)
                    solrClient.commit();
                
                source.close();
            }
            catch (Throwable t)
            {

            }
        }
        
    }

    public static boolean addDoc(SolrInputDocument doc, DocData d)
    {

        if (d.getTitle().indexOf(":") > 0)
            return false;

        doc.clear();
        doc.addField("id", d.getName());
        doc.addField("title", d.getTitle());
        doc.addField("body", d.getBody());
        doc.addField("date", d.getDate());

        return true;
    }

    public static void usage()
    {
        System.err.println("usage: wikipedia_import --wikifile [filepath] --limit X");
        System.err.println("see set-solr-options.sh for additional options running against secure DSE");
        System.exit(1);
    }

    public static void main(String[] args)
    {

        if (args.length == 0)
            usage();

        System.out.println("args: " + Arrays.asList(args));

        // parse args
        for (int i = 0; i < args.length; i = i + 2)
        {

            if (args[i].startsWith("--"))
            {
                try
                {
                    String arg = args[i].substring(2);
                    String value = args[i + 1];

                    if (arg.equalsIgnoreCase("wikifile"))
                        wikifile = value;
                    if (arg.equalsIgnoreCase("limit"))
                        limit = Integer.parseInt(value);
                    if (arg.equalsIgnoreCase("host"))
                        host = value;
                    if (arg.equalsIgnoreCase("scheme"))
                        scheme = value;
                    if (arg.equalsIgnoreCase("user"))
                        user = value;
                    if (arg.equalsIgnoreCase("password"))
                        password = value;
                }
                catch (Throwable t)
                {
                    usage();
                }
            }
        }
        url = String.format(urlTemplate, scheme, host);
        // Initialize Solr with our custom HTTP Client helpers - which handle
        // SSL & Kerberos. We must have dse.yaml on the classpath for this
        try
        {
            if (DseConfig.isSslEnabled())
            {
                SolrHttpClientInitializer.initEncryption(
                           new EncryptionOptions()
                               .withSSLContext(DseConfig.getSSLContext())
                               .withHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
            }
            
            if (DseConfig.isKerberosEnabled())
            {
                // Obtain kerberos credentials from local ticket cache
                AuthenticationOptions options = new AuthenticationOptions();
                if (DseConfig.isSslEnabled())
                {
                    options.withSSLContext(DseConfig.getSSLContext())
                        .withHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                }
                                
                SolrHttpClientInitializer.initAuthentication(options);
            }
        }
        catch (Exception e)
        {
            System.out.println("Fatal error when initializing Solr clients, exiting");
            e.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("Start indexing wikipedia...");
        long startTime = System.currentTimeMillis();

        indexWikipedia();

        long endTime = System.currentTimeMillis();

        System.out.println("Finished");
      
        System.exit(0);

    }
    
    static class PreEmptiveBasicAuthenticator implements HttpRequestInterceptor {
        private final UsernamePasswordCredentials credentials;

        private final Header authHeader;
        
        public PreEmptiveBasicAuthenticator(String user, String pass) {
          credentials = new UsernamePasswordCredentials(user, pass);
          authHeader = BasicScheme.authenticate(credentials,"US-ASCII",false);
        }

        @Override
        public void process(HttpRequest request, HttpContext context)
            throws HttpException, IOException {
            request.addHeader(authHeader);
        }
      }
}
