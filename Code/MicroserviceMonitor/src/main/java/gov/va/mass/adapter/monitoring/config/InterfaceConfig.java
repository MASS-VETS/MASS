package gov.va.mass.adapter.monitoring.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author avolkano
 */
public class InterfaceConfig {
	private String name;
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@NestedConfigurationProperty
	private MicroserviceConfig transform = new MicroserviceConfig();
	
	public MicroserviceConfig getTransform() {
		return this.transform;
	}
	
	public void setTransform(MicroserviceConfig transform) {
		this.transform = transform;
	}
	
	@NestedConfigurationProperty
	private MicroserviceConfig receiver = new MicroserviceConfig();
	
	public MicroserviceConfig getReceiver() {
		return this.receiver;
	}
	
	public void setReceiver(MicroserviceConfig receiver) {
		this.receiver = receiver;
	}
	
	@NestedConfigurationProperty
	private MicroserviceConfig sender = new MicroserviceConfig();
	
	public MicroserviceConfig getSender() {
		return this.sender;
	}
	
	public void setSender(MicroserviceConfig sender) {
		this.sender = sender;
	}
}
