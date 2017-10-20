package gov.va.mass.epic.mockepic;



        import org.apache.http.HttpEntity;
        import org.apache.http.client.methods.CloseableHttpResponse;
        import org.apache.http.client.methods.HttpGet;
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
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestBody;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;

        import javax.net.ssl.SSLContext;
        import java.io.FileInputStream;
        import java.io.IOException;
        import java.io.InputStream;
        import java.security.*;
        import java.security.cert.CertificateException;

/**
 * Demonstrate connecting to a server secured with client-side SSL certificates.
 */
@RestController
@RequestMapping("/Epic")

public class ClientController {


    @Value("${app.serviceName}")
    private String serviceName;

    @Value("${app.service.url}")
    private String URL;

    @Value("${app.ssl.key-store}")
    private String KEY_STORE_PATH ;

    @Value("${app.ssl.key-store-password}")
    private String KEY_STORE_PASSWORD ;

    @Value("${app.ssl.keyStoreType}")
    private String KEY_STORE_TYPE ;

    String HL7;

    @PostMapping("/mockCount")
    public void mockEpicSecureConnect(@RequestBody String Count) throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

        HL7 = 	"MSH|^~\\&|||||200803051508||ADT^A31|2|P|2.5\r" +
                "EVN||200803051509\r" +
                "PID|||ZZZZZZ83M64Z148R^^^SSN^SSN^^20070103\r";

        HL7 = HL7.toString().replace("\r", "\r\n");;
        System.out.println("Requst is " + HL7);
        // Load the key store, containing the client-side certificate.
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        InputStream keyStoreInput = new FileInputStream(KEY_STORE_PATH);
        keyStore.load(keyStoreInput, KEY_STORE_PASSWORD.toCharArray());
        System.out.println("Key store has " + keyStore.size() + " keys");

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
            for(int i = 0; i<Integer.parseInt(Count); i++)
            {
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
            }
            long endTime = System.currentTimeMillis();
            System.out.println( " Start Time " + startTime + " End Time " + endTime + " Total Time " + ( endTime - startTime ));
        }
    }
}