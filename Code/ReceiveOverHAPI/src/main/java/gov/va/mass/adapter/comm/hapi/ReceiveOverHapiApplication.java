package gov.va.mass.adapter.comm.hapi;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;


import javax.jms.Queue;

import org.apache.activemq.command.ActiveMQQueue;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// TODO dedup the log entries

@SpringBootApplication
@EnableJms

public class ReceiveOverHapiApplication {
     
    private static final Logger logger = LoggerFactory.getLogger(ReceiveOverHapiApplication.class);
 
    
	public static void main(String[] args) {
		
		ApplicationContext springctxt = SpringApplication.run(ReceiveOverHapiApplication.class, args);
		
		logger.info("Current Adapter Environment: " + System.getenv("ENV")); 
		
		Server server = new Server();

		JettyTLSConfig tlscfg =  springctxt.getBean(JettyTLSConfig.class) ;
		tlscfg.setServer(server);
		tlscfg.setup();
			
		
		ServiceConfig srvcfg = (ServiceConfig)springctxt.getBean("serviceConfig") ;
		srvcfg.setServer(server);
		srvcfg.setSpringContext(springctxt);
		srvcfg.setup();
		
		
	
		try	{
			server.start();
		}
		catch (Exception e) {
			logger.error("Unable to start Jetty");
			e.printStackTrace();
		}	
	}

	// TODO : Consider externalizing queue name Will need multipe queues
	@Bean
	public Queue msgqueue() {
		return new ActiveMQQueue("readytosendtoepic.queue");
	}
	
	@Bean
	public Queue dbqueue() {
		return new ActiveMQQueue("writetodb.queue");
	}
	
}

//private static final String KEYSTORE_LOCATION = "C:/work/1twowayssl/epickeys/epicpvtks.jks" ; 