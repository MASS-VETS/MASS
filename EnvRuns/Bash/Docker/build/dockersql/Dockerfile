# This is your Editor pane. Write the Dockerfile here and 
# use the command line to execute commands
FROM java
ADD message-db-service-0.0.1.jar app.jar
EXPOSE 8080

#ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+PrintGCDetails", "-XX:+UseG1GC", "-jar","app.jar"]
ENTRYPOINT ["java", "-XX:+PrintFlagsFinal", "-XX:+UseG1GC", "-jar", "app.jar"]

#would need to use entrypoint script to get JAVA_OPTIONS and spring parameters working together!
#ENTRYPOINT exec java $GC_OPTIONS $JAVA_OPTIONS -jar app.jar
