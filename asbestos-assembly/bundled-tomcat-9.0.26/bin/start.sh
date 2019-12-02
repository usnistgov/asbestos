#!/bin/sh

# Startup script for FHIR Toolkit
# There are two CATALINA_BASE directories: Toolkits/XdsToolkit and Toolkits/FhirToolkit.
# Toolkits/XdsToolkit may contain the HAPI FHIR server and/or XDS Toolkit. If either are present
# (if Toolkits/XdsToolkit/webapps/ is not empty) then this CATALINA_BASE is started.
# Next Toolkits/FhirToolkit is started.  This contains the FHIR Toolkit components.

# NOTE: HAPI FHIR and XDS Toolkit must be started before FhirToolkit.

TOOLKITS=../Toolkits
FHIRTOOLKIT=$TOOLKITS/FhirToolkit
XDSTOOLKIT=$TOOLKITS/XdsToolkit
XDSWEBAPPS=$XDSTOOLKIT/webapps

export CATALINA_HOME=..
echo "CATALINA_HOME is `pwd`/$CATALINA_HOME"

echo "Looking at $XDSWEBAPPS"
WEBAPPSCOUNT=`find $XDSWEBAPPS -maxdepth 1 \( -type d -o -name *.war \) -printf x | wc -c`

# start XdsToolkit base if its webapps dir is not empty
echo "count is $WEBAPPSCOUNT"
exit

# account for webapps.txt file that allows this to be checked in to git
if [ $WEBAPPSCOUNT -gt 0 ]
then
	echo "XdsToolkit should be started"
	mkdir $XDSTOOLKIT/logs
	export CATALINA_BASE=$XDSTOOLKIT
	./startup.sh
else
	echo "XdsToolkit should not be started"
fi

# start FhirToolkit

echo "Starting FhirToolkit"
mkdir $FHIRTOOLKIT/logs
export CATALINA_BASE=$FHIRTOOLKIT
./startup.sh
