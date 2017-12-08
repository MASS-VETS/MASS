#FROM anapsix/alpine-java
#FROM minjava
FROM java
ADD file-transfer-service-0.0.1.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar","app.jar"]
