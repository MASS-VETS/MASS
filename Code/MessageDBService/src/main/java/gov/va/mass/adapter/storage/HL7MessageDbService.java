package gov.va.mass.adapter.storage;

import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import gov.va.mass.adapter.core.JmsMicroserviceBase;
import gov.va.mass.adapter.core.MicroserviceException;

/**
 * VA Adapter Message Database Service
 * 
 * @author avolkano
 */
@Component
@PropertySource("classpath:application.properties")
public class HL7MessageDbService extends JmsMicroserviceBase {
	static final Logger log = LoggerFactory.getLogger(HL7MessageDbService.class);
	
	@Value("${database.pingonstartup}")
	Boolean pingOnStartup;
	
	@Autowired
	ApplicationContext context;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@PostConstruct
	public void checkConnection() throws SQLException {
		
		// Only check to make sure that the database is running when in production.
		if (!pingOnStartup) {
			return;
		}
		log.info("checking connection to database...");
		
		// Initialize the connection state then check to see if the connection is valid
		// with 30 second timeout.
		boolean connectionValid = false;
		try {
			connectionValid = jdbcTemplate.getDataSource().getConnection().isValid(30);
			if (connectionValid) {
				log.info("Connection good!");
				return;
			}
		} catch (SQLException e) {
			log.info("Could not connect to the database.");
			throw e;
		}
		
		// This should be unreachable in theory as both valid connections and the broken
		// ones will have been caught in the above try/catch.
		throw new SQLException("Connection to the database is no longer valid.");
	}
	
	@JmsListener(destination = "${jms.inputQ}")
	public void storeHL7Message(MapMessage msg) throws MicroserviceException {
		this.state.serviceCalled();
		// Initialize the parameters for the query.
		String messageContent = "";
		String interfaceId = "";
		String fieldList = "";
		String dateTime = "";
		
		// Attempt to get the values from the queue message.
		try {
			messageContent = msg.getString("messageContent");
			interfaceId = msg.getString("interfaceId");
			fieldList = msg.getString("fieldList");
			dateTime = msg.getString("dateTime");
		} catch (JMSException e1) {
			log.info("Message received does not contain appropriate mapping for interface or message content.");
			this.state.serviceFailed();
			return;
		}
		
		// Create the query & New parameters objects.
		SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("storeHAPIMessage");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		
		// Log and add the parameters to the parameter list object.
		log.info("interfaceId=" + interfaceId);
		log.info("messageContent=" + messageContent);
		log.info("fieldList=" + fieldList);
		log.info("dateTime=" + dateTime);
		
		addParam(parameters, "interface", interfaceId);
		addParam(parameters, "messageContent", messageContent);
		addParam(parameters, "fieldList", fieldList);
		addParam(parameters, "dateTime", dateTime);
		
		// Attempt to execute the query to store the data.
		try {
			call.execute(parameters);
			log.info("Message stored");
			this.state.serviceSucceeded();
		} catch (DataAccessException e) {
			this.state.serviceFailed();
			throw this.enterErrorState("Data access exception shutting down the service.");
		}
	}
	
	// Handle the addition of null parameters to the parameter list object.
	private void addParam(MapSqlParameterSource parameters, String paramName, String value) {
		if (value == null || value.length() == 0) {
			parameters.addValue(paramName, null, java.sql.Types.NULL); // SQL throws a fit if it's not at least there
		} else {
			parameters.addValue(paramName, value);
		}
	}
	
	@Override
	protected String serviceName() {
		return "MessageDBService";
	}
}
