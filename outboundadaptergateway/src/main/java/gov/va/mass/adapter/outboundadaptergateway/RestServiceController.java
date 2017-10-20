package gov.va.mass.adapter.outboundadaptergateway;

import ca.uhn.hl7v2.hoh.api.IReceivable;
import ca.uhn.hl7v2.hoh.api.ISendable;
import ca.uhn.hl7v2.hoh.encoder.EncodingStyle;
import ca.uhn.hl7v2.hoh.hapi.api.MessageSendable;
import ca.uhn.hl7v2.hoh.raw.api.RawSendable;
import ca.uhn.hl7v2.hoh.raw.client.HohRawClientSimple;
import ca.uhn.hl7v2.hoh.sockets.CustomCertificateTlsSocketFactory;
import ca.uhn.hl7v2.model.Message;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonObject;
import javax.net.ssl.SSLContext;
import javax.websocket.DecodeException;
import javax.websocket.EncodeException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

@RestController
@RequestMapping("/SendAdapter")
public class RestServiceController {
    @Value("${app.serviceName}")
    private String serviceName;

    @Value("${app.service.url}")
    private String url;

    @Value("${app.service.url}")
    private String URL;

    @Value("${app.ssl.key-store}")
    private String KEY_STORE_PATH ;

    @Value("${app.ssl.key-store-password}")
    private String KEY_STORE_PASSWORD ;

    @Value("${app.ssl.keyStoreType}")
    private String KEY_STORE_TYPE ;

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
    public void mockEpicSecureConnect(@RequestBody String HL7) throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {


        // Load the key store, containing the client-side certificate.
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        InputStream keyStoreInput = new FileInputStream(KEY_STORE_PATH);
        keyStore.load(keyStoreInput, KEY_STORE_PASSWORD.toCharArray());
        System.out.println("HL7 message is " + HL7 + " keys");

        // Create an SSL context with our private key store.
        // We are only loading the key-material here, but if your server uses a self-signed certificate,
        // you will need to load the trust-material (a JKS key-store containing the server's public SSL
        // certificate) as well.
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, KEY_STORE_PASSWORD.toCharArray())
                .useTLS()
                .build();

        // Prepare the HTTPClient.
        HttpClientBuilder builder = HttpClientBuilder.create();

        SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(
                sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        builder.setSSLSocketFactory(sslConnectionFactory);
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslConnectionFactory)
                .register("http", new PlainConnectionSocketFactory())
                .build();
        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
        builder.setConnectionManager(ccm);

        // Perform a sample HTTP request.

        try (CloseableHttpClient httpClient = builder.build()) {
            HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Content-Type","application/hl7-v2+er7; charset=utf-8");
            StringEntity HL7Entity = new StringEntity(HL7);
            httpPost.setEntity(HL7Entity);
            System.out.println(httpPost.getURI() +  httpPost.getMethod());
            long startTime = System.currentTimeMillis();
            System.out.println(startTime);
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    HttpEntity entity = response.getEntity();

                    System.out.println("----------------------------------------");
                    System.out.println(response.getStatusLine());
                    if (entity != null) {
                        System.out.println("Response content length: " + entity.getContentLength());
                        System.out.printf(EntityUtils.toString(entity));
                    }
                    EntityUtils.consume(entity);
                }

            long endTime = System.currentTimeMillis();
            System.out.println( " Start Time " + startTime + " End Time " + endTime + " Total Time " + ( endTime - startTime ));
        }
    }

    /*
    public void OutBoundHL7Handler(@RequestBody String  outBoundString) {
        MSMonitor.incrementServiceIn();
        CustomCertificateTlsSocketFactory customtlsSF = new CustomCertificateTlsSocketFactory(KEYSTORE_TYPE, KEYSTORE_LOCATION, KEYSTORE_PASSWORD);
        try {
        outBoundURL = new URL(url);
        HohRawClientSimple client = new HohRawClientSimple(outBoundURL);// parser);
        client.setSocketFactory(customtlsSF);
        ISendable sendable = new RawSendable(outBoundString);
        EncodingStyle es = sendable.getEncodingStyle();
        System.out.println("respstring");
        IReceivable <String> receivable = client.sendAndReceive(sendable);
        MSMonitor.incrementServiceOutSuccess();
        } catch (Exception e) {
        MSMonitor.incrementServiceOutFailed();
        }
    }*/
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
