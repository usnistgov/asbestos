README.txt

NIST FHIR(R) Toolkit
November 2019 Release

Requirements
The instructions assume your system is capable of running Apache Tomcat Server Version 9.0.26. This means Java 8 should already be installed and accessible through your system environment. It also assumes the following system ports
9705 Tomcat Shutdown port
9709 AJP
${fhirToolkitTlsPort} TLS (reserved for future)
${fhirToolkitHttpPort} HTTP
are all available and free to be used by the FHIR Toolkit's Tomcat CATALINA_BASE. The CATALINA_BASE environment variable specifies location of the root directory of the "active configuration" of Tomcat. See <installation-directory>/tomcat/RUNNING.txt for more details on CATALINA_BASE. Each type of Toolkit (FHIR, XDS) makes use of a dedicated CATALINA_BASE.

How to Setup\Install
Unzip the contents to a folder of your choice. The full path of this folder will be referred to as "<installation-directory>". In the examples below, you will have to replace the <installation-directory> token with the directory location of the folder where the zip file was extracted.

Contents
The zip file contains a Tomcat Application Server bundled with the FHIR Toolkit web application. It is configured to run on HTTP port ${fhirToolkitHttpPort}.

NOTE: The zip file does NOT include XDS Toolkit or the HL7 HAPI FHIR distributions.
For information on installing them if you do not already have them on your system, please see below:

 General XDS Toolkit Installation instructions can be found at: https://github.com/usnistgov/iheos-toolkit2/wiki/installing
 The following instructions demonstrate how to install an XDS Toolkit release in to the provided Toolkit Catalina base shell. A Catalina base shell is only the configuration to start Tomcat but without the XDS Toolkit Java Web Application Archive (WAR) file.
 The XDS Toolkit Catalina base shell is located in the <installation-directory>/tomcat/Toolkits/XdsToolkit folder.
     Download an XDS Toolkit release (WAR file) from the Releases Page: https://github.com/usnistgov/iheos-toolkit2/releases
     Rename the downloaded war file to "xdstools.war". This will simplify the web application context reference.
     Place the downloaded xdstools.war into the <installation-directory>/tomcat/Toolkits/XdsToolkit/webapps folder
     Start Tomcat for XdsToolkit

     On Windows
     REM Set CATALINA_HOME to the Tomcat directory
     SET CATALINA_HOME=<installation-directory>\tomcat
     REM Set the XDS Toolkit Catalina Base
     SET CATALINA_BASE=%CATALINA_HOME%\Toolkits\XdsToolkit

     REM Start Tomcat
     %CATALINA_HOME%\bin\startup.bat

     REM Uncomment the following to shutdown Tomcat
     REM %CATALINA_HOME%\bin\shutdown.bat

     On *nix
     # Set CATALINA_HOME to the Tomcat directory
     export CATALINA_HOME=<installation-directory>/tomcat
     # Set the XDS Toolkit Catalina Base
     export CATALINA_BASE=$CATALINA_HOME/Toolkits/XdsToolkit

     # Start Tomcat
     $CATALINA_HOME/bin/startup.sh

     # Uncomment the following to shutdown Tomcat
     # $CATALINA_HOME/bin/shutdown.sh

     If there are any port conflicts, check to see if these ports are free and available to be used by XDS Toolkit:
     9775 Tomcat Shutdown port
     9779 AJP
     ${xdsToolkitTlsPort} TLS
     ${xdsToolkitHttpPort} HTTP
     7297 Proxy Port
     5000-5015 PIF Listener port range

     Open a web browser to ${xdsToolkitBase} and dismiss any alerts or pop-ups related to External Cache.

     At the top left of the window is a link labeled Toolkit Configuration. Open it. It will challenge you for a password. It is easy.
     If the Toolkit Properties are not displayed and you get another error dialog box instead then there is a problem you need to report.
     Update the External Cache to (On Windows) /C:/<installation-directory>/tomcat/Toolkits/ExternalCache
        (On *nix) External Cache can be specified as <installation-directory>/tomcat/Toolkits/ExternalCache
     Update Toolkit Port to ${xdsToolkitHttpPort}
     Update Toolkit TLS Port to ${xdsToolkitTlsPort}
     Note other port numbers with default values:
        Proxy Port is 7297
        PIF Listener Port Range is 5000-5020
     Click Save
    Refresh (Ctrl+F5) browser for settings to take effect.
    XDS Toolkit should load without errors.

    XDS Toolkit Keystore (TLS)
     The TLS ports from the toolkit.properties should be updated to match Tomcat configuration <installation-directory>/tomcat/Toolkits/XdsToolkit/conf/server.xml.
     The Toolkit environment certificate must use the same copy as <installation-directory>/tomcat/Toolkits/XdsToolkit/connectathon-certificate/keystore.
     You may copy the provided XdsToolkit/connectathon-certificate/keystore to ExternalCache/environment/default/keystore file. This is used for all outbound-TLS connections in the Default Toolkit environment.
     The keystore makes use of the European Gazelle Security Suite (GSS) CA issued certificate (Id 3129). Certificate details can be viewed from: https://gazelle.ihe.net/gss/certificate/view.seam?id=3129

 HL7 HAPI FHIR: https://hapifhir.io/
 The version of HAPI FHIR that was used to test FHIR Toolkit can be found here: https://github.com/usnistgov/asbestos/releases/download/0.1/fhir.zip
 The fhir.zip file can be extracted to <installation-directory>/tomcat/Toolkits/XdsToolkit/webapps
 You may need to update the FHIR Toolkit service.properties file to reflect the FHIR Base URL that is in use.

