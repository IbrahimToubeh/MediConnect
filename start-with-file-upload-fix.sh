#!/bin/bash
# Startup script for MediConnect with file upload limit fix
# This sets the JVM argument to allow up to 200 file attachments per multipart request

echo "Starting MediConnect with file upload limit fix..."
echo "File count limit set to 200 attachments per request"

./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dorg.apache.tomcat.util.http.fileupload.FileCountLimit=200"

