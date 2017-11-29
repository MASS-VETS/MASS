package gov.va.mass.adapter.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;


@SpringBootApplication
public class MockaudioensembleApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(MockaudioensembleApplication.class);


	public static void main(String[] args) {
		
		logger.info("Current Adapter Environment: " + System.getenv("ENV"));

		ApplicationContext springctxt = SpringApplication.run(MockaudioensembleApplication.class, args);

		logger.info("In MOCK ensemble for audio app");
	}
	
	
	@Bean
    public HttpMessageConverters customConverters() {
        ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        return new HttpMessageConverters(arrayHttpMessageConverter);
    }
}
