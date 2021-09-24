CONTENTS OF TOMCAT-DIST-TEMPLATE ARE OBSOLETE SINCE ASBESTOS 2.X
NEED TO REVISIT

This 'testing-tools' directory is intended to co-exist along Tomcat's home directory (CATALINA_HOME). It contains application bases for each type of toolkit.

testing-tools directory contents:

fhirtookit-base
	The Tomcat base directory for FHIR Toolkit (Asbestos).
	To start FHIR Toolkit run the following commands:
		Windows (YMMV):
		CATALINA_BASE=%CATALINA_HOME%\testing-tools\fhirtoolkit-base
		%CATALINA_HOME%\bin\startup.bat 
external_cache
	The Toolkit External Cache directory.

xdstoolkit-base
	The Tomcat base directory for XDS Toolkit.
	To start XDS Toolkit run the following commands:
		Windows (YMMV):
		set CATALINA_BASE=%CATALINA_HOME%\testing-tools\xdstoolkit-base
		%CATALINA_HOME%\bin\startup.bat 

service.properties
FhirToolkit needs a separate service.properties file because we are presenting the user with the option to point to another instance of XdsToolkit effectively ignoring the copy of the XdsToolkit (and its toolkit.properties) which were supplied with the testing-tools distribution package.
	Hostname=
	Port=
	Context path=