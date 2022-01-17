FROM adoptopenjdk/openjdk11-openj9:alpine-slim
MAINTAINER oliver egger <oliver.egger@ahdis.ch>
EXPOSE 7080
EXPOSE 8080
EXPOSE 9705
EXPOSE 9709
EXPOSE 9743
EXPOSE 9760
EXPOSE 9970

RUN apk add --no-cache bash
RUN apk add --no-cache findutils 

# asbestos.zip must be currently provided from the release and be put in the dist directory
# copy it from here https://github.com/usnistgov/asbestos/releases
ADD ./dist/asbestos.zip .
RUN unzip /asbestos.zip
RUN rm /asbestos.zip

ADD ./start.sh .

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

ENTRYPOINT ./start.sh


# docker build -t asbestos . 
# docker run --name asbestos -p 9760:9760 asbestos

