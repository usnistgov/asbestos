SET TOOLKITS_DIR=%CATALINA_HOME%\Toolkits

REM External Cache
REM If an alternate External Cache location is desired, please update the -DEXTERNAL_CACHE Java System Property below OR Use the Toolkit Configuration UI.
SET "CATALINA_OPTS=%CATALINA_OPTS% -DEXTERNAL_CACHE=%TOOLKITS_DIR%\ExternalCache"

REM Toolkit Properties
SET "CATALINA_OPTS=%CATALINA_OPTS% -DTOOLKIT_PROPERTIES=%TOOLKITS_DIR%\XdsToolkit\conf\toolkit.properties"

