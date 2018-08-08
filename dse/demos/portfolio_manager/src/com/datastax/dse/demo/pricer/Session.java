/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demo.pricer;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cassandra.exceptions.AlreadyExistsException;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.cassandra.auth.IAuthenticator;
import org.apache.cassandra.db.ColumnFamilyType;
import org.apache.cassandra.thrift.*;

import com.datastax.bdp.util.CassandraProxyClient;
import com.datastax.bdp.util.CassandraProxyClientUtil;
import com.datastax.bdp.util.CassandraProxyClient.ConnectionStrategy;

public class Session
{
    // command line options
    public static final Options availableOptions = new Options();

    public final AtomicInteger operations;
    public final AtomicInteger keys;
    public final AtomicLong    latency;

    static
    {
        availableOptions.addOption("h",  "help",                 false,  "Show this help message and exit");
        availableOptions.addOption("n",  "num-keys",             true,   "Number of keys, default:1000000");
        availableOptions.addOption("N",  "skip-keys",            true,   "Fraction of keys to skip initially, default:0");
        availableOptions.addOption("t",  "threads",              true,   "Number of threads to use, default:10");
        availableOptions.addOption("c",  "columns",              true,   "Number of columns per key, default:5");
        availableOptions.addOption("S",  "column-size",          true,   "Size of column values in bytes, default:34");
        availableOptions.addOption("C",  "cardinality",          true,   "Number of unique values stored in columns, default:50");
        availableOptions.addOption("d",  "nodes",                true,   "Host nodes (comma separated), default:locahost");
        availableOptions.addOption("D",  "nodesfile",            true,   "File containing host nodes (one per line)");
        availableOptions.addOption("s",  "stdev",                true,   "Standard Deviation Factor, default:0.1");
        availableOptions.addOption("r",  "random",               false,  "Use random key generator (STDEV will have no effect), default:false");
        availableOptions.addOption("f",  "file",                 true,   "Write output to given file");
        availableOptions.addOption("p",  "port",                 true,   "Thrift port, default:9160");
        availableOptions.addOption("m",  "unframed",             false,  "Use unframed transport, default:false");
        availableOptions.addOption("o",  "operation",            true,   "Operation to perform (INSERT_PRICES, UPDATE_PORTFOLIOS), default:INSERT_PRICES");
        availableOptions.addOption("u",  "supercolumns",         true,   "Number of super columns per key, default:1");
        availableOptions.addOption("y",  "family-type",          true,   "Column Family Type (Super, Standard), default:Standard");
        availableOptions.addOption("K",  "keep-trying",          true,   "Retry on-going operation N times (in case of failure). positive integer, default:10");
        availableOptions.addOption("k",  "keep-going",           false,  "Ignore errors inserting or reading (when set, --keep-trying has no effect), default:false");
        availableOptions.addOption("i",  "progress-interval",    true,   "Progress Report Interval (seconds), default:10");
        availableOptions.addOption("g",  "keys-per-call",        true,   "Number of keys to get_range_slices or multiget per call, default:1000");
        availableOptions.addOption("l",  "replication-factor",   true,   "Replication Factor to use when creating needed column families, default:1");
        availableOptions.addOption("e",  "consistency-level",    true,   "Consistency Level to use (ONE, QUORUM, LOCAL_QUORUM, EACH_QUORUM, ALL, ANY), default:ONE");
        availableOptions.addOption("x",  "create-index",         true,   "Type of index to create on needed column families (KEYS)");
        availableOptions.addOption("R",  "replication-strategy", true,   "Replication strategy to use (only on insert if keyspace does not exist), default:org.apache.cassandra.locator.SimpleStrategy");
        availableOptions.addOption("O",  "strategy-properties",  true,   "Replication strategy properties in the following format <dc_name>:<num>,<dc_name>:<num>,...");
        availableOptions.addOption("U",  "username",             true,   "Cassandra username if password authentication is configured");
        availableOptions.addOption("P",  "password",             true,   "Cassandra password if password authentication is configured");
    }

