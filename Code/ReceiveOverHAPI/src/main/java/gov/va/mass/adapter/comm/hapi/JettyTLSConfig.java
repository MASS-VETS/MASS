package gov.va.mass.adapter.comm.hapi;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;



// By default, Spring creates only a single instance of Component - scope = singleton
@Component
//@Configuration
@PropertySource("classpath:application.properties")
public class JettyTLSConfig {

	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION ;// = "C:/work/1twowayssl/adapterkeys/adapterks.jks" 
			
	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD ;
	
	@Value("${keystore.type}")
	private String KEYSTORE_TYPE ;

	@Value("${tls.destination.port}")
	private int TLS_DESTINATION_PORT ; //= 8443; //5105
	
	private Server server = null;
	
	private static final Logger logger = LoggerFactory.getLogger(JettyTLSConfig.class);
	
	public JettyTLSConfig() {
		super();
	}

    
	 public Server getServer() {
		return server;
	}



	public void setServer(Server server) {
		this.server = server;
	}



	void setup () {
			
		
			logger.info("Setting up TLS");
			SslConnectionFactory sslConnectionFactory = createSSLConnectionFactory();
			HttpConnectionFactory httpConnectionFactory = createHttpConnectionFactory();
			ServerConnector sslConnector = new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
			sslConnector.setPort(TLS_DESTINATION_PORT);
			logger.debug("TLS_DESTINATION_PORT " + TLS_DESTINATION_PORT);
			
			
			server.setConnectors(new Connector[] {  sslConnector });
			logger.info("Setting up TLS Done");
	}
	
	

	private  HttpConnectionFactory createHttpConnectionFactory() {

		// specifies that this httpconfig is only for use in conjunction with https
		HttpConfiguration httpconfig = new HttpConfiguration();
		httpconfig.setSecureScheme("https"); 
		httpconfig.setSecurePort(TLS_DESTINATION_PORT);    
		
		HttpConfiguration httpConfiguration = new HttpConfiguration(httpconfig);  		//Q: Should be possible to use just one HttpConfiguration object?
		httpConfiguration.addCustomizer(new SecureRequestCustomizer());   

		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration); //httpconfig);
		return httpConnectionFactory;
	}

	private  SslConnectionFactory createSSLConnectionFactory() { //SSLProperties sslprops
		
		// Q: Password will have to be hidden. Default store is JKS, so setKSType should not be needed - clean up 
		logger.debug( "KeyStore : " + KEYSTORE_LOCATION  + KEYSTORE_PASSWORD + KEYSTORE_TYPE );
		SslContextFactory contextFactory = new SslContextFactory();
		contextFactory.setKeyStorePath(KEYSTORE_LOCATION ) ; 
		contextFactory.setKeyStorePassword(KEYSTORE_PASSWORD);
		contextFactory.setKeyStoreType(KEYSTORE_TYPE); 
		contextFactory.setNeedClientAuth(true);
//		System.out.println("default protocol on ssl factory " + contextFactory.getProtocol() +"\n\n" );
		SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(contextFactory, "http/1.1");
																			// org.eclipse.jetty.http.HttpVersion.HTTP_1_1.toString());

		return sslConnectionFactory;
	}

	
}
