package gov.va.mass.adapter.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import gov.va.mass.adapter.monitoring.config.MonitorConfig;
import gov.va.mass.adapter.monitoring.email.EmailTemplate;

/**
 * @author avolkano
 */
@SpringBootApplication
@EnableScheduling
public class MonitorServiceApplication {
	static final Logger log = LoggerFactory.getLogger(MonitorServiceApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(MonitorServiceApplication.class, args);
	}
	
	@Autowired
	MonitorConfig config;
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		String brokerUsername = config.getJms().getUsername();
		String brokerPassword = config.getJms().getPassword();
		if (!brokerUsername.isEmpty() && !brokerPassword.isEmpty()) {
			log.info("using basic auth");
			builder = builder.basicAuthorization(brokerUsername, brokerPassword);
		} else {
			log.info("not using basic auth...");
		}
		return builder.build();
	}
	
	@Bean
	public EmailTemplate emailTemplate()
	{
		return new EmailTemplate(config.getEmail().getFromAddress(), config.getEmail().getPassword(), config.getSMTP().getHost(), config.getSMTP().getPort());
	}
	
}
