####                                                                            ####
## Note your system must have at least 2 cores reserved by Spark to run this demo ##
####                                                                            ####

This is a standalone Spark Streaming application that can run both on 
a single and remote cluster. This demo will start up HTTP servers as spark
receivers and then send a constant stream of requests at those servers.
The events will be persisted to Cassandra and have some small batch level 
analytics run at the same time. Since we are using the built-in Oracle JDK
HTTP Server you will need to be running Oracle Java for this demo. 

Prerequisites: A DSE cluster upgraded to 4.6, or a fresh installation of DSE 4.6
with at least one datacenter running Spark.

Automated Execution:

1. Edit the configuration variables at the top of run_demo.sh to match your environment
2. Execute run_demo.sh
3. End the process to shut down the demo (ctrl-c)

Manual Execution:

1. Start the HttpSparkStream streaming Spark Application
    cd HttpSparkStream
    dse spark-submit --class com.datastax.HttpSparkStream target/HttpSparkStream.jar

For a distributed streaming environment substitute
    dse spark-submit --class HttpSparkStream target/HttpSparkStream.jar -d <Number of Spark Workers>

For more information and options run
    dse spark-submit --class com.datastax.HttpSparkStream target/HttpSparkStream.jar --help 

This will start a single HTTP receiver on the Spark Cluster listening on 9999. It will populate the
following tables in the requests_ks keyspace. You should see the text "Streaming receivers starting now" when
the application is ready to start receving requests.

    timeline
        persisted records of every event processed by the stream

    method_agg 
        Aggregates for each batch counting how many requests and of what method were made to each url

    sorted_urls
        Aggregate of the number of hits to each url stored per batch

2. Run The HttpRequestGenerator against the HttpSparkStream Application
    # In another terminal window
    cd $DSE_HOME/demos/http_receiver 
    ./gradlew -b HttpRequestGenerator/build.gradle run 

For a distributed streaming environment substitute
    ./gradlew -b HttpRequestGenerator/build.gradle run -Pips="ip1,ip2,ip3,..." -PnumThreads="X" # Where ip1,... are the ip addresses of the spark workers, and X is the number of threads (default 4)

This will start a multi-threaded application which will make HTTP requests against localhost until the process is stopped.
While this is running, in a web browser look at localhost:4040/streaming . This should show you a constant flow of HTTP events
being processed by your application. Each even counts as a single record on this page.

You can also use your browser or curl to send http requests to the server at localhost:9999

To query the data as it is coming in try some of the following queries
    cqlsh
    use requests_ks
    # See 5 events as persisted to C*
    SELECT * FROM timeline limit 5; 
    # See how many hits occured to /about/lemon/kiwi in the last 5 batches
    SELECT * FROM method_agg where url='/about/lemon/kiwi' AND method='DELETE' ORDER BY time DESC  LIMIT 5; 
    See how that url compared to others queried in that batch
    SELECT * FROM sorted_urls WHERE time = '<place a timestamp from the last query here>'; 

-----

Source code for both demos can be found under install_location/demos/http_receiver/
