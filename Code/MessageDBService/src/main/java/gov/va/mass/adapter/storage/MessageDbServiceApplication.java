package gov.va.mass.adapter.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class MessageDbServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(MessageDbServiceApplication.class, args);
	}
	
}
