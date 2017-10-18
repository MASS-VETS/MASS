package gov.va.mass.adapter.writeactivemq;


import org.apache.activemq.broker.jmx.DestinationViewMBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import javax.json.Json;
import javax.json.JsonObject;


@RestController
@RequestMapping("/transaction")
public class restServiceController {

    @Autowired private JmsTemplate jmsTemplate;

    @Value("${app.queueName}")
    private String queueName;

    @Value("${app.serviceName}")
    private String serviceName;

    //HeartBeat and isAlive MicroService
   microServiceMonitor MSMonitor = new microServiceMonitor(queueName,serviceName);



    @GetMapping("/isAlive") // HeartBeat
    public String isAlive() {
        System.out.println("Checking Service" );
        JsonObject isAliveObject = Json.createObjectBuilder()
                .add("serviceName",serviceName)
                .add("isAlive", true)
                .build();


        return isAliveObject.toString();
    }

    @GetMapping("/Pulse") // HeartBeat
    public String Pulse() {
     MSMonitor.setServiceName(serviceName);
     MSMonitor.setQueueName(queueName);
     System.out.println("Checking HeartBeat");
     return String.valueOf(MSMonitor.getPulse());
    }

// Receive through Post Service and write to a queue
    @PostMapping("/writeToActiveMQ")
    public void send(@RequestBody String HL7Message) {
        MSMonitor.incrementServiceIn();
        System.out.println("Sending the transaction. " + HL7Message);
        try{
            jmsTemplate.convertAndSend( queueName, HL7Message);
            MSMonitor.incrementServiceOutSuccess();
        }catch (Exception e) {
            System.out.println("JMS Error");
            MSMonitor.setAlertMessage(serviceName +  " Having Connection issue to " + queueName);
            MSMonitor.incrementServiceOutFailed();
        }
        }
}













/*

        long mainTime = System.currentTimeMillis()/1000;
        while (true) {
            Socket socket = serverSocket.accept();
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os, true);

            if((System.currentTimeMillis()/1000-mainTime)%60>1)){
                String time = getTime();
                pw.println("Current server time is: " + time);
            }

            pw.close();
            socket.close();
        }
    }
 */