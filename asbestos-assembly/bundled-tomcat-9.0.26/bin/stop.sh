#!/bin/sh

# Shutdown script for FHIR Toolkit

TOOLKITS=../Toolkits
FHIRTOOLKIT=$TOOLKITS/FhirToolkit
XDSTOOLKIT=$TOOLKITS/XdsToolkit
HAPIFHIRBASE=../HapiFhir/base

export CATALINA_HOME=..
echo "CATALINA_HOME is `pwd`/$CATALINA_HOME"

echo "Stopping HAPI FHIR"
export CATALINA_BASE=$HAPIFHIRBASE
./shutdown.sh

echo "Stopping XDS Toolkit"
export CATALINA_BASE=$XDSTOOLKIT
./shutdown.sh

echo "Stopping FHIR Toolkit"
export CATALINA_BASE=$FHIRTOOLKIT
./shutdown.sh
