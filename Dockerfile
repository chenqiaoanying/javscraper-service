FROM eclipse-temurin:17-jdk AS builder
WORKDIR application
COPY build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jdk
RUN apt-get update && apt-get install -y libgtk2.0-0

WORKDIR application
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT java ${VM_OPTIONS} org.springframework.boot.loader.JarLauncher