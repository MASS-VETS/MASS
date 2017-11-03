package gov.va.mass.adapter.comm;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
 * Pick up messages from readytosendtoepic queue and send to epic via hl7 over hapi
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

	@Value("${maxattempts.epicsend}")
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
	public void sendMessagesToEpicFromReadyQueue(String msgtxt) throws MalformedURLException {
		logger.info("Received from Q: " + msgtxt);
		sendMessageToWriteToDBQueue(msgtxt);
		putMsgIdAndSenderOnMDC(msgtxt);
		sendToEpicOverHapi(msgtxt);
		MDC.clear();
		logger.debug("MDC cleared");
	}

	private void sendToEpicOverHapi(String msgtxt) throws MalformedURLException {
		HohRawClientSimple client = new HohRawClientSimple(new URL(DESTINATION_URL));
		CustomCertificateTlsSocketFactory customtlsSF;
		if (TLS_ENABLED) {
			customtlsSF = new CustomCertificateTlsSocketFactory(KEYSTORE_TYPE, KEYSTORE_LOCATION, KEYSTORE_PASSWORD);
			client.setSocketFactory(customtlsSF);
		}
		ISendable rawsendable = new RawSendable(msgtxt);
		EncodingStyle es = rawsendable.getEncodingStyle();
		logger.debug(" rawsendable " + " encoding style " + es.toString() + " content type " + es.getContentType());
		sendattemptcounter = 0;
		do {
			try {
				++sendattemptcounter;
				logger
						.info(" \n\nSending to EPIC attempt# " + sendattemptcounter + "\n " + rawsendable.getMessage().toString());
				IReceivable<String> receivable = client.sendAndReceive(rawsendable);
				String respstring = receivable.getMessage();
				logger.info("\nEpic response :\n" + respstring + "\n");
				sendMessageToWriteToDBQueue(respstring);
				break;
			} catch (DecodeException | IOException | EncodeException e) {
				if (sendattemptcounter == MAX_SEND_ATTEMPTS) {
					logger.info("MAX attempts to send to EPIC exceeded.");
					writeMessageToErrorQueue();
					break;
				} else
					logger.debug("Wait for a certain period, before reattempting to send. Or push this on a hold queue");
				try {
					Thread.sleep(SEND_ATTEMPT_INTERVAL);
				} catch (InterruptedException e1) {
					// TODO: what would cause this?
					// what is the best response to this situation?
				}
			}
		} while (sendattemptcounter < MAX_SEND_ATTEMPTS);
	}

	private void sendMessageToWriteToDBQueue(String msg) {
		// Create the Hashmap for MapMessage JMS queue.
		HashMap<String, Object> mmsg = new HashMap<String, Object>();
		// Build the MapMessage
		mmsg.put("messageContent", msg);
		mmsg.put("fieldList", fieldList);
		mmsg.put("interfaceId", interfaceId);
		jmsMsgTemplate.convertAndSend(databaseQueue, mmsg); // send to the database
		logger.info("Forwarded to queue = " + databaseQueue);
	}

	private void writeMessageToErrorQueue() {
		logger.info("At this point will be marking this message as unable to send to EPIC");
	}

	// TODO : close HapiContext at the correct point
	private void putMsgIdAndSenderOnMDC(String msg) {
		HapiContext context = new DefaultHapiContext();
		Parser p = context.getGenericParser();
		Message hapiMsg = null;
		String sendingApplication = null;
		String msgid = null;
		try {
			hapiMsg = p.parse(msg);
		} catch (HL7Exception e) {
			logger.error("Unable to parse message.");
			e.printStackTrace();
		}
		;
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
