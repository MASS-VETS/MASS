package gov.va.mass.adapter.storage;

import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.MapMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * VA Adapter Message Database Service
 * 
 * @author avolkano
 */
@Component
@PropertySource("classpath:application.properties")
public class HL7MessageDbService {
	static final Logger log = LoggerFactory.getLogger(HL7MessageDbService.class);
	
	@Value("${database.pingonstartup}")
	Boolean pingOnStartup;
	
	@Autowired
	ApplicationContext context;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	// TODO: heartbeat
	
	// check the database connection at startup and fail.
	// TODO: use this with the heartbeat endpoint.
	@PostConstruct
	public void checkConnection() throws SQLException {
		if(!pingOnStartup) {
			return;
		}
		log.info("checking connection to database...");
		boolean connectionValid = false;
		try {
			connectionValid = jdbcTemplate.getDataSource().getConnection().isValid(30);
			if (connectionValid) {
				log.info("Connection good!");
				return;
			}
		}
		catch (SQLException e) {
			log.info("Could not connect to the database.");
			throw e;
		}
		
		// if we get here, the connection has become borked.
		throw new SQLException("Connection to the database is no longer valid.");
	}
	
	@JmsListener(destination = "${jms.inputQ}")
	public void storeHL7Message(MapMessage msg) {
		
		String messageContent = ""; 
		String interfaceId = "";
		String fieldList = "";
		
		try {
			messageContent = msg.getString("messageContent");
			interfaceId = msg.getString("interfaceId");
			fieldList = msg.getString("fieldList");
			
		} catch (JMSException e1) {
			log.info("Message received does not contain appropriate mapping for interface or message content.");
			return;
		}
		
		SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("storeHAPIMessage");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		
		log.info("interfaceId=" + interfaceId);
		log.info("messageContent=" + messageContent);
		log.info("fieldList=" + fieldList);
		
		addParam(parameters, "interface", interfaceId);
		addParam(parameters, "messageContent", messageContent);
		addParam(parameters, "fieldList", fieldList);
		
		try {
		call.execute(parameters);
		
		log.info("Message stored");
		}
		catch (DataAccessException e) {
			log.info("Data access exception shutting down the service.");
			
			SpringApplication.exit(context); //Do the shutdown if the server cannot get a connection.
			
			throw e;
		}
	}
	
	private void addParam(MapSqlParameterSource parameters, String paramName, String value) {
		if (value == null || value.length() == 0) {
			parameters.addValue(paramName, null, java.sql.Types.NULL); // SQL throws a fit if it's not at least there
		}
		else {
			parameters.addValue(paramName, value);
		}
	}
}
