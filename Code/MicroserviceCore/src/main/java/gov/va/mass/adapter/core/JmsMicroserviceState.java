package gov.va.mass.adapter.core;

import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.activemq.broker.jmx.DestinationViewMBean;

/**
 * state object used by a microservice to track run state so it is able to be
 * monitored
 * 
 * @author avolkano
 */
public class JmsMicroserviceState extends MicroserviceState {
	
	/*
	 * In order to actually use this, you need to enable JMX on the JMS broker. For
	 * ActiveMQ, see http://activemq.apache.org/jmx.html
	 * 
	 * You also need to expose port 1099 in the container so your microservice can
	 * talk to the JMS broker's JMX server.
	 */
	
	private final String jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi"; // should this be parameterized?
	private String[] queuesToWatch;
	
	public JmsMicroserviceState(String serviceName, String... queuesToWatch) {
		super(serviceName);
		this.queuesToWatch = queuesToWatch;
	}
	
	public enum RunState {
		Running, Paused, ErrorCondition
	}
	
	public RunState runState = RunState.Running;
	
	@Override
	protected void addCustomProperties(JsonObjectBuilder builder) {
		// add the run state. Maybe this should be in the base class instead?
		builder.add("runState", runState.toString());
		
		// add queue stats (switching to query ActiveMQ)
		JMXConnector connector = null;
		try {
			connector = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl));
			MBeanServerConnection mbsc = connector.getMBeanServerConnection();
			String nameString = "org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue";
			
			for (String queue : queuesToWatch) {
				ObjectName destinationName = new ObjectName(nameString + ",destinationName=" + queue);
				DestinationViewMBean dvmb = MBeanServerInvocationHandler.newProxyInstance(mbsc, destinationName,
						DestinationViewMBean.class, true);
				QueueStats stats = new QueueStats(dvmb);
				builder.add(queue, stats.toJson());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		} finally {
			if (connector != null) {
				
				try {
					connector.close();
				} catch (IOException e) {
					// I don't think we actually care about this error, but I'm writing it anyway.
					e.printStackTrace();
				}
			}
		}
	}
	
	private class QueueStats {
		private boolean exists;
		private long queueDepth;
		private long consumers;
		private long enqueueCount;
		private long dequeueCount;
		
		public QueueStats(DestinationViewMBean dvmb) {
			try {
				exists = true;
				queueDepth = dvmb.getQueueSize();
				consumers = dvmb.getConsumerCount();
				enqueueCount = dvmb.getEnqueueCount();
				dequeueCount = dvmb.getDequeueCount();
			} catch (Exception e) {
				exists = false;
			}
		}
		
		public JsonObjectBuilder toJson() {
			JsonObjectBuilder queueStats = Json.createObjectBuilder();
			queueStats.add("exists", exists);
			if (exists) {
				queueStats.add("depth", queueDepth);
				queueStats.add("consumers", consumers);
				queueStats.add("enqueueCount", enqueueCount);
				queueStats.add("dequeueCount", dequeueCount);
			}
			return queueStats;
		}
	}
}
