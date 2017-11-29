# This is your Editor pane. Write the Dockerfile here and 
# use the command line to execute commands
FROM java
ADD message-db-service-0.0.1.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar","app.jar"]
CMD ["--jms.inputQ=sqloutschedQ"]
