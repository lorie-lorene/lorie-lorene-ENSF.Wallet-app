FROM openjdk:17
VOLUME /tmp
ADD target/serviceUser.jar serviceUser.jar
ENTRYPOINT [ "java","-jar","/serviceUser.jar" ]