FROM java
ADD the-hammer-0.0.1.jar app.jar
ADD hammer.yaml.cfg app.yaml
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=app.yaml"]
