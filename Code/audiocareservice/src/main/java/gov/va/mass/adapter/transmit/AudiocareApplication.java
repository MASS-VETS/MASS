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

		TLSHttpClientProvider tlsHttpClientProvider = springctxt.getBean(TLSHttpClientProvider.class);

		// Initialize the sender
		FileSenderOverHttpClient filesender = springctxt.getBean(FileSenderOverHttpClient.class);
		filesender.setTLSHttpClientProvider(tlsHttpClientProvider);

		// Initialize the sender
		FileGetterOverHttpClient filegetter = springctxt.getBean(FileGetterOverHttpClient.class);
		filegetter.setTLSHttpClientProvider(tlsHttpClientProvider);

		// AppointmentsFileSenderWithSpring filesender =
		// springctxt.getBean(AppointmentsFileSender.class);
		// filesender.setTemplateProvider (templateProvider);

	}

}
