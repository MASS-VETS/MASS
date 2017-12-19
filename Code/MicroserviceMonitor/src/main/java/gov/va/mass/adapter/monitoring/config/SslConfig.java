package gov.va.mass.adapter.monitoring.config;

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
}
