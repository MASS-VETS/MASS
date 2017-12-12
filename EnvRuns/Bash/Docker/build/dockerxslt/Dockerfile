# This is your Editor pane. Write the Dockerfile here and 
# use the command line to execute commands
FROM java
ADD transform-service-0.0.1.jar app.jar
EXPOSE 8080
#ENTRYPOINT exec java -XX:+PrintFlagsFinal -XX:+PrintGCDetails -XX:+UseG1GC $JAVA_OPTS -jar app.jar
ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-jar", "app.jar"]
