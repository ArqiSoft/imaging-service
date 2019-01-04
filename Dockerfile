FROM FROM maven:3.6-jdk-10-slim AS builder
ARG BUILD_NUMBER=undefined
ARG BUILD_TIMESTAMP=undefined
ARG GIT_COMMIT_SHORT=undefined
ARG GIT_AUTHOR_NAME=undefined

ENV BUILD_NUMBER ${BUILD_NUMBER}
ENV BUILD_TIMESTAMP ${BUILD_TIMESTAMP}
ENV GIT_COMMIT_SHORT ${GIT_COMMIT_SHORT}
ENV GIT_AUTHOR_NAME ${GIT_AUTHOR_NAME}

COPY . /usr/src/imaging-service
WORKDIR /usr/src/imaging-service
RUN mvn -B -Pdocker clean package

#FROM openjdk:10.0.2-jre-slim-sid
FROM openjdk:8-jre

RUN mkdir /temp

VOLUME /logs
VOLUME /temp

ENV SPRING_BOOT_APP sds-imaging-service.jar
ENV SPRING_BOOT_APP_JAVA_OPTS -Xmx256m -XX:NativeMemoryTracking=summary
WORKDIR /app
RUN apt-get update && apt-get install -y curl
RUN curl https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh > /app/wait-for-it.sh && chmod 777 /app/wait-for-it.sh

COPY --from=builder /usr/src/imaging-service/target/$SPRING_BOOT_APP ./

ENTRYPOINT java -Djava.awt.headless=true $SPRING_BOOT_APP_JAVA_OPTS -jar $SPRING_BOOT_APP
