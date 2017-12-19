package gov.va.mass.monitoring;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MicromonitoringApplication {

	public static void main(String[] args) {
		// close the application context to shut down the custom ExecutorService
		SpringApplication.run(MicromonitoringApplication.class, args);
	}



	@Bean
	@Scheduled(fixedRate = 1000)
	public Executor asyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("HeartBeat-");
		executor.initialize();
		return executor;
	}


}