    private int numKeys          = 10000;
    private float skipKeys       = 0;
    private int threads          = 10;
    private int columns          = 10;
    private int columnSize       = 34;
    private int cardinality      = 50;
    private String[] nodes       = new String[] { "localhost" };
    private boolean random       = false;
    private boolean unframed     = false;
    private int retryTimes       = 10;
    private int port             = 9160;
    private int superColumns     = 1;

    private int progressInterval  = 10;
    private int keysPerCall       = 1000;
    private int replicationFactor = 1;
    private boolean ignoreErrors  = false;
    
    private String username       = null;
    private String password       = null;

    private PrintStream out = System.out;

    private IndexType indexType = null;
    private Pricer.Operations operation = Pricer.Operations.INSERT_PRICES;
    private ColumnFamilyType columnFamilyType = ColumnFamilyType.Standard;
    private ConsistencyLevel consistencyLevel = ConsistencyLevel.ONE;
    private String replicationStrategy = "org.apache.cassandra.locator.SimpleStrategy";
    private Map<String, String> replicationStrategyOptions = new HashMap<String, String>();


    // required by Gaussian distribution.
    protected int   mean;
    protected float sigma;

    public Session(String[] arguments) throws IllegalArgumentException
    {
        float STDev = 0.1f;
        CommandLineParser parser = new PosixParser();

        try
        {
            CommandLine cmd = parser.parse(availableOptions, arguments);

            if (cmd.hasOption("h"))
                throw new IllegalArgumentException("help");

            if (cmd.hasOption("n"))
                numKeys = Integer.parseInt(cmd.getOptionValue("n"));

            if (cmd.hasOption("N"))
                skipKeys = Float.parseFloat(cmd.getOptionValue("N"));

            if (cmd.hasOption("t"))
                threads = Integer.parseInt(cmd.getOptionValue("t"));

            if (cmd.hasOption("c"))
                columns = Integer.parseInt(cmd.getOptionValue("c"));

            if (cmd.hasOption("S"))
                columnSize = Integer.parseInt(cmd.getOptionValue("S"));

            if (cmd.hasOption("C"))
                cardinality = Integer.parseInt(cmd.getOptionValue("C"));

            if (cmd.hasOption("d"))
                nodes = cmd.getOptionValue("d").split(",");

            if (cmd.hasOption("D"))
            {
                try
                {
                    String node = null;
                    List<String> tmpNodes = new ArrayList<String>();
                    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(cmd.getOptionValue("D"))));
                    while ((node = in.readLine()) != null)
                    {
                        if (node.length() > 0)
                            tmpNodes.add(node);
                    }
                    nodes = tmpNodes.toArray(new String[tmpNodes.size()]);
                    in.close();
                }
                catch(IOException ioe)
                {
                    throw new RuntimeException(ioe);
                }
            }

            if (cmd.hasOption("s"))
                STDev = Float.parseFloat(cmd.getOptionValue("s"));

            if (cmd.hasOption("r"))
                random = true;

            if (cmd.hasOption("f"))
            {
                try
                {
                    out = new PrintStream(new FileOutputStream(cmd.getOptionValue("f")));
                }
                catch (FileNotFoundException e)
                {
                    System.out.println(e.getMessage());
                }
            }

            if (cmd.hasOption("p"))
                port = Integer.parseInt(cmd.getOptionValue("p"));

            if (cmd.hasOption("m"))
                unframed = Boolean.parseBoolean(cmd.getOptionValue("m"));

            if (cmd.hasOption("o"))
                operation = Pricer.Operations.valueOf(cmd.getOptionValue("o").toUpperCase());

            if (cmd.hasOption("u"))
                superColumns = Integer.parseInt(cmd.getOptionValue("u"));

            if (cmd.hasOption("y"))
                columnFamilyType = ColumnFamilyType.valueOf(cmd.getOptionValue("y"));

            if (cmd.hasOption("K"))
            {
                retryTimes = Integer.valueOf(cmd.getOptionValue("K"));

                if (retryTimes <= 0)
                {
                    throw new RuntimeException("--keep-trying option value should be > 0");
                }
            }

