package gov.va.mass.adapter.monitoring.stats;

import javax.json.JsonObject;

/**
 * @author avolkano
 */
public class QueueStats {
	public int queueSize;
	public int consumerCount;
	public int enqueuedCount;
	public int dequeuedCount;
	public String name;
	
	public QueueStats(JsonObject queue) {
		JsonObject value = queue.getJsonObject("value");
		name = value.getString("Name");
		queueSize = value.getInt("QueueSize");
		consumerCount = value.getInt("ConsumerCount");
		enqueuedCount = value.getInt("EnqueueCount");
		dequeuedCount = value.getInt("DequeueCount");
	}
	
	@Override
	public String toString() {
		return name + " | " + queueSize + " | " + consumerCount + " | " + enqueuedCount + " | " + dequeuedCount;
	}
}
