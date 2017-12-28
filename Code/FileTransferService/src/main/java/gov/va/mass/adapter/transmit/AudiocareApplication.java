package gov.va.mass.adapter.transmit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.ApplicationContext;

@SpringBootApplication

public class AudiocareApplication {

	public static void main(String[] args) {
		ApplicationContext springctxt = SpringApplication.run(AudiocareApplication.class, args);

		TLSHttpClientProvider tlsHttpClientProvider = springctxt.getBean(TLSHttpClientProvider.class);

		// Initialize the sender
		FileSenderOverHttpClient filesender = springctxt.getBean(FileSenderOverHttpClient.class);
		filesender.setTLSHttpClientProvider(tlsHttpClientProvider);

		// Initialize the getter
		FileGetterOverHttpClient filegetter = springctxt.getBean(FileGetterOverHttpClient.class);
		filegetter.setTLSHttpClientProvider(tlsHttpClientProvider);
	}

}
