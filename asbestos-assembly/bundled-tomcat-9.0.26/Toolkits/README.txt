The Toolkits directory contains the ExternalCache directory and one or more application bases (FHIR, XDS). Each application base has a pre-configured conf\server.xml with a default port number and a TLS certificate where applicable. If the HTTP port needs to be updated, the HttpConnector in the conf\server.xml can be updated. 

You can launch the FhirToolkit application base instance by running:
    set CATALINA_HOME=your_path_to\bundled-tomcat-9.0.26 (YMMV)
    set CATALINA_BASE=%CATALINA_HOME%\Toolkits\FhirToolkit

    (Foreground)
    %CATALINA_HOME%/bin/catalina.bat run
     or
    (Background)
    %CATALINA_HOME%/bin/startup.bat

Attention: The ports in conf\server.xml might be bound by a
    different instance. Please review your config files
    and update them where necessary.