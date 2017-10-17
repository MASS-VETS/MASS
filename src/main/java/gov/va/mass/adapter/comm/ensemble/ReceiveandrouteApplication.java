package gov.va.mass.adapter.comm.ensemble;

import javax.jms.Queue;

import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class ReceiveandrouteApplication {

	private static final Logger logger = LoggerFactory.getLogger(ReceiveandrouteApplication.class);


	public static void main(String[] args) {
		SpringApplication.run(ReceiveandrouteApplication.class, args);

		logger.info("Running in environment: dev " ); 
	}

	@Bean
	public Queue queue() {
		return new ActiveMQQueue("readytosendtoepic.queue");
	}
}
