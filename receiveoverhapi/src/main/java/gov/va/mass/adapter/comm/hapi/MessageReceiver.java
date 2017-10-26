package gov.va.mass.adapter.comm.hapi;

import java.io.IOException;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import ca.uhn.hl7v2.util.Terser;

@Component("MsgReceiver")
@PropertySource("classpath:application.properties")
public class MessageReceiver implements ReceivingApplication
{

	private static final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

	int i = 0 ;

	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;

	@Autowired
	private Queue msgqueue;

	@Autowired
	private Queue dbqueue;
	

	/**
	 * processMessage is fired each time a new message arrives.
	 * @param theMessage
	 *            The message which was received
	 * @param theMetadata
	 *            A map containing additional information about the message, where it came from, etc.
	 */
	public Message processMessage(Message theMessage, Map<String, Object> theMetadata)
			throws ReceivingApplicationException, HL7Exception
	{
		Message response = null;
		putMsgIdAndSenderOnMDC(theMessage.toString());
		logger.info("Received :\n" + theMessage.encode());
		logger.debug("Default encoding " + theMessage.getParser().getDefaultEncoding() + 
				" version " + theMessage.getParser().getVersion(theMessage.encode()));
		boolean msgAcceptanceFailed = false;

		
		try {
			sendMessageToReadyToSendToEpicQueue (theMessage.toString());
			sendMessageToWriteToDBQueue (theMessage.toString());
			response = theMessage.generateACK();
			logger.debug("response version " + response.getVersion() + " name " + response.getName() ); 
			logger.debug("response encoding " + response.getParser().getDefaultEncoding() );
			logger.info(" \nAck Sent:" + response.printStructure() );
		} catch (Exception e){
			logger.error("Unable to send message to internal queue. Write to error queue and wait for the next messsage " + e.getMessage() );
			msgAcceptanceFailed = true;
		} 

		if (msgAcceptanceFailed) {
			try	{
				response = theMessage.generateACK(AcknowledgmentCode.AE, new HL7Exception("Unable to accept message at this time!"));
			}
			catch (IOException e) {
				MDC.clear();
				logger.debug("MDC cleared");	
				throw new ReceivingApplicationException(e);
			}
		}

		MDC.clear();
		logger.debug("MDC cleared");			

		return response;
	}

	private void sendMessageToWriteToDBQueue(String msg)  throws Exception {
		try {
			jmsMsgTemplate.convertAndSend(dbqueue, msg); 
			logger.info( "Forwarded to queue = " + dbqueue.getQueueName()  );
		} catch (JMSException e ) {
			logger.error( "Problem occured when accessing the queue. Name may be wrong or queue may not exist." + e.getMessage()  );
			throw e;
		} 


	}

	private void sendMessageToReadyToSendToEpicQueue(String msg) throws Exception {
		// Later - based on the message type  
		// send the message to correct active mq destination using a lookup.


		try {
			jmsMsgTemplate.convertAndSend(msgqueue, msg); 
			logger.info( "Forwarded to queue = " + msgqueue.getQueueName()  );
		} catch (Exception e ) {
			logger.error( "Problem occured when accessing the queue. Name may be wrong or queue may not exist." + e.getMessage()  );
			throw e;
		} 


	}


	// TODO : close HapiContext
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


	/**
	 * {@inheritDoc}
	 */
	public boolean canProcess(Message theMessage)
	{
		//		logger.debug("can process called" );
		return true;
	}

}


