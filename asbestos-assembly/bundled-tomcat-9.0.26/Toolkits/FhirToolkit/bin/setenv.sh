TOOLKITS_DIR=$CATALINA_HOME/Toolkits

# Service Properties
# Enable the Java system property to use a specific property file
# CATALINA_OPTS="$CATALINA_OPTS -DSERVICE_PROPERTIES=$TOOLKITS_DIR/service.properties"
# The default copy of the service properties is inside the FhirToolkit\webapps\WEB-INF\classes directory.

# External Cache
# If an alternate External Cache location is desired, please update the -DEXTERNAL_CACHE Java System Property below.
CATALINA_OPTS="$CATALINA_OPTS -DEXTERNAL_CACHE=$TOOLKITS_DIR/ExternalCache"
