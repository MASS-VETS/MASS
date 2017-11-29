package gov.va.mass.adapter.monitoring.stats;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.springframework.web.client.RestTemplate;

/**
 * @author avolkano
 */
public class PulseStats {
	public String status;
	public String serviceName;
	public int pastTime;
	public int currentTime;
	public int lastCalledTime;
	public int serviceIn;
	public int serviceOutSuccess;
	public int serviceOutFailed;
	
	//{"status":"pulseSuccess","serviceName":"ReceiveOverHapiService","pastTime":1511672356,"currentTime":1511694073,"lastCalledTime":-1,"serviceIn":0,"serviceOutSuccess":0,"serviceOutFailed":0}dd
	
	public PulseStats(RestTemplate restTemplate, String brokerUri) throws URISyntaxException {
		//String brokerObjectName = "org.apache.activemq:type=Broker,brokerName=localhost";
		String brokerObjectName = "heartbeat/pulse";
		JsonObject broker = getObjectWithName(restTemplate, brokerUri, brokerObjectName);
		setPulseStats(broker);
	}
	
	private void setPulseStats(JsonObject value) {
		status = value.getString("status");
		serviceName = value.getString("serviceName");
		pastTime = value.getInt("pastTime");
		currentTime = value.getInt("currentTime");
		lastCalledTime = value.getInt("lastCalledTime");
		serviceIn = value.getInt("serviceIn");
		serviceOutSuccess = value.getInt("serviceOutSuccess");
		serviceOutFailed = value.getInt("serviceOutFailed");
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
	
	@Override
	public String toString() {
		return 	status + " | " + serviceName + " | " + pastTime	+ " | " + currentTime + " | " + lastCalledTime + " | " + serviceIn + " | " + serviceOutSuccess + " | " + serviceOutFailed;
	}
}
