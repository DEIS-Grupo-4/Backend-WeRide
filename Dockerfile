#Etapa 1 Build
#FROM maven:3.9.10-eclipse-temurin-24 AS build

#WORKDIR /app
#COPY pom.xml .
#COPY src ./src
#RUN mvn clean package -DskipTests

#Etapa 2: Run
#FROM eclipse-temurin:24-jdk-alpine
#WORKDIR /app
#COPY --from=build /app/target/*.jar app.jar

#EXPOSE 8080
##ENTRYPOINT ["java", "-jar", "app.jar"]

FROM maven:3.9.11-eclipse-temurin-24 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:24-jre

WORKDIR /app

COPY --from=build /app/target/backend-weride-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]