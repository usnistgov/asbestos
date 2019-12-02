#!/bin/sh

# Shutdown script for FHIR Toolkit

TOOLKITS=../Toolkits
FHIRTOOLKIT=$TOOLKITS/FhirToolkit
XDSTOOLKIT=$TOOLKITS/XdsToolkit

export CATALINA_HOME=..
echo "CATALINA_HOME is `pwd`/$CATALINA_HOME"

export CATALINA_BASE=$XDSTOOLKIT
./shutdown.sh

export CATALINA_BASE=$FHIRTOOLKIT
./shutdown.sh
