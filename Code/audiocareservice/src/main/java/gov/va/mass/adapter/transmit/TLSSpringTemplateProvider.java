package gov.va.mass.adapter.transmit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/*
 * Provides a TLS capable Spring Resttemplate
 */


@Component
@PropertySource("classpath:application.properties")
public class TLSSpringTemplateProvider {

	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION;// = "C:/work/1twowayssl/adapterkeys/adapterks.jks"

	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD;

	@Value("${keystore.type}")
	private String KEYSTORE_TYPE;

	private static final Logger logger = LoggerFactory.getLogger(TLSSpringTemplateProvider.class);


	public TLSSpringTemplateProvider() {
		super();
	}


	// TODO: http and https?
	// TODO: Consider using PoolingHttpClientConnectionManager		
	private HttpClientBuilder prepareHttpClientBuilder(SSLContext sslContext) {
		HttpClientBuilder builder = HttpClientBuilder.create();
		SSLConnectionSocketFactory sslConnectionFactory = null;
		String env = System.getenv("ENV") ; 
		if (env.equals("prod") || env.equals("preprod") || env.equals("accept") ) {
			sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,  //TODO: SSLConnectionSocketFactory is deprecated.. need to substitute
					SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER );  // TODO: Verify that that the default or the strict verifier should be used in prod
		} else {
			sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
					SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		}

		builder.setSSLSocketFactory(sslConnectionFactory);
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslConnectionFactory)
				.register("http", new PlainConnectionSocketFactory()).build();
		HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
		builder.setConnectionManager(ccm).setConnectionManagerShared(true);
		return builder;
	}

	
	// TODO: Consider thread safety
	public RestTemplate getTLSSpringTemplate () {
		RestTemplate restTemplate = null;
		SSLContext sslContext = configureSSLContext();
		HttpClientBuilder builder = prepareHttpClientBuilder(sslContext);
		
		try (CloseableHttpClient httpClient = builder.build()) {

			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

			requestFactory.setHttpClient(httpClient);
			requestFactory.setBufferRequestBody(false);  // to prevent buffering when downloading large files

			restTemplate = new RestTemplate(requestFactory);
						

		} catch (IOException e) {
			e.printStackTrace();
//			logger.error("Unable to create " + e.toString());
		}
		return restTemplate;
	}

	
	private SSLContext configureSSLContext() {
		KeyStore keyStore = null;

		try {
			keyStore = KeyStore.getInstance( KEYSTORE_TYPE);
			InputStream keyStoreInput = new FileInputStream(KEYSTORE_LOCATION);
			keyStore.load(keyStoreInput, KEYSTORE_PASSWORD.toCharArray());  // kspassword.toCharArray( )
			logger.debug("Key store has " + keyStore.size() + " keys");
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error("Problem with keystore " + e.toString());
		}

		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom().loadKeyMaterial(keyStore,KEYSTORE_PASSWORD.toCharArray()) // KEYSTORE_PASSWORD.toCharArray())
					.loadTrustMaterial(new TrustSelfSignedStrategy()).setProtocol("TLSv1") // TODO: TLS version needs to
																							// be uniform
					.build();
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
			logger.error("Could not create SSLContext " + e.toString());
		}

		return sslContext;
	}
	
}
