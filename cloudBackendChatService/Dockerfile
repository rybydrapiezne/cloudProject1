FROM openjdk:17
WORKDIR /app
COPY build/libs/ChatApp-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar","--server.address=0.0.0.0"]