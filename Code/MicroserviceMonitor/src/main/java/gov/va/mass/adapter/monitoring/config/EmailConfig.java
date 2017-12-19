package gov.va.mass.adapter.monitoring.config;

/**
 * @author sleader
 */
public class EmailConfig
{
	private String fromAddress;
	
	public String getFromAddress() {
		return this.fromAddress;
	}
	
	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}
	
	private String toAddress;
	
	public String getToAddress() {
		return this.toAddress;
	}
	
	public void setToAddress(String toAddress) {
		this.toAddress = toAddress;
	}
	
	private String password;
	
	public String getPassword() {
		return this.password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
}
