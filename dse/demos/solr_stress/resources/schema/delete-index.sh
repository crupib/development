#!/bin/bash

HOST="localhost"
PORT=8983
CORE_NAME="demo.solr"

FORCE=0

# SCHEME  - (http|https)
# CERT_FILE - client certificate file
# AUTH_OPTS - Additional security options for curl

cd `dirname $0`
. ./set-solr-options.sh $*

usage() {
   echo
   echo "Usage: ./delete_index.sh [-p PORT] [-h HOST] [-c CORE-NAME] [-f force delete]"
   echo
}

delete_index() {
  URL="$SCHEME://$HOST:$PORT/solr/$CORE_NAME/update"
  echo
  echo "Deleting indices from core '$CORE_NAME' at $SCHEME://$HOST:$PORT/solr"
  echo "URL: $URL"
  echo
  curl -s $AUTH_OPTS $CLIENT_CERT_FILE $CERT_FILE $URL --data '<delete><query>*:*</query></delete>' -H 'Content-type:text/xml; charset=utf-8'
  curl -s $AUTH_OPTS $CLIENT_CERT_FILE $CERT_FILE $URL --data '<commit/>' -H 'Content-type:text/xml; charset=utf-8'
}


# Read options
while getopts ":ufp:h:c:" opt; do
  case $opt in
    u) usage
       exit 0
      ;;
    f) FORCE=1
      ;;
    h)
      HOST=$OPTARG
      ;;
    p)
      PORT=$OPTARG
      ;;
    c)
      CORE_NAME=$OPTARG
      ;;
  esac
done

if [ "$FORCE" = "0" ]; then
   while true; do
      usage
      read -p "Do you wish to delete indices from core '$CORE_NAME' at $SCHEME://$HOST:$PORT/solr [y/n]? " yn
      case $yn in
          [Yy]* ) break;;
          [Nn]* ) exit;;
          * ) echo "Please answer yes or no.";;
      esac
   done
fi

delete_index

