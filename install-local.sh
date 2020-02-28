#!/bin/bash

# asbestos and asbestos-assembly have been built
# now install asbestos-assembly in /opt and
# add xdstoolkit and
# add hapi fhir

# this assumes that the directory /opt/asbestos has already been removed

DEVELOP=`pwd`/..    # develop directory where asbestos and xdstoolkit live
INSTALL=/opt/asbestos

echo "CHECK FOR EXISTING INSTALLATION"
if [ -d $INSTALL ]
then
  echo "$INSTALL already exists"
  exit -1
fi

# make some checks on $DEVELOP to make sure things are where we expect them
echo "CHECK ALL DEVELOPMENT ELEMENTS ARE AVAILABLE"
cd $DEVELOP
if [ ! -d asbestos ] 
then
  echo "$DEVELOP/asbestos does not exist"
  exit -1  
fi

if [ ! -d toolkit2 ]
then
  echo "$DEVELOP/toolkit2 does not exist"
  exit -1
fi

if [ ! -f fhir.zip ]
then
  echo "$DEVELOP/fhir.zip does not exist"
fi

echo "COPY ASBESTOS.ZIP"
mkdir $INSTALL
cp $DEVELOP/asbestos/asbestos-assembly/target/asbestos.zip $INSTALL

echo "EXPAND ASBESTOS.ZIP"
cd $INSTALL
unzip -qq asbestos.zip

echo "REMOVE ASBESTOS.ZIP"
rm asbestos.zip

echo "INSTALL XDSTOOLS"
cp $DEVELOP/toolkit2/xdstools2/target/xdstools*.war $INSTALL/tomcat/Toolkits/XdsToolkit/webapps/xdstools.war
echo "INSTALL FHIRTOOLS"
cp $DEVELOP/fhir.zip $INSTALL/tomcat/Toolkits/XdsToolkit/webapps

echo "INSTALL HAPI"
cd $INSTALL/tomcat/Toolkits/XdsToolkit/webapps
unzip -qq fhir.zip
echo "REMOVE FHIR.ZIP"
rm fhir.zip

