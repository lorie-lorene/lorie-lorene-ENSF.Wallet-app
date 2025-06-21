FROM openjdk:17
VOLUME /tmp
ADD target/service-configuration.jar service-configuration.jar
ENTRYPOINT [ "java","-jar","/service-configuration.jar" ]