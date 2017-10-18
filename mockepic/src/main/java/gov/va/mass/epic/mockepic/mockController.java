package gov.va.mass.epic.mockepic;

import ca.uhn.hl7v2.hoh.api.IReceivable;
import ca.uhn.hl7v2.hoh.api.ISendable;
import ca.uhn.hl7v2.hoh.encoder.EncodingStyle;
import ca.uhn.hl7v2.hoh.raw.api.RawSendable;
import ca.uhn.hl7v2.hoh.raw.client.HohRawClientSimple;
import ca.uhn.hl7v2.hoh.sockets.CustomCertificateTlsSocketFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonObject;
import java.net.URL;

@RestController
@RequestMapping("/Epic")
public class mockController {


    @Value("${app.serviceName}")
    private String serviceName;

    @Value("${app.service.url}")
    private String url;

    @Value("${app.ssl.key-store}")
    private String KEYSTORE_LOCATION ;

    @Value("${app.ssl.key-store-password}")
    private String KEYSTORE_PASSWORD ;

    @Value("${app.ssl.keyStoreType}")
    private String KEYSTORE_TYPE ;

    @Value("${maxattempts.epicsend}")
    private int MAX_SEND_ATTEMPTS ;

    @Value("${sendattempt.interval}")
    private int SEND_ATTEMPT_INTERVAL ;

    private int sendattemptcounter = 0 ;

    URL outBoundURL = null;


    private String HL7 = null;
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
    @PostMapping("/mockCount")
    public void send(@RequestBody String count) {

        HL7 = 	"MSH|^~\\&|||||200803051508||ADT^A31|2|P|2.5\r" +
                "EVN||200803051509\r" +
                "PID|||ZZZZZZ83M64Z148R^^^SSN^SSN^^20070103\r";

        HL7 = HL7.toString().replace("\r", "\r\n");;

        CustomCertificateTlsSocketFactory customtlsSF = new CustomCertificateTlsSocketFactory( KEYSTORE_TYPE, KEYSTORE_LOCATION , KEYSTORE_PASSWORD  );
        try {
            outBoundURL = new URL(url);
        }
        catch(Exception e)
        {

        }
        HohRawClientSimple client = new HohRawClientSimple (outBoundURL);// parser);
        client.setSocketFactory( customtlsSF );
        ISendable rawsendable = new RawSendable(HL7);
        EncodingStyle es = rawsendable.getEncodingStyle();

        /* Loop Start */

        long startTime = System.currentTimeMillis();
        System.out.println(" Start Time " + startTime );
        for(int i = 0;  i< Integer.parseInt(count); i++) {
        try {
            MSMonitor.incrementServiceIn();
            IReceivable<String> receivable = client.sendAndReceive(rawsendable);
            String respstring = receivable.getMessage();
            MSMonitor.incrementServiceOutSuccess();
        } catch (Exception e) {
            MSMonitor.incrementServiceOutFailed();
        }
    }
        /*for(int i = 0;  i< Integer.parseInt(count); i++) {
            MSMonitor.incrementServiceIn();
            System.out.println("Sending Mesage Number . " + i);
            RestTemplate restTemplate = new RestTemplate();
            try {
                String result = restTemplate.postForObject(url, HL7, String.class);
                System.out.println(result);
                MSMonitor.incrementServiceOutSuccess();
            }
            catch(Exception e){
                MSMonitor.incrementServiceOutFailed();
            }

        }*/

        long endTime = System.currentTimeMillis();
        System.out.println(" End Time " + endTime );

        long totalTime = endTime - startTime;
        System.out.println(" Total Time " + totalTime );

    }
}
