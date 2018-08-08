/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr;

import com.datastax.bdp.config.DseConfig;
import com.datastax.bdp.shade.com.datastax.solr.client.solrj.auth.SolrHttpClientInitializer;
import com.datastax.bdp.shade.com.datastax.solr.client.solrj.auth.SolrHttpClientInitializer.AuthenticationOptions;
import com.datastax.bdp.shade.com.datastax.solr.client.solrj.auth.SolrHttpClientInitializer.EncryptionOptions;
import com.datastax.bdp.shade.org.apache.http.conn.ssl.SSLSocketFactory;

import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils
{
    private static final Pattern randomPattern = Pattern.compile("\\$RANDOM_\\d+(:\\d+)?");
    private static final Pattern zipfPattern = Pattern.compile("\\$ZIPF_\\d+(:\\d+)?");
    private static final Pattern ipsumPattern = Pattern.compile("\\$IPSUM_\\d+(:\\d+)?");

    public static String runCmdNotationSubstitutions(String cmd, final Random random)
    {
        // First replace UUIDs
        cmd = cmd.replaceAll("\\$RANDOM_UUID", UUID.randomUUID().toString());

        cmd = runVariableSubstitutions(cmd, randomPattern,
            new Randomizable() {
                @Override
                public String nextValue(int randValue)
                {
                    return Integer.toString(random.nextInt(randValue + 1));
                }
            });

        cmd = runVariableSubstitutions(cmd, zipfPattern,
            new Randomizable() {
                @Override
                public String nextValue(int randValue)
                {
                    ZipfGenerator zipfGenerator = ZipfGenerator.getInstance(randValue);
                    return Integer.toString(zipfGenerator.next());
                }
            });

        cmd = runVariableSubstitutions(cmd, ipsumPattern,
             new Randomizable(){
                 @Override
                 public String nextValue(int randValue)
                 {
                     return IpsumGenerator.generate(randValue, random);
                 }
             });

        return cmd;
    }

    public static void init()
    {
        // Initialize Solr with our custom HTTP Client helpers - which handle
        // SSL & Kerberos. We must have dse.yaml on the classpath for this
        try
        {
            if (DseConfig.isSslEnabled())
            {
                SolrHttpClientInitializer.initEncryption(new EncryptionOptions().withSSLContext(
                        DseConfig.getSSLContext()).withHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
            }

            if (DseConfig.isKerberosEnabled())
            {
                // Obtain kerberos credentials from local ticket cache
                AuthenticationOptions options = new AuthenticationOptions();
                if (DseConfig.isSslEnabled())
                {
                    options.withSSLContext(DseConfig.getSSLContext()).withHostnameVerifier(
                            SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                }

                SolrHttpClientInitializer.initAuthentication(options);
            }
        }
        catch(Exception e)
        {
            System.out.println("Fatal error when initializing Solr clients, exiting");
            e.printStackTrace();
            System.exit(1);
        }
    }


    private interface Randomizable {
        public String nextValue(int value);
    }

    private static String runVariableSubstitutions(String cmd, Pattern pattern, Randomizable randomizer)
    {
        String varName = pattern.toString().replace("\\$", "").replace("_\\d+(:\\d+)?", "");
        String varPrefix = "$" + varName + "_";

        // Now replace the varPrefix notation ('RANDOM', 'ZIPF', 'IPSUM).
        // RegExp are expensive so check first it is needed
        if (cmd.indexOf(varPrefix) != -1)
        {
            StringBuffer newCmd = new StringBuffer();
            Matcher matcher = pattern.matcher(cmd);
            while (matcher.find())
            {
                String parsedRandValue = matcher.group().toString().replaceAll("[$_]", "").replace(varName, "");

                // Parse repetition generation
                String[] values = parsedRandValue.split(":");
                int num = values.length == 1 ? 1 : Integer.parseInt(values[1]);
                String generatedRandValue = "";
                for (int i = 0; i < num; i++)
                {
                    generatedRandValue += randomizer.nextValue(Integer.parseInt(values[0])) + " ";
                }
                matcher.appendReplacement(newCmd, generatedRandValue.trim());
            }
            matcher.appendTail(newCmd);
            return newCmd.toString();
        }
        return cmd;
    }
}
