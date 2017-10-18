package gov.mass.adapter.inboundadaptergateway;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;


@RestController
@RequestMapping("/ReceiveAdapter")
public class RestServiceController {

    RestTemplate restTemplate;

    @Value("${app.serviceName}")
    private String serviceName;

    @Value("${app.service.url}")
    private String url;


    Message HL7Message;
    int  requestCount = 0;
    Date lastChecked = new Date();

    public RestServiceController() {
        restTemplate = new RestTemplate();
    }



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


    @PostMapping("/Inbound") // HeartBeat
//    @RequestMapping(value="Inbound",method = RequestMethod.POST, produces = "application/hl7-v2+er7")
    public String HL7Handler(@RequestHeader(value = "Content-Type") String contentType, @RequestHeader(value = "Content-Length") int contentLength, @RequestBody String requestBody) {
        HapiContext context = new DefaultHapiContext();
        Parser genericParser = context.getGenericParser();
        Message hapiMessage, ackResponse;
        String ACK;
        hapiMessage = null;
        ACK= null;
        try {
            System.out.println("Happy Message is   :" + requestBody + "end");

            hapiMessage = genericParser.parse(requestBody);
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.ALL));
            headers.set("Content-Type", "application/hl7-v2+er7;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<String>(requestBody, headers);
            ResponseEntity postResponse = restTemplate.postForEntity(url, entity,  String.class);

            if (postResponse.getStatusCodeValue() == 200)
            {
                ackResponse = hapiMessage.generateACK();
                ACK = ackResponse.toString().replace("\r", "\r\n");;
                System.out.println(" ======================================================");
                System.out.println("Happy ACK Response is " + ACK);
                System.out.println(" ======================================================\n");
                requestCount++;}
            else
            {
                ackResponse = hapiMessage.generateACK(AcknowledgmentCode.AE, new HL7Exception("HL7Exception"));
                ACK = ackResponse.toString().replace("\r", "\r\n");;
                //ACK = ackResponse.toString();
            }
        } catch (HL7Exception e) {
            e.printStackTrace();
            try {
                ackResponse = hapiMessage.generateACK(AcknowledgmentCode.AE, new HL7Exception("HL7Exception"));
                ACK = ackResponse.toString().replace("\r", "\r\n");;
                } catch (Exception ioe) {
                ioe.printStackTrace();
                return ACK;
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                ackResponse = hapiMessage.generateACK(AcknowledgmentCode.AE, new HL7Exception("IOException"));
                ACK = ackResponse.toString().replace("\r", "\r\n");
            } catch (Exception ioe) {
                ioe.printStackTrace();
                return ACK;
            }
        }
        return ACK;
    }

}
