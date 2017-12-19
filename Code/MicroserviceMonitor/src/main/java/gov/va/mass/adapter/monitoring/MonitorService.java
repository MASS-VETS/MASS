package gov.va.mass.adapter.monitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import gov.va.mass.adapter.monitoring.config.AlertConfig;
import gov.va.mass.adapter.monitoring.config.InterfaceConfig;
import gov.va.mass.adapter.monitoring.config.MicroserviceConfig;
import gov.va.mass.adapter.monitoring.config.MonitorConfig;
import gov.va.mass.adapter.monitoring.config.SslConfig;
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
	EmailTemplate emailTemplate;
	
	CloseableHttpClient simpleClient;
	CloseableHttpClient brokerClient;
	CloseableHttpClient sslTlsClient;
	
	private CloseableHttpClient getSimpleClient() {
		if (simpleClient == null) {
			simpleClient = HttpClientBuilder.create().build();
		}
		return simpleClient;
	}
	
	private CloseableHttpClient getBrokerClient() {
		if (brokerClient == null) {
			String username = config.getJms().getUsername();
			String password = config.getJms().getPassword();
			if (!username.isEmpty() && !password.isEmpty()) {
				CredentialsProvider provider = new BasicCredentialsProvider();
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
				provider.setCredentials(AuthScope.ANY, credentials);
				brokerClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
			} else {
				brokerClient = getSimpleClient();
			}
		}
		return brokerClient;
	}
	
	private CloseableHttpClient getSslTlsClient() {
		if (sslTlsClient == null) {
			SslConfig ssl = config.getSsl();
			try {
				KeyStore keyStore = KeyStore.getInstance(ssl.getKeyStoreType());
				InputStream keyStoreInput = new FileInputStream(ssl.getKeyStore());
				keyStore.load(keyStoreInput, ssl.getKeyStorePassword().toCharArray());
				SSLContext sslContext = new SSLContextBuilder()
						.loadKeyMaterial(keyStore, ssl.getKeyStorePassword().toCharArray())
						.loadTrustMaterial(new TrustSelfSignedStrategy())
						.build();
				SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext);
				sslTlsClient = HttpClientBuilder.create().setSSLSocketFactory(sslConnectionFactory).build();
			} catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException
					| KeyManagementException
					| UnrecoverableKeyException e) {
				e.printStackTrace();
				sslTlsClient = getSimpleClient();
			}
		}
		return sslTlsClient;
	}
	
	private long beganAt = now();
	
	@Scheduled(cron = "${monitor.rate}")
	public void monitor() throws URISyntaxException {
		
		// wait to allow all microservices time to start before hitting them for info
		if (now() - beganAt < config.getStartWait()) {
			log.info(
					"Waiting " + config.getStartWait() + " seconds so that monitored microservices may have time to start...");
			return;
		}
		checkMicroService(config.getMessagedb(), "MessageDB");
		for (InterfaceConfig intf : config.getInterfaces()) {
			log.info("interface '" + intf.getName() + "'");
			checkMicroService(intf.getReceiver(), intf.getName() + " Reciever");
			checkMicroService(intf.getTransform(), intf.getName() + " Transform");
			checkMicroService(intf.getSender(), intf.getName() + " Sender");
		}
		checkBroker();
	}
	
	private void checkMicroService(MicroserviceConfig microService, String name) throws URISyntaxException {
		if (microService == null) {
			return;
		}
		String url = microService.fullUri(config.getServer());
		if (url == null || url.isEmpty()) {
			return;
		}
		
		CloseableHttpClient client;
		if (microService.getUseSsl()) {
			client = getSslTlsClient();
		} else {
			client = getSimpleClient();
		}
		MicroserviceStats stats = new MicroserviceStats(client, url);
		log.info(stats.toString() + " url: " + url);
		
		String emailAddress = config.getEmail().getToAddress();
		if (!stats.isAlive && spamPreventor.shouldSendEmail(AlertType.ServiceDown, name)) {
			emailTemplate.SendMail(emailAddress, "Service Down Alert",
					getEmailText(AlertType.ServiceDown, name));
		}
		if (stats.runState.equalsIgnoreCase("ErrorCondition")
				&& spamPreventor.shouldSendEmail(AlertType.ServiceStoppedItself, name)) {
			emailTemplate.SendMail(emailAddress, "Service Error Alert",
					getEmailText(AlertType.ServiceStoppedItself, name, stats.errorMessage));
		}
	}
	
	private void checkBroker() throws URISyntaxException {
		BrokerStats stats = new BrokerStats(getBrokerClient(), config.getJms().fullUri(config.getServer()));
		String emailAddress = config.getEmail().getToAddress();
		if (!stats.successfullyPolled) {
			if (spamPreventor.shouldSendEmail(AlertType.ServiceDown, "JMS Broker")) {
				emailTemplate.SendMail(emailAddress, "Service Down Alert",
						getEmailText(AlertType.ServiceDown, "JMS Broker"));
			}
		}
		for (QueueStats q : stats.queues) {
			log.info(q.toString());
			checkAlertsForQueue(q);
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
	
	protected long now() {
		return System.currentTimeMillis() / 1000;
	}
}
