package gov.va.mass.adapter.core.hl7v2;

import java.util.ArrayList;

public class Segment {
	protected ArrayList<String> fields = new ArrayList<String>();
	
	public String get(int fieldNo) {
		if (fields.size() <= fieldNo) {
			return "";
		}
		return fields.get(fieldNo);
	}
	
	public void set(int fieldNo, String value) {
		while (fields.size() <= fieldNo) {
			fields.add("");
		}
		fields.set(fieldNo, value);
	}
	
	public Segment(String segmentName) {
		set(0, segmentName);
	}
	
	public Segment(Segment toCopy) {
		fields.addAll(toCopy.fields);
	}
}
