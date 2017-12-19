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
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
 * Provides a TLS capable Apache HttpClient.
 * 
 * This is a singleton. So the httpClient instance is shared. 
 * We need to ensure connections are shared too.
 */


@Component
@PropertySource("classpath:application.properties")
public class TLSHttpClientProvider {

	@Value("${keystore.enabled}")
	private boolean TLS_ENABLED = true;

	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION;// = "C:/work/1twowayssl/adapterkeys/adapterks.jks"

	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD;

	@Value("${keystore.type}")
	private String KEYSTORE_TYPE;

	private static final Logger logger = LoggerFactory.getLogger(TLSHttpClientProvider.class);

	private static CloseableHttpClient httpClient = null;

	public TLSHttpClientProvider() {
		super();
	}


	public CloseableHttpClient getTLSHttpClient ( ) {
		if (httpClient == null ) {
			SSLContext sslContext = null;
			// If TLS is enabled.
			if (TLS_ENABLED) {
				sslContext = configureSSLContext();
				if (sslContext == null) {
					return null;
				}
			}

			HttpClientBuilder builder = prepareHttpClientBuilder(sslContext);
			httpClient = builder.build() ;
		}
		return httpClient;	
	}
	
	// TODO: http and https?
	// TODO: Consider using PoolingHttpClientConnectionManager	
	// TODO: Use multithreadedconnectionmanager
	// Create an SSL context with our private key store.
	// We are only loading the key-material here, but if your server uses a
	// self-signed certificate,
	// you will need to load the trust-material (a JKS key-store containing the
	// server's public SSL
	// certificate) as well.

	private SSLContext configureSSLContext() {
		KeyStore keyStore = null;
		SSLContext sslContext = null;

		try {
			keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
			InputStream keyStoreInput = new FileInputStream(KEYSTORE_LOCATION);
			keyStore.load(keyStoreInput, KEYSTORE_PASSWORD.toCharArray());
			logger.debug("Key store has " + keyStore.size() + " keys");
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error("Problem with keystore " + e.toString());
		}

		try {
			sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, KEYSTORE_PASSWORD.toCharArray())
					.loadTrustMaterial(new TrustSelfSignedStrategy()).setProtocol("TLSv1") // TODO: TLS version needs to
																							// be uniform
					.build();
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
			logger.error("Could not create SSLContext " + e.toString());
		}
		
		return sslContext;
	}

	// = new SSLConnectionSocketFactory(sslContext,
	// SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

	// TODO: sslconnectionfactory is deprecated
	private HttpClientBuilder prepareHttpClientBuilder(SSLContext sslContext) {
		// Prepare the HTTPClient.
		HttpClientBuilder builder = HttpClientBuilder.create();
		Registry<ConnectionSocketFactory> registry = null;

		if (TLS_ENABLED) {
			SSLConnectionSocketFactory sslConnectionFactory = null;
			String env = System.getenv("ENV") ; 
			logger.debug("ssl conn fact. creation " + env);
			if (env.equals("prod") || env.equals("preprod") || env.equals("accept") ) {
				sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
						BrowserCompatHostnameVerifier.INSTANCE); 
			} else {
				sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
						NoopHostnameVerifier.INSTANCE);
			}
	
			builder.setSSLSocketFactory(sslConnectionFactory);

			// TODO : Need to disable the http socket factory? Or is this used after ssl is stripped.
			registry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("https", sslConnectionFactory)
					.build();
		}
		else {
			registry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", new PlainConnectionSocketFactory()).build();
		}
		
		HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
		builder.setConnectionManager(ccm);
//		PoolingHttpClientConnectionManager pcm = new PoolingHttpClientConnectionManager (registry);
//		builder.setConnectionManager(pcm);
		return builder;
	}

}
