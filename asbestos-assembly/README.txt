README.txt

NIST FHIR(R) Toolkit
November 2019 Release

Requirements
The instructions assume your system is capable of running Apache Tomcat Server Version 9.0.26. This means Java 8 should already be installed and accessible through your system environment. It also assumes the following system ports 9705, 9709, 9743, and 9760 are all available and free to be used by the FHIR Toolkit's Tomcat CATALINA_BASE. The CATALINA_BASE environment variable specifies location of the root directory of the "active configuration" of Tomcat. See <installation-directory>\tomcat\RUNNING.txt for more details on CATALINA_BASE.

Contents
The zip file contains a Tomcat Application Server bundled with the FHIR Toolkit web application. It is configured to run on HTTP port 9760.

NOTE: The zip file does NOT include XDS Toolkit or the HL7 HAPI FHIR distributions.
For information on installing them if you do not already have them on your system, please see:
 XDS Toolkit Wiki Page: https://github.com/usnistgov/iheos-toolkit2/wiki/installing
 HL7 HAPI FHIR: https://hapifhir.io/
 The version of HAPI FHIR that was used to test FHIR Toolkit can be found here: https://github.com/usnistgov/asbestos/releases/tag/0.1

ExternalCache
The default location for the FHIR Toolkit ExternalCache is <installation-directory>/tomcat/Toolkits/ExternalCache

Service Properties
All of the backend API related URLs (XDS Toolkit, HAPI FHIR) are configured in the following file: <installation-directory>\tomcat\Toolkits\FhirToolkit\webapps\asbestos\WEB-INF\classes\service.properties

How to Setup\Install
Unzip the contents to a folder of your choice. The full path of this folder will be referred to as "<installation-directory>" hereon. In the examples below, you will have to replace the <installation-directory> token with the directory location of the folder where the zip file was extracted.

Running
If you are using XDS Toolkit for MHD Testing, XDS Toolkit must be started before FHIR Toolkit. The FHIR Toolkit initialization will setup the required simulators on the backend using the XDS Toolkit Simulator API.

If you are NOT using XDS Toolkit, you should comment off the xdsToolkitBase property in your service.properties file so that the FHIR Toolkit will not attempt to create the required simulators.

Set the CATALINA_HOME and the CATALINA_BASE system environment variables and run the Tomcat startup batch file/script.

On Windows
REM Set CATALINA_HOME to the Tomcat directory
SET CATALINA_HOME=<installation-directory>\tomcat
REM Set the FHIR Toolkit Catalina Base
SET CATALINA_BASE=%CATALINA_HOME%\Toolkits\FhirToolkit

REM Start Tomcat
%CATALINA_HOME%\bin\startup.bat

Now, Tomcat should be up and running on HTTP port 9760.

To access FHIR Toolkit, open browser to http://localhost:9760/

REM Shutdown Tomcat
%CATALINA_HOME%\bin\shutdown.bat

On *nix
# Set CATALINA_HOME to the Tomcat directory
export CATALINA_HOME=<installation-directory>/tomcat
# Set the FHIR Toolkit Catalina Base
export CATALINA_BASE=$CATALINA_HOME/Toolkits/FhirToolkit

# Start Tomcat
$CATALINA_HOME/bin/startup.sh

Now, Tomcat should be up and running on HTTP port 9760.

To access FHIR Toolkit, open browser to http://localhost:9760/

# Shutdown Tomcat
$CATALINA_HOME/bin/shutdown.sh

Other Topics
For help with other topics, see the HOW-TO folder.

HL7(R), HEALTH LEVEL SEVEN(R), FHIR(R) and the FHIR Logo (R) are trademarks owned by Health Level Seven International, registered with the United States Patent and Trademark Office.

