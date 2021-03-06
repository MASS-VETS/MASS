package gov.va.mass.adapter.monitoring.stats;

import java.net.URISyntaxException;
import javax.json.JsonArray;
import javax.json.JsonObject;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * @author avolkano
 */
public class BrokerStats {
	public QueueStats[] queues;
	public boolean successfullyPolled = false;
	
	public BrokerStats(CloseableHttpClient client, String brokerUri) throws URISyntaxException {
		String brokerObjectName = "org.apache.activemq:type=Broker,brokerName=localhost";
		JsonObject broker = RestJsonHelper.getObjectAtPath(client, brokerUri, brokerObjectName);
		if (broker.isEmpty()) {
			return; // couldn't get stats from broker
		}
		JsonObject responseObject = broker.getJsonObject("value");
		JsonArray queueObjectArray = responseObject.getJsonArray("Queues");
		queues = new QueueStats[queueObjectArray.size()];
		for (int i = 0; i < queues.length; i++) {
			String queueObjectName = queueObjectArray.getJsonObject(i).getString("objectName");
			queues[i] = new QueueStats(RestJsonHelper.getObjectAtPath(client, brokerUri, queueObjectName));
		}
		successfullyPolled = true;
	}
}
