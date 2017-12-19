package gov.va.mass.adapter.monitoring;

import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import gov.va.mass.adapter.monitoring.config.AlertConfig;
import gov.va.mass.adapter.monitoring.config.InterfaceConfig;
import gov.va.mass.adapter.monitoring.config.MicroserviceConfig;
import gov.va.mass.adapter.monitoring.config.MonitorConfig;
import gov.va.mass.adapter.monitoring.email.EmailTemplate;
import gov.va.mass.adapter.monitoring.stats.BrokerStats;
import gov.va.mass.adapter.monitoring.stats.MicroserviceStats;
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
	private AlertSpamPreventor spamPreventor;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	EmailTemplate emailTemplate;
	
	@Value("${start.wait}")
	private long startWaitSecs;

	@Value("${docker.server}")
	private String dockerServer;

	private long beganAt = now();
		
	@Scheduled(cron = "${monitor.rate}")
	public void showProperty() throws URISyntaxException {

		//wait to allow all microservices time to start before hitting them for info
		if (now() - beganAt < startWaitSecs)
		{
			log.info("Waiting " + startWaitSecs + " seconds so that monitored microservices may have time to start...");
			return;
		}

		MicroserviceStats stats = new MicroserviceStats(restTemplate, serverUrl(config.getMessagedb().getUrl()));
		log.info("Message db");
		log.info("   " + stats.toString() + " url: " + serverUrl(config.getMessagedb().getUrl()));
		checkAlertsForMicroServiceStats(stats, "MessageDB");
				
		// poll all the interfaces
		for (InterfaceConfig intf : config.getInterfaces()) {
			log.info("Interface '" + intf.getName() + "'");
			checkAlertsForMicroService(intf, intf.getReceiver(), "Receiver");
			checkAlertsForMicroService(intf, intf.getTransform(), "Transform");
			checkAlertsForMicroService(intf, intf.getSender(), "Sender");
		}
		
		// poll activemq
		BrokerStats bstats = new BrokerStats(restTemplate, serverUrl(config.getJms().getUri()));
		log.info("ActiveMQ");
		checkAlertsForBroker(bstats);
		for (QueueStats q : bstats.queues) {
			log.info("   " + q.toString());
			checkAlertsForQueue(q);
		}
	}
	
	private void checkAlertsForBroker(BrokerStats s) {
		String emailAddress = config.getEmail().getToAddress();
		if (!s.successfullyPolled) {
			if (spamPreventor.shouldSendEmail(AlertType.ServiceDown, "JMS Broker")) {
				emailTemplate.SendMail(emailAddress, "Service Down Alert",
						getEmailText(AlertType.ServiceDown, "JMS Broker"));
			}
		}
	}
	
	private void checkAlertsForMicroService(InterfaceConfig intf, MicroserviceConfig msConfig, String msName) throws URISyntaxException {
		String url = msConfig.getUrl();
		if (url != null) {
			String serverUrl = serverUrl(url);
			MicroserviceStats stats = new MicroserviceStats(restTemplate, serverUrl);
			log.info("   " + stats.toString() + " url: " + serverUrl);
			checkAlertsForMicroServiceStats(stats, intf.getName() + " Transform");
		}
	}
	
	private void checkAlertsForMicroServiceStats(MicroserviceStats s, String name) {
		String emailAddress = config.getEmail().getToAddress();
		if (!s.isAlive && spamPreventor.shouldSendEmail(AlertType.ServiceDown, name)) {
			emailTemplate.SendMail(emailAddress, "Service Down Alert",
					getEmailText(AlertType.ServiceDown, name));
		}
		if (s.runState.equalsIgnoreCase("ErrorCondition")
				&& spamPreventor.shouldSendEmail(AlertType.ServiceStoppedItself, name)) {
			emailTemplate.SendMail(emailAddress, "Service Error Alert",
					getEmailText(AlertType.ServiceStoppedItself, name, s.errorMessage));
		}
	}
	
	
	
	private void checkAlertsForQueue(QueueStats q) {
		String emailAddress = config.getEmail().getToAddress();
		boolean found = false;
		for (AlertConfig alert : config.getAlerts()) {
			if (!(q.name.equals(alert.getName())))
				continue;
			found = true;
			// check to see if messages are backed up
			if (greaterThanThreshold(q.queueSize, alert.getQueueMax())
					&& spamPreventor.shouldSendEmail(AlertType.QueueSizeThresholdReached, "Q" + q.name)) {
				emailTemplate.SendMail(emailAddress, "Queue Depth Alert",
						getEmailText(AlertType.QueueSizeThresholdReached, q.name, q.queueSize, alert.getQueueMax()));
			}
			// check to make sure someone is listening to this queue
			if (lessThanThreshold(q.consumerCount, alert.getConsumerMin())
					&& spamPreventor.shouldSendEmail(AlertType.NotEnoughConsumersOnQueue, "Q" + q.name)) {
				emailTemplate.SendMail(emailAddress, "Consumer Count Alert",
						getEmailText(AlertType.NotEnoughConsumersOnQueue, q.name, alert.getConsumerMin(), q.consumerCount));
			}
		}
		if (!found && spamPreventor.shouldSendEmail(AlertType.UnmonitoredQueue, "Q" + q.name)) {
			emailTemplate.SendMail(emailAddress, "Unmonitored Queue Alert",
					getEmailText(AlertType.UnmonitoredQueue, q.name));
		}
	}
	
	private String getEmailText(AlertType type, String entity, Object... args) {
		switch (type) {
		case NotEnoughConsumersOnQueue:
			return String.format(
					"Queue %s requires at least %d consumer(s), but only has %d.", entity, args[0], args[1]);
		case QueueSizeThresholdReached:
			return String.format(
					"Queue %s depth is %d. Threshold: %d.", entity, args[0], args[1]);
		case ServiceDown:
			return String.format(
					"Service %s is not responding.", entity);
		case ServiceStoppedItself:
			return String.format(
					"Service %s has stopped and is waiting for error resolution.\n\nError:\n%s", args[0]);
		case UnmonitoredQueue:
			return String.format(
					"Queue %s was not configured in the monitor.", entity);
		default:
			return "An unknown error has occurred within the adapter.";
		}
	}
	
	private boolean greaterThanThreshold(int value, Integer threshold) {
		if (threshold == null) {
			return false;
		}
		return value > threshold;
	}
	
	private boolean lessThanThreshold(int value, Integer threshold) {
		if (threshold == null) {
			return false;
		}
		return value < threshold;
	}
	
	private String serverUrl(String url) {
		return this.dockerServer + ":" + url + "/";	
	}
		
	protected long now() {
		return System.currentTimeMillis() / 1000;
	}
}
