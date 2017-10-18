package gov.va.mhv.ensemble.mockensemble;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.json.Json;
import javax.json.JsonObject;

@RestController
@RequestMapping("/Ensemble")
public class restServiceController {

    @Value("${app.serviceName}")
    private String serviceName;



    //HeartBeat and isAlive MicroService
    microServiceMonitor MSMonitor = new microServiceMonitor(serviceName);


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
        System.out.println("Checking HeartBeat");
        return String.valueOf(MSMonitor.getPulse());
    }


    // Receive through Post Service and write to a queue
    @PostMapping("/ReceiveHL7FromMASS")
    public void send(@RequestBody String HL7Message) {
        MSMonitor.incrementServiceIn();
        System.out.println("Ensemble Received. " + HL7Message);
        MSMonitor.incrementServiceOutSuccess();

    }


    }