package gov.va.mass.adapter.monitoring;

import java.util.Hashtable;
import org.springframework.stereotype.Component;
import gov.va.mass.adapter.monitoring.config.MonitorConfig;

/**
 * @author avolkano
 */
@Component
public class AlertSpamPreventor {
	
	private MonitorConfig config;
	
	private Hashtable<String, Long> lastSentTime = new Hashtable<String, Long>();
	
	public AlertSpamPreventor(MonitorConfig config) {
		this.config = config;
	}
	
	public boolean shouldAlert(AlertType type, String forEntity) {
		String key = type.name() + "." + forEntity;
		long timeBetween = config.getTimeBetweenAlerts() * 60000;
		
		if (lastSentTime.containsKey(key) && System.currentTimeMillis() - lastSentTime.get(key) < timeBetween) {
			return false;
		}
		// need to send one again, flag current time.
		lastSentTime.put(key, System.currentTimeMillis());
		return true;
	}
}
