FROM maven:3.9-eclipse-temurin-17 AS compilacao

WORKDIR /projeto
COPY pom.xml .
COPY usuarios-api/pom.xml usuarios-api/pom.xml
COPY board-api/pom.xml board-api/pom.xml
COPY gateway/pom.xml gateway/pom.xml
RUN mvn -B -DskipTests dependency:go-offline

COPY usuarios-api/src usuarios-api/src
COPY board-api/src board-api/src
COPY gateway/src gateway/src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre AS usuarios-api
WORKDIR /app
COPY --from=compilacao /projeto/usuarios-api/target/usuarios-api-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:17-jre AS board-api
WORKDIR /app
COPY --from=compilacao /projeto/board-api/target/board-api-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:17-jre AS gateway
WORKDIR /app
COPY --from=compilacao /projeto/gateway/target/gateway-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
