FROM openjdk:17
VOLUME /tmp
ADD target/money-service.jar money-service.jar
ENTRYPOINT [ "java","-jar","/money-service.jar" ]