package avo.hax.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BulkSenderServiceApplication {
	static final Logger log = LoggerFactory.getLogger(BulkSenderServiceApplication.class);
	
	@Autowired
	BulkSenderService svc;
	
	public static void main(String[] args) {
		SpringApplication.run(BulkSenderServiceApplication.class, args);
	}
	
	@Bean
	CommandLineRunner run() {
		return new CommandLineRunner() {
			
			@Override
			public void run(String... args) throws Exception {
				String response = svc.sendAndWait();
				log.info(response);
			}
		};
	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}
}
