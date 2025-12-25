FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY ../common-lib /workspace/common-lib
COPY pom.xml .
COPY src src

RUN apk add --no-cache maven
RUN cd /workspace/common-lib && mvn clean install -DskipTests
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG JAR_FILE=/workspace/app/target/*.jar
COPY --from=build ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

