#!/bin/sh
export DSE_HOME="/Users/crupib/dse"

#we need to load the dependencies for the hive driver
if [ -z "$DSE_LIB" ]; then
    for dir in "$DSE_HOME/lib" \
        "$DSE_HOME/build" \
        "$DSE_HOME/build/lib/jars" \
        "$DSE_HOME/resources/cassandra/lib" \
        "$DSE_HOME/resources/dse/lib" \
        "/usr/share/dse" \
        "/usr/share/dse/common" \
        "/opt/dse" \
        "/opt/dse/common"; do

        if [ -r "$dir" ]; then
            export DSE_LIB="$DSE_LIB
                            $dir"
                            fi
    done
fi

#initialize it

for dir in $DSE_LIB; do
    for jar in "$dir"/*.jar; do
    if [ -r "$jar" ]; then
        if [ -z $DSE_CLASSPATH ]
        then
                DSE_CLASSPATH="$jar"
        else
            DSE_CLASSPATH="$DSE_CLASSPATH:$jar"
        fi
    fi
    done
done

export DSE_CLASSPATH
