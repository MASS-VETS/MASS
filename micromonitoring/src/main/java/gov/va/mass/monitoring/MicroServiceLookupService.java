package gov.va.mass.monitoring;

/**
 * Created by n_nac on 10/26/2017.
 */

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.json.Json;
import javax.json.JsonObject;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.CompletableFuture;

@Service
public class MicroServiceLookupService {

    private static final Logger logger = LoggerFactory.getLogger(MicroServiceLookupService.class);

    private final RestTemplate restTemplate;

    @Value("${microService.ssl.key-store}")
    private String[] KEY_STORE_PATH ;

    @Value("${microService.ssl.key-store-password}")
    private String[] KEY_STORE_PASSWORD ;

    @Value("${microService.ssl.keyStoreType}")
    private String[] KEY_STORE_TYPE ;




    public MicroServiceLookupService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Async
    public CompletableFuture<String> checkHTTPSService( String URL, String serviceName,String keyStorePath,String keyStorePassword,String keyStoreType) throws InterruptedException {
        logger.info("Looking up ");
        String Output = null;

           try {

            System.out.println("Real Output" +Output);
            Output = mockEpicSecureConnect(URL,serviceName, keyStorePath,keyStorePassword,keyStoreType);

            }
        catch(Exception e){
            Output = "{\"serviceName\":\""+ serviceName + "\",\"isAlive\": false}";
            }
        Thread.sleep(1000L);

        return CompletableFuture.completedFuture(Output);
    }

    @Async
    public CompletableFuture<String> checkHTTPService( String URL, String serviceName) throws InterruptedException {
        logger.info("Looking up ");
        String Output = null;

        try{
            Output =  restTemplate.getForObject(URL,String.class);
            System.out.println(Output);
        }
        catch(Exception e){
            Output = "{\"serviceName\":\""+ serviceName + "\",\"isAlive\": false}";
        }
        Thread.sleep(1000L);
        MicroService results = null;
        return CompletableFuture.completedFuture(Output);
    }



    public String mockEpicSecureConnect(String url,String serviceName,String keyStorePath,String keyStorePassword,String keyStoreType) throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

        // Load the key store, containing the client-side certificate.

        System.out.println(url + serviceName + keyStorePath + keyStoreType+ keyStorePassword);
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        InputStream keyStoreInput = new FileInputStream(keyStorePath);
        keyStore.load(keyStoreInput, keyStorePassword.toCharArray());
        System.out.println("Key store has " + keyStore.size() + " keys");

        // Create an SSL context with our private key store.
        // We are only loading the key-material here, but if your server uses a self-signed certificate,
        // you will need to load the trust-material (a JKS key-store containing the server's public SSL
        // certificate) as well.
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
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
            HttpGet httpGet = new HttpGet(url);
            long startTime = System.currentTimeMillis();
               try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    HttpEntity entity = response.getEntity();

                    System.out.println("----------------------------------------");
                    System.out.println(response.getStatusLine()+"  " +entity.getContent());
                    if (entity != null) {
                        String returnString = EntityUtils.toString(entity);
                        System.out.println("Is it here" + returnString);
                        return returnString;
                    }
                    else{
                        JsonObject statusObject = Json.createObjectBuilder()
                                .add("serviceName",serviceName)
                                .add("isAlive", false)
                                .build();
                        return statusObject.toString();
                    }

                }
        }
    }
}