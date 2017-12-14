package gov.va.mass.adapter.monitoring.config;

/**
 * @author sleader
 */
public class AlertConfig {
	
	private String name;
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	private Integer queueMax;
	
	public Integer getQueueMax() {
		return this.queueMax;
	}
	
	public void setQueueMax(Integer queueMax) {
		this.queueMax = queueMax;
	}
	
	private Integer consumerMin;
	
	public Integer getConsumerMin() {
		return this.consumerMin;
	}
	
	public void setConsumerMin(Integer consumerMin) {
		this.consumerMin = consumerMin;
	}
}
