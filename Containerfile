FROM eclipse-temurin:21-jre

ARG JAR_FILE=target/config-viewer-1.0.0-jar-with-dependencies.jar

RUN useradd -m -u 1000 appuser
WORKDIR /app/data
COPY ${JAR_FILE} /app/app.jar

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
