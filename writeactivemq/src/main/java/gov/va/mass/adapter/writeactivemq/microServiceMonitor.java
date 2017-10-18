package gov.va.mass.adapter.writeactivemq;

import org.apache.activemq.broker.jmx.DestinationViewMBean;
import org.springframework.beans.factory.annotation.Value;

import javax.json.Json;
import javax.json.JsonObject;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class microServiceMonitor {

    @Value("${app.queueName}")
    private String queueName;

    private String serviceName;
    private int serviceIn;
    private int serviceOutSuccess;
    private int serviceOutFailed;
    private long messageInPipeOld;
    private long messageInPipeNow;
    private long pastTime;
    private long currentTime;
    private String alertMessage;
    private long queueSize;
    private JsonObject pulseObject = null;

    /* Constructor */
    public microServiceMonitor(String queueName, String serviceName) {
        serviceIn = 0 ;
        serviceOutSuccess = 0;
        serviceOutFailed = 0;
        messageInPipeOld = 0;
        messageInPipeNow = 0;
        pastTime = System.currentTimeMillis()/1000;
        currentTime = System.currentTimeMillis()/1000;
        queueSize = 0;
        this.serviceName = serviceName;
        this.queueName = queueName;
    }

/* Service Name */
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /* Queue Name */
    public String getQueueName() {
        return serviceName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }



    /* Service In */
    public int getServiceIn() {
        return serviceIn;
    }

    public void incrementServiceIn() {
        serviceIn++;
    }


/* service Out Success */
    public int getServiceOutSuccess() {
        return serviceOutSuccess;
    }

    public void incrementServiceOutSuccess() {
        serviceOutSuccess++;
    }

/* service Out Failed */
    public int getServiceOutFailed() {
        return serviceOutFailed;
    }

    public void incrementServiceOutFailed() {
        serviceOutFailed++;
    }


/* Alert Message */
    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
        System.out.println("Setting Alert");
        printAlert();
    }


    public String getPulse(){

        try {
            String url = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi";
            JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url));
            MBeanServerConnection connection = connector.getMBeanServerConnection(); /* Change Queue Name */
            ObjectName nameConsumers = new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=OrderTransactionQueue");
            DestinationViewMBean mbView = MBeanServerInvocationHandler.newProxyInstance(connection, nameConsumers, DestinationViewMBean.class, true);
            messageInPipeOld = messageInPipeNow;
            messageInPipeNow = mbView.getQueueSize();
            pastTime = currentTime;
            currentTime = System.currentTimeMillis()/1000;
            System.out.println(queueName + "," + messageInPipeOld + "," + messageInPipeNow + "," + pastTime + "," + currentTime);

            pulseObject = Json.createObjectBuilder()
                    .add("status", "PulseSuccess")
            //        .add("serviceName",serviceName)
                    .add("queueName", queueName)
                    .add("messageInPipeOld", messageInPipeOld)
                    .add("messageInPipeNow",messageInPipeNow)
                    .add("pastTime",pastTime)
                    .add("currentTime",currentTime)
                    .add("serviceIn",serviceIn)
                    .add("serviceOutSuccess",serviceOutSuccess)
                    .add("serviceOutFailed",serviceOutFailed)
                    .build();
            System.out.println(pulseObject.toString());
            return pulseObject.toString();
        }
        catch(Exception e)
        {
            setAlertMessage(serviceName +  "  service is up. But connection to get pulse is not up " );
            pulseObject = Json.createObjectBuilder()
                    .add("status", "PulseError")
                    .build();
            return pulseObject.toString();

        }
    }

    public void printAlert( ) {
        System.out.println("***************" +  alertMessage +"*****************");
    }

    }