FROM java
ADD monitor-service-0.0.1.jar app.jar
ADD monitor.yaml.cfg app.yaml
EXPOSE 8080
#ENTRYPOINT eval "echo java $JAVA_OPTIONS $JGC_OPTIONS -jar app.jar"
#ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-jar","app.jar"]
#ENTRYPOINT ["java", $JAVA_OPTS1, $JAVA_OPTS2, $JAVA_OPTS3, $JGC_OPTS1, $JGC_OPTS2, $JGC_OPTS3, "-jar", "app.jar"]
#ENTRYPOINT java $JAVA_OPTS1 $JAVA_OPTS2 $JAVA_OPTS3 $JAVA_OPTS4 $JAVA_OPTS5 $JAVA_OPTS6 -jar app.jar --spring.config.location=app.yaml
ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-Djavax.net.ssl.trustStore=/keystore/cacerts", "-Djavax.net.ssl.trustStorePassword=changeit", "-Djavax.net.debug=ssl", "-jar", "app.jar", "--spring.config.location=app.yaml"]
