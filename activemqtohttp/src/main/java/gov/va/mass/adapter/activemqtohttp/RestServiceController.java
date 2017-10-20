package gov.va.mass.adapter.activemqtohttp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonObject;

@Component
public class RestServiceController {

    @Value("${app.queueName}")
    private String queueName;

    @Value("${app.serviceName}")
    private String serviceName;

    @Value("${app.service.url}")
    private String url;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }


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


    @JmsListener(destination = "${app.queueName}")
    public void receiveQueue(String text) {

        System.out.println( queueName  + " received "  +text);
        text = normalizeString(text);

        MSMonitor.incrementServiceIn();
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response
                    = restTemplate.postForEntity(url,text, String.class);

            System.out.println("Result is" + response.getBody());
            System.out.println("Result Status is" + response.getStatusCode());
            MSMonitor.incrementServiceOutSuccess();
        }
        catch(Exception e){

            MSMonitor.incrementServiceOutFailed();
        }
    }

    public String normalizeString(String data){
        String NewString = data.substring(1, data.length()-1);
        return NewString;
    }

}
