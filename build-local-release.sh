#!/bin/bash

# build asbestos then asbestos-assembly
# then install in /opt/asbestos along with xdstools and hapi

INSTALL=/opt/asbestos

echo "CHECK FOR EXISTING INSTALLATION"
if [ -d "$INSTALL" ]
then
  echo "$INSTALL already exists"
  exit -1
fi

echo "BUILD ASBESTOS"
mvn clean
if [ -d asbestos-war/target ]; then
  echo "asbestos clean failed"
fi

echo ""
echo ''
echo "PACKAGE asbestos-war - this will take a while"
echo ''
echo ""

mkdir asbestos-war/target
mvn package &> asbestos-war/build-log.txt   # does not run integration tests, there is more environment setup required for that
if [ ! -f asbestos-war/target/asbestos-war.war ]; then
  echo "asbestos build failed"
  exit -1
fi

echo "BUILD ASBESTOS-ASSEMBLY"
cd asbestos-assembly
mvn clean
if [ -d target ]; then
  echo "asbestos-assembly clean failed"
  exit -1
fi

mkdir target
mvn package &> target/build-log.txt
mvn package
if [ ! -f target/asbestos.zip ]; then
  echo "build asbestos-assembly failed"
  exit -1
fi

echo "ALL BUILDS PASSED"

echo "INSTALL LOCALLY"
cd ..   # back to asbestos
./install-local.sh

echo "*********************************************************"
echo "DONE"
echo "*********************************************************"
