package gov.va.mass.adapter.comm;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.ErrorCode;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;

@RestController
@PropertySource("classpath:application.properties")
public class ReceiveOverHapiService {
	private static final Logger logger = LoggerFactory.getLogger(ReceiveOverHapiService.class);

	int i = 0;

	@Value("${interface.processingId}")
	String processingId;
	
	@Value("${interface.id}")
	String interfaceId;

	@Value("${index.fieldList}")
	String fieldList;

	@Value("${service.name}")
	String appname;

	@Autowired
	JmsMessagingTemplate jmsMsgTemplate;

	@Value("${jms.outputQ}")
	String outputQueue;

	@Value("${jms.databaseQ}")
	String databaseQueue;

	// what the standard tells us to send if the message is invalid
	@ResponseStatus(value = HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public class InvalidMessageException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	@PostMapping(path = "/receive", consumes = "application/hl7-v2", produces = "application/hl7-v2; charset=UTF-8")
	public String receiveAndRouteMessage(@RequestBody String msg) {
		logger.info("In service: " + appname);
		
		//Get the current time for later.
		String dateTime = String.format("%1$tF %1$tT",new Date());
		
		// Later - based on the message type
		// send the message to correct active mq destination using a lookup.
		// Get the msg id from the msg body
		Message message = makeMessage(msg);
		if (message == null) {
			// bad format
			throw new InvalidMessageException(); // return HTTP 415 error if the HL7 could not be parsed.
		}
		HashMap<String,String> msgValues = getMessageIdFromMessage(message);
		
		//Check the Processing ID before we allow this message to go any further. If the processing ID doesn't match then NACK the message.
		if (msgValues.get("processingId") != processingId) {
			try {
				return message.generateACK(AcknowledgmentCode.CR, new HL7Exception(("Invalid Processing ID. Expected: " + processingId + " Received: " + msgValues.get("controlId")), ErrorCode.UNSUPPORTED_PROCESSING_ID)).encode();
			} catch (HL7Exception | IOException e) {
				e.printStackTrace();
				throw new InvalidMessageException(); // return HTTP 415 error if the HL7 is invalid.
			}
		}
		
		MDC.put("MSGID", msgValues.get("processingId"));
		logger.info("Message received = " + msg);
		
		//Create the database communication object with the appropriate values.
		HashMap<String, Object> mmsg = new HashMap<String, Object>();
		mmsg.put("messageContent", msg);
		mmsg.put("fieldList", fieldList);
		mmsg.put("interfaceId", interfaceId);
		mmsg.put("dateTime", dateTime);

		//Put the message on the respective JMS queues.
		jmsMsgTemplate.convertAndSend(databaseQueue, mmsg);
		jmsMsgTemplate.convertAndSend(outputQueue, msg);

		//Log the actions.
		logger.info("Msg Id " + msgValues.get("processingId") + " Message forwarded to queue = " + outputQueue);
		logger.info("Msg Id " + msgValues.get("processingId") + " Message forwarded to queue = " + databaseQueue);
		MDC.clear();
		logger.debug("Threadmapcontext cleared");
		try {
			return message.generateACK().encode();
		} catch (HL7Exception | IOException e) {
			e.printStackTrace();
			throw new InvalidMessageException(); // return HTTP 415 error if the HL7 is invalid.
		}
	}

	private Message makeMessage(String rawMessage) {
		HapiContext context = new DefaultHapiContext();
		Parser p = context.getGenericParser();
		Message hapiMsg = null;
		try {
			hapiMsg = p.parse(rawMessage);
		} catch (HL7Exception e) {
			logger.error("Unable to parse message");
			e.printStackTrace();
		}
		try {
			context.getExecutorService(); // stupid hack to prevent close() from nullref-ing itself
			context.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return hapiMsg;
	}

	// Get the required values for processing from the message.
	private HashMap<String,String> getMessageIdFromMessage(Message msg) {
		HashMap<String,String> msgValues = new HashMap<String,String>();
		
		Terser terser = new Terser(msg);
		try {
			msgValues.put("controlId", terser.get("/MSH-10")); //Control ID
			msgValues.put("processingId",terser.get("/MSH-11")); //Processing ID (PRD/DEV/TST)
		} catch (HL7Exception e) {
			logger.error("Unable to get msg id from the message.");
			e.printStackTrace();
		}
		return msgValues;
	}
}