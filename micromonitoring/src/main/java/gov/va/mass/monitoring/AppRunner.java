package gov.va.mass.monitoring;

/**
 * Created by n_nac on 10/26/2017.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Component
public class AppRunner implements CommandLineRunner {

    @Value("${microservice.internal.httpendpoints}")
    private String[] INTERNAL_HTTPURLS;

    @Value("${microservice.monitor.endpoints}")
    private String[] MONITOR_ENDPOINTS;

    @Value("${microService.httpendpoints}")
    private String[] HTTPSURLS;

    @Value("${microService.ssl.key-store}")
    private String[] KEY_STORE_PATH ;

    @Value("${microService.ssl.key-store-password}")
    private String[] KEY_STORE_PASSWORD ;

    @Value("${microService.ssl.keyStoreType}")
    private String[] KEY_STORE_TYPE ;



    private static final Logger logger = LoggerFactory.getLogger(AppRunner.class);

    private final MicroServiceLookupService MicroServiceLookupService;


    public AppRunner(MicroServiceLookupService MicroServiceLookupService) {
        this.MicroServiceLookupService = MicroServiceLookupService;
    }


    @Override
    public void run(String... args) throws Exception {

        String ServiceState[] = new String[HTTPSURLS.length+INTERNAL_HTTPURLS.length];
        CompletableFuture<String> heartBeat[] = null;
        heartBeat = new CompletableFuture[HTTPSURLS.length+INTERNAL_HTTPURLS.length];
        // Start the clock
        long start = System.currentTimeMillis();
        for(int j= 0; j< MONITOR_ENDPOINTS.length;j++) {


        /* Http Monitors */
            for (int i = 0; i < INTERNAL_HTTPURLS.length; i++) {
                String URL = INTERNAL_HTTPURLS[i].substring(INTERNAL_HTTPURLS[i].indexOf('@') + 1)+MONITOR_ENDPOINTS[j];
                String serviceName = INTERNAL_HTTPURLS[i].substring(+0, INTERNAL_HTTPURLS[i].indexOf('@'));
                heartBeat[i] = new CompletableFuture<String>();
                heartBeat[i] = MicroServiceLookupService.checkHTTPService(URL, serviceName);
                ServiceState[i] = String.valueOf(heartBeat[i].get());
            }
      /* Https Monitors */
            for (int i = 0; i < HTTPSURLS.length; i++) {
                String keyStorePath, keyStorePassword, keyStoreType = null;
                System.out.println(HTTPSURLS[i]);
                String URL = HTTPSURLS[i].substring(HTTPSURLS[i].indexOf('@') + 1)+MONITOR_ENDPOINTS[j];
                String serviceName = HTTPSURLS[i].substring(+0, HTTPSURLS[i].indexOf('@'));
                if (KEY_STORE_PATH[i].substring(0, KEY_STORE_PATH[i].indexOf('@')).equals(serviceName) &&
                        KEY_STORE_PASSWORD[i].substring(0, KEY_STORE_PASSWORD[i].indexOf('@')).equals(serviceName) &&
                        KEY_STORE_TYPE[i].substring(0, KEY_STORE_TYPE[i].indexOf('@')).equals(serviceName)) {
                    keyStorePath = KEY_STORE_PATH[i].substring(KEY_STORE_PATH[i].indexOf('@') + 1);
                    keyStorePassword = KEY_STORE_PASSWORD[i].substring(KEY_STORE_PASSWORD[i].indexOf('@') + 1);
                    keyStoreType = KEY_STORE_TYPE[i].substring(KEY_STORE_TYPE[i].indexOf('@') + 1);
                    System.out.println(URL + serviceName + keyStorePath + keyStoreType + keyStorePassword);
                    heartBeat[INTERNAL_HTTPURLS.length + i] = new CompletableFuture<String>();
                    heartBeat[INTERNAL_HTTPURLS.length + i] = MicroServiceLookupService.checkHTTPSService(URL, serviceName, keyStorePath, keyStorePassword, keyStoreType);
                } else {
                    System.out.println("Key Store Error");
                }
                ServiceState[INTERNAL_HTTPURLS.length + i] = String.valueOf(heartBeat[INTERNAL_HTTPURLS.length + i].get());
            }
        }
        CompletableFuture.allOf(heartBeat).join();

        // Kick of multiple, asynchronous lookups
        // Wait until they are all done



        System.out.println("All Reult " + Arrays.toString(ServiceState));
        

        // Print results, including elapsed time
        logger.info("Total time: " + (System.currentTimeMillis() - start));


    }

}