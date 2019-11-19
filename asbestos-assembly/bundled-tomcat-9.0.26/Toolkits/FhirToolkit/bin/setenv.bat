SET TOOLKITS_DIR=%CATALINA_HOME%\Toolkits

REM Service Properties
REM Enable the Java system property to use a specific property file
REM SET "CATALINA_OPTS=%CATALINA_OPTS% -DSERVICE_PROPERTIES=%TOOLKITS_DIR%\service.properties"
REM The default copy of the service properties is inside the FhirToolkit\webapps\WEB-INF\classes directory.

REM External Cache
REM If an alternate External Cache is desired, please use update the -DEXTERNAL_CACHE Java System Property below.
SET "CATALINA_OPTS=%CATALINA_OPTS% -DEXTERNAL_CACHE=%TOOLKITS_DIR%\external_cache"

