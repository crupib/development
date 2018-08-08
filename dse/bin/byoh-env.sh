#!/bin/sh

# add any environment overrides you need here. This is where users
# may set thirdparty variables like HADOOP_LOG_DIR etc.

# This probably does not need to be set, this is mostly for testing
#export HOME="/home/hdfs/dse"

#
export DSE_HOME="/Users/crupib/dse"
export DSE_CONF_DIR="/etc/dse"

export CASSANDRA_DRIVER="$DSE_HOME/resources/driver"

# The home directory for Cassandra
export CASSANDRA_HOME="$DSE_HOME/cassandra"
# set this to the configuration directory for Cassandra (location of the YAML file)
export CASSANDRA_CONF="/etc/dse/cassandra"

#set HADOOP_PATH_HOME to your hadoop directory (for your current hadoop distribution)
#this will be overridden by /etc/default/hadoop if it exists
export HADOOP_PATH_HOME="/usr/lib/hadoop"
export HADOOP_CONF_DIR="$HADOOP_PATH_HOME/conf"

# Set these to your chosen partitioners.
#for example: export PIG_PARTITIONER=org.apache.cassandra.dht.Murmur3Partitioner
export PIG_PARTITIONER=`"$DSE_HOME"/bin/dsetool partitioner`
if [ "x${PIG_PARTITIONER}x" = "xx" ]; then
   export PIG_PARTITIONER=org.apache.cassandra.dht.Murmur3Partitioner
fi
export HIVE_PARTITIONER=$PIG_PARTITIONER

#set PIG_HOME to your prefered pig distribution, this should match your current Hadoop distribution
export PIG_HOME="/usr/lib/pig"

#set HIVE_HOME to your selected hive distribution
export HIVE_HOME="/usr/lib/hive"

#try to determine hive version, set the version explicitly if it failed
HIVE_VERSION=`basename "$HIVE_HOME"/lib/hive-exec-*.jar | sed -n 's/^hive-exec-\(0\.1[0-9]\.[0-9]\)\(-cdh\)*.*\.jar$/\1\2/p'`
if [ "x${HIVE_VERSION}x" = "xx" ]; then 
    echo "ERROR: Can not find hive version. Check HIVE_HOME setting"
    exit -1
fi
# hdp 2.1 reports hive version as 0.13.0...., while actually it has some back ported fixes from 0.13.1.
if [ "$HIVE_VERSION" = "0.13.0" ]; then
    HIVE_VERSION=0.13.1
fi

export HIVE_CONNECTOR_CLASSPATH=`ls "$DSE_HOME"/resources/byoh/lib/hive-$HIVE_VERSION-cassandra-connector*jar`
if [ "x${HIVE_CONNECTOR_CLASSPATH}x" = "xx" ]; then 
    echo "Detected hive version: '$HIVE_VERSION'"
    echo "ERROR: Can not find hive cassandra connector for the version in DSE_HOME"
    exit -1
fi

# the directory is used for hive-0.10.0-cdh and 0.11.0 to store aux jars
HIVE_AUX_JARS_DIR=/tmp/hive_aux_jars

#Make sure to migrate any cluster settings over to your new configs if you are using a different distribution
export HIVE_CONF_DIR="$DSE_HOME/resources/byoh/conf"

#Environment variables that must be set for Pig to work with Cassandra
# INITIAL_ADDRESS is used to first contact the cluster
export PIG_INITIAL_ADDRESS=localhost
export PIG_OUTPUT_INITIAL_ADDRESS=localhost
export PIG_INPUT_INITIAL_ADDRESS=localhost
# this is your RPC port as defined in your YAML
export PIG_OUTPUT_RPC_PORT=9160
export PIG_INPUT_RPC_PORT=9160
export PIG_RPC_PORT=9160

#set Mahout home
export MAHOUT_HOME="/usr/lib/mahout"

# a comma separated list of hadoop data nodes we can access from this machine, will be set to mapreduce.job.hdfs-servers in the client configuration
export DATA_NODE_LIST="localhost"

# the hadoop data node that we want to resolve to for our hive metastore data
# used for setting hive.metastore.warehouse.dir
export NAME_NODE="localhost"



# ==================================
# don't change after this.
if [ -r "`dirname "$0"`/byoh.in.sh" ]; then
    # File is right where the executable is
    . "`dirname "$0"`/byoh.in.sh"
elif [ -r "/usr/share/dse/byoh.in.sh" ]; then
    # Package install location
    . "/usr/share/dse/byoh.in.sh"
elif [ -r "$DSE_HOME/bin/byoh.in.sh" ]; then
    # Package install location
    . "$DSE_HOME/bin/byoh.in.sh"
else
    # Go up directory tree from where we are and see if we find it
    DIR="`dirname $0`"
    for i in 1 2 3 4 5 6; do
        if [ -r "$DIR/bin/byoh.in.sh" ]; then
            BYOH_IN="$DIR/bin/byoh.in.sh"
            break
        fi
        DIR="$DIR/.."
    done
    if [ -r "$BYOH_IN" ]; then
        . "$BYOH_IN"
    else
        echo "Cannot determine location of byoh.in.sh"
        exit 1
    fi
fi
