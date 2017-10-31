package gov.va.mass.adapter.storage;

import java.util.List;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@PropertySource("classpath:application.properties")
public class MessageDbServiceApplicationTests {
	static final Logger log = LoggerFactory.getLogger(MessageDbServiceApplicationTests.class);
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	private String fieldList = "MSH-10,PID-5,PID-3";
	
	private String messageContent = "MSH|^~\\&|XDPS|500^HL7.VEHU.DOMAIN.GOV:5026^DNS|HLO DEMO RECEIVING APPLICATION|^dhcp.epic.com:5106^DNS|20170914151436-0500||ORU^R01^|500 880|T^|2.4|||AL|NE|\r"
			+ "PID|1||3^^^USVHA^IEN~10108^^^USVHA^NI||DXL^PATIENT^^^^||19350407|M|P8^^^^^~DXL^TEST^^^^||Line 1^LINE 2^SEYMOUR^TN^37865^^BA^~123 Conf Street^Addr Line 2^Knoxville^TN^37865^^C^||(222)555-8235^^P^DXLTEST@email.com|(222)555-7720|||||666000008||||||||||\r"
			+ "OBR|1|||BEHAVIORAL\r" + "OBX|1|ST|Status||INACTIVE\r"
			+ "OBX|2|TX|Narrative||shows signs of potential violence\r" + "OBR|2|||HIGH RISK FOR SUICIDE\r"
			+ "OBX|1|ST|Status||ACTIVE\r" + "OBX|2|TX|Narrative||LINE ONE\r" + "OBX|2|TX|Narrative||LINE TWO\r"
			+ "OBX|2|TX|Narrative||LINE THREE\r" + "OBX|2|TX|Narrative||LINE FOUR\r" + "OBX|2|TX|Narrative||LINE FIVE\r"
			+ "OBX|2|TX|Narrative||LINE SIX\r" + "OBX|2|TX|Narrative||LINE SEVEN\r" + "OBX|2|TX|Narrative||LINE EIGHT\r"
			+ "OBX|3|TX|Narrative||LINE NINE\r";
	
	/**/
	@Autowired
	HL7MessageDbService svc;
	
	@Value("${interface.id}")
	private String interfaceId;
	
	private String[] getMessagesWithKeyValue(String key, String value) {
		String sql = "SELECT messageData.MessageContent FROM messageData JOIN keyValues ON messageData.ID=keyValues.MessageID WHERE keyValues.Type=? AND keyValues.Value=?";
		List<String> results = jdbcTemplate.queryForList(sql, String.class, key, value);
		return results.toArray(new String[0]);
	}
	
	private boolean haveTestInterface() {
		if (doesInterfaceExist(interfaceId)) {
			log.info("interface '" + interfaceId + "' already exists.");
			return true;
		}
		else {
			log.info("interface '" + interfaceId + "' does not exist.");
			if (!insertInterface(interfaceId, "AVO TEST INTERFACE", "Incoming", 1)) {
				log.error("could not insert interface '" + interfaceId + "'");
				return false;
			}
			else {
				log.info("interface '" + interfaceId + "' inserted");
				return true;
			}
		}
	}
	
	private boolean doesInterfaceExist(String interfaceId) {
		String sql = "SELECT [ID] FROM interfaces WHERE ID = '" + interfaceId + "'";
		try {
			String result = jdbcTemplate.queryForObject(sql, String.class);
			return (result.length() > 0);
		}
		catch (EmptyResultDataAccessException e) {
			return false;
		}
	}
	
	private boolean insertInterface(String interfaceId, String name, String direction, int purgeDays) {
		return jdbcTemplate.update("INSERT INTO interfaces VALUES (?, ?, ?, ?)", interfaceId, name, direction,
				purgeDays) > 0;
	}
	
	public void testInsertMessage() {
		if (!haveTestInterface()) {
			return;
		}
		MapMessage msg = new ActiveMQMapMessage();
		try {
			msg.setString("messageContent", messageContent);
			msg.setString("interfaceId", interfaceId);
			msg.setString("fieldList", fieldList);
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
		svc.storeHL7Message(msg);
	}
	
	@Test
	public void testQuery() {
		String[] results = getMessagesWithKeyValue("MSH-10", "500 880");
		for (String message : results) {
			log.info("message: {");
			System.out.println(message);
			log.info("}");
		}
	}
	
	/**/
}
