#!/bin/bash

VERS=2.4.0
GROUP_ID=edu.mit
ARTIFACT_ID=jwi
JAR_FILE=${GROUP_ID}.${ARTIFACT_ID}_${VERS}.jar
DOWNLOAD_DIR=/tmp
FULLPATH=${DOWNLOAD_DIR}/${JAR_FILE}

# Download
URL=https://projects.csail.mit.edu/jwi/download.php?f=${JAR_FILE}
curl --silent  -o $FULLPATH "${URL}"

# Install
mvn install:install-file \
  -Dfile=$FULLPATH \
  -DgroupId=$GROUP_ID \
  -DartifactId=$ARTIFACT_ID \
  -Dversion=$VERS \
  -Dpackaging=jar

# Clean-up
rm $FULLPATH
