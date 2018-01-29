FROM java
ADD the-nail-0.0.1.jar app.jar
ADD nail.yaml.cfg app.yaml
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-Djavax.net.ssl.trustStore=/keystore/cacerts", "-Djavax.net.ssl.trustStorePassword=changeit", "-Djavax.net.debug=ssl", "-jar", "app.jar", "--spring.config.location=app.yaml"]