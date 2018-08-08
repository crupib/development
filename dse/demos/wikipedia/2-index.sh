#!/bin/bash 

cd `dirname $0`

if [ -z "$DSE_ENV" ]; then
    for include in "~/.dse-env.sh" \
                   "`dirname $0`/dse-env.sh" \
                   "`dirname $0`/../../bin/dse-env.sh" \
                   "/etc/dse/dse-env.sh"; do
        if [ -r "$include" ]; then
            DSE_ENV="$include"
            break
        fi
    done
fi

if [ -z "$DSE_ENV" ]; then
    echo "DSE_ENV could not be determined."
    exit 1
elif [ -r "$DSE_ENV" ]; then
    . "$DSE_ENV"
else
    echo "Location pointed by DSE_ENV not readable: $DSE_ENV"
    exit 1
fi

CLASSPATH="wikipedia_import.jar:$CLASSPATH"

# Need Windows-style paths under cygwin
case "`uname`" in
    CYGWIN*)
        CLASSPATH=`cygpath -p -w "$CLASSPATH"`
    ;;
esac

USER=""
PASS=""
ARGS=()
while true 
do
  if [ ! $1 ]; then break; fi
  if [ $1 = "--wikifile" ] 
  then
    WIKIFILE=$2
    shift
  elif [ $1 = "--limit" ]
  then
    LIMIT="--limit $2"
    shift
  elif [ $1 = "-u" ]
  then
    USER="--user $2"
    shift
  elif [ $1 = "-p" ]
  then
    PASS="--password $2"
    shift
  else
    ARGS="$ARGS $1"
  fi;
  shift
done

if [ "$WIKIFILE" = "" ]; then
  echo No wiki sample file specified, defaulting to local wikipedia-sample.bz2.
  WIKIFILE=wikipedia-sample.bz2
fi

if [ ! -r "$WIKIFILE" ]; then
  echo Unable to read sample data file: $WIKIFILE. Please specufy one with --wikifile.
  exit 1
fi

# Set authentication & encryption options 
. ./set-solr-options.sh $ARGS

java $JVM_OPTS $DSE_OPTS -Dlogback.configurationFile=logback-tools.xml $JAVA_AGENT -ea -Xmx1G -Xms256M -cp "$CLASSPATH" com.datastax.dse.demos.solr.Wikipedia --host $HOST --scheme $SCHEME --wikifile $WIKIFILE $LIMIT $USER $PASS

echo Visit http://localhost:8983/demos/wikipedia/ to see data