            if (cmd.hasOption("k"))
            {
                retryTimes = 1;
                ignoreErrors = true;
            }

            if (cmd.hasOption("i"))
                progressInterval = Integer.parseInt(cmd.getOptionValue("i"));

            if (cmd.hasOption("g"))
                keysPerCall = Integer.parseInt(cmd.getOptionValue("g"));

            if (cmd.hasOption("l"))
                replicationFactor = Integer.parseInt(cmd.getOptionValue("l"));

            if (cmd.hasOption("e"))
                consistencyLevel = ConsistencyLevel.valueOf(cmd.getOptionValue("e").toUpperCase());

            if (cmd.hasOption("x"))
                indexType = IndexType.valueOf(cmd.getOptionValue("x").toUpperCase());

            if (cmd.hasOption("R"))
                replicationStrategy = cmd.getOptionValue("R");

            if (cmd.hasOption("O"))
            {
                String[] pairs = StringUtils.split(cmd.getOptionValue("O"), ',');

                for (String pair : pairs)
                {
                    String[] keyAndValue = StringUtils.split(pair, ':');

                    if (keyAndValue.length != 2)
                        throw new RuntimeException("Invalid --strategy-properties value.");

                    replicationStrategyOptions.put(keyAndValue[0], keyAndValue[1]);
                }
            }
            
            if (cmd.hasOption("U"))
            {
                username = cmd.getOptionValue("U");
            }
            
            if (cmd.hasOption("P"))
            {
                password = cmd.getOptionValue("P");
            }
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        mean  = numKeys / 2;
        sigma = numKeys * STDev;

