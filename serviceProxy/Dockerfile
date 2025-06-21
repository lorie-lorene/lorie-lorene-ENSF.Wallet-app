FROM openjdk:17
VOLUME /tmp
ADD target/serviceProxy.jar serviceProxy.jar
ENTRYPOINT [ "java","-jar","/serviceProxy.jar" ]