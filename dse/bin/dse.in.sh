#!/bin/sh

set_HADOOP_JT() {
    export HADOOP_JT="$("$BIN"/dse-client-tool hadoop job-tracker-address)"
}

get_partitioner() {
    echo "$("$BIN"/dse-client-tool cassandra partitioner)"
}

contains() {
    string="$1"
    substring="$2"
    if test "${string#*$substring}" != "$string"
    then
        return 0    # $substring is in $string
    else
        return 1    # $substring is not in $string
    fi
}

remove_duplicates() {
    result=$(echo "$1" | awk -v RS=':' -v ORS=":" '!a[$1]++{if (NR > 1) printf ORS; printf $a[$1]}')
    echo $result
}

if [ -n "$2" ]; then
for PARAM in "$@"; do
   if [ "$PARAM" = "$1" ]; then
      continue
   fi

   if [ "$PARAM" = "-t" ]; then
      DSE_HADOOP_MODE="1"
   elif [ "$PARAM" = "-s" ]; then
      DSE_SOLR_MODE="1"
   elif contains "$PARAM" "cassandra.username" ; then
      DSE_CREDENTIALS_SUPPLIED="1"
   elif contains "$PARAM" "cassandra-username" ; then
      DSE_CREDENTIALS_SUPPLIED="1"
   elif contains "$PARAM" "dse.sasl.protocol" ; then
      DSE_CREDENTIALS_SUPPLIED="1"
   fi
done
fi

# For when starting under JSVC
if [ "$JSVC_INIT" = "1" ]; then
    DSE_CMD="cassandra"
else
    DSE_CMD="$1"
fi

# Use JAVA_HOME if set, otherwise look for java in PATH
if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif [ "`uname`" = "Darwin" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home)
    if [ -x "$JAVA_HOME/bin/java" ]; then
      JAVA="$JAVA_HOME/bin/java"
    fi
else
    JAVA="`which java`"
    if [ "$JAVA" = "" -a -x "/usr/lib/jvm/default-java/bin/java" ]; then
        # Use the default java installation
        JAVA="/usr/lib/jvm/default-java/bin/java"
    fi
    if [ "$JAVA" != "" ]; then
        export JAVA_HOME=$(readlink -f "$JAVA" | sed "s:bin/java::")
    fi
fi
export JAVA

if [ "x$JAVA" = "x" ]; then
    echo "Java executable not found (hint: set JAVA_HOME)" >&2
    exit 1
fi

# Helper functions
filematch () { case "$2" in $1) return 0 ;; *) return 1 ;; esac ; }

#########################################
# Setup DSE env
#########################################

if [ -z "$DSE_HOME" ]; then
    abspath=$(cd "$(dirname $0)" && pwd -P)
    DSE_HOME="`dirname "$abspath"`"
    if [ -x "$DSE_HOME/bin/dse" ]; then
        export DSE_HOME
    elif [ -x "/usr/bin/dse" ]; then
        export DSE_HOME="/usr"
    elif [ -x "/usr/share/dse/cassandra" ]; then
        export DSE_HOME="/usr/share/dse"
    elif [ -x "/usr/share/dse/bin/dse" ]; then
        export DSE_HOME="/usr/share/dse"
    elif [ -x "/opt/dse/bin/dse" ]; then
        export DSE_HOME="/opt/dse"            
    else 
        DIR="`dirname $0`"
        for i in 1 2 3 4 5 6; do
            if [ -x "$DIR/bin/dse" ]; then
                export DSE_HOME="$DIR"
                break
            fi
            DIR="$DIR/.."
        done
        if [ ! -x "$DSE_HOME/bin/dse" ]; then
            echo "Cannot determine DSE_HOME."
            exit 1
        fi
    fi
fi

