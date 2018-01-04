FROM java
ADD file-transfer-service-0.0.1.jar app.jar
EXPOSE 8080
#ENTRYPOINT exec java -XX:+PrintFlagsFinal -XX:+PrintGCDetails -XX:+UseG1GC $JAVA_OPTIONS -jar app.jar
#ENTRYPOINT exec java -XX:+PrintFlagsFinal -XX:+UseG1GC -XX:MaxRAM='cat /sys/fs/cgroup/memory/memory.limit_in_bytes' $JAVA_OPTIONS -jar app.jar
#ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-XX:NativeMemoryTracking=summary", "-jar", "app.jar"]
#ENTRYPOINT ["java", $JAVA_OPTIONS, $JGC_OPTIONS, "-jar", "app.jar"]
#ENTRYPOINT exec java $JAVA_OPTIONS $JGC_OPTIONS -jar app.jar
#ENTRYPOINT eval echo java $JAVA_OPTIONS $JGC_OPTIONS -jar app.jar
ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-Djavax.net.ssl.trustStore=/keystore/cacerts", "-Djavax.net.ssl.trustStorePassword=changeit", "-Djavax.net.debug=ssl", "-jar", "app.jar"]
