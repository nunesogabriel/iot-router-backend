#!/bin/bash
echo "Aguardando 30 segundos antes de iniciar o aplicativo..."
sleep 60
echo "Iniciando o aplicativo..."
java -Dspring.profiles.active=dev -jar /app/iot-router-backend.jar