if [ -z "$DSE_CONF" ]; then
    for dir in "$DSE_HOME/resources/dse/conf" \
               "$DSE_HOME/conf" \
               "/etc/dse" \
               "/usr/share/dse" \
               "/usr/share/dse/conf" \
               "/usr/local/share/dse" \
               "/usr/local/share/dse/conf" \
               "/opt/dse/conf"; do
        if [ -r "$dir/dse.yaml" ]; then
            export DSE_CONF="$dir"
            break
        fi
    done
    if [ -z "$DSE_CONF" ]; then
        echo "Cannot determine DSE_CONF."
        exit 1
    fi
fi

#include the DSERC environment script (pulls in credentials for basic authentication)
if [ -r "$DSE_HOME/bin/dserc-env.sh" ]; then
    . "$DSE_HOME/bin/dserc-env.sh"
elif [ -r "$DSE_CONF/dserc-env.sh" ]; then
    . "$DSE_CONF/dserc-env.sh"
else
    echo "Location pointed by DSERC_ENV not readable: $DSERC_ENV"
    exit 1
fi


if [ -z "$DSE_LOG_ROOT" ]; then
    DSE_LOG_ROOT_DEFAULT="/Users/crupib/dse/logs"
    if [ -w "$DSE_HOME/logs" ]; then
        export DSE_LOG_ROOT="$DSE_HOME/logs"
    else
        export DSE_LOG_ROOT="$DSE_LOG_ROOT_DEFAULT"
    fi
fi

if [ -z "$DSE_LIB" ]; then
    for dir in "$DSE_HOME/build" \
               "$DSE_HOME/lib" \
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

if [ -z "$DSE_COMPONENTS_ROOT" ]; then
    for dir in $DSE_HOME/resources \
               $DSE_HOME \
               /usr/share/dse \
               /usr/local/share/dse \
               /opt/dse; do

        if [ -r "$dir/hadoop" ] && [ -r "$dir/hive" ] && [ -r "$dir/pig" ] && [ -r "$dir/solr" ]; then
            DSE_COMPONENTS_ROOT="$dir"
            break
        fi
    done
fi

