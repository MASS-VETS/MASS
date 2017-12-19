package gov.va.mass.adapter.monitoring.config;

/**
 * @author avolkano
 */
public class MicroserviceConfig {
	
	private String url;
	
	public String getUrl() {
		return this.url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	private Boolean useSsl = false;
	
	public Boolean getUseSsl() {
		return this.useSsl;
	}
	
	public void setUseSsl(Boolean useSsl) {
		this.useSsl = useSsl;
	}
}
