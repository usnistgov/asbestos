SET TOOLKITS_DIR=%CATALINA_HOME%\Toolkits

REM Service Properties
REM Enable the Java system property to use a specific property file
SET "CATALINA_OPTS=%CATALINA_OPTS% -DSERVICE_PROPERTIES=%TOOLKITS_DIR%\FhirToolkit\conf\service.properties"

REM External Cache
REM If an alternate External Cache location is desired, please update the -DEXTERNAL_CACHE Java System Property below.
SET "CATALINA_OPTS=%CATALINA_OPTS% -DEXTERNAL_CACHE=%TOOLKITS_DIR%\ExternalCache"

