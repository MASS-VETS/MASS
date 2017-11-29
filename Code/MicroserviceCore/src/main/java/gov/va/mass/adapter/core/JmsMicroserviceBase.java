package gov.va.mass.adapter.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import gov.va.mass.adapter.core.JmsMicroserviceState.RunState;

/**
 * base class that a jms-listening microservice should extend in order to be
 * able to be monitored
 * 
 * @author avolkano
 */

@RestController
@RequestMapping("/")
public abstract class JmsMicroserviceBase extends MicroserviceBase {
	protected JmsMicroserviceState state = new JmsMicroserviceState(serviceName());
	
	@Autowired
	private JmsListenerEndpointRegistry registry;
	
	/**
	 * extending class should use this if it should pause the JMS listener, but can
	 * restart it without a hard reset.
	 */
	@GetMapping("/jms/pauselistener")
	public void pauseJmsListener() {
		if (registry != null) {
			registry.stop();
		}
		state.runState = RunState.Paused;
	}
	
	/**
	 * extending class should use this if it encounters an unrecoverable error
	 * condition and should stop.
	 * 
	 * @throws MicroserviceException
	 *           always thrown so that microservice will leave message on queue
	 */
	protected MicroserviceException enterErrorState(String errorMessage) {
		if (registry != null) {
			registry.stop();
		}
		state.errorMessage = errorMessage;
		state.runState = RunState.ErrorCondition;
		
		return new MicroserviceException(errorMessage);
	}
	
	/**
	 * extending class may use this if it is able to restart without a hard reset.
	 */
	@GetMapping("/jms/restartlistener")
	public void restartJmsListener() {
		if (registry != null) {
			registry.start();
		}
		state.runState = RunState.Running;
	}
}
