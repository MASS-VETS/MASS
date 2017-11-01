package gov.va.mass.adapter.transform.patientproviders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.jms.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;

@Component
@PropertySource("classpath:application.properties")
public class CsvToADTConverter {

	
	private static final Logger logger = LoggerFactory.getLogger(CsvToADTConverter.class);
	private String[] terserAddress = new String[20];

	private Message hapiMsg = null;

	private int providercount;

	private String lastlinepatientien = "uninitialized";
	private String curlinepatientien = null;
	private Terser terser = null;
	private int patientcount = 0;
	
	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;

	@Autowired
	private Queue queue;

	public void convert(String csvfilepath) {
		String separator = ",";
		String line = "";

		try (BufferedReader br = new BufferedReader(new FileReader(csvfilepath))) {

			while ((line = br.readLine()) != null) {
				logger.debug("line " + line);
				String[] elements = line.split(separator);
				createADTMessageFromElements(elements); // , terser);
			}
			if (patientcount > 0) { // send the last message
				sendMessageToReadyToSendToEpicQueue();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void initTerser() {
		HapiContext context = new DefaultHapiContext();
		Parser p = context.getGenericParser();

		String msg = "MSH|^~\\&|||||||ADT^A31||P|2.6"; // initialize

		try {
			hapiMsg = p.parse(msg);
		} catch (HL7Exception e) {
			logger.error("Unable to parse message.");
			e.printStackTrace();
		}

		terser = new Terser(hapiMsg);
		logger.debug("created terser");
	}

	private void createADTMessageFromElements(String[] elements) { // , Terser terser) {

		String epicAddr = null, value = null;

		int patientiencsvposition = 0;
		curlinepatientien = elements[patientiencsvposition];

		logger.debug("cur patient ien " + curlinepatientien + " last patient ien " + lastlinepatientien);
		if (!lastlinepatientien.equals(curlinepatientien)) { // if a new patient ien is being sent, then fire off the
																// old message
			sendMessageToReadyToSendToEpicQueue();
			lastlinepatientien = curlinepatientien;
			initTerser();
			++patientcount;
			providercount = -1; // ROL segment# 0 is the first row, hence starting the provider count by 1 less.
		}

		for (int i = 0; i < elements.length; ++i) {

			value = elements[i];
			try {
				if (value != null) {
					if (i < 5 && providercount == -1) { // Upto patient elements create PID only when it is a new
														// patient.
						epicAddr = terserAddress[i];
						terser.set(epicAddr, value);
						// logger.debug("Loading on terser " + epicAddr + " " + value + " provider count
						// " + providercount);
					}
					if (i == 5 && elements[i] != null) { // rest of the elements will be handled by the
															// addProvidersToTerser method
						++providercount;
						addProvidersToTerser(elements); // , terser);
					}
				}
			} catch (HL7Exception e) {
				logger.error("Could not create ADT message");
			}

		}
		logger.debug("last step");
	}

	private void sendMessageToReadyToSendToEpicQueue() {

		try {
			if (hapiMsg != null) {
				String msg = hapiMsg.encode();
				jmsMsgTemplate.convertAndSend(queue, msg);
				logger.debug ("Forwarding\n"+msg+" to queue = " + queue.getQueueName());
			}
		} catch (Exception e) {
			logger.error("Problem occured when accessing the queue. Name may be wrong " +
		               "or queue may not exist. Unable to send messages. Blocked at msg" +
					" with patient id " + lastlinepatientien +
					 e.getMessage());			
		}

	}

	// Add the role segment
	private void addProvidersToTerser(String[] elements) { // , Terser terser) {
		logger.debug("Adding provider # " + providercount);
		for (int i = 5; i < elements.length; ++i) {
			String epicAddr = terserAddress[i];
			String value = elements[i];
			epicAddr = epicAddr.replaceAll("[N]", Integer.toString(providercount));

			try {
				terser.set(epicAddr, value);
				// logger.debug("Set " + epicAddr + " " + value);
			} catch (HL7Exception e) {
				logger.error("Could not add provider to message");
				e.printStackTrace();
			}
		}
	}

	// incoming file structure
	// <patient local IEN>,<facility of IEN>,<patient name>,
	// <patient date of birth>,<patient sex>,
	// <provider role>,<provider ID>,<provider ID system>,<provider id
	// type>,<provider name>
	public void initMapping() {

		// terserAddress[csvColumnPosition] = terser address for EPIC formatted HL7 ADT
		// message
		terserAddress[0] = "/PID-3-1"; // patient ien 
		terserAddress[1] = "/PID-3-4"; // facility ien 
		terserAddress[2] = "/PID-5"; // patient name
		terserAddress[3] = "/PID-7"; // DOB and time
		terserAddress[4] = "/PID-8"; // sex
		terserAddress[5] = "/ROL(N)-3";// provider role, N indicates provider record number
		terserAddress[6] = "/ROL(N)-4-1"; // provider id
		terserAddress[7] = "/ROL(N)-4-9"; // provide id system
		terserAddress[8] = "/ROL(N)-4-13"; // provider id type
		terserAddress[9] = "/ROL(N)-4-2"; // provider name


	}


}

//TODO : file closing
//TODO : what HL7 version should be sent?
//TODO : add message id for logging in the send application
//TODO : handle lastname and firstname split
