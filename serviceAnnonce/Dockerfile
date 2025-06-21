FROM openjdk:17
VOLUME /tmp
ADD target/serviceAnnonce.jar serviceAnnonce.jar
ENTRYPOINT [ "java","-jar","/serviceAnnonce.jar" ]