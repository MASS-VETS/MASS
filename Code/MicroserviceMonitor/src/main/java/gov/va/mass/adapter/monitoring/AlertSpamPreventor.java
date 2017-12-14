package gov.va.mass.adapter.monitoring;

import java.util.Hashtable;
import org.springframework.beans.factory.annotation.Autowired;
import gov.va.mass.adapter.monitoring.config.MonitorConfig;

public class AlertSpamPreventor {
	
	@Autowired
	private MonitorConfig config;
	
	private Hashtable<String, Long> lastSentTime = new Hashtable<String, Long>();
	
	public boolean shouldSendEmail(AlertType type, String forEntity) {
		String key = type.name() + "." + forEntity;
		
		if (lastSentTime.containsKey(key)
				&& System.currentTimeMillis() - lastSentTime.get(key) < (config.getTimeBetweenAlerts() * 60000)) {
			return false;
		}
		// need to send one again, flag current time.
		lastSentTime.put(key, System.currentTimeMillis());
		return true;
	}
}
