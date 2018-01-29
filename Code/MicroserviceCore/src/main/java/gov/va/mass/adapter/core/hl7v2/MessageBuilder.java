package gov.va.mass.adapter.core.hl7v2;

import java.util.ArrayList;

public class MessageBuilder {
	private MSH MSH;
	private ArrayList<Segment> segments;
	
	public MessageBuilder(MSH msh) {
		MSH = msh;
		segments = new ArrayList<Segment>();
	}
	
	public int addSegment(String segmentName) {
		segments.add(new Segment(segmentName));
		return segments.size() - 1;
	}
	
	public void set(int segNo, int fieldNo, String value) {
		segments.get(segNo).set(fieldNo, value);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(MSH.toString());
		sb.append("\r");
		for (Segment segment : segments) {
			sb.append(String.join(MSH.fs, segment.fields));
			sb.append("\r");
		}
		return sb.toString();
	}
}
