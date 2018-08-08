#!/bin/sh

./create-schema.sh -x schema_geo.xml -t create_table_geo_rt.cql -r solrconfig-rt.xml -k demo.geort $*