package gov.va.mass.adapter.transmit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.ApplicationContext;


@SpringBootApplication

public class AudiocareApplication {

	private static final Logger logger = LoggerFactory.getLogger(AudiocareApplication.class);
	
	public static void main(String[] args) {
		logger.info("Current Adapter Environment: " + System.getenv("ENV")); 
		
		ApplicationContext springctxt = SpringApplication.run(AudiocareApplication.class, args);
		
		logger.info("In Audiocare App");
		
		// get the tls configured resttemplate from the provider and then pass it to the FileGetter
		TLSSpringTemplateProvider templateProvider = springctxt.getBean(TLSSpringTemplateProvider.class);

		//Initialize the file receiver (getter)
		AudioResponseFileGetter filegetter = springctxt.getBean(AudioResponseFileGetter.class);
		filegetter.setTemplateProvider (templateProvider);
		
		
		
		
		//Initialize the  sender
		AppointmentsFileSender filesender = springctxt.getBean(AppointmentsFileSender.class);
		filesender.setTemplateProvider (templateProvider);


		
//		AppointmentsFileSenderWithSpring filesender = springctxt.getBean(AppointmentsFileSender.class);
//		filesender.setTemplateProvider (templateProvider);

		
	}
		
}

//FileTransmitter srvc = springctxt.getBean(FileTransmitter.class);
//srvc.sendApointmentFileToEnsemble();
//	srvc.getResponseFromEnsemble();

//logger.debug("Testing get over resttemplate");	
//FileGetterOverRestTemplate getsvc =  springctxt.getBean(FileGetterOverRestTemplate.class);
//getsvc.getWithRestTemplate( );

