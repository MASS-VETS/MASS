package gov.va.mass.adapter.monitoring;

import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import gov.va.mass.adapter.monitoring.config.AlertConfig;
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
	
	private long lastQueueMaxTime;
	private long lastConsumerMaxTime;
	private long lastEnqueuedMaxTime;
	private long lastDequeuedMaxTime;
	
	@Autowired
	private MonitorConfig config;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	EmailTemplate emailTemplate;
	
	@Scheduled(cron = "${monitor.rate}")
	public void showProperty() throws URISyntaxException {
		
		PulseStats stats = new PulseStats(restTemplate, config.getMessagedb().getUrl());
		log.info(stats.toString() + " url: " + config.getMessagedb().getUrl());
		
		// poll all the interfaces
		for (InterfaceConfig intf : config.getInterfaces()) {
			System.out.println("interface '" + intf.getName() + "'");
			
			stats = new PulseStats(restTemplate, intf.getReceiver().getUrl());
			log.info(stats.toString() + " url: " + intf.getReceiver().getUrl());

			MicroserviceConfig transform = intf.getTransform();
			if (transform != null && transform.getUrl() != null) {
				stats = new PulseStats(restTemplate, transform.getUrl());
				log.info(stats.toString() + " url: " + transform.getUrl());
			} else {
				System.out.println("     transform url: n/a");
			}

			MicroserviceConfig sender = intf.getSender();
			if (sender != null && sender.getUrl() != null) {
				stats = new PulseStats(restTemplate, sender.getUrl());
				log.info(stats.toString() + " url: " + sender.getUrl());
			} else {
				System.out.println("     sender url: n/a");
			}

		}
		System.out.println();
		
		// poll activemq
		BrokerStats bstats = new BrokerStats(restTemplate, config.getJms().getUri());
		for (QueueStats q : bstats.queues) {
			log.info(q.toString());
			checkQueueAlert(q, config);
		}
		System.out.println();
		
	}
	
	private void checkQueueAlert(QueueStats q, MonitorConfig config)
	{
		for (AlertConfig alert : config.getAlerts() ) {
			if (!(q.name.equals(alert.getName()))) continue;
         	if ((alert.getQueueMax() != null)&&(q.queueSize > alert.getQueueMax())) {
         		if ((System.currentTimeMillis() - lastQueueMaxTime) < (alert.getTimer() * 60000)) break;
				emailTemplate.SendMail(config.getEmail().getToAddress(), "Queue Size Alert", "Queue " + q.name + " has a queue size greater than " + alert.getQueueMax() + ".");
				lastQueueMaxTime = System.currentTimeMillis();
			}
			if ((alert.getConsumerMax() != null)&&(q.consumerCount > alert.getConsumerMax())) {
				if ((System.currentTimeMillis() - lastConsumerMaxTime) < (alert.getTimer() * 60000)) break;
				emailTemplate.SendMail(config.getEmail().getToAddress(), "Consumer Count Alert", "Queue " + q.name + " has a consumer count greater than " + alert.getConsumerMax() + ".");
				lastConsumerMaxTime = System.currentTimeMillis();
			}
			if ((alert.getEnqueuedMax() != null)&&(q.enqueuedCount > alert.getEnqueuedMax())) {
				if ((System.currentTimeMillis() - lastEnqueuedMaxTime) < (alert.getTimer() * 60000)) break;
				emailTemplate.SendMail(config.getEmail().getToAddress(), "Enqueued Count Alert", "Queue " + q.name + " has an enqueued count greater than " + alert.getEnqueuedMax() + ".");
				lastEnqueuedMaxTime = System.currentTimeMillis();
			}
			if ((alert.getDequeuedMax() != null)&&(q.dequeuedCount > alert.getDequeuedMax())) {
				if ((System.currentTimeMillis() - lastDequeuedMaxTime) < (alert.getTimer() * 60000)) break;
				emailTemplate.SendMail(config.getEmail().getToAddress(), "Dequeued Count Alert", "Queue " + q.name + " has a dequeued count greater than " + alert.getDequeuedMax() + ".");
				lastDequeuedMaxTime = System.currentTimeMillis();
			}
		} 
	}
}