ExternalCache
The default ExternalCache location for all Toolkit CATALINA_BASEs is <installation-directory>/tomcat/Toolkits/ExternalCache

FHIR Toolkit Service Properties
All of the backend API related URLs (XDS Toolkit, HAPI FHIR) are configured in the following file: <installation-directory>/tomcat/Toolkits/FhirToolkit/webapps/asbestos/WEB-INF/classes/service.properties

For XDS Toolkit, the default base URL is:
xdsToolkitBase=${xdsToolkitBase}

If XDS Toolkit is running on port other than ${xdsToolkitHttpPort}, update the port number part of the xdsToolkitBase.

For HAPI FHIR, the default base URL is:
hapiFhirBase=${hapiFhirBase}

Running FHIR Toolkit the easy way
We have installed two custom scripts in tomcat/bin
  start.sh   -- to start both the XDSToolkit base and the FhirToolkit base (in that order)
  stop.sh    -- to stop both



Running FHIR Toolkit
If you are using XDS Toolkit for MHD Document Source Testing, XDS Toolkit must be started BEFORE FHIR Toolkit. The FHIR Toolkit initialization will setup the required simulators on the backend using the XDS Toolkit Simulator API.

If you are NOT using XDS Toolkit, you should comment off the xdsToolkitBase property in your service.properties file so that the FHIR Toolkit will not attempt to create the required XDS simulators.

Set the CATALINA_HOME and the CATALINA_BASE system environment variables and run the Tomcat startup batch file/script.

On Windows
REM Set CATALINA_HOME to the Tomcat directory
SET CATALINA_HOME=<installation-directory>\tomcat
REM Set the FHIR Toolkit Catalina Base
SET CATALINA_BASE=%CATALINA_HOME%\Toolkits\FhirToolkit

REM Start Tomcat
%CATALINA_HOME%\bin\startup.bat

Now, Tomcat should be up and running on HTTP port ${fhirToolkitHttpPort}.

To access FHIR Toolkit, open browser to ${fhirUI}

REM Uncomment the following to shutdown Tomcat
REM %CATALINA_HOME%\bin\shutdown.bat

On *nix
# Set CATALINA_HOME to the Tomcat directory
export CATALINA_HOME=<installation-directory>/tomcat
# Set the FHIR Toolkit Catalina Base
export CATALINA_BASE=$CATALINA_HOME/Toolkits/FhirToolkit

# Start Tomcat
$CATALINA_HOME/bin/startup.sh

Now, Tomcat should be up and running on HTTP port ${fhirToolkitHttpPort}.

To access FHIR Toolkit, open browser to ${fhirUI}

# Uncomment the following to shutdown Tomcat
# $CATALINA_HOME/bin/shutdown.sh

Other Topics
For help with other topics, see the HOW-TO folder.

HL7(R), HEALTH LEVEL SEVEN(R), FHIR(R) and the FHIR Logo (R) are trademarks owned by Health Level Seven International, registered with the United States Patent and Trademark Office.