#
# Add dse jars to the classpath
#
for dir in $DSE_LIB; do
    for jar in "$dir"/*.jar; do
        if [ -r "$jar" ]; then
            DSE_CLASSPATH="$DSE_CLASSPATH:$jar"
        fi
    done

    for jar in "$dir"/dse*.jar; do
        if [ -r "$jar" ]; then
            found_dse_jars="$found_dse_jars:$jar"
        fi
    done
done

export DSE_JARS="$found_dse_jars"

# check if there are jars from older/other DSE versions in the classpath
DSE_JARS_ERROR_CHECK=$(echo $DSE_JARS | \
    tr ':' '\n' | \
    ( while read jar; do basename "$jar" ; done ) | \
    awk -v dirs="$(echo "$DSE_LIB" | tr '\n' ' ' )" '
        BEGIN { old_dse = 0; dse_core = 0 }
        /^dse-[0-9]/ { old_dse++ }
        /^dse\.jar/ { old_dse++ }
        /^dse-core-[0-9]/ { dse_core++ }
        END {
            if (old_dse != 0) {
                printf "Found DSE jar from an older DSE version in %s. Please remove it.", dirs
            } else if (dse_core == 0) {
                printf "Found no DSE core jar files in %s. Please make sure there is one.", dirs
            } else if (dse_core > 1) {
                printf "Found multiple DSE core jar files in %s. Please make sure there is only one.", dirs
            }
        }
    ' )

if [ ! -z "$DSE_JARS_ERROR_CHECK" ]; then
    echo $DSE_JARS_ERROR_CHECK
    exit 1
fi

if [ -r $DSE_HOME/build/classes ]; then
    DSE_CLASSPATH="$DSE_HOME/build/classes:$DSE_CLASSPATH"
fi

if [ -r $DSE_HOME/build/classes/main ]; then
    DSE_CLASSPATH="$DSE_HOME/build/classes/main:$DSE_CLASSPATH"
fi

#
# Add dse conf
#
DSE_CLASSPATH=$DSE_CLASSPATH:$DSE_CONF

# Java System ClassLoader to be used by Hadoop, Pig, Hive and other Hadoop-related client apps of DSE.
export DSE_CLIENT_CLASSLOADER="com.datastax.bdp.loader.DseClientClassLoader"

export DSE_CLASSPATH=$(remove_duplicates "$DSE_CLASSPATH")

#########################################
# Setup Cassandra env
#########################################

export CASSANDRA_LOG_DIR="/Users/crupib/dse/logs/cassandra"

if [ -z "$CASSANDRA_HOME" -o ! -d "$CASSANDRA_HOME"/tools/lib ]; then
    for dir in $DSE_HOME/resources/cassandra \
               $DSE_HOME/cassandra \
               /usr/share/dse/cassandra \
               /usr/local/share/dse/cassandra \
               /opt/cassandra; do

        if [ -r "$dir" ]; then
            export CASSANDRA_HOME="$dir"
            # Resetting java agent... otherwise it's likely to point
            # to an invalid file
            export JAVA_AGENT=
            break
        fi
    done
    if [ -z "$CASSANDRA_HOME" ]; then
        echo "Cannot determine CASSANDRA_HOME."
        exit 1
    fi
fi

if [ -z "$CASSANDRA_BIN" -o ! -x "$CASSANDRA_BIN"/cassandra ]; then
    for dir in $CASSANDRA_HOME/bin /usr/bin /usr/sbin; do
        if [ -x "$dir/cassandra" ]; then
            export CASSANDRA_BIN="$dir"
            break
        fi
    done
    if [ -z "$CASSANDRA_BIN" ]; then
        echo "Cannot determine CASSANDRA_BIN."
        exit 1
    fi
fi

if [ -z "$CASSANDRA_CONF" -o ! -r "$CASSANDRA_CONF"/cassandra.yaml ]; then
    for dir in $CASSANDRA_HOME/conf \
               /etc/dse/cassandra \
               /etc/dse/ \
               /etc/cassandra; do
        if [ -r "$dir/cassandra.yaml" ]; then
            export CASSANDRA_CONF="$dir"
            break
        fi
    done
    if [ -z "$CASSANDRA_CONF" ]; then
        echo "Cannot determine CASSANDRA_CONF."
        exit 1
    fi
fi

if [ -z "$CASSANDRA_DRIVER_CLASSPATH" ]; then
    for jar in "$CASSANDRA_HOME"/../driver/lib/*.jar; do
        CASSANDRA_DRIVER_CLASSPATH="$CASSANDRA_DRIVER_CLASSPATH:$jar"
    done
fi

export CASSANDRA_DRIVER_CLASSPATH

if [ -z "$CASSANDRA_CLASSPATH" ]; then
    CASSANDRA_CLASSPATH="$CASSANDRA_CONF"
    CASSANDRA_CLASSPATH="$CASSANDRA_CLASSPATH:$CASSANDRA_HOME/tools/lib/stress.jar"
    FOUND_CASSANDRA_JAR=0
    for jar in "$CASSANDRA_HOME"/lib/*.jar; do
        if filematch "*/lib/cassandra-all-*" "$jar" ; then
            if [ "$FOUND_CASSANDRA_JAR" != "0" ]; then
                echo "Found multiple Cassandra jar files in $(dirname "$jar"). Please make sure there is only one."
                exit 1
            fi
            FOUND_CASSANDRA_JAR=1
        fi
        CASSANDRA_CLASSPATH="$CASSANDRA_CLASSPATH:$jar"
    done
    CASSANDRA_CLASSPATH="$CASSANDRA_CLASSPATH:$CASSANDRA_DRIVER_CLASSPATH"
    for dir in $DSE_LIB; do
        for jar in $dir/slf4j*; do
            if [ -r "$jar" ]; then
                CASSANDRA_CLASSPATH="$CASSANDRA_CLASSPATH:$jar"
            fi
        done
    done
fi

