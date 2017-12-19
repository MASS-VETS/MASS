FROM java
ADD send-and-get-ack-0.0.1.jar app.jar
EXPOSE 8080
#ENTRYPOINT exec java -XX:+PrintFlagsFinal -XX:+PrintGCDetails -XX:+UseG1GC $JAVA_OPTS -jar app.jar
ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-jar","app.jar"]
