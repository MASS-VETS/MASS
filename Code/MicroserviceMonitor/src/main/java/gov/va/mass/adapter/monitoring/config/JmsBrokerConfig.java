package gov.va.mass.adapter.monitoring.config;

/**
 * @author avolkano
 */
public class JmsBrokerConfig extends MicroserviceConfig {
	
	private String username;
	
	public String getUsername() {
		return this.username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	private String password;
	
	public String getPassword() {
		return this.password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
}
