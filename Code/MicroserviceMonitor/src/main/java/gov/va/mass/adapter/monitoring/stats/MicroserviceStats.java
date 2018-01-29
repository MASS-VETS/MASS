package gov.va.mass.adapter.monitoring.stats;

import java.net.URISyntaxException;
import javax.json.JsonObject;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * @author avolkano
 */
public class MicroserviceStats {
	public boolean isAlive = false;
	public String status;
	public String serviceName;
	public String runState = "";
	public String errorMessage = "";
	public int pastTime;
	public int currentTime;
	public int lastCalledTime;
	public int serviceIn;
	public int serviceOutSuccess;
	public int serviceOutFailed;
	
	// {"status":"pulseSuccess","serviceName":"ReceiveOverHapiService","pastTime":1511672356,"currentTime":1511694073,"lastCalledTime":-1,"serviceIn":0,"serviceOutSuccess":0,"serviceOutFailed":0}
	
	public MicroserviceStats(CloseableHttpClient client, String baseUri) throws URISyntaxException {
		String path = "heartbeat/pulse";
		JsonObject pulse = RestJsonHelper.getObjectAtPath(client, baseUri, path);
		
		if (pulse.isEmpty()) {
			return; // couldn't ping the microservice
		}
		
		status = pulse.getString("status");
		if (pulse.containsKey("runState")) {
			runState = pulse.getString("runState");
		}
		if (pulse.containsKey("errorMessage")) {
			errorMessage = pulse.getString("errorMessage");
		}
		serviceName = pulse.getString("serviceName");
		pastTime = pulse.getInt("pastTime");
		currentTime = pulse.getInt("currentTime");
		lastCalledTime = pulse.getInt("lastCalledTime");
		serviceIn = pulse.getInt("serviceIn");
		serviceOutSuccess = pulse.getInt("serviceOutSuccess");
		serviceOutFailed = pulse.getInt("serviceOutFailed");
		isAlive = true;
	}
	
	@Override
	public String toString() {
		return status + " | " + serviceName + " | " + pastTime + " | " + currentTime + " | " + lastCalledTime + " | "
				+ serviceIn + " | " + serviceOutSuccess + " | " + serviceOutFailed + " | " + runState + " | " + errorMessage;
	}
}
