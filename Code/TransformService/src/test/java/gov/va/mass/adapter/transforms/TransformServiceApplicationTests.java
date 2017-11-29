package gov.va.mass.adapter.transforms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.listener.adapter.JmsResponse;
import org.springframework.test.context.junit4.SpringRunner;
import ca.uhn.hl7v2.HL7Exception;
import gov.va.mass.adapter.core.MicroserviceException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransformServiceApplicationTests {
	TransformService svc = new TransformService();
	static final Logger LOG = LoggerFactory.getLogger(TransformServiceApplicationTests.class);
	
	public TransformServiceApplicationTests() {
		svc.outputQueue = "UTresponseQ";
	}
	
	@Test
	public void testUnknownTransform() throws HL7Exception {
		String input = "MSH|^~\\&|XDPS||||20170914151436-0500||ADT^A03|1|T|2.3\r";
		
		svc.xsltName = "ADT_A01";
		try {
			svc.transformPipeMessage(input);
			
			fail("Shouldn't get here, because transform should have thrown an exception!");
		} catch (MicroserviceException e) {
			assertTrue(true);
		}
	}
	
	@Test
	// Patient Record Flags (ORU^R01)
	public void testDLUMessage1() throws HL7Exception, MicroserviceException {
		String pipeMsg = "MSH|^~\\&|XDPS|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|HLO DEMO RECEIVING APPLICATION|^dhcp.epic.com:5106^DNS|20170914151436-0500||ORU^R01^|500 880|T^|2.4|||AL|NE|\r"
				+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PATIENT^^^^||19350407|M|P8^^^^^~DXL^TEST^^^^||Line 1^LINE 2^SEYMOUR^TN^37865^^BA^~123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C^||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008||||||||||\r"
				+ "OBR|1|||BEHAVIORAL\r" + "OBX|1|ST|Status||INACTIVE\r"
				+ "OBX|2|TX|Narrative||shows signs of potential violence\r" + "OBR|2|||HIGH RISK FOR SUICIDE\r"
				+ "OBX|1|ST|Status||ACTIVE\r" + "OBX|2|TX|Narrative||LINE ONE\r" + "OBX|2|TX|Narrative||LINE TWO\r"
				+ "OBX|2|TX|Narrative||LINE THREE\r" + "OBX|2|TX|Narrative||LINE FOUR\r" + "OBX|2|TX|Narrative||LINE FIVE\r"
				+ "OBX|2|TX|Narrative||LINE SIX\r" + "OBX|2|TX|Narrative||LINE SEVEN\r" + "OBX|2|TX|Narrative||LINE EIGHT\r"
				+ "OBX|3|TX|Narrative||LINE NINE\r";
		
		String expected = "MSH|^~\\&|XDPS|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|HLO DEMO RECEIVING APPLICATION|^dhcp.epic.com:5106^DNS|20170914151436-0500||ADT^A31|500 880|T|2.4|||AL|NE\r"
				+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PATIENT||19350407|M|P8~DXL^TEST||Line 1^LINE 2^SEYMOUR^TN^37865^^BA~123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008\r"
				+ "ZFY|BEHAVIORAL|INACTIVE|BEHAVIORAL|shows signs of potential violence\r"
				+ "ZFY|HIGH RISK FOR SUICIDE|ACTIVE|HIGH RISK FOR SUICIDE|LINE ONE\\.br\\LINE TWO\\.br\\LINE THREE\\.br\\LINE FOUR\\.br\\LINE FIVE\\.br\\LINE SIX\\.br\\LINE SEVEN\\.br\\LINE EIGHT\\.br\\LINE NINE\r";
		
		svc.xsltName = "ORU_R01";
		JmsResponse<String> response = svc.transformPipeMessage(pipeMsg);
		String output = response.getResponse();
		LOG.debug(output);
		assertEquals(expected, output);
	}
	
	@Test
	// Bad Address Indicator (ORU^R01)
	public void testDLUMessage2() throws HL7Exception, MicroserviceException {
		String pipeMsg = "MSH|^~\\&|DG-REG-OUT|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|MASS|^dhcp.epic.com:5106^DNS|20170914151436-0500||ORU^R01^|500 8207|T^|2.4|||AL|NE|\r"
				+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PATIENT^^^^||19350407|M|P8^^^^^~DXL^TEST^^^^||Line 1^LINE 2^SEYMOUR^TN^37865^^BA^~123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C^||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008||||||||||\r"
				+ "OBR|1|||BAD ADDRESS\r" + "OBX|1|ST|Status||ACTIVE\r" + "OBX|2|TX|Narrative||HOMELESS2\r";
		
		String expected = "MSH|^~\\&|DG-REG-OUT|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|MASS|^dhcp.epic.com:5106^DNS|20170914151436-0500||ADT^A31|500 8207|T|2.4|||AL|NE\r"
				+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PATIENT||19350407|M|P8~DXL^TEST||Line 1^LINE 2^SEYMOUR^TN^37865^^BA~123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008\r"
				+ "ZFY|BAD ADDRESS|ACTIVE|BAD ADDRESS|HOMELESS2\r";
		
		svc.xsltName = "ORU_R01";
		JmsResponse<String> response = svc.transformPipeMessage(pipeMsg);
		String output = response.getResponse();
		LOG.debug(output);
		assertEquals(expected, output);
	}
	
	@Test
	// Patient Sensitivity Flag (ORU^R01)
	public void testDLUMessage3() throws HL7Exception, MicroserviceException {
		String pipeMsg = "MSH|^~\\&|DG-REG-OUT|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|MASS|^dhcp.epic.com:5106^DNS|20170914151436-0500||ORU^R01^|500 8207|T^|2.4|||AL|NE|\r"
				+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PATIENT^^^^||19350407|M|P8^^^^^~DXL^TEST^^^^||Line 1^LINE 2^SEYMOUR^TN^37865^^BA^~123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C^||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008\r"
				+ "OBR|1|||SENSITIVITY\r" + "OBX|1|ST|Status||ACTIVE\r";
		
		String expected = "MSH|^~\\&|DG-REG-OUT|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|MASS|^dhcp.epic.com:5106^DNS|20170914151436-0500||ADT^A31|500 8207|T|2.4|||AL|NE\r"
				+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PATIENT||19350407|M|P8~DXL^TEST||Line 1^LINE 2^SEYMOUR^TN^37865^^BA~123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008\r"
				+ "ZFY|SENSITIVITY|ACTIVE|SENSITIVITY\r";
		
		svc.xsltName = "ORU_R01";
		JmsResponse<String> response = svc.transformPipeMessage(pipeMsg);
		String output = response.getResponse();
		LOG.debug(output);
		assertEquals(expected, output);
	}
	
	@Test
	// Patient Update Messages (ADT^A08)
	public void testDLUMessage4() throws HL7Exception, MicroserviceException {
		String pipeMsg = "MSH|^~\\&|DG-REG-OUT|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|MASS|^dhcp.epic.com:5106^DNS|20171010163409-0500||ADT^A08^|500 9406|T^|2.4|||AL|NE\r"
				+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PA^^^^||19350409|M|P8^^^^^~DXL^TEST^^^^||123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C^||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008|||||||||\r"
				+ "PV1|1|||||||||||||||||SC VETERAN\r"
				+ "ZCT|1|DXL^EMERGENCY|Brother|123 Emergency St^Apt 111^Madison^WI^^^^|(865)776-0002\r"
				+ "ZEL|1|SC LESS THAN 50%||||||||VERIFIED|||||||||||||||||||||||||||YES|20090917\r"
				+ "ZEL|2|ALLIED VETERAN|||||||||||||||||||||||||||||||||||\r"
				+ "ZEL|3|WORLD WAR I|||||||||||||||||||||||||||||||||||\r" + "ZEN|1|||UNVERIFIED|||||GROUP 1|||\r"
				+ "ZSP|1|Y|50|||||||||CLASS II\r";
		
		String expected = "MSH|^~\\&|DG-REG-OUT|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|MASS|^dhcp.epic.com:5106^DNS|20171010163409-0500||ADT^A31|500 9406|T|2.4|||AL|NE\r"
				+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PA||19350409|M|P8~DXL^TEST||123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008\r"
				+ "NK1|1||123 Emergency St Apt 111 Madison WI\r"
				+ "PV1|1|||||||||||||||||SC VETERAN\r"
				+ "ZFY|SERVICE CONNECTED|ACTIVE|SERVICE CONNECTED|50% Service Connected\r"
				+ "ZEL|SC LESS THAN 50%~ALLIED VETERAN~WORLD WAR I|CLASS II|GROUP 1|UNVERIFIED|YES|20090917\r";
		
		svc.xsltName = "ADT_A08";
		JmsResponse<String> response = svc.transformPipeMessage(pipeMsg);
		String output = response.getResponse();
		LOG.debug(output);
		assertEquals(expected, output);
	}
