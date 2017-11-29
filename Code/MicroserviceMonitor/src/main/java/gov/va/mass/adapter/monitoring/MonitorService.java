package gov.va.mass.adapter.monitoring;

import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import gov.va.mass.adapter.monitoring.config.InterfaceConfig;
import gov.va.mass.adapter.monitoring.config.MicroserviceConfig;
import gov.va.mass.adapter.monitoring.config.MonitorConfig;
import gov.va.mass.adapter.monitoring.email.EmailTemplate;
import gov.va.mass.adapter.monitoring.stats.BrokerStats;
import gov.va.mass.adapter.monitoring.stats.PulseStats;
import gov.va.mass.adapter.monitoring.stats.QueueStats;

/**
 * @author avolkano
 */
@Component
public class MonitorService {
	static final Logger log = LoggerFactory.getLogger(MonitorService.class);
	
	
	@Autowired
	private MonitorConfig config;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	EmailTemplate emailTemplate;
	
	@Scheduled(cron = "${monitor.rate}")
	public void showProperty() throws URISyntaxException {
		
		System.out.println("database url: " + config.getMessageDB().getUrl());
		
		// poll all the interfaces
		for (InterfaceConfig intf : config.getInterfaces()) {
			System.out.println("interface '" + intf.getName() + "'");
			
			System.out.println("   receiver url: " + intf.getReceiver().getUrl());
			PulseStats stats = new PulseStats(restTemplate, intf.getReceiver().getUrl());
			log.info(stats.toString());

			MicroserviceConfig transform = intf.getTransform();
			if (transform != null && transform.getUrl() != null) {
				System.out.println("  has transform? " + (!transform.getUrl().isEmpty()));
				System.out.println("  transform url: " + transform.getUrl());
				//stats = new PulseStats(restTemplate, transform.getUrl());
				//log.info(stats.toString());
			} else {
				System.out.println("  has transform? false");
			}

			System.out.println("     sender url: " + intf.getSender().getUrl());
			//stats = new PulseStats(restTemplate, intf.getSender().getUrl());
			//log.info(stats.toString());
		}
		System.out.println();
		
		// poll activemq
		BrokerStats stats = new BrokerStats(restTemplate, config.getJms().getUri());
		for (QueueStats q : stats.queues) {
			log.info(q.toString());
		}
		System.out.println();
		emailTemplate.SendMail(config.getEmail().getToAddress(), "Hello", "You received an email from the cloud!");
		
	}
}