        operations = new AtomicInteger();
        keys = new AtomicInteger();
        latency = new AtomicLong();
    }

    public int getCardinality()
    {
        return cardinality;
    }

    public int getColumnSize()
    {
        return columnSize;
    }

    public boolean isUnframed()
    {
        return unframed;
    }

    public int getColumnsPerKey()
    {
        return columns;
    }

    public ColumnFamilyType getColumnFamilyType()
    {
        return columnFamilyType;
    }

    public int getNumKeys()
    {
        return numKeys;
    }

    public int getThreads()
    {
        return threads;
    }

    public float getSkipKeys()
    {
        return skipKeys;
    }

    public int getSuperColumns()
    {
        return superColumns;
    }

    public int getKeysPerThread()
    {
        return numKeys / threads;
    }

    public int getTotalKeysLength()
    {
        return Integer.toString(numKeys).length();
    }

    public ConsistencyLevel getConsistencyLevel()
    {
        return consistencyLevel;
    }

    public int getRetryTimes()
    {
        return retryTimes;
    }

    public boolean ignoreErrors()
    {
        return ignoreErrors;
    }

    public Pricer.Operations getOperation()
    {
        return operation;
    }

    public PrintStream getOutputStream()
    {
        return out;
    }

    public int getProgressInterval()
    {
        return progressInterval;
    }

    public boolean useRandomGenerator()
    {
        return random;
    }

    public int getKeysPerCall()
    {
        return keysPerCall;
    }

    // required by Gaussian distribution
    public int getMean()
    {
        return mean;
    }

    // required by Gaussian distribution
    public float getSigma()
    {
        return sigma;
    }

    /**
     * Create Keyspace1 with Standard1 and Super1 column families
     */
    public void createKeySpaces()
    {
        KsDef keyspace = new KsDef();
      
        // column family for standard columns
        CfDef portfolioCfDef = new CfDef("PortfolioDemo", "Portfolios")
                .setGc_grace_seconds(60)
                .setKey_validation_class("LongType")
                .setComparator_type("UTF8Type")
                .setDefault_validation_class("DoubleType")
                .setRead_repair_chance(1.0)
                .setMin_compaction_threshold(4)
                .setMax_compaction_threshold(32)
                .setReplicate_on_write(true);

        // column family with super columns
        CfDef stockCfDef = new CfDef("PortfolioDemo", "Stocks")
                .setGc_grace_seconds(60)
                .setKey_validation_class("UTF8Type")
                .setComparator_type("UTF8Type")
                .setDefault_validation_class("DoubleType")
                .setRead_repair_chance(1.0)
                .setMin_compaction_threshold(4)
                .setMax_compaction_threshold(32)
                .setReplicate_on_write(true);
        
        CfDef histCfDef = new CfDef("PortfolioDemo", "StockHist")
                .setGc_grace_seconds(60)
                .setKey_validation_class("UTF8Type")
                .setComparator_type("UTF8Type")
                .setDefault_validation_class("DoubleType")
                .setRead_repair_chance(1.0)
                .setMin_compaction_threshold(4)
                .setMax_compaction_threshold(32)
                .setReplicate_on_write(true);
        
        CfDef histLossCfDef = new CfDef("PortfolioDemo", "HistLoss")
                .setGc_grace_seconds(60)
                .setKey_validation_class("UTF8Type")
                .setComparator_type("UTF8Type")
                .setDefault_validation_class("UTF8Type")
                .setRead_repair_chance(1.0)
                .setMin_compaction_threshold(4)
                .setMax_compaction_threshold(32)
                .setReplicate_on_write(true);
        
        keyspace.setName("PortfolioDemo");
        keyspace.setStrategy_class(replicationStrategy);

        if (!replicationStrategyOptions.isEmpty())
        {
            keyspace.setStrategy_options(replicationStrategyOptions);
        }

        if (replicationStrategy.equalsIgnoreCase("org.apache.cassandra.locator.SimpleStrategy"))
        {
            if (!keyspace.isSetStrategy_options())
                keyspace.setStrategy_options(new HashMap<String, String>());

            keyspace.putToStrategy_options("replication_factor", Integer.toString(replicationFactor));
        }

        keyspace.setCf_defs(new ArrayList<CfDef>(Arrays.asList(portfolioCfDef, stockCfDef, histCfDef, histLossCfDef)));

        Cassandra.Iface client = getClient(false);

        try
        {
            try
            {
                client.describe_keyspace(keyspace.getName());
            }
            catch (NotFoundException ex)
            {
                client.system_add_keyspace(keyspace);
                out.println(String.format("Created keyspaces. Sleeping %ss for propagation.", nodes.length));

                Thread.sleep(nodes.length * 1000); // seconds
            }
        }
        catch (InvalidRequestException e)
        {
            out.println(e.getWhy());
        }
        catch (Exception e)
        {
            out.println(e.getMessage());
        }
    }

    /**
     * Thrift client connection with Keyspace1 set.
     * @return cassandra client connection
     */
    public Cassandra.Iface getClient()
    {
        return getClient(true);
    }
    /**
     * Thrift client connection
     * @param setKeyspace - should we set keyspace for client or not
     * @return cassandra client connection
     */
    @SuppressWarnings("deprecation")
    public Cassandra.Iface getClient(boolean setKeyspace)
    {
        // random node selection for fake load balancing
        String currentNode = nodes[Pricer.randomizer.nextInt(nodes.length)];
        Cassandra.Iface client = null;

        try
        {
            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password))
            {
                Map<String,String> credentials = new LinkedHashMap<String,String>();
                credentials.put(IAuthenticator.USERNAME_KEY, username);
                credentials.put(IAuthenticator.PASSWORD_KEY, password);
                client = CassandraProxyClient.newProxyConnection(currentNode, port,
                        CassandraProxyClientUtil.getDefaultClientTransportFactory(), ConnectionStrategy.RANDOM, 
                        credentials);
            }
            else
            {
                client = CassandraProxyClient.newProxyConnection(currentNode, port,
                        CassandraProxyClientUtil.getDefaultClientTransportFactory(), ConnectionStrategy.RANDOM);
            }
            if (setKeyspace)
            {
                client.set_keyspace("PortfolioDemo");
            }
        }
        catch (InvalidRequestException e)
        {
            throw new RuntimeException(e.getWhy());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }

        return client;
    }

}
