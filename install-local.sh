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
else
  echo "No existing installation"
fi

# make some checks on $DEVELOP to make sure things are where we expect them
echo "CHECK ALL DEVELOPMENT ELEMENTS ARE AVAILABLE"
cd $DEVELOP
if [ ! -d asbestos ]
then
  echo "$DEVELOP/asbestos does not exist"
  exit -1
else
  echo "$DEVELOP/asbestos exists"
fi

if [ ! -f $DEVELOP/asbestos/asbestos-assembly/target/asbestos.zip ]
then
  echo "$DEVELOP/asbestos/asbestos-assembly/target/asbestos.zip does not exist"
  exit -1
else
    echo "$DEVELOP/asbestos/asbestos-assembly/target/asbestos.zip exists"
fi

if [ ! -d toolkit2 ]
then
  echo "$DEVELOP/toolkit2 does not exist"
  exit -1
else
  echo "$DEVELOP/toolkit2 exists"
fi

if [ ! -f $DEVELOP/toolkit2/xdstools2/target/xdstools*.war ]
then
  echo "$DEVELOP/toolkit2/xdstools2/target/xdstools*.war does not exist"
  exit -1
else
  echo "$DEVELOP/toolkit2/xdstools2/target/xdstools*.war exists"
fi

if [ ! -f fhir.zip ]
then
  echo "$DEVELOP/fhir.zip does not exist"
else
  echo "$DEVELOP/fhir.zip exists"
fi

echo "COPY ASBESTOS.ZIP"
mkdir $INSTALL
cp $DEVELOP/asbestos/asbestos-assembly/target/asbestos.zip $INSTALL

echo "EXPAND ASBESTOS.ZIP"
cd $INSTALL
unzip -qq asbestos.zip

echo "REMOVE ASBESTOS.ZIP"
rm asbestos.zip

echo "BUILD FHIRTOOLKIT/TMP"
mkdir $INSTALL/tomcat/Toolkits/FhirToolkit/temp
echo "Place Holder" > $INSTALL/tomcat/Toolkits/FhirToolkit/temp/placeholder.txt

echo "INSTALL XDSTOOLS"
cp $DEVELOP/toolkit2/xdstools2/target/xdstools*.war $INSTALL/tomcat/Toolkits/XdsToolkit/webapps/xdstools.war

echo "EXPAND XDSTOOLS"
cd $INSTALL/tomcat/Toolkits/XdsToolkit/webapps
mkdir xdstools
cd xdstools
unzip -qq ../xdstools.war

echo "REMOVE XDSTOOLS.WAR"
rm -f ../xdstools.war

#echo "INSTALL PRODUCTION TOOLKIT.PROPERTIES"
# XdsToolkit/conf/toolkit.properties is the default
# cp $DEVELOP/asbestos/asbestos-assembly/src/main/assembly/toolkit.properties $INSTALL/tomcat/Toolkits/XdsToolkit/webapps/xdstools/WEB-INF/classes

# v2.2.0 require HAPI FHIR v5.4.0 ROOT war in the HapiFhir Tomcat base

# Enable this only for pre-v2.2.0 releases
#echo "INSTALL HAPI"
#cp $DEVELOP/fhir.zip $INSTALL/tomcat/Toolkits/XdsToolkit/webapps
#cd $INSTALL/tomcat/Toolkits/XdsToolkit/webapps
#unzip -qq fhir.zip
#echo "REMOVE FHIR.ZIP"
#rm -f fhir.zip
echo "DONE"

