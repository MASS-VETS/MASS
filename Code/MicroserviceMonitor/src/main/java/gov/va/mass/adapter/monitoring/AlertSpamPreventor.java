package gov.va.mass.adapter.monitoring;

import java.util.Hashtable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import gov.va.mass.adapter.monitoring.config.MonitorConfig;

@Component
public class AlertSpamPreventor {
	
	@Autowired
	private MonitorConfig config;
	
	private Hashtable<String, Long> lastSentTime = new Hashtable<String, Long>();
	
	public boolean shouldSendEmail(AlertType type, String forEntity) {
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
