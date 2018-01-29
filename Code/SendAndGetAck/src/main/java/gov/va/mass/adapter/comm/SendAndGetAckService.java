package gov.va.mass.adapter.comm;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Date;
import java.util.HashMap;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;
import gov.va.mass.adapter.core.HttpClientProvider;
import gov.va.mass.adapter.core.JmsMicroserviceBase;
import gov.va.mass.adapter.core.MicroserviceException;
import gov.va.mass.adapter.core.hl7v2.Message;

/*
 * Pick up messages from readytosend queue and send via HAPI
 * Receive acknowledgement and write to Ack queue
 * 
 * TODO : Add log4j
 * TODO : Better data type for msg - Text?
 * TODO : When exception happens should do a retry only for ioexception, then send to error queue
 * TODO save response to database or queue to database
 */
@Component
@PropertySource("classpath:application.properties")
public class SendAndGetAckService extends JmsMicroserviceBase {
	@Value("${ssl.keystore.file}")
	private String keystoreFilename;
	
	@Value("${ssl.keystore.password}")
	private String keystorePassword;
	
	@Value("${ssl.keystore.type}")
	private String keystoreType;
	
	@Value("${ssl.truststore.file}")
	private String truststoreFilename;
	
	@Value("${ssl.truststore.password}")
	private String truststorePassword;
	
	@Value("${ssl.truststore.type}")
	private String truststoreType;
	
	@Value("${ssl.enabled}")
	private boolean tlsEnabled = true;
	
	@Value("${destination.url}")
	private String destinationUrl;
	
	private HttpClientProvider clients = new HttpClientProvider();
	
	@Value("${maxattempts.send}")
	private int sendAttempts;
	
	@Value("${sendattempt.interval}")
	private int retryWaitTime;
	
	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;
	
	@Value("${jms.databaseQ}")
	private String databaseQueue;
	
	@Value("${interface.id}")
	private String interfaceId;
	
	@Value("${index.fieldList}")
	private String fieldList;
	
	private static final Logger logger = LoggerFactory.getLogger(SendAndGetAckService.class);
	
	@JmsListener(destination = "${jms.inputQ}")
	public void sendMessagesFromReadyQueue(String msgtxt) throws MalformedURLException, MicroserviceException {
		
		// Log message and save to database before sending.
		logger.info("Received message from amq: {}", msgtxt);
		Message msg = null;
		try {
			msg = new Message(msgtxt);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		MDC.put("MsgId", msg.ControlId);
		MDC.put("Sender", msg.MSH.get(3));
		
		// Only write to the database that the message was sent if we successfully send
		// the message without NACK.
		if (sendOverHAPI(msgtxt)) {
			sendMessageToWriteToDBQueue(msgtxt);
		}
		
		// Clear MDC
		MDC.clear();
		logger.debug("MDC cleared");
	}
	
	// Function to send messages using the HAPI Specification.
	// RETURNS: True if message ACK'd.
	private boolean sendOverHAPI(String msgtxt) throws MalformedURLException, MicroserviceException {
		CloseableHttpClient client;
		if (tlsEnabled) {
			KeyStore keyStore = clients.createKeystore(keystoreType, keystoreFilename, keystorePassword);
			KeyStore trustStore = clients.createKeystore(truststoreType, truststoreFilename, truststorePassword);
			try {
				client = clients.getSslTlsClient(keyStore, trustStore, keystorePassword);
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				throw this.enterErrorState("Security exception in creating SSL client.");
			}
		} else {
			client = clients.getSimpleClient();
		}
		
		// Loop attempting to send until max attempts reached.
		int sendAttemptCounter = 0;
		do {
			++sendAttemptCounter;
			logger.info(" \n\nSending to attempt# {}\n {}", sendAttemptCounter, msgtxt);
			
			// build the post
			HttpPost post = new HttpPost(destinationUrl);
			post.setHeader("Content-Type", "application/hl7-v2; charset=UTF-8");
			try {
				post.setEntity(new StringEntity(msgtxt));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				throw this.enterErrorState("Message could not be encoded.");
			}
			
			// send!
			CloseableHttpResponse resp = null;
			try {
				resp = client.execute(post);
			} catch (HttpResponseException e) { // HTTP error code returned.
				logger.info("HTTP failure. Error: " + e.getStatusCode() + " - " + e.getMessage());
			} catch (IOException e) {
				logger.error("Error in post", e);
				e.printStackTrace();
			}
			
			if (resp != null) {
				String respstring = null;
				try {
					respstring = new BasicResponseHandler().handleResponse(resp);
					logger.info("\nResponse :\n{}\n", respstring);
					this.state.serviceSucceeded();
					return isPositiveAck(respstring); // Return whether the ack was positive or negative.
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (sendAttemptCounter == sendAttempts) {
				logger.info("MAX attempts to send to exceeded.");
				this.state.serviceFailed();
				throw this.enterErrorState("Exception during sending. Shutting down the service.");
			} else {
				logger.info("Waiting for {} seconds before reattempting to send.", retryWaitTime);
			}
			
			// Pause for the set amount of time if we were unsuccessful in sending the
			// message. If a single message reaches this point something is wrong with
			// communications.
			try {
				Thread.sleep(retryWaitTime);
			} catch (InterruptedException e1) {
				this.state.serviceFailed();
				throw this.enterErrorState("Interrupted exception shutting down the service.");
			}
		} while (sendAttemptCounter < sendAttempts); // Loop
		
		return false; // shouldn't actually get here, but compiler can't tell that.
	}
	
	// Write to the database function.
	private void sendMessageToWriteToDBQueue(String msg) {
		// Get current date time for later.
		String dateTime = String.format("%1$tF %1$tT", new Date());
		
		// Create the HashMap for MapMessage JMS queue.
		HashMap<String, Object> mmsg = new HashMap<String, Object>();
		
		// Build the MapMessage
		mmsg.put("messageContent", msg);
		mmsg.put("fieldList", fieldList);
		mmsg.put("interfaceId", interfaceId);
		mmsg.put("dateTime", dateTime);
		
		// Send to the database
		jmsMsgTemplate.convertAndSend(databaseQueue, mmsg);
		logger.info("Forwarded to queue = {}", databaseQueue);
	}
	
	private boolean isPositiveAck(String respmsg) {
		Message msg;
		try {
			msg = new Message(respmsg);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		String AckValue = msg.FieldFromSegment(1, 1);
		return (AckValue.equals("AA") || AckValue.equals("CA"));
	}
	
	@Override
	protected String serviceName() {
		return "SendAndGetAckService";
	}
}
