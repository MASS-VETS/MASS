package gov.va.mass.adapter.monitoring.config;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import gov.va.mass.adapter.core.HttpClientProvider;

/**
 * @author avolkano
 */
public class SslConfig {
	
	private String keyStore;
	
	public String getKeyStore() {
		return this.keyStore;
	}
	
	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}
	
	private String keyStorePassword;
	
	public String getKeyStorePassword() {
		return this.keyStorePassword;
	}
	
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}
	
	private String keyStoreType;
	
	public String getKeyStoreType() {
		return this.keyStoreType;
	}
	
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}
	
	public KeyStore createKeystore(HttpClientProvider clients) {
		return clients.createKeystore(keyStoreType, keyStore, keyStorePassword);
	}
}
