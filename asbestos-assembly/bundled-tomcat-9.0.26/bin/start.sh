#!/bin/sh

# Startup script for FHIR Toolkit
# There are two CATALINA_BASE directories: Toolkits/XdsToolkit and Toolkits/FhirToolkit.
# Toolkits/XdsToolkit may contain the HAPI FHIR server and/or XDS Toolkit. If either are present
# (if Toolkits/XdsToolkit/webapps/ is not empty) then this CATALINA_BASE is started.
# Next Toolkits/FhirToolkit is started.  This contains the FHIR Toolkit components.

# NOTE: HAPI FHIR and XDS Toolkit must be started before FhirToolkit.

# directory this script resides in
# most references are relative to this
SCRIPT=$(readlink -f "$0")
BASEDIR=$(dirname "$SCRIPT")
echo "BASEDIR is $BASEDIR"

TOOLKITS=$BASEDIR/../Toolkits
FHIRTOOLKIT=$TOOLKITS/FhirToolkit
XDSTOOLKIT=$TOOLKITS/XdsToolkit
XDSWEBAPPS=$XDSTOOLKIT/webapps

export CATALINA_HOME=$BASEDIR/..
echo "CATALINA_HOME is $CATALINA_HOME"

echo "Looking at $XDSWEBAPPS"
WEBAPPSCOUNT=`find $XDSWEBAPPS -maxdepth 1 \( -type d -o -name *.war \) -printf x | wc -c`

# start XdsToolkit base if its webapps dir is not empty
echo "count is $WEBAPPSCOUNT"

# account for find counting base directory
if [ $WEBAPPSCOUNT -gt 1 ]
then
	echo "XdsToolkit should be started"
	mkdir $XDSTOOLKIT/logs
	export CATALINA_BASE=$XDSTOOLKIT
	echo "CATALINA_BASE=$CATALINA_BASE"
	./startup.sh
else
	echo "XdsToolkit should not be started"
fi

# start FhirToolkit

echo "Starting FhirToolkit"
mkdir $FHIRTOOLKIT/logs
export CATALINA_BASE=$FHIRTOOLKIT
echo "CATALINA_BASE=$CATALINA_BASE"
./startup.sh
