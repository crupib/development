#!/bin/bash

MAVEN_REPOSITORY="http://central.maven.org/maven2"
JTS_VERSION=1.13
JTS_ARTIFACT_URL="${MAVEN_REPOSITORY}/com/vividsolutions/jts/${JTS_VERSION}/jts-${JTS_VERSION}.jar"

echo "Downloading jts-${JTS_VERSION}.jar ..."
curl ${JTS_ARTIFACT_URL} -o jts.jar &> /dev/null
echo "Downloaded"
mv jts.jar lib/
echo "Installation completed, (re)start DSE process"

exit 0