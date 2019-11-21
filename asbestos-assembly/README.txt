README.txt

Requirements
The instructions assume your system is capable of running Apache Tomcat Server Version 9.0.26. This means Java 8 should already be installed and accessible through your system environment. It also assumes the following system ports 9705, 9709, 9743, and 9760 are all available and free to be used by the FhirToolkit Tomcat application base.

Contents
The zip file contains a Tomcat Application Server bundled with an application base for the FhirToolkit which is configured to run on HTTP port 9760. 

NOTE: The zip file does NOT include an application base for the XdsToolkit nor the HL7 HAPI FHIR distributions. Also, the provided Tomcat bundle does include the Apache Tomcat examples directory.

How to Setup\Install
Unzip the contents to a folder of your choice. The full path of this folder will be referred to as "<installation-directory>" hereon. In the examples below, you will have to replace the <installation-directory> token with the directory location of the folder where the zip file was extracted.

Running
Set the CATALINA_HOME and the CATALINA_BASE system environment variables and run the Tomcat startup batch file/script.

On Windows
REM Set CATALINA_HOME to the installation directory
SET CATALINA_HOME=<installation-directory>\tomcat
REM Set the FhirToolkit application base
SET CATALINA_BASE=%CATALINA_HOME%\Toolkits\FhirToolkit

REM Start Tomcat
%CATALINA_HOME%\bin\startup.bat

Now, Tomcat should be up and running on HTTP port 9760.

To access FhirToolkit, open browser to http://localhost:9760/

REM Shutdown Tomcat
%CATALINA_HOME%\bin\shutdown.bat

On *nix
# Set CATALINA_HOME to the installation directory
export CATALINA_HOME=<installation-directory>/tomcat
# Set the FhirToolkit application base
export CATALINA_BASE=$CATALINA_HOME/Toolkits/FhirToolkit

# Start Tomcat
$CATALINA_HOME/bin/startup.sh

Now, Tomcat should be up and running on HTTP port 9760.

To access FhirToolkit, open browser to http://localhost:9760/

# Shutdown Tomcat
$CATALINA_HOME/bin/shutdown.sh

ExternalCache
The default location for the FhirToolkit ExternalCache is <installation-directory>/tomcat/Toolkits/ExternalCache

Other Topics
For help with other topics, see the HOW-TO folder.

