# Use uma imagem base do OpenJDK 17
FROM openjdk:17-jdk-slim

# Defina o diretório de trabalho dentro do container
WORKDIR /app

# Copie o JAR da sua aplicação para o container
COPY target/iot-router-backend-*.jar /app/iot-router-backend.jar

# Exponha a porta da aplicação (ajuste conforme a porta que sua aplicação Camel usa)
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "/app/iot-router-backend.jar"]