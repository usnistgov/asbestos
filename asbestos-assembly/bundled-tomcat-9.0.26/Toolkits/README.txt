The Toolkits directory contains the ExternalCache directory and one or more Toolkit bases (FhirToolkit, XdsToolkit). Each application base has a pre-configured conf\server.xml with a default port number and a TLS certificate where applicable. If the HTTP port needs to be updated, the HttpConnector in the conf\server.xml can be updated. 


Attention: The ports in conf\server.xml might be bound by a
    different instance. Please review your config files
    and update them where necessary.