#!/bin/bash

SOLR_URL="http://dse-perf-02:8983"
CORE_NAME="demo.geort"
CORE_ENDPOINT="$SOLR_URL/solr/$CORE_NAME"
COMMIT_BEFORE_SHUTDOWN=true

ITERATIONS=10
DOC_COUNT=1000000

function count_docs {
	count=`curl -s "$CORE_ENDPOINT/select?q=*:*&rows=0" | grep numFound | awk -F ' ' '{print $9}' | awk -F '"' '{print $2}'`
	echo "Total commited documents: $count"
}

function delete_index {
	local delete_duration=`curl -s "$CORE_ENDPOINT/update" --data '<delete><query>*:*</query></delete>' -H 'Content-type:text/xml; charset=utf-8' | grep 'QTime' | awk -F '>' '{print $5}' | awk -F '<' '{print $1}'`
	echo "Index deleted, took $delete_duration ms"
}

for (( i = 1; i < ITERATIONS+1; i++ )); do
	echo "==== Starting benchmark ($i/$ITERATIONS) ===="
	echo "Deleting $CORE_NAME index..."
	delete_index
	echo "Indexing started..."
	./run-geo.sh allCountries.txt ${SOLR_URL} 20 ${DOC_COUNT} ${CORE_NAME} ${COMMIT_BEFORE_SHUTDOWN} > "run-geo-$i.log"
	benchmark_duration=`tail -n 20 "run-geo-$i.log" | grep '^Duration=' | awk -F '=' '{print $2}'`
	echo "Indexing finished, took $benchmark_duration ms"
	count_docs
done

delete_index

exit 0
