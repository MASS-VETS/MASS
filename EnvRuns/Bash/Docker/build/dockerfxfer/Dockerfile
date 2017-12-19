#FROM anapsix/alpine-java
#FROM minjava
FROM java
ADD file-transfer-service-0.0.1.jar app.jar
EXPOSE 8080
#ENTRYPOINT exec java -XX:+PrintFlagsFinal -XX:+PrintGCDetails -XX:+UseG1GC $JAVA_OPTS -jar app.jar
#ENTRYPOINT exec java -XX:+PrintFlagsFinal -XX:+UseG1GC -XX:MaxRAM='cat /sys/fs/cgroup/memory/memory.limit_in_bytes' $JAVA_OPTS -jar app.jar
ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-jar", "app.jar"]
