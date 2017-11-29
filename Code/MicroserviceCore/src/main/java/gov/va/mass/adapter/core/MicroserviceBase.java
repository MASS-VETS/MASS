package gov.va.mass.adapter.core;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * base class that a microservice should extend in order to be able to be
 * monitored
 * 
 * @author avolkano
 */

@RestController
@RequestMapping("/")
public abstract class MicroserviceBase {
	protected MicroserviceState state = new MicroserviceState(serviceName());
	
	/**
	 * extending class needs to implement this to construct their state object
	 */
	protected abstract String serviceName();
	
	/**
	 * monitor will call this to determine if the microservice is even running
	 */
	@GetMapping("/heartbeat/isalive")
	public String IsAlive() {
		return state.isAliveResponse();
	}
	
	/**
	 * monitor will call this to get vital statistics and judge if the condition of
	 * the microservice is such that we should alert an administrator.
	 */
	@GetMapping("/heartbeat/pulse")
	public String Pulse() {
		return state.pulseResponse();
	}
}
