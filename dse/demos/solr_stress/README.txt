About the solr_stress utility
-----------------------------

The search stress demo is a benchmark tool to demonstrate the capabilities of your Search cluster. 
It will simulate a number of requests to read and write data to the Search cluster over a sequential or random set of iterations, as specified on a "test data" file.

Before starting this demo, be sure that you have started DataStax Enterprise with Solr enabled on one or more nodes. 
See "Starting DSE and DSE Search" in the DataStax documentation (http://www.datastax.com/docs).

Directory structure
----------------

Directory structure should look as follows

├── resources
│   ├── schema
│   │   ├── create-schema-geo-rt.sh
│   │   ├── create-schema-geo.sh
│   │   ├── create-schema.sh
│   │   ├── create_table.cql
│   │   ├── create_table_geo.cql
│   │   ├── create_table_geo_rt.cql
│   │   ├── delete-index-geo.sh
│   │   ├── delete-index.sh
│   │   ├── schema.xml
│   │   ├── schema_geo.xml
│   │   ├── set-solr-options.sh
│   │   ├── solrconfig-rt.xml
│   │   └── solrconfig.xml
│   ├── testCqlQuery.txt
│   ├── testCqlWrite.txt
│   ├── testGenerateIndexLatencyTest.txt
│   ├── testGenerateQueries.txt
│   ├── testLucRead.txt
│   ├── testMixed.txt
│   ├── testQuery.txt
│   ├── testUpdate.txt
│   └── testWrite.txt
├── run-benchmark.sh
├── run-geo.sh
├── download-geonames.sh
└── solr_stress.jar

'resources' directory contains a set of example input files for solr_stress. Under 'schema' subdirectory there are scripts for creating CQL keyspaces, tables and Solr cores.

Running
----------------

1. Open a shell window or tab and make the solr_stress directory your current directory. The location of the demo directory depends on your platform:

RPM-Redhat or Debian installations:

    cd  /usr/share/demos/solr_stress

Tar distribution, such as Mac:

    cd ~/dse-*/demos/solr_stress


2. Open another shell window or tab and add the schema: 

    cd resources/schema
    ./create-schema.sh [options]

    CQL Table Creation Options:
        --ssl    use SSL for Cassandra table creation over cqlsh

    Solr HTTP Options:
        -e CA_CERT_FILE        use HTTPS with the provided CA certificate
        -E CLIENT_CERT_FILE    use the provided client certificate
        -h HOST                hostname or IP for Solr HTTP requests
        -a                     enable Kerberos
        -u USERNAME            Kerberos username
        -p PASSWORD            Kerberos password

The script creates the Cassandra schema and posts solrconfig.xml and schema.xml 
to these locations:

    http://localhost:8983/solr/resource/demo.solr/solrconfig.xml

    http://localhost:8983/solr/resource/demo.solr/schema.xml

Then creates the core/index by posting to the following location:

    http://localhost:8983/solr/admin/cores?action=CREATE&name=demo.solr
        
    You can override defaults by specifying them as command line parameters: -x schemafile.xml -t tableCreationFile.cql -r solrCofgFile.xml -k solrCore


3. Run the benchmark:  

./run-benchmark.sh [--clients=<clients count>] [--loops=<loops count>] [--solrCore=<solr core>] [--testData=<test data file>] [--url=<url1,url2,url3,...>] [--qps=<qps>] [--stats=<true|false>] [--seed=<seed value>]
    clients        Number of client threads to create: Default is 1
    loops          The number of times the commands list gets executed if running sequentially or the number of commands to run if running randomly. Default is 1
    fetch          Fetch size for cql pagination (disabled by default). Only the first page is retrieved.
    solrCore       Solr core name to run the benchmark against
    testData       Name of the file containing the test data
    seed           Value to set the random generator seed to
    qps            Maximum number of queries per second allowed
    stats          Whether statics should be gather during runtime. A csv file will be created with the recorded values. Default is false.
    url            A comma delimited list of servers to run the benchmark against. Default is http://localhost:8983. Example: --url=http://localhost:8983,http://192.168.10.45:8983,http://192.168.10.46:8983

      
4. Writing a testData file:

Execution modes

You can specify RANDOM, SEQUENTIAL or SHARED_SEQUENTIAL execution modes for the list of commands:
    * RANDOM - a single command from a test file will be executed, chosen randomly.
    When ran with '--loops=N' flag, N commands will be executed instead. When ran with multiple clients '--clients=C', every client will execute exactly N commands, regardless of the test file size.
    * SEQUENTIAL - all commands from a test file will be executed in the order specified in a test file.
    When ran with '--loops=N' flag, the whole file will be executed N times. When ran with multiple clients '--clients=C', every client wil execute whole file N times. Overall every command will be executed up to N x C times.
    * SHARED_SEQUENTIAL - all commands from a test file will be executed, but the test file is shared between all clients. It means that multiple clients will concurrently execute a shared list of commands.
    When ran with '--loops=N' flag, the whole file will be executed N times. When ran with multiple clients '--clients=C', commands from a test file will be executed concurrently. Overall every command will be executed up to N times.

Available commands

    * HTTP read (HTTPREAD), see examples:
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testQuery.txt --solrCore=demo.solr
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testMixed.txt --solrCore=demo.solr
    you can specify the expected number of results such as: HTTPREAD|q=body:benchmark|100 (100 expected results)
    * HTTP write (HTTPWRITE), see examples:
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testWrite.txt --solrCore=demo.solr
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testMixed.txt --solrCore=demo.solr
    * HTTP updates (HTTPUPDATE), see example:
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testUpdate.txt --solrCore=demo.solr
    you can specify the String fields to be updated as a csv list (or ALLSTR to get all string fields updated) and a query. The command will run the query and randomly update one of
    the returned results by prepending 'update N' to that String field.
    * CQL commands can be executed using the syntax `CQL|<CQL command>`, see examples:
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testCqlQuery.txt --solrCore=demo.solr
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testCqlWrite.txt --solrCore=demo.solr
    * Lucene perf package test tasks (LUCPERF only some supported), see example:
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testLucRead.txt --solrCore=demo.solr
    currently supports: High/Med/LowTerm, Prefix3, Wildcard, Fuzzy1/2, AndHigh/High/Med/Low, OrHigh/High/Med/Low, IntNRQ, HighPhrase, HighSloppyPhrase, MedPhrase, MedSloppyPhrase, LowPhrase and LowSloppyPhrase
    * INDEX_LATENCY command, which measures the time between sending document for indexing and when it became visible for searches. It needs the same set of data as `HTTPWRITE` command, because it needs to insert a document before measuring it's visibility latency
  There're also commands which will result in generating a whole test case:
    * GENERATE_QUERIES, see example:
        ./run-benchmark.sh --url=http://localhost:8983 --testData=resources/testGenerateQueries.txt --solrCore=demo.solr
    generates a random set of search queries (both term and boolean), using terms fetched from Solr's TermsComponent. Generated queries are saved to a file in a format readable for Solr stress tool, either as `CQL` or `HTTPREAD` commands.
    Example:
        SHARED_SEQUENTIAL
        GENERATE_QUERIES|fields=country,name,timezone,published|terms-limit=500|boolean-queries=25000|term-queries=5000|output=queries-http.txt|api=HTTP
        GENERATE_QUERIES|fields=country,name,timezone,published|terms-limit=500|boolean-queries=25|term-queries=25|output=queries-cql.txt|api=CQL
    The first command will create 30 000 queries (`boolean-queries=25000` + `term-queries=5000`) and store them to a file (`output=queries-http.txt`) as a set of `HTTPREAD` commands (`api=HTTP` parameter), whereas the second file will create 50 queries (`boolean-queries=25` + `term-queries=25`) and store them (`output=queries-cql.txt`) as a set of `CQL` commands (`api=CQL` parameter). In both cases, queries will be generated using 4 index fields `fields=country,name,timezone,published`. Generator will fetch up to 500 terms (`terms-limit=500`) for each field and construct queries from them. Later on both output files can be consumed by the Solr stress tool.
    Note: If other test file header than SHARED_SEQUENTIAL were used, this command would be repeated --clients or --loops times. To avoid redundant executions, use SHARED_SEQUENTIAL.
    * GENERATE_INDEX_LATENCY_TEST, e.g. 'resources/testGenerateIndexLatencyTest.txt'
    Generates a Solr stress tool input file, consisting of `HTTPWRITE` and `INDEX_LATENCY` commands. Test file may have an arbitrary size, but it should have at least several thousands commands to be sensible. It uses geonames dataset to generate input data, which can be downloaded from [here](http://download.geonames.org/export/dump/allCountries.zip).
    Example:
        SHARED_SEQUENTIAL
        GENERATE_INDEX_LATENCY_TEST|geonames-file=allCountries.txt|commands-count=50000|output=latency-test.txt
    Command will generate `commands-count=50000` commands, where about 15% will be `INDEX_LATENCY` commands, and the rest will be `HTTPWRITE` commads. It will simulate read/write heavy scenario - while indexing documents, it also generates search queries, each matching exactly one document. Each `INDEX_LATENCY` will probe Solr to measure the time needed for a newly inserted document to become visible. Output test file `output=latency-test.txt` can be directly consumed by Solr stress tool.
    Note: If other test file header than SHARED_SEQUENTIAL were used, this command would be repeated --clients or --loops times. To avoid redundant executions, use SHARED_SEQUENTIAL.

Additional notes

- Each field can contain unique or randomly generated data via the $RANDOM_UUID, $RANDOM_XX, $IPSUM_XX or $ZIPF_XX notations. These will be substituted respectively by either a UUID, a random number up to the specified integer, a random number following a ZIPF distribution, or a random string of words from Lorem Ipsum vocabulary: $RANDOM_100 will get string replaced by an uniformly sampled random number between 0 and 100 each time that command is run. On the other hand, $ZIPF_100 will get string replaced by a random number between 1 and 100, but in this case lower ranking numbers (e.g., 1) will appear much more times than higher rank numbers (e.g., 69). Finally, $IPSUM_100 will generate a 100 word sentence with words randomly picked from a Lorem Ipsum dictionary. You can also specify a number of times a RANDOM should occur: $RANDOM_1000:10 to get 10 random numbers.
- You can specify a number of repetitions so that the same command gets executed N number of times: HTTPREAD(5) will run this read command 5 times
- You can add comments to your test data files via the # char
- You can escape the |, & and = special characters by doubling them ||, && and == if you need them as doc content. No need to escape them in cql queries

Let's see an example to stress test the demo.solr core.

SEQUENTIAL
HTTPWRITE(10)|text=This is a benchmark of dse search|type=$RANDOM_10|id=$RANDOM_UUID
HTTPREAD|wt=json&q=text:benchmark&facet=true&facet.field=type
 
                                                                                                                  
5. Reading the results:

When the test is done, you should see an output like this:

Reading command line...
Reading test data file...
Starting Benchmark...
Terminated thread: 7, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 0, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 9, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 2, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 8, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 1, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 6, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 4, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 3, writes: 10000, reads: 1000, exceptions: 0
Terminated thread: 5, writes: 10000, reads: 1000, exceptions: 0

Test running, 110000 commands executed so far.

Finished with clients: 10, loops: 1000, rate (ops/sec): 2700.0, random seed: 1405363864239, commands executed: 110000, total time: 40 seconds

0 queries exceeded the 300000000 (usec) limit.

Percentiles (usec):
	10%: 	32
	20%: 	35
	30%: 	37
	40%: 	39
	50%: 	42
	60%: 	46
	70%: 	68
	80%: 	235
	90%: 	6296
	100%: 	2875392

The test will provide feedback of how many commands have been run every minute. 
Each thread will output how many reads, writes and exceptions it registered. 
Finally we get some performance metrics and the 10% percentiles to get an idea of how we are performing.
You can check the exceptions.log for any errors that might have occurred.
