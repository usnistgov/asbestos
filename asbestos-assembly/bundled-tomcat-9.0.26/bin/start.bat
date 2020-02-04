@ECHO OFF

REM Startup script for FHIR Toolkit
REM There are two CATALINA_BASE directories: Toolkits/XdsToolkit and Toolkits/FhirToolkit.
REM Toolkits/XdsToolkit may contain the HAPI FHIR server and/or XDS Toolkit. If either are present
REM (if Toolkits/XdsToolkit/webapps/ is not empty) then this CATALINA_BASE is started.
REM Next Toolkits/FhirToolkit is started.  This contains the FHIR Toolkit components.

REM NOTE: HAPI FHIR and XDS Toolkit must be started before FhirToolkit.

REM Start with clean ERRORLEVEL.
TYPE NUL>NUL

SET "CATALINA_HOME="
SET "TOOLKITS="

REM Resolve full path
FOR /F %%I IN ("..\") DO SET CATALINA_HOME=%%~FI

REM Remove the trailing slash off the full path
IF "%CATALINA_HOME:~-1%"=="\" SET CATALINA_HOME=%CATALINA_HOME:~0,-1%

SET TOOLKITS=%CATALINA_HOME%\Toolkits

SET FHIRTOOLKIT=%TOOLKITS%\FhirToolkit
SET XDSTOOLKIT=%TOOLKITS%\XdsToolkit
SET XDSWEBAPPS=%XDSTOOLKIT%\webapps


ECHO CATALINA_HOME is %CATALINA_HOME%

REM this count includes parent dir so count of 1 means no sub-directories
ECHO Looking at %XDSWEBAPPS%

SET /A "WEBAPPSCOUNT=0"


FOR /D %%G IN ("%XDSWEBAPPS%\*") DO (
    SET FLDR_NAME=%%G
    SET "FLDR_NAME_PART=%FLDR_NAME:~-4%"
    IF /I NOT %FLDR_NAME_PART%==ROOT SET /A "WEBAPPSCOUNT=WEBAPPSCOUNT+1"
)

IF %WEBAPPSCOUNT% EQU 0 (
 REM Any war file
 IF EXIST "%XDSWEBAPPS%\*.war" (
    SET /A WEBAPPSCOUNT=WEBAPPSCOUNT+1
 )
)

REM start XdsToolkit base if its webapps dir is not empty

ECHO Count is %WEBAPPSCOUNT%

IF %WEBAPPSCOUNT% GTR 0 (
    ECHO XdsToolkit should be started
    MKDIR %XDSTOOLKIT%\logs
    SET CATALINA_BASE=%XDSTOOLKIT%
    @CALL .\startup.bat
    REM Wait (in seconds) for XdsToolkit (and maybe HAPI FHIR) to startup
    ECHO Waiting for XdsToolkit to startup
    TIMEOUT /T 60
 )
) ELSE (
	ECHO XdsToolkit should not be started
)

REM start FhirToolkit

ECHO Starting FhirToolkit
MKDIR %FHIRTOOLKIT%\logs
SET CATALINA_BASE=%FHIRTOOLKIT%
@CALL .\startup.bat
