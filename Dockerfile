FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD service-graph-platform-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]