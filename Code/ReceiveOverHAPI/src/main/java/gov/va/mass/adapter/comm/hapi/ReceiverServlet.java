package gov.va.mass.adapter.comm.hapi;

import ca.uhn.hl7v2.hoh.hapi.server.HohServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


@Component("receiverSrvlt")
@PropertySource("classpath:application.properties")

public class ReceiverServlet extends HohServlet {

	private static final Logger logger = LoggerFactory.getLogger(ReceiverServlet.class);
	private static ApplicationContext springctxt = null;
	
	@Value("${service.name}")
	private  String SERVICE_NAME;
	
	/*
	 * Servlet should be initialized with an instance of ReceivingApplication, which 
	 * handles incoming messages
	 */	
	@Override
	public void init(ServletConfig theConfig) throws ServletException
	{
		logger.info("In service: " + SERVICE_NAME); 
	
		MessageReceiver mr =  (MessageReceiver) springctxt.getBean("MsgReceiver") ; 
		
		setApplication(mr);
	}
	public void setSpringContext(ApplicationContext springctxt) {
		this.springctxt = springctxt;
	}
	
        
            
    }
