# Weather Sensors Demo

This weather demo dynamically creates auto-generated data for a handful of cities (stations) and then runs analytical
queries on the generated data.

## Developer Version

This demo contains two tracks: the demo track and the developer track.
For additional information for developers, see README.dev.md.

## Prerequisites

Using the following commands will install all prerequisites necessary for
using python-driver (for the front-end), and running the web server:

    sudo apt-get install python2.7-dev python-pip libsasl2-dev
    sudo pip install --upgrade pip

    cd demos/weather_sensors
    sudo pip install --find-links web/sdists/ pyhs2 six flask
    sudo pip install --find-links web/sdists/ --upgrade cassandra-driver

Note: If you have an older version of pip you may see some error messages and in some
operating systems warning messages may be displayed. Re-running the command twice
should remove these messages. Confirmation of successful installs is seen with the messages:
"Successfully installed hive-utils six flask" and "Successfully installed cassandra-driver".

## DataStax Enterprise

The demo requires that DataStax Enterprise is running with both Spark and Hadoop enabled. To enable this mode:

### Package Installs

1. Edit the file `/etc/default/dse` and set `HADOOP_ENABLED=1` and `SPARK_ENABLED=1`
2. Restart DSE with: `sudo service dse restart`

### Tarball Installs

1. Change into the tarball install location
2. Stop Cassandra: `bin/dse cassandra-stop`
3. Start Cassandra with Hadoop/Spark enabled: `bin/dse cassandra -k -t`

## Load Pre-Generated and Aggregated Data

Run the following script on a node that is running Cassandra
(if `cqlsh`, `dse` are not in your path, see Note 1 below).

    cd demos/weather_sensors
    bin/create-and-load

## Web UI

To try the custom query portion of the website make sure to start the Spark SQL Thriftserver and Hive services in separate terminals:

```
dse start-spark-sql-thriftserver  --hiveconf hive.server2.thrift.port=5588
```
```
dse hive --service hiveserver2 --hiveconf hive.server2.thrift.port=5587
```

Open a new terminal and run the demo by issuing the following command:

    cd demos/weather_sensors
    python web/weather.py

Then browse to:

    http://localhost:8983/

## Cleanup

To remove all generated data from this machine, run the following script
(if `cqlsh`, `dse` are not in your path, see Note 1 below).

    cd demos/weather_sensors
    bin/cleanup
    dse stop-spark-sql-thriftserver

If not planning on running the demo on this machine again, run the following command:

    echo "DROP KEYSPACE weathercql;" | cqlsh

## Notes

1. Depending on your Cassandra installation method, the `dse` and `cqlsh` commands may not automatically be in your path.
If they aren't:

    1. Add the `dse/bin` location to the PATH. For example: `export PATH=$PATH:/usr/share/dse/bin`.
    2. Include the path as an argument. For example: `bin/create-and-load /path/to/dse`.
