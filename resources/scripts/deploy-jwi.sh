#!/bin/bash

VERS=2.4.0
GROUP_ID=edu.mit
ARTIFACT_ID=jwi
ZIP_FILE=${GROUP_ID}.${ARTIFACT_ID}_${VERS}_src.zip
DOWNLOAD_DIR=/tmp
FULLPATH=${DOWNLOAD_DIR}/${ZIP_FILE}
PROJ_DIR=resources/jwi
SRC_PATH=${PROJ_DIR}/src/java

# Download
URL=https://projects.csail.mit.edu/jwi/download.php?f=${ZIP_FILE}
curl --silent -o $FULLPATH "${URL}"

# Unzip
mkdir -p $SRC_PATH
unzip $FULLPATH -d $SRC_PATH

# Build and deploy JAR
cd $PROJ_DIR && \
    lein jar && \
    lein deploy clojars
