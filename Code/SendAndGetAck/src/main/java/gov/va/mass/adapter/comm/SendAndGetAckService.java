package gov.va.mass.adapter.comm;

import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.Queue;

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
	
	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION ;
			
	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD ;
	
	@Value("${keystore.type}")
	private String KEYSTORE_TYPE ;
	
	@Value("${tls.destination.host}")
	private String TLS_DESTINATION_HOST ;
	
	@Value("${tls.destination.port}")
	private int TLS_DESTINATION_PORT ; 
	
	@Value("${tls.destination.uri}")
	private String TLS_DESTINATION_URI;
	
	@Value("${app.name}")
	private  String appname;
	
	@Value("${maxattempts.epicsend}")
	private int MAX_SEND_ATTEMPTS ; 
	
	@Value("${sendattempt.interval}")
	private int SEND_ATTEMPT_INTERVAL ;
	
	private int sendattemptcounter = 0 ;
	
	@Autowired
	private Queue dbqueue;
	
	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;
	
	private static final Logger logger = LoggerFactory.getLogger(SendAndGetAckService.class);
	
	@JmsListener(destination = "readytosendtoepic.queue")
	public void sendMessagesToEpicFromReadyQueue ( String msgtxt) {
		
		logger.info("Received from Q: "+ msgtxt);
		
		putMsgIdAndSenderOnMDC(msgtxt);
		
	    sendToEpicOverHapi ( msgtxt);	
	    
	    MDC.clear();
		logger.debug("MDC cleared");
	}

	private void sendToEpicOverHapi(String msgtxt) {
		
		CustomCertificateTlsSocketFactory customtlsSF = new CustomCertificateTlsSocketFactory( KEYSTORE_TYPE, KEYSTORE_LOCATION , KEYSTORE_PASSWORD  );

		HohRawClientSimple client = new HohRawClientSimple (TLS_DESTINATION_HOST, TLS_DESTINATION_PORT, TLS_DESTINATION_URI);// parser);
		client.setSocketFactory( customtlsSF );
		ISendable rawsendable = new RawSendable(msgtxt);
		
		EncodingStyle es = rawsendable.getEncodingStyle();
		logger.debug( " rawsendable " +
				" encoding style " + es.toString()+ 
				" content type " + es.getContentType() ); 
		sendattemptcounter = 0 ;
		do {
			try {
				++sendattemptcounter;
				logger.info(" \n\nSending to EPIC attempt# "+sendattemptcounter+"\n " +
						rawsendable.getMessage().toString());
				IReceivable<String> receivable = client.sendAndReceive(rawsendable );
				String respstring = receivable.getMessage();
				logger.info("\nEpic response :\n" + respstring + "\n"  );
				sendMessageToWriteToDBQueue (respstring );
				break;
			} catch (DecodeException | IOException | EncodeException e) {
                if (sendattemptcounter == MAX_SEND_ATTEMPTS ) {
                	logger.info("MAX attempts to send to EPIC exceeded.");
                	writeMessageToErrorQueue ();
                	break;
                }
                else
                	logger.debug("Wait for a certain period, before reattempting to send. Or push this on a hold queue");
               
             try {
				Thread.sleep (SEND_ATTEMPT_INTERVAL);
			} catch (InterruptedException e1) {
				// TODO: what would cause this?
				// what is the best response to this situation?
			}
                
			} 
		} while ( sendattemptcounter < MAX_SEND_ATTEMPTS );
	}
	
	
	private void sendMessageToWriteToDBQueue(String msg)  {
		try {
			jmsMsgTemplate.convertAndSend(dbqueue, msg); 
			logger.info( "Forwarded to queue = " + dbqueue.getQueueName()  );
		} catch ( JMSException je ) {
			logger.error( "Problem occured when accessing the queue name. Name may be wrong or queue may not exist." + je.getMessage()  );
		} 


	}
	
	
	private void writeMessageToErrorQueue() {
		logger.info("At this point will be marking this message as unable to send to EPIC");
	}


	// TODO : close HapiContext at the correct point
	private void putMsgIdAndSenderOnMDC(String msg) {

		HapiContext context = new DefaultHapiContext();
		Parser p = context.getGenericParser();
		Message hapiMsg = null;
		String sendingApplication = null ;
		String msgid = null;

		
		try {
			hapiMsg = p.parse(msg);
		} catch (HL7Exception e) {
			logger.error("Unable to parse message.");
			e.printStackTrace();
		}	;
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
