package gov.va.mass.adapter.core;

import javax.annotation.PostConstruct;
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
	public JmsMicroserviceState state;
	
	/**
	 * initialize the monitor state object.
	 */
	@PostConstruct
	private void setStateObject() {
		state = initStateObject();
	}
	
	/**
	 * extending class needs to implement this to construct their state object
	 */
	@Override
	protected abstract JmsMicroserviceState initStateObject();
	
	@Autowired
	private JmsListenerEndpointRegistry registry;
	
	/**
	 * extending class should use this if it should pause the JMS listener, but can
	 * restart it without a hard reset.
	 */
	@GetMapping("/jms/pauselistener")
	public void pauseJmsListener() {
		registry.stop();
		state.runState = RunState.Paused;
	}
	
	/**
	 * extending class should use this if it encounters an unrecoverable error
	 * condition and should stop.
	 */
	protected void enterErrorState() {
		registry.stop();
		state.runState = RunState.ErrorCondition;
	}
	
	/**
	 * extending class may use this if it is able to restart without a hard reset.
	 */
	@GetMapping("/jms/restartlistener")
	public void restartJmsListener() {
		registry.start();
		state.runState = RunState.Running;
	}
}
