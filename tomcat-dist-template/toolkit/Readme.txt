The Toolkit directory is intended to co-exist along Tomcat's home directory (CATALINA_HOME).

Toolkit directory contents:

asbestos-base
	The Tomcat base directory for Asbestos.
	To start Asbestos run the following command from its base directory:
		(Windows)
		CATALINA_BASE=%CATALINA_HOME%\toolkit\asbestos-base
		%CATALINA_HOME%\bin\startup.bat 
external_cache
	The Toolkit External Cache directory.

xdstoolkit-base
	The Tomcat base directory for XDS Toolkit.
	To start XDS Toolkit run the following commands from its base directory:
		(Windows)
		set CATALINA_BASE=%CATALINA_HOME%\toolkit\xdstoolkit-base
		%CATALINA_HOME%\bin\startup.bat 

service.properties
	This file helps discovery of services and communication among other types of toolkits and related applications.
	Hostname=
	Port=
	Context path=