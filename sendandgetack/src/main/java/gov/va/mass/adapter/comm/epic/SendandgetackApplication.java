package gov.va.mass.adapter.comm.epic;

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
public class SendandgetackApplication {

	private static final Logger logger = LoggerFactory.getLogger(SendandgetackApplication.class);

	public static void main(String[] args) {
		logger.info("SendandgetackApplication running in environment: " + System.getenv("ENV"));
		SpringApplication.run(SendandgetackApplication.class, args);
	}
	
	@Bean
	public Queue dbqueue() {
		return new ActiveMQQueue("writetodb.queue");
	}
}
