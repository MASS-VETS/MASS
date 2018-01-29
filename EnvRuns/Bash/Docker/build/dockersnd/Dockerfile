FROM java
ADD send-and-get-ack-0.0.1.jar app.jar
EXPOSE 8080

#ENTRYPOINT exec java -XX:+PrintFlagsFinal -XX:+PrintGCDetails -XX:+UseG1GC $JAVA_OPTS -jar app.jar
#ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-jar","app.jar"]
#ENTRYPOINT java $JAVA_OPTS1 $JAVA_OPTS2 $JAVA_OPTS3 $JAVA_OPTS4 $JAVA_OPTS5 $JAVA_OPTS6 -jar app.jar
#ENTRYPOINT ["/bin/sh", "-c", "java $JAVA_OPTS1 $JAVA_OPTS2 ${JAVA_OPTS3} ${JAVA_OPTS4} ${JAVA_OPTS5} ${JAVA_OPTS6} -jar app.jar"]
ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-Djavax.net.ssl.trustStore=/keystore/cacerts", "-Djavax.net.ssl.trustStorePassword=changeit", "-Djavax.net.debug=ssl", "-jar", "app.jar"]

