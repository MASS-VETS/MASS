package avo.hax.comm;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;
import gov.va.mass.adapter.core.HttpClientProvider;

//@ RestController
@Component
@PropertySource("classpath:application.yaml")
public class TheHammerService {
	
	@Value("${send.count}")
	int messageCount;
	
	@Value("${send.maxrate}")
	int maxRate;
	
	@Value("${send.pinginstead}")
	boolean pingInstead;
	
	@Value("${send.contentType}")
	String contentType;
	
	@Value("${send.checkAck}")
	boolean checkAck;
	
	@Value("${destination.url}")
	String destinationUrl;
	
	@Value("${destination.ssl.enabled}")
	boolean tlsEnabled;
	
	@Value("${destination.ssl.keystore.file}")
	String keystore;
	
	@Value("${destination.ssl.keystore.password}")
	String keystorePassword;
	
	@Value("${destination.ssl.keystore.type}")
	String keystoreType;
	
	@Value("${destination.ssl.truststore.file}")
	String truststore;
	
	@Value("${destination.ssl.truststore.password}")
	String truststorePassword;
	
	@Value("${destination.ssl.truststore.type}")
	String truststoreType;
	
	@Value("${logging.verbose}")
	boolean verboseLogging;
	static final Logger log = LoggerFactory.getLogger(TheHammerService.class);
	
	private void logIfVerbose(String message) {
		if (!verboseLogging) {
			return;
		}
		log.info(message);
	}
	
	// @Autowired
	// RestTemplate restTemplate;
	
	private HttpClientProvider clients = new HttpClientProvider();
	
	@Autowired
	MessageCache messageCache;
	
	private int messagesSent = 0;
	private HapiContext hapiContext = null;
	private Parser hapiParser = null;
	
	@PostConstruct
	private void postConstruct() {
		hapiContext = new DefaultHapiContext();
		hapiParser = hapiContext.getGenericParser();
	}
	
	@PreDestroy
	private void preDestroy() {
		if (hapiContext != null) {
			try {
				hapiContext.getExecutorService();
				hapiContext.close();
				hapiContext = null;
			} catch (Exception e) {
			}
		}
	}
	
	// @ RequestMapping(path = { "/send" }, method = RequestMethod.GET)
	public String sendAllTheThings() {
		messagesSent = 0;
		if (destinationUrl.isEmpty()) {
			return "No destination URL specified";
		}
		if (!messageCache.IsInitialized()) {
			return "Message cannot be loaded.";
		}
		CompletableFuture.supplyAsync(() -> sendManyMessages())
				.thenAcceptAsync((x) -> {
					log.info(x ? "Success" : "Failed");
				});
		
		return String.format("queued up %d messages to send", messageCount);
	}
	
	public String sendAndWait() {
		messagesSent = 0;
		if (destinationUrl.isEmpty()) {
			return "No destination URL specified.";
		}
		if (!messageCache.IsInitialized()) {
			return "Message cannot be loaded.";
		}
		long start = System.currentTimeMillis();
		try {
			if (!sendManyMessages()) {
				return "Could not send messages.";
			} else {
				return "Sent " + messagesSent + " messages!";
			}
		} finally {
			long duration = System.currentTimeMillis() - start;
			log.info("Messages Sent:   " + messagesSent);
			log.info("Turnaround Time: " + duration + "ms");
			log.info("Throughput:      " + (messagesSent * 1000 / duration) + " per second.");
		}
	}
	
	private boolean sendManyMessages() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", contentType);
		
		long delay = 0;
		if (maxRate > 0) {
			delay = 1000 / maxRate;
		}
		log.info("delay = " + delay);
		try {
			long lastStart = System.currentTimeMillis();
			if (!sendOne(messageCache.GetMessage(), headers)) {
				return false;
			}
			messagesSent++;
			for (int i = 1; i < messageCount; i++) {
				long timeSinceLast = System.currentTimeMillis() - lastStart;
				if (timeSinceLast < delay) {
					Thread.sleep(delay - timeSinceLast);
				}
				lastStart = System.currentTimeMillis();
				if (!sendOne(messageCache.GetMessage(), headers)) {
					return false;
				}
				messagesSent++;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	boolean sendOne(String msg, HttpHeaders headers) {
		// HttpEntity<String> entity = new HttpEntity<String>(msg, headers);
		try {
			CloseableHttpClient client;
			if (tlsEnabled) {
				KeyStore ks = clients.createKeystore(keystoreType, keystore, keystorePassword);
				KeyStore ts = clients.createKeystore(truststoreType, truststore, truststorePassword);
				client = clients.getSslTlsClient(ks, ts, keystorePassword);
			} else {
				client = clients.getSimpleClient();
			}
			HttpUriRequest request;
			if (pingInstead) {
				request = new HttpGet(destinationUrl);
				logIfVerbose("sending: GET");
			} else {
				HttpPost post = new HttpPost(destinationUrl);
				post.setHeader("Content-Type", "application/hl7-v2; charset=UTF-8");
				post.setEntity(new StringEntity(msg));
				request = post;
				logIfVerbose("sending: POST " + msg);
			}
			CloseableHttpResponse resp = client.execute(request);
			String response = new BasicResponseHandler().handleResponse(resp);
			logIfVerbose("Received: " + response);
			if (pingInstead) {
				return true;
			}
			if (!checkAck) {
				return true;
			}
			return checkAck(response);
		} catch (HttpResponseException e) {
			log.info("Status code: " + e.getStatusCode() + " message: " + e.getMessage());
			return false;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean checkAck(String response) {
		Message msg = null;
		try {
			msg = hapiParser.parse(response);
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
		if (msg == null) {
			return false;
		}
		
		Terser terser = new Terser(msg);
		String ackValue;
		try {
			ackValue = terser.get("/MSA-1-1");
			if (ackValue.equals("AA") || ackValue.equals("CA")) {
				return true;
			} else {
				log.error("NAK from receiver: " + terser.get("/MSA-3-1"));
				return false;
			}
		} catch (HL7Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
