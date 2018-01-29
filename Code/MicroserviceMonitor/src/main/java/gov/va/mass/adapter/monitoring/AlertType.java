package gov.va.mass.adapter.monitoring;

/**
 * @author avolkano
 */
public enum AlertType {
	// @formatter:off
	QueueSizeThresholdReached,
	NotEnoughConsumersOnQueue,
	UnmonitoredQueue,
	ServiceDown,
	ServiceStoppedItself,
	// @formatter:on
}
