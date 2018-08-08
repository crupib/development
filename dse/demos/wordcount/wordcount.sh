#!/bin/bash

if [ -z "$DSE_ENV" ]; then
    for include in "~/.dse-env.sh" \
                   "`dirname $0`/dse-env.sh" \
                   "`dirname $0`/../../../bin/dse-env.sh" \
                   "`dirname $0`/../../bin/dse-env.sh" \
                   "/etc/dse/dse-env.sh"; do
        if [ -r "$include" ]; then
            DSE_ENV="$include"
            break
        fi
    done
fi

# We found a conf file... load that. If we didn't, fall back to defaults.
if [ -r "$DSE_ENV" ]; then
    . "$DSE_ENV"
    echo DSE_HOME=$DSE_HOME
fi

if [ -f $DSE_HOME/resources/hadoop/lib/hadoop-examples-*.jar ]; then
  EXAMPLE_JAR=$DSE_HOME/resources/hadoop/lib/hadoop-examples-*.jar
elif [ -f $DSE_HOME/hadoop/lib/hadoop-examples-*.jar ]; then
  EXAMPLE_JAR=$DSE_HOME/hadoop/lib/hadoop-examples-*.jar
elif [ -f /usr/share/dse/resources/hadoop/lib/hadoop-examples-*.jar ]; then
  EXAMPLE_JAR=/usr/share/dse/resources/hadoop/lib/hadoop-examples-*.jar
elif [ -f /usr/share/dse/hadoop/lib/hadoop-examples-*.jar ]; then
  EXAMPLE_JAR=/usr/share/dse/hadoop/lib/hadoop-examples-*.jar
else
  echo Unable to find hadoop examples, please set DSE_HOME
  exit 1
fi

# Use /tmp for cases where DSE demos is not writeable by current user
rm -rf /tmp/wikipedia-sample
cp ../wikipedia/wikipedia-sample.bz2 /tmp
pushd /tmp
bunzip2 wikipedia-sample.bz2
if [ $? != 0 ]; then
  echo Unable to bunzip2 wikipedia-sample.bz2. Make sure bunzip2 is installed.
  exit 1
fi

dse hadoop fs -rm /wikipedia-sample 2>/dev/null 1>/dev/null
dse hadoop fs -rmr wc-output 2>/dev/null 1>/dev/null
dse hadoop fs -put wikipedia-sample /
dse hadoop jar $EXAMPLE_JAR wordcount /wikipedia-sample wc-output
dse hadoop fs -ls wc-output
