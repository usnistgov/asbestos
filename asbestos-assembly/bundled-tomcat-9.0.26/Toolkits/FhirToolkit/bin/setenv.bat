SET TOOLKITS_DIR=%CATALINA_HOME%\Toolkits

REM THIS SECTION IS DELEGATED TO A START.BAT PARAMETER
REM Gson library problem in Jre16
REM See https://github.com/google/gson/issues/1875
REM SET "CATALINA_OPTS=%CATALINA_OPTS% --add-opens java.base/java.util=ALL-UNNAMED"

REM Service Properties
REM Enable the Java system property to use a specific property file
SET "CATALINA_OPTS=%CATALINA_OPTS% -DSERVICE_PROPERTIES=%TOOLKITS_DIR%\FhirToolkit\conf\service.properties"

REM External Cache
REM If an alternate External Cache location is desired, please update the -DEXTERNAL_CACHE Java System Property below.
SET "CATALINA_OPTS=%CATALINA_OPTS% -DEXTERNAL_CACHE=%TOOLKITS_DIR%\ExternalCache"

