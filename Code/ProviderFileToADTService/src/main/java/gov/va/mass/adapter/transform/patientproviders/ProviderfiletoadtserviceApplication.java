package gov.va.mass.adapter.transform.patientproviders;

import javax.jms.Queue;

import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;


@SpringBootApplication
@EnableJms
public class ProviderfiletoadtserviceApplication {
	
	 private static final Logger logger = LoggerFactory.getLogger(ProviderfiletoadtserviceApplication.class);

	public static void main(String[] args) {
		
		logger.info("Current Adapter Environment: " + System.getenv("ENV")); 
		
		ApplicationContext springctxt = SpringApplication.run(ProviderfiletoadtserviceApplication.class, args);
		
		String csvfilepath = "C:/work/sprint5/Patient_PCP_Columbus.txt";
		
		CsvToADTConverter converter =  springctxt.getBean(CsvToADTConverter.class);
		converter.initMapping();
		converter.convert(csvfilepath);
	}
	
	@Bean
	public Queue queue() {
		return new ActiveMQQueue("readytosendtoepic.queue");
	}	
}
