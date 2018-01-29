package gov.va.mass.adapter.core.hl7v2;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class MSH {
	
	private ArrayList<String> fields = new ArrayList<String>();
	public String fs;
	public String cs;
	public String rs;
	public String es;
	public String ss;
	
	int getSize() {
		return fields.size();
	}
	
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
	
	public MSH(String rawMsh) {
		fs = "|";
		if (rawMsh.length() > 3) {
			fs = String.valueOf(rawMsh.charAt(3));
		}
		String[] mshFields = rawMsh.split(Pattern.quote(fs));
		fields.add(""); // technically MSH-0 isn't a thing.
		fields.add(fs);
		for (int i = 1; i < mshFields.length; i++) {
			fields.add(mshFields[i]);
		}
		cs = getControlChar(0, "^");
		rs = getControlChar(1, "~");
		es = getControlChar(2, "\\");
		ss = getControlChar(3, "&");
	}
	
	private String getControlChar(int pos, String defaultVal) {
		if (get(2).length() > pos) {
			return get(2).substring(pos, pos + 1);
		} else {
			return defaultVal;
		}
	}
	
	public MSH(MSH toCopy) {
		fs = String.valueOf(toCopy.fs);
		fields.addAll(toCopy.fields);
	}
	
	@Override
	public String toString() {
		ArrayList<String> flds = new ArrayList<String>(fields);
		flds.remove(0); // MSH is off by one.
		flds.set(0, "MSH"); // and MSH-1 is the pipe.
		return String.join(fs, flds);
	}
}
