package gov.va.mass.adapter.comm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class SendAndGetAckApplication {

	public static void main(String[] args) {
		SpringApplication.run(SendAndGetAckApplication.class, args);
	}
}
