package gov.va.mass.adapter.transforms;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.listener.adapter.JmsResponse;
import org.springframework.stereotype.Component;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.validation.impl.NoValidation;
import gov.va.mass.adapter.core.JmsMicroserviceBase;
import gov.va.mass.adapter.core.MicroserviceException;

/**
 * VA Adapter Message Transform Service
 * 
 * @author avolkano
 * @author daves
 */
@Component
@PropertySource("classpath:application.properties")
public class TransformService extends JmsMicroserviceBase {
	static final Logger LOG = LoggerFactory.getLogger(TransformService.class);
	
	@Value("${xslt.name}")
	String xsltName;
	
	@Value("${jms.outputQ}")
	String outputQueue;
	
	@JmsListener(destination = "${jms.inputQ}")
	public JmsResponse<String> transformPipeMessage(String pipeMessage) throws MicroserviceException {
		LOG.debug("Received message from amq");
		logMessage(pipeMessage);
		
		this.state.serviceCalled();
		
		// Set up HAPI context
		HapiContext context = new DefaultHapiContext();
		context.setValidationContext(new NoValidation());
		
		try {
			// Make HAPI message
			Parser hl7Parser = context.getGenericParser();
			Message hapiMessage = hl7Parser.parse(pipeMessage);
			
			// Get the actual transform
			Source xsltSource = xsltSource(xsltName);
			if (xsltSource == null) {
				LOG.error("No transform found '{}'", xsltName);
				throw this.enterErrorState("No transform found '" + xsltName + "'");
			}
			
			// Turn the message into XML...
			Parser xmlParser = context.getXMLParser();
			String xmlMessage = xmlParser.encode(hapiMessage);
			LOG.debug("Message converted to XML");
			
			// ...transform the XML...
			String transMessage = transformXmlMessage(xmlMessage, xsltSource);
			LOG.debug("Message transformed using '{}'", xsltName);
			
			// ...And turn it back into pipe-based to return it.
			hapiMessage = xmlParser.parse(transMessage);
			String transPipeMessage = hl7Parser.encode(hapiMessage);
			
			LOG.debug("Message transformation complete");
			logMessage(transPipeMessage);
			
			return JmsResponse.forQueue(transPipeMessage, outputQueue);
		} catch (HL7Exception e) {
			LOG.error("HL7 Exception in TransformService.", e);
			
			throw this.enterErrorState(e.getMessage());
		} finally {
			try {
				context.close();
			} catch (IOException e) {
				LOG.error("Could not close HapiContext.", e);
			}
		}
	}
	
	private static void logMessage(String message) {
		for (String s : message.split("(\r\n|\r|\n)")) {
			LOG.debug(s);
		}
	}
	
	private static Source xsltSource(String transformName) {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			InputStream input = classLoader.getResourceAsStream(transformName + ".xslt");
			if (input == null || input.available() <= 0) {
				return null;
			}
			return new StreamSource(input);
		} catch (IOException e) {
			return null;
		}
	}
	
	private String transformXmlMessage(String xmlMessage, Source xsltSource) {
		StringReader reader = null;
		StringWriter writer = null;
		try {
			// read the message
			reader = new StringReader(xmlMessage);
			Source xml = new StreamSource(reader);
			
			// prepare output
			writer = new StringWriter();
			
			// create the transformer
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(xsltSource);
			
			// transform the message
			StreamResult result = new StreamResult(writer);
			transformer.transform(xml, result);
			
			// return the result
			return writer.toString();
		} catch (TransformerException e) {
			LOG.error("Exception in XSLT process.", e);
			return "";
		} finally {
			// clean up after ourselves
			if (reader != null) {
				reader.close();
			}
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					LOG.error("Could not close XML writer.", e);
				}
			}
		}
	}
	
	@Override
	protected String serviceName() {
		return "TransformService";
	}
}
