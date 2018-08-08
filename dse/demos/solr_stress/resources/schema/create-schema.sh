#!/bin/bash

if [ "$#" -eq "0" ]
  then
    echo "******************************************************************************************************************************************"
    echo "Using default values schema.xml, solrconfig.xml, create_table.cql and Solr core: demo.solr"
    echo "You can override defaults by specifying: -t tableCreationFile.cql -x solrSchema.xml -r solrConfig.xml -k solrCore after any Solr options"
    echo "******************************************************************************************************************************************"
fi

CREATE_TABLE_FILE="create_table.cql"
SOLRCONFIG="solrconfig.xml"
SCHEMA="schema.xml"
SOLR_CORE="demo.solr"

#Read options
while getopts ":t:r:x:k:ae:h:ku:p:E: " opt; do
  case $opt in
    t)
      if [ ! -f $OPTARG ]
        then
          echo "The specified file $OPTARG does not exist"
          exit
      fi
      CREATE_TABLE_FILE=$OPTARG
      ;;
    r)
      if [ ! -f $OPTARG ]
        then
          echo "The specified file $OPTARG does not exist"
          exit
      fi
      SOLRCONFIG=$OPTARG
      ;;
    x)
      if [ ! -f $OPTARG ]
        then
          echo "The specified file $OPTARG does not exist"
          exit
      fi
      SCHEMA=$OPTARG
      ;;
    k)
      SOLR_CORE=$OPTARG
      ;;
    :)
      "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
    \?)
      ## ignore unknown options
      ;;
  esac
done

#Reset the opts processor
OPTIND=1
export OPTIND

# options parser sets the following variables:
# HOST - server hostname
# SCHEME  - (http|https)
# CERT_FILE - client certificate file
# AUTH_OPTS - Additional security options for curl
cd `dirname $0`
. ./set-solr-options.sh $*

echo "Creating Cassandra table from $CREATE_TABLE_FILE..."
if [ -x ../../../../bin/cqlsh ]; then
  CQLSH_BIN=../../../../bin/cqlsh
elif [ -x /usr/bin/cqlsh ]; then
  CQLSH_BIN=/usr/bin/cqlsh
else
  CQLSH_BIN=cqlsh
fi

if [[ $* == *--ssl* ]]; then
  "$CQLSH_BIN" `hostname -f` --ssl <$CREATE_TABLE_FILE
else
  "$CQLSH_BIN" `hostname -f` <$CREATE_TABLE_FILE
fi

SOLRCONFIG_URL="$SCHEME://$HOST:8983/solr/resource/$SOLR_CORE/solrconfig.xml"

echo "Posting $SOLRCONFIG to $SOLRCONFIG_URL..."
curl -s $AUTH_OPTS $CLIENT_CERT_FILE $CERT_FILE --data-binary @$SOLRCONFIG -H 'Content-type:text/xml; charset=utf-8' $SOLRCONFIG_URL
echo "Posted $SOLRCONFIG to $SOLRCONFIG_URL"

SCHEMA_URL="$SCHEME://$HOST:8983/solr/resource/$SOLR_CORE/schema.xml"

echo "Posting $SCHEMA to $SCHEMA_URL..."
curl -s $AUTH_OPTS $CLIENT_CERT_FILE $CERT_FILE --data-binary @$SCHEMA -H 'Content-type:text/xml; charset=utf-8' $SCHEMA_URL
echo "Posted $SCHEMA to $SCHEMA_URL"

CREATE_URL="$SCHEME://$HOST:8983/solr/admin/cores?action=CREATE&name=$SOLR_CORE"

echo "Creating index..."
curl -s $AUTH_OPTS $CLIENT_CERT_FILE $CERT_FILE  -X POST -H 'Content-type:text/xml; charset=utf-8' $CREATE_URL 
echo "Created index."
