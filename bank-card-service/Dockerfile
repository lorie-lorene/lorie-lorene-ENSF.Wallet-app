FROM openjdk:17
VOLUME /tmp
ADD target/bank-card-service.jar bank-card-service.jar
ENTRYPOINT [ "java","-jar","/bank-card-service.jar" ]