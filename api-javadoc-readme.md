# API JavaDoc Readme

The API for FHIR Toolkit is an early prototype that lives on the branch api. Its current 
capabilities are to create and delete
channels and to run server tests against a channel. The file fhirtoolkit.javadoc.zip contains the generated
JavaDocs.

There are two integration tests (on branch api) that demonstrate some basic capabilities.

ChannelApiIT - create, get, delete a channel

TestApiIT - run a conformance test against the default channel (integrated HAPI server)

As noted above the API lives on its own development branch for now and is not part of any release. When we  have
expanded the capabilities to a useful point it will be added to the main development branch and released with the 
toolkit.
