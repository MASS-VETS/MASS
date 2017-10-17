package gov.va.mass.adapter.comm.ensemble;

import javax.jms.JMSException;

import javax.jms.Queue;

//import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;


// only application specific loggers should log at the debug level in dev
// parsed msg go get msg id
// add msg id to the json log
// Sprint 4 : 
// TODO: Handle the exception when queue is not available
// JsonLayout - Done
// Have both console and Rolling file appender outputs going simultaneously
// Absolute logging path not working in application.properties
// Abandoned application.properties & log4j.properties based configuration due to lack of documentation
// avoid excess logging, compress log
// Catch the activemq not available exception.

@RestController
@PropertySource("classpath:application.properties")
public class ReceiveAndRouteService {
	// who is responsible for appending ${="\r\n"} to end of each line ensemble. is this hapi need?
	// two way ssl configuration
	// should the message be received as json?
	// What form will ensemble send me?

	// Decision: the logger instance be shared or be separate for each instance of this class.
	private static final Logger logger = LoggerFactory.getLogger(ReceiveAndRouteService.class);

	int i = 0 ;

	@Value("${app.name}")
	private  String appname;

	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;

	@Autowired
	private Queue queue;

	@RequestMapping( path="/receive", method = RequestMethod.POST)
	String receiveAndrouteMessage (@RequestBody String msg ) {
		logger.info("In service: " + appname);
		// Later - based on the message type  
		// send the message to correct active mq destination using a lookup.

		

		// Get the msg id from the msg body 
		String id = getMessageIdFromMessage(msg);
		MDC.put("MSGID", id); 
		
		logger.info( "Message received = " + msg  );
		jmsMsgTemplate.convertAndSend(queue, msg); //why convert and send?

		// debug
		try {
			logger.info( "Msg Id "+ id +" Message forwarded to queue = " + queue.getQueueName()  );
		} catch (JMSException e) {
			logger.error( "Msg Id "+ id + " Problem occured when reading queue name. Name may not exist or queue may not exist"  );
		}
		MDC.clear();
		logger.debug("Threadmapcontext cleared");
		return msg; 
	}

	// replace sending application with message id
	private String getMessageIdFromMessage(String msg) {

		HapiContext context = new DefaultHapiContext();
		Parser p = context.getGenericParser();
		Message hapiMsg = null;
		String sendingApplication = null ;
		String msgid = null;

		
		try {
			hapiMsg = p.parse(msg);
		} catch (HL7Exception e) {
			logger.error("Unable to parse message");
			e.printStackTrace();
		}	;
		Terser terser = new Terser(hapiMsg);

		try {
			sendingApplication = terser.get("/.MSH-3-1");
			msgid = terser.get("/MSH-10");
			
		} catch (HL7Exception e) {
			logger.error("Unable to get msg id from the message.");
			e.printStackTrace();
		}


//		System.out.println("idusingterser " + idusingterser + " sendingApplication " + sendingApplication);
		return msgid;
	}

}
