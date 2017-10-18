package gov.va.mass.adapter.outboundadaptergateway;


import javax.json.Json;
import javax.json.JsonObject;

public class microServiceMonitor {


    private String serviceName;
    private int serviceIn;
    private int serviceOutSuccess;
    private int serviceOutFailed;
    private long pastTime;
    private long currentTime;
    private String alertMessage;
    private long queueSize;
    private JsonObject pulseObject = null;

    /* Constructor */
    public microServiceMonitor( String serviceName) {
        serviceIn = 0 ;
        serviceOutSuccess = 0;
        serviceOutFailed = 0;
        pastTime = System.currentTimeMillis()/1000;
        currentTime = System.currentTimeMillis()/1000;
        queueSize = 0;
        this.serviceName = serviceName;
    }

    /* Service Name */
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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
            pastTime = currentTime;
            currentTime = System.currentTimeMillis()/1000;
            System.out.println( pastTime + "," + currentTime);

            pulseObject = Json.createObjectBuilder()
                    .add("status", "PulseSuccess")
                    .add("serviceName",serviceName)
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