FROM openjdk:17
VOLUME /tmp
ADD target/serviceRegister.jar serviceRegister.jar
ENTRYPOINT [ "java","-jar","/serviceRegister.jar" ]