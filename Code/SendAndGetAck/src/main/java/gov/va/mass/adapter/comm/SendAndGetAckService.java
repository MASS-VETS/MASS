package gov.va.mass.adapter.comm;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.hoh.api.DecodeException;
import ca.uhn.hl7v2.hoh.api.EncodeException;
import ca.uhn.hl7v2.hoh.api.IReceivable;
import ca.uhn.hl7v2.hoh.api.ISendable;
import ca.uhn.hl7v2.hoh.encoder.EncodingStyle;
import ca.uhn.hl7v2.hoh.raw.api.RawSendable;
import ca.uhn.hl7v2.hoh.raw.client.HohRawClientSimple;
import ca.uhn.hl7v2.hoh.sockets.CustomCertificateTlsSocketFactory;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;

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
public class SendAndGetAckService {
	@Value("${tls.keystore.location}")
	private String KEYSTORE_LOCATION;

	@Value("${tls.keystore.password}")
	private String KEYSTORE_PASSWORD;

	@Value("${tls.keystore.type}")
	private String KEYSTORE_TYPE;

	@Value("${tls.enabled}")
	private boolean TLS_ENABLED = true;

	@Value("${destination.url}")
	private String DESTINATION_URL;

	@Value("${app.name}")
	private String appname;

	@Value("${maxattempts.send}")
	private int MAX_SEND_ATTEMPTS;

	@Value("${sendattempt.interval}")
	private int SEND_ATTEMPT_INTERVAL;

	private int sendattemptcounter = 0;

	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;

	@Value("${jms.databaseQ}")
	private String databaseQueue;

	@Value("${interface.id}")
	String interfaceId;

	@Value("${index.fieldList}")
	String fieldList;

	private static final Logger logger = LoggerFactory.getLogger(SendAndGetAckService.class);

	@JmsListener(destination = "${jms.inputQ}")
	public void sendMessagesFromReadyQueue(String msgtxt) throws MalformedURLException {

		// Log message and save to database before sending.
		logger.info("Received from Q: " + msgtxt);
		sendMessageToWriteToDBQueue(msgtxt);
		putMsgIdAndSenderOnMDC(msgtxt);
		sendOverHAPI(msgtxt);
		MDC.clear();
		logger.debug("MDC cleared");
	}

	// Function to send messages using the HAPI Specification.
	private void sendOverHAPI(String msgtxt) throws MalformedURLException {

		// Initialize variables and destination URL.
		HohRawClientSimple client = new HohRawClientSimple(new URL(DESTINATION_URL));
		CustomCertificateTlsSocketFactory customtlsSF;

		// If TLS is enabled.
		if (TLS_ENABLED) {
			customtlsSF = new CustomCertificateTlsSocketFactory(KEYSTORE_TYPE, KEYSTORE_LOCATION, KEYSTORE_PASSWORD);
			client.setSocketFactory(customtlsSF);
		}

		// Get sendable data.
		@SuppressWarnings("rawtypes")
		ISendable rawsendable = new RawSendable(msgtxt);
		EncodingStyle es = rawsendable.getEncodingStyle();
		logger.debug(" rawsendable " + " encoding style " + es.toString() + " content type " + es.getContentType());

		// Loop attempting to send until max attempts reached.
		sendattemptcounter = 0;
		do {
			try {
				// Increment send count and send.
				++sendattemptcounter;
				logger.info(
						" \n\nSending to attempt# " + sendattemptcounter + "\n " + rawsendable.getMessage().toString());
				IReceivable<String> receivable = client.sendAndReceive(rawsendable);
				String respstring = receivable.getMessage();
				logger.info("\nResponse :\n" + respstring + "\n");
				break;
			} catch (DecodeException | IOException | EncodeException e) {
				// If we are at the maximum number of attempts then log that to the error queue.
				if (sendattemptcounter == MAX_SEND_ATTEMPTS) {
					logger.info("MAX attempts to send to exceeded.");
					writeMessageToErrorQueue(); // TODO : Close / Sleep the service here.
					break;
				} else {
					logger.debug(
							"Wait for a certain period, before reattempting to send. Or push this on a hold queue");
				}

				// Pause for the set amount of time.
				try {
					Thread.sleep(SEND_ATTEMPT_INTERVAL);
				} catch (InterruptedException e1) {
					// TODO: what would cause this?
					// what is the best response to this situation? Shut down the service if this
					// happens it would only happen if there was a major system error.
				}
			}
		} while (sendattemptcounter < MAX_SEND_ATTEMPTS); // Loop
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
		logger.info("Forwarded to queue = " + databaseQueue);
	}

	// TODO : complete this function.
	private void writeMessageToErrorQueue() {
		logger.info("At this point will be marking this message as unable to send.");
	}

	// Function to add to MDC.
	private void putMsgIdAndSenderOnMDC(String msg) {

		// Initialize the context.
		HapiContext context = new DefaultHapiContext();
		Parser p = context.getGenericParser();
		Message hapiMsg = null;
		String sendingApplication = null;
		String msgid = null;

		// Parse the message into the HAPI structure.
		try {
			hapiMsg = p.parse(msg);
			context.close();
		} catch (HL7Exception | IOException e) {
			logger.error("Unable to parse message.");
			e.printStackTrace();
		}

		//Make sure that we return if we were unable to parse the message string into a message.
		if (hapiMsg == null) {
			return;
		}

		// Use the HAPI Terser to parse the message.
		Terser terser = new Terser(hapiMsg);
		try {
			sendingApplication = terser.get("/.MSH-3-1");
			msgid = terser.get("/MSH-10");
		} catch (HL7Exception e) {
			logger.error("Unable to parse message to get the Msg Id and Sender.");
			e.printStackTrace();
		}
		MDC.put("MsgId", msgid);
		MDC.put("Sender", sendingApplication);
	}
}
