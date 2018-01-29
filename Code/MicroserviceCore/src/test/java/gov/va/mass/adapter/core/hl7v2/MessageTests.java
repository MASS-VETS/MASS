package gov.va.mass.adapter.core.hl7v2;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MessageTests {
	
	@Test
	public void TestMessageEmpty() {
		assertThatThrownBy(() -> new Message("")).hasMessage("Message must start with MSH");
	}
	
	@Test
	public void TestMessagePID() {
		assertThatThrownBy(() -> new Message("PID")).hasMessage("Message must start with MSH");
	}
	
	@Test
	public void TestMessageMSH() {
		assertThatThrownBy(() -> new Message("MSH")).hasMessage("Message must end with character 13");
	}
	
	@Test
	public void TestMessageMSHr() throws Exception {
		Message msg = new Message("MSH\r");
		assertEquals("", msg.ControlId);
		assertEquals("", msg.ProcessingId);
		assertEquals("", msg.Version);
	}
	
	@Test
	public void TestMessageMSHfull() throws Exception {
		Message msg = new Message("MSH|^~\\&|||||20180118183134|ME|ADT^A08|123|T|2.4\r");
		assertEquals("123", msg.ControlId);
		assertEquals("T", msg.ProcessingId);
		assertEquals("2.4", msg.Version);
	}
	
	@Test
	public void TestMessageMSHPID() throws Exception {
		Message msg = new Message("MSH|^~\\&|||||20180118183134|ME|ADT^A08|123|T|2.4\rPID|1||123^^MRN||ME\r");
		assertEquals("123", msg.ControlId);
		assertEquals("T", msg.ProcessingId);
		assertEquals("2.4", msg.Version);
		assertEquals("123^^MRN", msg.FieldFromSegment(1, 3));
	}
	
	@Test
	public void TestMessageAck() throws Exception {
		Message msg = new Message("MSH|^~\\&|SA|SF|RA|RF|20180118183134|ME|ADT^A08|123|T|2.4\rPID|1||123^^MRN||ME\r");
		assertEquals("MSH|^~\\&|RA|RF|SA|SF|20180118183134|ME|ACK^A08|123|T|2.4\rMSA|AA|123\r", msg.generateAck());
	}
	
	@Test
	public void TestMessageAck2() throws Exception {
		Message msg = new Message(
				"MSH|^~\\&|SA|SF|RA|RF|20180118183134|ME|ADT^A08^ADT_A01|123|T|2.4\rPID|1||123^^MRN||ME\r");
		assertEquals("MSH|^~\\&|RA|RF|SA|SF|20180118183134|ME|ACK^A08^ACK|123|T|2.4\rMSA|AA|123\r", msg.generateAck());
	}
	
	@Test
	public void TestMessageNak() throws Exception {
		Message msg = new Message("MSH|^~\\&|SA|SF|RA|RF|20180118183134|ME|ADT^A08|123|T|2.4\rPID|1||123^^MRN||ME\r");
		assertEquals("MSH|^~\\&|RA|RF|SA|SF|20180118183134|ME|ACK^A08|123|T|2.4\rMSA|AE|123|BANANA\r",
				msg.generateNak("BANANA"));
	}
	
	@Test
	public void TestOutOfBounds() throws Exception {
		Message msg = new Message("MSH|^~\\&|SA|SF|RA|RF|20180118183134|ME|ADT^A08|123|T|2.4\rPID|1||123^^MRN||ME\r");
		assertEquals("", msg.FieldFromSegment(1, 34));
		assertEquals("", msg.FieldFromSegment(12, 0));
	}
}
