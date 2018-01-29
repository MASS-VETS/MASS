package gov.va.mass.adapter.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author avolkano
 */
public class HttpClientProvider {
	static final Logger log = LoggerFactory.getLogger(HttpClientProvider.class);
	
	private CloseableHttpClient simpleClient;
	private CloseableHttpClient usrPwdClient;
	private CloseableHttpClient sslTlsClient;
	
	public CloseableHttpClient getSimpleClient() {
		if (simpleClient == null) {
			simpleClient = HttpClientBuilder.create().build();
		}
		return simpleClient;
	}
	
	public CloseableHttpClient getUsrPwdClient(String username, String password) {
		if (usrPwdClient == null) {
			if (!username.isEmpty() && !password.isEmpty()) {
				CredentialsProvider provider = new BasicCredentialsProvider();
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
				provider.setCredentials(AuthScope.ANY, credentials);
				usrPwdClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
			} else {
				usrPwdClient = getSimpleClient();
			}
		}
		return usrPwdClient;
	}
	
	public CloseableHttpClient getSslTlsClient(KeyStore keyStore, KeyStore trustStore, String keyStorePassword) throws GeneralSecurityException {
		if (sslTlsClient == null) {
			try {
				SSLContext sslContext = SSLContextBuilder.create()
						.loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
						.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
						.build();
				
				SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext);
				
				sslTlsClient = HttpClientBuilder.create().setSSLSocketFactory(sslConnectionFactory).build();
				
			} catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
				sslTlsClient = getSimpleClient();
				throw e;
			}
		}
		return sslTlsClient;
	}
	
	public KeyStore createKeystore(String keyStoreType, String keyStoreFilename, String keyStorePassword) {
		try {
			KeyStore store = KeyStore.getInstance(keyStoreType);
			InputStream trustStoreInput = new FileInputStream(keyStoreFilename);
			store.load(trustStoreInput, keyStorePassword.toCharArray());
			return store;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			log.error("Attempt to create keystore {} caused error.", keyStoreFilename, e);
			return null;
		}
	}
}
