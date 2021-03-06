@ECHO OFF

REM Shutdown script for FHIR Toolkit

SET TOOLKITS=..\Toolkits
SET FHIRTOOLKIT=%TOOLKITS%\FhirToolkit
SET XDSTOOLKIT=%TOOLKITS%\XdsToolkit

SET CATALINA_HOME=..
ECHO CATALINA_HOME is %CD%\%CATALINA_HOME

SET CATALINA_BASE=%XDSTOOLKIT%
@CALL .\shutdown.bat

SET CATALINA_BASE=%FHIRTOOLKIT%
@CALL .\shutdown.bat
