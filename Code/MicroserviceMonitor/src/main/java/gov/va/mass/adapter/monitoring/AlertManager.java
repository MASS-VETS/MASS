package gov.va.mass.adapter.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import gov.va.mass.adapter.monitoring.email.EmailTemplate;

/**
 * @author avolkano
 */
@Component
public class AlertManager {
	
	@Autowired
	private AlertSpamPreventor spamPreventor;
	
	private HashMap<AlertType, ArrayList<Alert>> alerts = new HashMap<AlertType, ArrayList<Alert>>();
	
	public void clear() {
		alerts.clear();
	}
	
	public void raiseAlert(AlertType alertType, String entity, Object... args) {
		if (!spamPreventor.shouldAlert(alertType, entity)) {
			return;
		}
		if (!alerts.containsKey(alertType)) {
			alerts.put(alertType, new ArrayList<Alert>());
		}
		alerts.get(alertType).add(new Alert(alertType, entity, args));
	}
	
	public void sendAlertsAsEmails(String toAddress, EmailTemplate emailTemplate) {
		for (AlertType alertType : alerts.keySet()) {
			StringBuilder messageBody = new StringBuilder();
			for (Alert alert : alerts.get(alertType)) {
				messageBody.append(alert.getEmailText() + "\n");
			}
			emailTemplate.SendMail(toAddress, getEmailSubject(alertType), messageBody.toString());
		}
		clear();
	}
	
	private String getEmailSubject(AlertType type) {
		switch (type) {
		case NotEnoughConsumersOnQueue:
			return "Unwatched Queue Alert";
		case QueueSizeThresholdReached:
			return "Queue Size Alert";
		case ServiceDown:
			return "Service Down Alert";
		case ServiceStoppedItself:
			return "Service Paused Alert";
		case UnmonitoredQueue:
			return "Unmonitored Queue Alert";
		default:
			return "Unknown Error Thrown";
		}
	}
	
	private class Alert {
		public Alert(AlertType alertType, String entity, Object... args) {
			this.alertType = alertType;
			this.entity = entity;
			this.args = args;
		}
		
		public AlertType alertType;
		public String entity;
		public Object[] args;
		
		public String getEmailText() {
			switch (alertType) {
			case NotEnoughConsumersOnQueue:
				return String.format(
						"Queue %s requires at least %d consumer(s), but only has %d.", entity, args[0], args[1]);
			case QueueSizeThresholdReached:
				return String.format(
						"Queue %s depth is %d. Threshold: %d.", entity, args[0], args[1]);
			case ServiceDown:
				return String.format(
						"Service %s is not responding.", entity);
			case ServiceStoppedItself:
				return String.format(
						"Service %s has stopped and is waiting for error resolution.\n\nError:\n%s", args[0]);
			case UnmonitoredQueue:
				return String.format(
						"The following queue(s) were not configured in the monitor: \n\n", entity);
			default:
				return "An unknown error has occurred within the adapter.";
			}
		}
	}
}
