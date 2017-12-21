package gov.va.mass.adapter.monitoring;

import java.net.URISyntaxException;
import java.security.KeyStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import gov.va.mass.adapter.core.HttpClientProvider;
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
	EmailTemplate emailTemplate;
	
	@Autowired
	AlertManager alertManager;
	
	private HttpClientProvider clients = new HttpClientProvider();
	
	private long beganAt = System.currentTimeMillis() / 1000;
	
	@Scheduled(cron = "${monitor.rate}")
	public void monitor() throws URISyntaxException {
		
		// wait to allow all microservices time to start before hitting them for info
		if ((System.currentTimeMillis() / 1000) - beganAt < config.getStartWait()) {
			log.info(
					"Waiting " + config.getStartWait() + " seconds so that monitored microservices may have time to start...");
			return;
		}
		
		alertManager.clear();
		
		log.info("Checking DB service");
		checkMicroService(config.getMessagedb(), "MessageDB");
		for (InterfaceConfig intf : config.getInterfaces()) {
			log.info("Checking interface '" + intf.getName() + "'");
			checkMicroService(intf.getReceiver(), intf.getName() + " Reciever");
			checkMicroService(intf.getTransform(), intf.getName() + " Transform");
			checkMicroService(intf.getSender(), intf.getName() + " Sender");
		}
		
		log.info("Checking JMS Broker");
		checkBroker();
		
		alertManager.sendAlertsAsEmails(config.getEmail().getToAddress(), emailTemplate);
	}
	
	private void checkMicroService(MicroserviceConfig microService, String name)
			throws URISyntaxException {
		if (microService == null) {
			return;
		}
		String url = microService.fullUri(config.getServer());
		if (url == null || url.isEmpty()) {
			return;
		}
		CloseableHttpClient client;
		if (microService.getUseSsl()) {
			KeyStore keyStore = config.getKeyStore().createKeystore(clients);
			KeyStore trustStore = config.getTrustStore().createKeystore(clients);
			if (keyStore != null && trustStore != null) {
				client = clients.getSslTlsClient(keyStore, trustStore, config.getKeyStore().getKeyStorePassword());
			} else {
				client = clients.getSimpleClient();
			}
		} else {
			client = clients.getSimpleClient();
		}
		MicroserviceStats stats = new MicroserviceStats(client, url);
		log.info(stats.toString() + " url: " + url);
		
		if (!stats.isAlive) {
			alertManager.raiseAlert(AlertType.ServiceDown, name);
		}
		
		if (stats.runState.equalsIgnoreCase("ErrorCondition")) {
			alertManager.raiseAlert(AlertType.ServiceStoppedItself, name, stats.errorMessage);
		}
		return;
	}
	
	private void checkBroker() throws URISyntaxException {
		CloseableHttpClient client = clients.getUsrPwdClient(config.getJms().getUsername(), config.getJms().getPassword());
		BrokerStats stats = new BrokerStats(client, config.getJms().fullUri(config.getServer()));
		if (!stats.successfullyPolled) {
			alertManager.raiseAlert(AlertType.ServiceDown, "JMS Broker");
		}
		for (QueueStats q : stats.queues) {
			log.info(q.toString());
			boolean found = false;
			for (AlertConfig alert : config.getAlerts()) {
				if (!(q.name.equals(alert.getName())))
					continue;
				found = true;
				// check to see if messages are backed up
				if (greaterThanThreshold(q.queueSize, alert.getQueueMax())) {
					alertManager.raiseAlert(AlertType.QueueSizeThresholdReached, q.name, q.queueSize, alert.getQueueMax());
				}
				// check to make sure someone is listening to this queue
				if (lessThanThreshold(q.consumerCount, alert.getConsumerMin())) {
					alertManager.raiseAlert(AlertType.NotEnoughConsumersOnQueue, q.name, alert.getConsumerMin(), q.consumerCount);
				}
			}
			if (!found) {
				alertManager.raiseAlert(AlertType.UnmonitoredQueue, q.name);
			}
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
}
