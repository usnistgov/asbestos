#!/bin/bash

# build asbestos.zip ready for upload to github
# resulting file will be at /opt/asbestos.zip

./rebuild-local-release.sh

echo "BUILDING ZIP"
cd /opt
zip -r --q asbestos.zip asbestos

echo "ZIP IS /OPT/ASBESTOS.ZIP"

