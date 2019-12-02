@ECHO OFF

REM Startup script for FHIR Toolkit
REM There are two CATALINA_BASE directories: Toolkits/XdsToolkit and Toolkits/FhirToolkit.
REM Toolkits/XdsToolkit may contain the HAPI FHIR server and/or XDS Toolkit. If either are present
REM (if Toolkits/XdsToolkit/webapps/ is not empty) then this CATALINA_BASE is started.
REM Next Toolkits/FhirToolkit is started.  This contains the FHIR Toolkit components.

REM NOTE: HAPI FHIR and XDS Toolkit must be started before FhirToolkit.

REM Start with clean ERRORLEVEL.
TYPE NUL>NUL

SET TOOLKITS=..\Toolkits
SET FHIRTOOLKIT=%TOOLKITS%\FhirToolkit
SET XDSTOOLKIT=%TOOLKITS%\XdsToolkit
SET XDSWEBAPPS=%XDSTOOLKIT%\webapps

SET CATALINA_HOME=..
ECHO CATALINA_HOME is %CD%\%CATALINA_HOME%

REM this count includes parent dir so count of 1 means no sub-directories
ECHO Looking at %XDSWEBAPPS%

SET /A WEBAPPSCOUNT=0
REM Any directory
FOR /D %%G IN ("%XDSWEBAPPS%\*") DO (
    REM IF /I NOT "%%G"=="..\Toolkits\XdsToolkit\webapps\ROOT" (
        SET /A WEBAPPSCOUNT=WEBAPPSCOUNT+1
    REM )
)

IF %WEBAPPSCOUNT% EQU 0 (
 REM Any war file
 IF EXIST "%XDSWEBAPPS%\*.war" (
    SET /A WEBAPPSCOUNT=WEBAPPSCOUNT+1
 )
)

REM start XdsToolkit base if its webapps dir is not empty

ECHO count is %WEBAPPSCOUNT%
IF %WEBAPPSCOUNT% GEQ 1 (
    ECHO XdsToolkit should be started
    MKDIR %XDSTOOLKIT%\logs
    SET CATALINA_BASE=%XDSTOOLKIT%
    @CALL .\startup.bat
    REM Wait (in seconds) for XdsToolkit (and maybe HAPI FHIR) to startup
    ECHO Waiting for XdsToolkit to startup
    TIMEOUT /T 15
 )
) ELSE (
	ECHO XdsToolkit should not be started
)

REM start FhirToolkit

ECHO Starting FhirToolkit
MKDIR %FHIRTOOLKIT%\logs
SET CATALINA_BASE=%FHIRTOOLKIT%
@CALL .\startup.bat
