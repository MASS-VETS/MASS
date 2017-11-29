package gov.va.mass.adapter.monitoring.stats;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.springframework.web.client.RestTemplate;

/**
 * @author avolkano
 */
public class BrokerStats {
	public QueueStats[] queues;
	
	public BrokerStats(RestTemplate restTemplate, String brokerUri) throws URISyntaxException {
		String brokerObjectName = "org.apache.activemq:type=Broker,brokerName=localhost";
		JsonObject broker = getObjectWithName(restTemplate, brokerUri, brokerObjectName);
		JsonObject responseObject = broker.getJsonObject("value");
		JsonArray queueObjectArray = responseObject.getJsonArray("Queues");
		queues = new QueueStats[queueObjectArray.size()];
		for (int i = 0; i < queues.length; i++) {
			String queueObjectName = queueObjectArray.getJsonObject(i).getString("objectName");
			queues[i] = new QueueStats(getObjectWithName(restTemplate, brokerUri, queueObjectName));
		}
	}

	private JsonObject getObjectWithName(RestTemplate restTemplate, String brokerUri, String objectName)
			throws URISyntaxException {
		URI uri = new URI(brokerUri + objectName);
		String responseString = restTemplate.getForObject(uri, String.class);
		JsonReader jsonReader = Json.createReader(new StringReader(responseString));
		JsonObject resp = jsonReader.readObject();
		jsonReader.close();
		return resp;
	}
}
