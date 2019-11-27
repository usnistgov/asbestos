TOOLKITS_DIR=$CATALINA_HOME/Toolkits

# External Cache
# If an alternate External Cache location is desired, please update the -DEXTERNAL_CACHE Java System Property below.
CATALINA_OPTS="$CATALINA_OPTS -DEXTERNAL_CACHE=$TOOLKITS_DIR/ExternalCache"