export CASSANDRA_CLASSPATH=$(remove_duplicates "$CASSANDRA_CLASSPATH")

# Set JAVA_AGENT option like we do in cassandra.in.sh
# as some tools (nodetool/dsetool) don't call that
if [ "$JVM_VENDOR" != "OpenJDK" -o "$JVM_VERSION" \> "1.6.0" ] \
      || [ "$JVM_VERSION" = "1.6.0" -a "$JVM_PATCH_VERSION" -ge 23 ]
then
    export JAVA_AGENT="$JAVA_AGENT -javaagent:$CASSANDRA_HOME/lib/jamm-0.3.0.jar"
fi


case "`uname`" in
    Linux)
        system_memory_in_mb=`free -m | awk '/:/ {print $2;exit}'`
        system_cpu_cores=`egrep -c 'processor([[:space:]]+):.*' /proc/cpuinfo`
    ;;
    FreeBSD)
        system_memory_in_bytes=`sysctl hw.physmem | awk '{print $2}'`
        system_memory_in_mb=`expr $system_memory_in_bytes / 1024 / 1024`
        system_cpu_cores=`sysctl hw.ncpu | awk '{print $2}'`
    ;;
    SunOS)
        system_memory_in_mb=`prtconf | awk '/Memory size:/ {print $3}'`
        system_cpu_cores=`psrinfo | wc -l`
    ;;
    Darwin)
        system_memory_in_bytes=`sysctl hw.memsize | awk '{print $2}'`
        system_memory_in_mb=`expr $system_memory_in_bytes / 1024 / 1024`
        system_cpu_cores=`sysctl hw.ncpu | awk '{print $2}'`
    ;;
    *)
        # assume reasonable defaults for e.g. a modern desktop or
        # cheap server
        system_memory_in_mb="2048"
        system_cpu_cores="2"
    ;;
esac

# Include DSE's custom configuration loader
export DSE_OPTS="$DSE_OPTS -Ddse.system_memory_in_mb=$system_memory_in_mb -Dcassandra.config.loader=com.datastax.bdp.config.DseConfigurationLoader"

#########################################
# Setup Hadoop env
#########################################

export HADOOP_HOME_WARN_SUPPRESS=true

