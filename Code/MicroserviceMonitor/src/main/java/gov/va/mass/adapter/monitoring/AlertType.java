package gov.va.mass.adapter.monitoring;

public enum AlertType {
	// @formatter:off
	QueueSizeThresholdReached,
	NotEnoughConsumersOnQueue,
	UnmonitoredQueue,
	ServiceDown,
	ServiceStoppedItself,
	// @formatter:on
}
