package gov.va.mass.adapter.monitoring.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * @author avolkano
 */
@ConfigurationProperties(prefix = "monitor")
@Component
public class MonitorConfig {
	
	private String rate;
	
	public String getRate() {
		return this.rate;
	}
	
	public void setRate(String rate) {
		this.rate = rate;
	}
	
	@NestedConfigurationProperty
	private MicroserviceConfig messagedb = new MicroserviceConfig();
	
	public MicroserviceConfig getMessagedb() {
		return this.messagedb;
	}
	
	public void setMessagedb(MicroserviceConfig messagedb) {
		this.messagedb = messagedb;
	}
	
	@NestedConfigurationProperty
	private JmsBrokerConfig jms = new JmsBrokerConfig();
	
	public JmsBrokerConfig getJms() {
		return this.jms;
	}
	
	public void setJms(JmsBrokerConfig jms) {
		this.jms = jms;
	}
	
	@NestedConfigurationProperty
	private List<InterfaceConfig> interfaces = new ArrayList<InterfaceConfig>();
	
	public List<InterfaceConfig> getInterfaces() {
		return this.interfaces;
	}
	
	public void setInterfaces(List<InterfaceConfig> interfaces) {
		this.interfaces = interfaces;
	}
	
	@NestedConfigurationProperty
	private EmailConfig email = new EmailConfig();
	
	public EmailConfig getEmail() {
		return this.email;
	}
	
	public void setEmail(EmailConfig email) {
		this.email = email;
	}
	
	@NestedConfigurationProperty
	private SMTPConfig smtp = new SMTPConfig();
	
	public SMTPConfig getSMTP() {
		return this.smtp;
	}
	
	public void setSMTP(SMTPConfig smtp) {
		this.smtp = smtp;
	}
	
	@NestedConfigurationProperty
	private List<AlertConfig> alerts = new ArrayList<AlertConfig>();
	
	public List<AlertConfig> getAlerts() {
		return this.alerts;
	}
	
	public void setAlerts(List<AlertConfig> alerts) {
		this.alerts = alerts;
	}
	
	private Integer timeBetweenAlerts;
	
	public Integer getTimeBetweenAlerts() {
		return this.timeBetweenAlerts;
	}
	
	public void setTimeBetweenAlerts(Integer timeBetweenAlerts) {
		this.timeBetweenAlerts = timeBetweenAlerts;
	}
}