if [ -z "$HADOOP_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export HADOOP_HOME="$DSE_COMPONENTS_ROOT/hadoop"
    else
        echo "Cannot determine HADOOP_HOME."
        exit 1
    fi
fi

if [ -z "$HADOOP_BIN" ]; then
    export HADOOP_BIN="$HADOOP_HOME/bin"
fi

if [ -z "$HADOOP_CONF_DIR" ]; then
    if [ -r "$HADOOP_HOME/conf" ]; then
        export HADOOP_CONF_DIR="$HADOOP_HOME/conf"
    elif [ -r "/etc/dse/hadoop" ]; then
        export HADOOP_CONF_DIR="/etc/dse/hadoop"
    else
        echo "Cannot determine HADOOP_CONF_DIR."
        exit 1
    fi
fi

if [ -z "$HADOOP_LOG_DIR" ]; then
    if [ -w "$DSE_LOG_ROOT" ] || [ -w "$DSE_LOG_ROOT/hadoop" ]; then
        export HADOOP_LOG_DIR="$DSE_LOG_ROOT/hadoop"
    else
        export HADOOP_LOG_DIR="$HOME/hadoop"
    fi
fi

if [ "$DSE_TOOL" != "1" ]; then
    #
    # Add hadoop native libs
    #
    JAVA_PLATFORM=`$HADOOP_BIN/hadoop org.apache.hadoop.util.PlatformName | sed -e 's/ /_/g'`
    export JAVA_LIBRARY_PATH="$JAVA_LIBRARY_PATH:$HADOOP_HOME/native/$JAVA_PLATFORM/lib"
    # hadoop-child-wrapper and task-controller
    export PATH="$PATH:$HADOOP_HOME/native/$JAVA_PLATFORM/bin"

    #
    # Optional for things like lzo compression libs
    #
    if [ -n "$OTHER_HADOOP_NATIVE_ROOT" ]; then
        for jar in "$OTHER_HADOOP_NATIVE_ROOT"/*.jar; do
            HADOOP_CLASSPATH="$HADOOP_CLASSPATH:$jar"
        done

        export JAVA_LIBRARY_PATH="$JAVA_LIBRARY_PATH:$OTHER_HADOOP_NATIVE_ROOT/lib/native/${JAVA_PLATFORM}/"
    fi
fi

# needed for webapps
HADOOP_CLASSPATH="$HADOOP_CLASSPATH:$HADOOP_HOME:$HADOOP_CONF_DIR"

for jar in "$HADOOP_HOME"/*.jar "$HADOOP_HOME"/lib/*.jar "$HADOOP_HOME"/lib/jsp-2.1/*.jar; do
    if [ -r "$jar" ]; then
        HADOOP_CLASSPATH="$HADOOP_CLASSPATH:$jar"
    fi
done

HADOOP_CLASSPATH="$DSE_CLASSPATH:$HADOOP_CLASSPATH:$CASSANDRA_DRIVER_CLASSPATH"

# We need to add antlr runtime to the Hadoop classpath because
# the initializer for CassandraFileSystemThriftStore ends up calling
# down into CFMetaData.compile which requires it.
# C* currently provides an earlier version of antlr than Hive, 
# which could cause conflicts as HIVE_CLASSPATH is a superset of
# HADOOP_CLASSPATH. 
# However, as we set
# HADOOP_CLASSPATH=$HIVE_CLASSPATH:$HADOOP_CLASSPATH in bin/dse
# the ordering should save us

for jar in $CASSANDRA_HOME/lib/antlr-runtime-*.jar; do
  HADOOP_CLASSPATH="$HADOOP_CLASSPATH:$jar"
done

export HADOOP_CLASSPATH=$(remove_duplicates "$HADOOP_CLASSPATH")

#########################################
# Setup Solr env
#########################################

if [ -z "$SOLR_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export SOLR_HOME="$DSE_COMPONENTS_ROOT/solr"
    else
        echo "Cannot determine SOLR_HOME."
        exit 1
    fi
fi

#
# Add solr jars
#
for jar in "$SOLR_HOME"/lib/*.jar; do
    SOLR_CLASSPATH="$SOLR_CLASSPATH:$jar"
done
SOLR_CLASSPATH="$SOLR_CLASSPATH:$SOLR_HOME/conf"

export SOLR_CLASSPATH=$(remove_duplicates "$SOLR_CLASSPATH")

#only set these things when starting cassandra
if [ "$DSE_CMD" = "cassandra" -o "$(basename $0)" = "sstablescrub" ]; then

    #
    # Initialize Tomcat env
    #
    if [ -z "$TOMCAT_HOME" ]; then
        if [ -r "$DSE_COMPONENTS_ROOT/tomcat" ]; then
            export TOMCAT_HOME="$DSE_COMPONENTS_ROOT/tomcat"
        else
            echo "Cannot determine TOMCAT_HOME."
            exit 1
        fi
    fi

    if [ -z "$TOMCAT_CONF_DIR" ]; then
        if [ -r "$TOMCAT_HOME/conf" ]; then
            export TOMCAT_CONF_DIR="$TOMCAT_HOME/conf"
        elif [ -r "/etc/dse/tomcat" ]; then
            export TOMCAT_CONF_DIR="/etc/dse/tomcat"
        else
            echo "Cannot determine TOMCAT_CONF_DIR."
            exit 1
        fi
    fi

    if [ -z "$TOMCAT_LOGS" ]; then
        if [ -w "$DSE_LOG_ROOT/tomcat" ]; then
            export TOMCAT_LOGS="$DSE_LOG_ROOT/tomcat"
        else
            export TOMCAT_LOGS="$HOME/tomcat"
        fi
    fi

    export TOMCAT_BIN="$TOMCAT_HOME/bin"
    export CATALINA_BASE="$TOMCAT_HOME"
    export CATALINA_HOME="$CATALINA_BASE"

    #
    # Add Tomcat jars
    #
    for jar in "$TOMCAT_HOME/lib"/*.jar; do
        TOMCAT_CLASSPATH="$TOMCAT_CLASSPATH:$jar"
    done
    TOMCAT_CLASSPATH="$TOMCAT_CLASSPATH:$TOMCAT_CONF_DIR"

    export TOMCAT_CLASSPATH=$(remove_duplicates "$TOMCAT_CLASSPATH")

fi

#########################################
# Setup Pig env
#########################################

if [ -z "$PIG_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export PIG_HOME="$DSE_COMPONENTS_ROOT/pig"
    else
        echo "Cannot determine PIG_HOME."
        exit 1
    fi
fi

if [ -z "$PIG_CONF_DIR" ]; then
    if [ -r "$PIG_HOME/conf" ]; then
        export PIG_CONF_DIR="$PIG_HOME/conf"
    elif [ -r "/etc/dse/pig" ]; then
        export PIG_CONF_DIR="/etc/dse/pig"
    else
        echo "Cannot determine PIG_CONF_DIR."
        exit 1
    fi
fi

if [ -z "$PIG_LOG_ROOT" ]; then
    if [ -w "$DSE_LOG_ROOT" ] || [ -w "$DSE_LOG_ROOT/pig" ]; then
        export PIG_LOG_ROOT="$DSE_LOG_ROOT/pig"
    else
        export PIG_LOG_ROOT="$HOME/pig"
    fi
fi

for jar in "$PIG_HOME"/lib/*.jar; do
    PIG_CLASSPATH="$PIG_CLASSPATH:$jar"
done

PIG_CLASSPATH="$PIG_CLASSPATH:$PIG_HOME/autocomplete"

export PIG_CLASSPATH=$(remove_duplicates "$PIG_CLASSPATH")

#########################################
# Setup Hive env
#########################################

if [ -z "$HIVE_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export HIVE_HOME="$DSE_COMPONENTS_ROOT/hive"
    else
        echo "Cannot determine HIVE_HOME."
        exit 1
    fi
fi

export HIVE_BIN="$HIVE_HOME/bin"

if [ -z "$HIVE_CONF_DIR" ]; then
    if [ -r "$HIVE_HOME/conf" ]; then
        export HIVE_CONF_DIR="$HIVE_HOME/conf"
    elif [ -r "/etc/dse/hive" ]; then
        export HIVE_CONF_DIR="/etc/dse/hive"
    else
        echo "Cannot determine HIVE_CONF_DIR."
        exit 1
    fi
fi

if [ -z "$HIVE_LOG_ROOT" ]; then
    if [ -w "$DSE_LOG_ROOT" ] || [ -w "$DSE_LOG_ROOT/hive" ]; then
        export HIVE_LOG_ROOT="$DSE_LOG_ROOT"
    else
        export HIVE_LOG_ROOT="$HOME"
    fi
fi

HIVE_CLASSPATH="$HIVE_CLASSPATH:$HIVE_CONF_DIR"

for jar in "$HIVE_HOME"/lib/*.jar; do
    HIVE_CLASSPATH="$HIVE_CLASSPATH:$jar"
done

export HIVE_CLASSPATH=$(remove_duplicates "$HIVE_CLASSPATH")

#########################################
# Setup Sqoop env
#########################################

if [ -z "$SQOOP_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export SQOOP_HOME="$DSE_COMPONENTS_ROOT/sqoop"
    else
        echo "Cannot determine SQOOP_HOME."
        exit 1
    fi
fi

if [ -z "$SQOOP_CONF_DIR" ]; then
    if [ -r "$SQOOP_HOME/conf" ]; then
        export SQOOP_CONF_DIR="$SQOOP_HOME/conf"
    elif [ -r "/etc/dse/sqoop" ]; then
        export SQOOP_CONF_DIR="/etc/dse/sqoop"
    else
        echo "Cannot determine SQOOP_CONF_DIR."
        exit 1
    fi
fi

export SQOOP_USER_CLASSPATH="$SQOOP_CONF_DIR:$HIVE_CONF_DIR:$CASSANDRA_CONF"

# keeps Sqoop from complaining about HBase imports failing
export HBASE_HOME=/tmp


#########################################
# Setup Mahout env
#########################################

if [ -z "$MAHOUT_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export MAHOUT_HOME="$DSE_COMPONENTS_ROOT/mahout"
    else
        echo "Cannot determine MAHOUT_HOME."
        exit 1
    fi
fi

if [ -z "$MAHOUT_CONF_DIR" ]; then
    if [ -r "$MAHOUT_HOME/conf" ]; then
        export MAHOUT_CONF_DIR="$MAHOUT_HOME/conf"
    elif [ -r "/etc/dse/mahout" ]; then
        export MAHOUT_CONF_DIR="/etc/dse/mahout"
    else
        echo "Cannot determine MAHOUT_CONF_DIR."
        exit 1
    fi
fi

export MAHOUT_BIN="$MAHOUT_HOME/bin"

for jar in "$MAHOUT_HOME"/*.jar; do
    if filematch "*/mahout-examples-*" "$jar" ; then
        continue
    fi
    MAHOUT_CLASSPATH="$MAHOUT_CLASSPATH:$jar"
done

export MAHOUT_CLASSPATH=$(remove_duplicates "$MAHOUT_CLASSPATH")

#########################################
# Setup Spark env
#########################################

if [ -z "$SPARK_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export SPARK_HOME="$DSE_COMPONENTS_ROOT/spark"
    else
        echo "Cannot determine SPARK_HOME."
        exit 1
    fi
fi

if [ -z "$SPARK_BIN" ]; then
    export SPARK_BIN="$SPARK_HOME/bin"
fi

if [ -z "$SPARK_SBIN" ]; then
    export SPARK_SBIN="$SPARK_HOME/sbin"
fi

if [ -z "$SPARK_CONF_DIR" ]; then
    if [ -r "$SPARK_HOME/conf" ]; then
        export SPARK_CONF_DIR="$SPARK_HOME/conf"
    elif [ -r "/etc/dse/spark" ]; then
        export SPARK_CONF_DIR="/etc/dse/spark"
    else
        echo "Cannot determine SPARK_CONF_DIR."
        exit 1
    fi
fi

if [ -z "$SPARK_JOBSERVER_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export SPARK_JOBSERVER_HOME="$DSE_COMPONENTS_ROOT/spark/spark-jobserver"
    else
        echo "Cannot determine SPARK_JOBSERVER_HOME."
        exit 1
    fi
fi


if [ -z "$SCALA_HOME" ]; then
    if [ -n "$DSE_COMPONENTS_ROOT" ]; then
        export SCALA_HOME="$DSE_COMPONENTS_ROOT/scala"
    else
        echo "Cannot determine SCALA_HOME."
        exit 1
    fi
fi

SPARK_LIB_CLASSPATH="$SPARK_LIB_CLASSPATH:$SPARK_CONF_DIR"
for jar in "$SPARK_HOME"/lib/*.jar; do
    SPARK_LIB_CLASSPATH="$SPARK_LIB_CLASSPATH:$jar"
done

export SPARK_LIB_CLASSPATH=$(remove_duplicates "$SPARK_LIB_CLASSPATH")

#########################################
# Add all components classpaths
# to global CLASSPATH
#########################################

export CLASSPATH="$DSE_CLASSPATH:$CASSANDRA_CLASSPATH:$SOLR_CLASSPATH:$TOMCAT_CLASSPATH:$HADOOP_CLASSPATH:$PIG_CLASSPATH:$HIVE_CLASSPATH:$SPARK_LIB_CLASSPATH"
