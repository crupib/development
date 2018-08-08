#!/bin/bash 

cd `dirname $0`

DSE="/usr/bin/dse"
if [ -z "$DSE_HOME" ]; then
    if [ -e "$DSE" ]; then
        export DSE_HOME=/usr/share/dse
    else
        abspath=$(cd "$(dirname "$0")" && pwd -P)
        export DSE_HOME=`dirname "$abspath"`/..
    fi
fi
if [ ! -e "$DSE" ]; then
    if [ -e "$DSE_HOME"/bin/dse ]; then
        DSE="$DSE_HOME"/bin/dse
    else
      echo "Cannot determine DSE_HOME, please set it manually to your DSE install directory"
      exit 1

    fi
fi

SYNTH_DATA_FILE="`dirname "$0"`/synthetic_control.data"

echo "Copying sample data to CFS"
$DSE hadoop fs -mkdir testdata
$DSE hadoop fs -put "$SYNTH_DATA_FILE" testdata

echo "Submitting Mahout job"
$DSE mahout org.apache.mahout.clustering.syntheticcontrol.canopy.Job

echo "Dumping results to outputfile"
$DSE mahout clusterdump --input output/clusters-0-final --pointsDir output/clusteredPoints --output /tmp/clusteranalyze.txt

echo "Output in /tmp/clusteranalyze.txt"


