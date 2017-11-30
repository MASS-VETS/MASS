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
	
	private int timer;
	
	public int getTimer() {
		return this.timer;
	}
	
	public void setTimer(int timer) {
		this.timer = timer;
	}
	
	private Integer queueMax;
	
	public Integer getQueueMax() {
		return this.queueMax;
	}
	
	public void setQueueMax(int queueMax) {
		this.queueMax = queueMax;
	}
	
	private Integer consumerMax;
	
	public Integer getConsumerMax() {
		return this.consumerMax;
	}
	
	public void setConsumerMax(Integer consumerMax) {
		this.consumerMax = consumerMax;
	}
	
	private Integer enqueuedMax;
	
	public Integer getEnqueuedMax() {
		return this.enqueuedMax;
	}
	
	public void setEnqueuedMax(Integer enqueuedMax) {
		this.enqueuedMax = enqueuedMax;
	}
	
	private Integer dequeuedMax;
	
	public Integer getDequeuedMax() {
		return this.dequeuedMax;
	}
	
	public void setDequeuedMax(Integer dequeuedMax) {
		this.dequeuedMax = dequeuedMax;
	}
}
