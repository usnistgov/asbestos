#!/bin/bash

# Helper File for Docker based deployment

echo "changing to asbestos bin"
cd ./asbestos/tomcat/bin
echo "start"
./start.sh
echo "run"
./catalina.sh run