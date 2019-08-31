Proxy IT tests

The running of Proxy IT tests is a little complicated so here are the details.

First, there are two modes: command line maven and IntelliJ.  Maven is supposed to be self contained
but it is not quite that good yet.  InteliJ mode is for debugging so all components except the
FHIR server (HAPI) can run in the debugger.

Maven mode

This module (pom.xml) launches a Tomcat with HAPI and Proxy loaded.  They run on port 8877.
The HAPI module (sibling to asbestos in the development directory) must have the following
parameters set in WEB-INF/classes/hapi.properties:

    server_address=http://localhost:8877/fhir/fhir/
    server.base=/fhir/fhir

Asbestos must be configured with the necessary ports so in ITConfig.java there is

    private static final boolean forMavenBuild = true;

which must be false for running in IntelliJ mode and true for running in Maven mode. I tried
to use systemPropertyVariables in the POM but IntelliJ remembers environment settings,
seemingly forever, and so this does not work properly since I run most things from within IntelliJ.

IntelliJ mode

This is where I spend most of my time to it is usually the way sources are checked in.

HAPI is running in a separate Tomcat on 8080.  I leave this up in my development environment
permanently.  HAPI takes a bit of time to load and I don't like waiting for it.
The critial hapi.properties are:

    server_address=http://localhost:8080/fhir/fhir/
    server.base=/fhir/fhir

Proxy is run out of IntelliJ on 8081.  This gets started/restarted a lot so having it isolated
is faster. When creating the IntelliJ run configuration:

HTTP Port 8081
Launch browser after Tomcat launch - no (unchecked)
Launch asbestos-proxy-war but not on the default app context - use /proxy instead
(this is hard coded into project code).

Asbestos must be configured with the necessary ports so in ITConfig.java there is

    private static final boolean forMavenBuild = false;