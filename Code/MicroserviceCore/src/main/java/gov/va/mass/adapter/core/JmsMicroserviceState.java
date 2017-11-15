package gov.va.mass.adapter.core;

import javax.json.JsonObjectBuilder;

/**
 * state object used by a microservice to track run state so it is able to be
 * monitored
 * 
 * @author avolkano
 */
public class JmsMicroserviceState extends MicroserviceState {
	
	public JmsMicroserviceState(String serviceName) {
		super(serviceName);
	}
	
	public enum RunState {
		Running, Paused, ErrorCondition
	}
	
	public RunState runState = RunState.Running;
	public String errorMessage = "";
	
	@Override
	protected void addCustomProperties(JsonObjectBuilder builder) {
		// add the run state. Maybe this should be in the base class instead?
		builder.add("runState", runState.toString());
	}
}
