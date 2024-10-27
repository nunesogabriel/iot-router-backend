# Use uma imagem base do OpenJDK 17
FROM openjdk:17-jdk-slim

# Instalar bash, utilitários de rede como ping, curl e ifconfig (net-tools)
RUN apt-get update && apt-get install -y bash iputils-ping curl net-tools iproute2

# Defina o diretório de trabalho dentro do container
WORKDIR /app

# Copie o JAR da sua aplicação para o container
COPY target/iot-router-backend-*.jar /app/iot-router-backend.jar

# Exponha a porta da aplicação (ajuste conforme a porta que sua aplicação Camel usa)
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "/app/iot-router-backend.jar"]

