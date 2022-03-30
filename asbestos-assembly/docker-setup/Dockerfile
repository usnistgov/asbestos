FROM adoptopenjdk/openjdk11-openj9:alpine-slim
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE ${hapiFhirHttpPort}
EXPOSE ${hapiFhirTlsPort}
EXPOSE ${fhirToolkitTlsPort}
EXPOSE ${fhirToolkitHttpPort}
EXPOSE ${xdsToolkitHttpPort}
EXPOSE ${xdsToolkitTlsPort}


RUN apk add --no-cache bash
RUN apk add --no-cache findutils 

# asbestos.zip must be currently provided from the release and be put in the dist directory
# copy it from here https://github.com/usnistgov/asbestos/releases
ADD ./dist/asbestos.zip .
RUN unzip /asbestos.zip
RUN rm /asbestos.zip

ADD ./start.sh .

RUN rm ./asbestos/tomcat/Toolkits/FhirToolkit/conf/service.properties
ADD ./service.properties ./asbestos/tomcat/Toolkits/FhirToolkit/conf/

RUN rm ./asbestos/tomcat/Toolkits/FhirToolkit/webapps/ROOT/serviceProperties.json
ADD ./serviceProperties.json ./asbestos/tomcat/Toolkits/FhirToolkit/webapps/ROOT/

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

ENTRYPOINT ./start.sh


# docker build -t asbestos . 
# docker run --name asbestos -p ${fhirToolkitHttpPort}:${fhirToolkitHttpPort}-p ${xdsToolkitHttpPort}:${xdsToolkitHttpPort} -p ${xdsToolkitTlsPort}:${xdsToolkitTlsPort} -p ${fhirToolkitTlsPort}:${fhirToolkitTlsPort} -p ${hapiFhirHttpPort}:${hapiFhirHttpPort} -p ${hapiFhirTlsPort}:${hapiFhirTlsPort} asbestos

