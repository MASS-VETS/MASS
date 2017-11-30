package gov.va.mass.adapter.comm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableJms
public class ReceiveOverHapiApplication {

	private static final Logger logger = LoggerFactory.getLogger(ReceiveOverHapiApplication.class);

	public static void main(String[] args) {
		logger.info("Current Adapter Environment: " + System.getenv("ENV"));
		SpringApplication.run(ReceiveOverHapiApplication.class, args);
	}
}