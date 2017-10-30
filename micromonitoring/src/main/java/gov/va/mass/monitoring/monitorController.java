package gov.va.mass.monitoring;

/**
 * Created by n_nac on 10/26/2017.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.CompletableFuture;

@Service
public class monitorController {
        private static final Logger logger = LoggerFactory.getLogger(monitorController.class);

        private final RestTemplate restTemplate;

        public monitorController(RestTemplateBuilder restTemplateBuilder) {
            this.restTemplate = restTemplateBuilder.build();
        }

        @Async
        public CompletableFuture<String> findUser(String user) throws InterruptedException {
            logger.info("Looking up " + user);
            String url = "https://tools.ietf.org/rfc/rfc2606.txt";
            String results = restTemplate.getForObject(url, String.class);
            System.out.println(results);
            // Artificial delay of 1s for demonstration purposes
            Thread.sleep(1000L);
            return CompletableFuture.completedFuture(results);
        }

    }

