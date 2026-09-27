FROM gradle:9.6.0-jdk25-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle ./

RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle build --no-daemon


FROM eclipse-temurin:25-jdk-alpine AS jre-build

RUN jlink \
    --add-modules java.base,java.logging,java.management,java.naming,java.sql,java.xml,java.desktop,java.instrument,java.security.jgss,java.compiler,java.net.http,jdk.crypto.ec,jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /custom-jre


FROM alpine:3.22

WORKDIR /app

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN addgroup -S -g 1001 spring && adduser -S -u 1001 spring -G spring

COPY --from=jre-build /custom-jre ${JAVA_HOME}
COPY --from=build /app/build/libs/user-mgmt-service-0.0.1-SNAPSHOT.jar app.jar

USER 1001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
