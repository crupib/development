#!/bin/sh

if [ -n "$2" ]; then
for PARAM in "$@"; do
   if [ "$PARAM" = "$1" ]; then
      continue
   fi
done
fi

#include the DSERC environment script (pulls in credentials for basic authentication)

if [ -z "$DSERC_ENV" ]; then
    for include in "$HOME/.dserc-env.sh" \
                   "`dirname "$0"`/dserc-env.sh" \
                   "/etc/dse/dserc-env.sh"; do
        if [ -r "$include" ]; then
            DSERC_ENV="$include"
            break
        fi
    done
fi

if [ -z "$DSERC_ENV" ]; then
    echo "DSERC_ENV could not be determined."
    exit 1
elif [ -r "$DSERC_ENV" ]; then
    . "$DSERC_ENV"
else
    echo "Location pointed by DSERC_ENV not readable: $DSERC_ENV"
    exit 1
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

# Java System ClassLoader to be used by Hadoop, Pig, Hive and other Hadoop-related client apps of DSE.
export DSE_CLIENT_CLASSLOADER="com.datastax.bdp.loader.DseClientClassLoader"

# Helper functions
filematch () { case "$2" in $1) return 0 ;; *) return 1 ;; esac ; }

# Set JAVA_AGENT option like we do in cassandra.in.sh
# as some tools (nodetool/dsetool) don't call that
if [ "$JVM_VENDOR" != "OpenJDK" -o "$JVM_VERSION" \> "1.6.0" ] \
      || [ "$JVM_VERSION" = "1.6.0" -a "$JVM_PATCH_VERSION" -ge 23 ]
then
    export JAVA_AGENT="$JAVA_AGENT -javaagent:$CASSANDRA_HOME/lib/jamm-0.3.0.jar"
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
