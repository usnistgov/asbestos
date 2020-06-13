#!/bin/bash

echo "Shutting down existing installation."
if [ -d /opt/asbestos/tomcat/bin ]
then
  (cd /opt/asbestos/tomcat/bin; ./stop.sh)
fi

echo "Removing existing installation."
rm -r -f /opt/asbestos
rm -r -f /opt/asbestos.zip
./build-local-release.sh
