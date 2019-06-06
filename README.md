The Asbestos project - extending XDS Toolkit to support the FHIR standard.

Asbestos defines a micro-service environment to support the combined testing of the IHE XDS collection of 
profiles as well as the FHIR-based profiles. When complete, all the necessary components will be pulled into a Docker
environment to create a consolidated runtime. For now this is a work-in-progress.

For now all the components are part of the repository except for HAPI FHIR which is brought in
as a git submodule.

# To clone from github

    git clone https://github.com/usnistgov/asbestos.git 

to pull shell of project. This will create directory asbestos

    cd asbestos
    git submodule update --remote --init
    
If this is the first time running on this box in development

    cd view
    npm init
    npm install

to update the Javascript dependencies. These can be run at any time to refresh the Javascript libraries. Then run:

    npm run serve
    
in a terminal to start UI in development mode. Do this from the view directory.


