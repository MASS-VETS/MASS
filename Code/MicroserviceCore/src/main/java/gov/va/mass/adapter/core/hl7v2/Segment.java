package gov.va.mass.adapter.core.hl7v2;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class Segment {
	protected ArrayList<String> fields = new ArrayList<String>();
	
	public String get(int fieldNo) {
		if (fields.size() <= fieldNo) {
			return "";
		}
		return fields.get(fieldNo);
	}
	
	public String get(int fieldNo, String cs, int compNo) {
		String[] comps = get(fieldNo).split(Pattern.quote(cs));
		if (comps.length < compNo) {
			return "";
		}
		return comps[compNo - 1];
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
