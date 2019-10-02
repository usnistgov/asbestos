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
	This file helps discovery of services and communication among other types of toolkits and related applications.
	Hostname=
	Port=
	Context path=