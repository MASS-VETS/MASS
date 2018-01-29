package gov.va.mass.adapter.comm;

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
import org.springframework.messaging.MessagingException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import gov.va.mass.adapter.core.MicroserviceBase;
import gov.va.mass.adapter.core.hl7v2.Message;

@RestController
@PropertySource("classpath:application.properties")
public class ReceiveOverHapiService extends MicroserviceBase {
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
		logger.debug("In service: {}", appname);
		
		// Get the current time for later.
		String dateTime = String.format("%1$tF %1$tT", new Date());
		
		Message message = null;
		try {
			message = new Message(msg);
		} catch (Exception e) {
			e.printStackTrace();
			throw new InvalidMessageException(); // return HTTP 415 error if the HL7 could not be parsed.
		}
		
		// Check the Processing ID before we allow this message to go any further. If
		// the processing ID doesn't match then NACK the message.
		if (!message.ProcessingId.equals(processingId)) {
			
			logger.info("Invalid Processing ID. Expected: \"{}\" Received: \"{}\"", processingId, message.ProcessingId);
			return message.generateNak("Invalid Processing ID.");
		}
		
		// Create the database communication object with the appropriate values.
		HashMap<String, Object> mmsg = new HashMap<String, Object>();
		mmsg.put("messageContent", msg);
		mmsg.put("fieldList", fieldList);
		mmsg.put("interfaceId", interfaceId);
		mmsg.put("dateTime", dateTime);
		
		// Put the message on the respective JMS queues.
		trySendToQueue(message.ControlId, outputQueue, msg);
		trySendToQueue(message.ControlId, databaseQueue, mmsg);
		
		// Log the Processing ID for later reference.
		MDC.put("MSGID", message.ControlId);
		logger.info("Message received = {}", message.ControlId);
		MDC.clear();
		logger.debug("Threadmapcontext cleared");
		
		return message.generateAck();
	}
	
	private void trySendToQueue(String messageId, String destination, Object payload) throws MessagingException {
		try {
			logger.debug("Adding to {}", destination);
			jmsMsgTemplate.convertAndSend(destination, payload);
			logger.info("Msg Id {} Message forwarded to queue = {}", messageId, destination);
		} catch (MessagingException e) {
			e.printStackTrace();
			this.state.serviceFailed();
			throw e;
		}
	}
	
	@Override
	protected String serviceName() {
		return "ReceiveOverHapiService";
	}
}
