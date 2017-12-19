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
import ca.uhn.hl7v2.validation.impl.NoValidation;
import gov.va.mass.adapter.core.JmsMicroserviceBase;
import gov.va.mass.adapter.core.MicroserviceBase;
import gov.va.mass.adapter.core.MicroserviceException;

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
public class SendAndGetAckService extends JmsMicroserviceBase{
	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION;

	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD;

	@Value("${keystore.type}")
	private String KEYSTORE_TYPE;

	@Value("${keystore.enabled}")
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
	public void sendMessagesFromReadyQueue(String msgtxt) throws MalformedURLException, MicroserviceException {

		// Log message and save to database before sending.
		logger.info("Received from Q: " + msgtxt);
		putMsgIdAndSenderOnMDC(msgtxt);
		
		//Only write to the database that the message was sent if we successfully send the message without NACK.
		if (sendOverHAPI(msgtxt)) {
			sendMessageToWriteToDBQueue(msgtxt);
		}
		
		//Clear MDC
		MDC.clear();
		logger.info("MDC cleared");
	}

	// Function to send messages using the HAPI Specification.
	// RETURNS: True if message ACK'd.
	private boolean sendOverHAPI(String msgtxt) throws MalformedURLException, MicroserviceException {
		boolean acked = false;

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
		logger.info(" rawsendable " + " encoding style " + es.toString() + " content type " + es.getContentType());

		// Loop attempting to send until max attempts reached.
		sendattemptcounter = 0;
		do {
			try {
				// Increment send count and send.
				++sendattemptcounter;
				
				//Log sending attemp and the raw message.
				logger.info(" \n\nSending to attempt# " + sendattemptcounter + "\n " + rawsendable.getMessage().toString());
				
				//Attempt to get the raw sendable information and transmit receiving the acknowledgement and logging it.
				IReceivable<String> receivable = client.sendAndReceive(rawsendable);
				String respstring = receivable.getMessage();
				logger.info("\nResponse :\n" + respstring + "\n");
				
				//Return whether the ack was positive or negative.
				acked = isPositiveAck(respstring);
				break; //If we reached this point then we want to make sure that we break this loop.
			} catch (DecodeException | IOException | EncodeException e) {
				// If we are at the maximum number of attempts then log that to the error queue.
				if (sendattemptcounter == MAX_SEND_ATTEMPTS) {
					logger.info("MAX attempts to send to exceeded.");
					this.state.serviceFailed();
					throw this.enterErrorState("Exception during sending shutting down the service.");
				} else {
					logger.info(
							"Wait for a certain period, before reattempting to send. Or push this on a hold queue");
				}

				// Pause for the set amount of time if we were unsuccessful in sending the message. If a single message reaches this point something is wrong with communications.
				try {
					Thread.sleep(SEND_ATTEMPT_INTERVAL);
				} catch (InterruptedException e1) {
					this.state.serviceFailed();
					throw this.enterErrorState("Interrupted exception shutting down the service.");
				}
			}
		} while (sendattemptcounter < MAX_SEND_ATTEMPTS); // Loop
		return acked;
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

	// Function to add to MDC.
	private void putMsgIdAndSenderOnMDC(String msg) {

		// Initialize the context.
		HapiContext context = new DefaultHapiContext();
		context.setValidationContext(new NoValidation());
		Parser p = context.getGenericParser();
		Message hapiMsg = null;
		String sendingApplication = null;
		String msgid = null;

		// Parse the message into the HAPI structure.
		try {
			hapiMsg = p.parse(msg);
			//Prevent close() from NULLREF on itself.
			try {
				context.getExecutorService();
				context.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} catch (HL7Exception e) {
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

	// Get the required values for processing from the message.
	// FUTURE: Update code to modularize the get values from message string function which is present here and other places into a library.
	private boolean isPositiveAck(String respmsg) {
			String AckValue = "";
		
			//Initialize parser and variables.
			HapiContext context = new DefaultHapiContext();
			context.setValidationContext(new NoValidation());
			Parser p = context.getGenericParser();
			Message msg = null;
			
			//Attempt to parse the message.
			try {
				msg = p.parse(respmsg);
			} catch (HL7Exception e) {
				logger.error("Unable to parse message");
				e.printStackTrace();
			}
			
			//Prevent close() from NULLREF on itself.
			try {
				context.getExecutorService();
				context.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			//Use the Terser to find a specific message value in this case that is the acknowledgement's code.
			Terser terser = new Terser(msg);
			try {
				AckValue = terser.get("/MSA-1-1");
			} catch (HL7Exception e) {
				logger.error("Unable to get msg id from the message.");
				e.printStackTrace();
			}
			
			return (AckValue.equals("AA") || AckValue.equals("CA"));
		}

	@Override
	protected String serviceName()
	{
		return "SendAndGetAckService";
	}
}
