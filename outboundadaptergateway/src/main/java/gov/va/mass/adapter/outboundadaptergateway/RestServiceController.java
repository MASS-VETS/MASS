package gov.va.mass.adapter.outboundadaptergateway;

import ca.uhn.hl7v2.hoh.api.IReceivable;
import ca.uhn.hl7v2.hoh.api.ISendable;
import ca.uhn.hl7v2.hoh.encoder.EncodingStyle;
import ca.uhn.hl7v2.hoh.raw.api.RawSendable;
import ca.uhn.hl7v2.hoh.raw.client.HohRawClientSimple;
import ca.uhn.hl7v2.hoh.sockets.CustomCertificateTlsSocketFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.EncodeException;
import java.io.IOException;
import java.net.URL;

@RestController
@RequestMapping("/SendAdapter")
public class RestServiceController {
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

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
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


    @PostMapping("/Outbound") // HeartBeat
    public void OutBoundHL7Handler(@RequestBody String  outBoundMessage) {

        CustomCertificateTlsSocketFactory customtlsSF = new CustomCertificateTlsSocketFactory(KEYSTORE_TYPE, KEYSTORE_LOCATION, KEYSTORE_PASSWORD);
        try {
            outBoundURL = new URL(url);
        } catch (Exception e) {

        }
        HohRawClientSimple client = new HohRawClientSimple(outBoundURL);// parser);
        client.setSocketFactory(customtlsSF);
        ISendable rawsendable = new RawSendable(outBoundMessage);
        EncodingStyle es = rawsendable.getEncodingStyle();
        try {
            IReceivable<String> receivable = client.sendAndReceive(rawsendable);
            String respstring = receivable.getMessage();
        } catch (Exception e) {

        }
    }
      /*
            sendattemptcounter = 0 ;
        do {
            try {
                ++sendattemptcounter;
                IReceivable<String> receivable = client.sendAndReceive(rawsendable);
                String respstring = receivable.getMessage();
                } catch (Exception e) {
               if (sendattemptcounter == MAX_SEND_ATTEMPTS ) {
                   System.out.println("MAX attempts to send to EPIC exceeded.");
                   break;
                }
        try {
                    Thread.sleep (SEND_ATTEMPT_INTERVAL);
                } catch (Exception e2) {
                    // TODO: what would cause this?
                    // what is the best response to this situation?
                }


      //  } while ( sendattemptcounter < MAX_SEND_ATTEMPTS );
    }
*/
}
