package gov.va.mass.adapter.comm;

import javax.jms.Queue;

import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class SendAndGetAckApplication {

	private static final Logger logger = LoggerFactory.getLogger(SendAndGetAckApplication.class);

	public static void main(String[] args) {
		logger.info("SendandgetackApplication running in environment: " + System.getenv("ENV"));
		SpringApplication.run(SendAndGetAckApplication.class, args);
	}

	@Bean
	public Queue dbqueue() {
		return new ActiveMQQueue("writetodb.queue");
	}
}
