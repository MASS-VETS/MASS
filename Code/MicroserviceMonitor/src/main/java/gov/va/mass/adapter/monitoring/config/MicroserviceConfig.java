package gov.va.mass.adapter.monitoring.config;

/**
 * @author avolkano
 */
public class MicroserviceConfig {
	
	private int port;
	
	public int getPort() {
		return this.port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	private String path;
	
	public String getPath() {
		return this.path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	private Boolean useSsl = false;
	
	public Boolean getUseSsl() {
		return this.useSsl;
	}
	
	public void setUseSsl(Boolean useSsl) {
		this.useSsl = useSsl;
	}
	
	public String fullUri(String server) {
		return (useSsl ? "https" : "http") + "://" + server + ":" + this.port + (this.path == null ? "" : this.path) + "/";
	}
}
