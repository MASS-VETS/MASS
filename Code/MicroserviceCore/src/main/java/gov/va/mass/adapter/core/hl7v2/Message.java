package gov.va.mass.adapter.core.hl7v2;

import java.util.regex.Pattern;

public class Message {
	private String[][] fields;
	private String[] segments;
	public MSH MSH;
	
	public String ControlId;
	public String ProcessingId;
	public String Version;
	
	public Message(String toParse) throws Exception {
		if (!toParse.startsWith("MSH")) {
			throw new Exception("Message must start with MSH");
		}
		if (!toParse.endsWith("\r")) {
			throw new Exception("Message must end with character 13");
		}
		int eol = toParse.indexOf("\r");
		String rawMsh = toParse.substring(0, eol);
		MSH = new MSH(rawMsh);
		segments = toParse.split(Pattern.quote("\r"));
		fields = new String[segments.length][];
		for (int i = 0; i < segments.length; i++) {
			fields[i] = null;
		}
		
		// set some shortcuts
		ControlId = MSH.get(10);
		ProcessingId = MSH.get(11, MSH.cs, 1);
		Version = MSH.get(12);
	}
	
	public String getField(int segNo, int fieldNo) {
		if (segNo >= segments.length) {
			return "";
		}
		if (fields[segNo] == null) {
			fields[segNo] = segments[segNo].split(Pattern.quote(MSH.fs));
		}
		if (fieldNo >= fields[segNo].length) {
			return "";
		}
		return fields[segNo][fieldNo];
	}
	
	public String generateAck() {
		return ackHelper("AA", null);
	}
	
	public String generateNak(String errorMessage) {
		return ackHelper("AE", errorMessage);
	}
	
	private String ackHelper(String ackCode, String errorMessage) {
		MSH newMsh = new MSH(MSH);
		newMsh.set(3, MSH.get(5));
		newMsh.set(4, MSH.get(6));
		newMsh.set(5, MSH.get(3));
		newMsh.set(6, MSH.get(4));
		
		String[] msgType = MSH.get(9).split(Pattern.quote(MSH.cs));
		msgType[0] = "ACK";
		if (msgType.length > 2) {
			msgType[2] = "ACK";
		}
		newMsh.set(9, String.join(MSH.cs, msgType)); // set message type to ACK
		
		MessageBuilder b = new MessageBuilder(newMsh);
		
		int msa = b.addSegment("MSA");
		b.set(msa, 1, ackCode);
		b.set(msa, 2, this.ControlId);
		if (errorMessage != null) {
			b.set(msa, 3, errorMessage);
		}
		
		return b.toString();
	}
}
