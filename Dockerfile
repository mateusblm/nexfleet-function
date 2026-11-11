# syntax=docker/dockerfile:1
# Imagem da Function para execução local. Na Azure o deploy é por pacote (azure-functions:deploy).
# Os testes completos (unitários + ITs com Testcontainers, que exigem Docker) rodam no CI;
# aqui só empacotamos.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn ./.mvn
COPY mvnw pom.xml host.json ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -DskipTests

FROM mcr.microsoft.com/azure-functions/java:4-java21
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true
COPY --from=build /workspace/target/azure-functions/nexfleet-notificacoes /home/site/wwwroot
