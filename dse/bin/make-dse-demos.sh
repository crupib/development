#!/bin/sh

echo This script will create a directory called ./dse-demos to run
echo your DSE demos in. Please do not attempt to run the demos directly
echo from the installation directory, as the demos need write permission
echo in their respective working directories.

if [ -z "$DSE_ENV" ]; then
    for include in "~/.dse-env.sh" \
                   "`dirname $0`/dse-env.sh" \
                   "`dirname $0`/../../../bin/dse-env.sh" \
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

if [ "$1" != "" ]; then
  DEMO_SOURCE=$1
elif [ "$DSE_HOME" != "" -a -d "$DSE_HOME/../dse-demos" ]; then
  abspath=$(cd "$DSE_HOME/../dse-demos" && pwd -P)
  DEMO_SOURCE="$abspath"
elif [ "$DSE_HOME" != "" -a -d "$DSE_HOME/demos" ]; then
  DEMO_SOURCE="$DSE_HOME/demos"
elif [ -d /usr/share/dse-demos ]; then
  DEMO_SOURCE=/usr/share/dse-demos
elif [ -d /usr/share/dse/demos ]; then
  DEMO_SOURCE=/usr/share/dse/demos
else
  echo Unable to locate the installed dse-demo direcotry. Please
  echo pass the directory name in as the first argument.
  exit 1
fi
echo DEMO_SOURCE=$DEMO_SOURCE

if [ "$2" != "" ]; then
  DSE_SOURCE=$2
elif [ "$DSE_HOME" != "" -a -d "$DSE_HOME/cassandra" ]; then
  DSE_SOURCE="$DSE_HOME"
elif [ "$DSE_HOME" != "" -a -d "$DSE_HOME/resources" ]; then
  DSE_SOURCE="$DSE_HOME"
elif [ -d /usr/share/dse ]; then
  DSE_SOURCE=/usr/share/dse
else
  echo Unable to locate the installed dse direcotry. Please
  echo pass the directory name in as the second argument. You will
  echo have to specify the dse-demos folder as first argument
  echo then the dse folder as second argument.
  exit 1
fi
echo DSE_SOURCE=$DSE_SOURCE

if [ -x `pwd`/dse-demos ]; then
  echo Local `pwd`/dse-demos folder already exists. Please remove 
  echo or link seomwhere else.
  exit 1
fi
DEMO_TARGET=`pwd`/dse-demos

echo Copying $DEMO_SOURCE to $DEMO_TARGET
mkdir -p "$DEMO_TARGET"
cd "$DEMO_SOURCE"
cp -r * "$DEMO_TARGET"

# some touchup work t ensure people can use the local copies properly
echo ant -Ddse.base=\"$DSE_SOURCE\" > "$DEMO_TARGET/solr_stress/rebuild_jar.sh"
chmod a+x "$DEMO_TARGET/solr_stress/rebuild_jar.sh"
echo ant -Ddse.base=\"$DSE_SOURCE\" > "$DEMO_TARGET/portfolio_manager/rebuild_jar.sh"
chmod a+x "$DEMO_TARGET/portfolio_manager/rebuild_jar.sh"

if [ ! -d "$DSE_SOURCE/resources" ]; then
  mv "$DEMO_TARGET/portfolio_manager/build.xml" "$DEMO_TARGET/portfolio_manager/build.xml.orig"
  sed -e 's/resources\/cassandra/cassandra/g' -e 's/base}\/lib/base}/g' -e 's/base}\/resources\/dse\/lib/base}/g' "$DEMO_TARGET/portfolio_manager/build.xml.orig" > "$DEMO_TARGET/portfolio_manager/build.xml"
fi

echo Done

