package gov.va.mass.adapter.core;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * state object used by a microservice to track run state so it is able to be
 * monitored
 * 
 * @author avolkano
 */
public class MicroserviceState {
	protected String serviceName;
	
	private long currentTime;
	private long lastCalled;
	private int serviceIn = 0;
	private int serviceOutSuccess = 0;
	private int serviceOutFailed = 0;
	
	public MicroserviceState(String serviceName) {
		this.serviceName = serviceName;
		currentTime = now();
		lastCalled = -1;
	}
	
	public void serviceCalled() {
		serviceIn++;
		lastCalled = now();
	}
	
	public void serviceSucceeded() {
		serviceOutSuccess++;
	}
	
	public void serviceFailed() {
		serviceOutFailed++;
	}
	
	public String isAliveResponse() {
		JsonObject isAliveObject = Json.createObjectBuilder()
				.add("serviceName", serviceName)
				.add("isAlive", true)
				.build();
		return isAliveObject.toString();
	}
	
	public String pulseResponse() {
		long pastTime = currentTime;
		currentTime = now();
		
		// build the standard properties first
		JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("status", "pulseSuccess")
				.add("serviceName", serviceName)
				.add("pastTime", pastTime)
				.add("currentTime", currentTime)
				.add("lastCalledTime", lastCalled)
				.add("serviceIn", serviceIn)
				.add("serviceOutSuccess", serviceOutSuccess)
				.add("serviceOutFailed", serviceOutFailed);
		
		// and then add the custom ones
		addCustomProperties(builder);
		JsonObject pulseObject = builder.build();
		
		// reset these for next pulse
		serviceIn = 0;
		serviceOutSuccess = 0;
		serviceOutFailed = 0;
		
		// and return the pulse
		return pulseObject.toString();
	}
	
	protected long now() {
		return System.currentTimeMillis() / 1000;
	}
	
	// extended state objects should implement this if they want custom props
	protected void addCustomProperties(JsonObjectBuilder builder) {
	}
}
