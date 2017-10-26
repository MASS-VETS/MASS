package gov.va.mass.adapter.comm.hapi;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.hoh.hapi.server.HohServlet;

@Component("serviceConfig")
@PropertySource("classpath:application.properties")
public class ServiceConfig {


	private Server server; 
	
    @Value("${service.contextpath}")
	private  String SERVICE_CONTEXTPATH ;
    
    @Value("${service.servletpath}")
	private  String SERVICE_SERVLETPATH ;
    
    private static final Logger logger = LoggerFactory.getLogger(ReceiveoverhapiApplication.class);
	
    private static ApplicationContext springctxt = null;

	public ServiceConfig() {
		super();
	}

	public void setServer(Server server) {
		this.server= server;

	}

	public void setup() {
		
		logger.info("SERVICE_CONTEXTPATH " + SERVICE_CONTEXTPATH );
		
		ServletContextHandler context = new ServletContextHandler(server, "/"+SERVICE_CONTEXTPATH,
				ServletContextHandler.SESSIONS);
		
		
		ReceiverServlet rcvSrvlt = (ReceiverServlet) springctxt.getBean("receiverSrvlt"); // new ReceiverServlet();
		HohServlet hohSrvlt =  rcvSrvlt;
		 
		rcvSrvlt.setSpringContext (springctxt );

		context.addServlet(new ServletHolder(hohSrvlt), "/" + SERVICE_SERVLETPATH);
	}
	
	public void setSpringContext(ApplicationContext springctxt) {
		this.springctxt = springctxt;
	}


}
