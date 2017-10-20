package gov.va.mass.adapter.microcontroller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class MicrocontrollerApplication {

	private static final Logger log = LoggerFactory.getLogger(MicrocontrollerApplication.class);

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(MicrocontrollerApplication.class, args);
	}

	@Bean
	public CommandLineRunner run(RestTemplate restTemplate) throws Exception {
		return args -> {
			serviceEndPoint serviceEndPoint = restTemplate.getForObject(
					"http://gturnquist-quoters.cfapps.io/api/random", serviceEndPoint.class);
			log.info(serviceEndPoint.toString());
		};
	}
}


