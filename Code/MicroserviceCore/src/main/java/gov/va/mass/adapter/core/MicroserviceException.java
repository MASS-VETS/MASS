package gov.va.mass.adapter.core;

@SuppressWarnings("serial")
public class MicroserviceException extends Exception {
	public MicroserviceException(String errorMessage) {
		super(errorMessage);
	}
}
