FROM eclipse-temurin:8-jdk-jammy

RUN apt-get update && apt-get install -y maven

WORKDIR /app