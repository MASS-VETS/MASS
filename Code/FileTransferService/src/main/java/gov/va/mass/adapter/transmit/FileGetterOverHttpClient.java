package gov.va.mass.adapter.transmit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import gov.va.mass.adapter.core.MicroserviceBase;
import gov.va.mass.adapter.core.MicroserviceException;

@RestController
@RequestMapping("/adapter/filetransferservice/getter")
@PropertySource("classpath:application.properties")
public class FileGetterOverHttpClient extends MicroserviceBase {
	
	@Value("${destination.url.get}")
	private String DESTINATION_URL_GET;
	
	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;
	
	@Value("${jms.databaseQ}")
	private String databaseQueue;
	
	@Value("${interface.id}")
	private String interfaceId;
	
	private TLSHttpClientProvider tlsHttpClientProvider;
	
	private static final Logger logger = LoggerFactory.getLogger(FileGetterOverHttpClient.class);
	
	private File saveStreamFile(InputStream is) throws IOException {
		
		File tempFile = null;
		
		tempFile = stream2file(is);
		byte[] filebytes = FileCopyUtils.copyToByteArray(tempFile);
		
		logger.debug("File saved of size " + filebytes.length);
		
		// Send to the database
		if (databaseQueue != null && !databaseQueue.isEmpty()) {
			// Provided that this executed, log to the database that this happened.
			// Get current date time for later.
			String dateTime = String.format("%1$tF %1$tT", new Date());
			
			// Create the HashMap for MapMessage JMS queue.
			HashMap<String, Object> mmsg = new HashMap<String, Object>();
			
			// Build the MapMessage
			mmsg.put("messageContent", new String(filebytes));
			mmsg.put("fieldList", ""); // There are not fields to be stored for this interface.
			mmsg.put("interfaceId", interfaceId);
			mmsg.put("dateTime", dateTime);
			
			jmsMsgTemplate.convertAndSend(databaseQueue, mmsg);
			logger.info("Forwarded to queue = " + databaseQueue);
		}
		
		return tempFile;
	}
	
	public static File stream2file(InputStream in) throws IOException {
		final File tempFile = File.createTempFile("stream2file", ".tmp");
		tempFile.deleteOnExit();
		try (FileOutputStream out = new FileOutputStream(tempFile)) {
			IOUtils.copy(in, out);
		}
		return tempFile;
	}
	
	@RequestMapping(value = "/get", method = RequestMethod.GET, produces = "text/csv")
	public @ResponseBody HttpEntity<byte[]> getAudiocareResponseToLastAppointmentFile()
			throws IOException, MicroserviceException {
		
		logger.debug("Connecting to Ensemble to obtain Audiocare responses for the last file");
		this.state.serviceCalled();
		
		CloseableHttpClient httpClient = null;
		HttpResponse response = null;
		HttpEntity<byte[]> httpEntity = null;
		
		HttpGet httpGet = null;
		
		try {
			httpClient = tlsHttpClientProvider.getTLSHttpClient(); // builder.build();
			httpGet = new HttpGet(DESTINATION_URL_GET);
			
			httpGet.setHeader(HttpHeaders.ACCEPT, "text/csv");
			response = httpClient.execute(httpGet);
			org.apache.http.HttpEntity entity = response.getEntity();
			
			int responseCode = response.getStatusLine().getStatusCode();
			
			System.out.println("Request Url: " + httpGet.getURI());
			System.out.println("Response Code: " + responseCode);
			
			InputStream is = entity.getContent();
			File file = saveStreamFile(is);
			is.close();
			byte[] filebytes = FileCopyUtils.copyToByteArray(file);
			
			HttpHeaders headers = new HttpHeaders();
			headers.set("Content-Type", "application/octet_stream");
			headers.set("Content-Disposition", "attachment; filename=" + file.getName());
			headers.setContentLength(file.length());
			httpEntity = new HttpEntity<byte[]>(filebytes, headers);
			this.state.serviceSucceeded();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			this.state.serviceFailed();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
			this.state.serviceFailed();
		} catch (IOException e) {
			e.printStackTrace();
			this.state.serviceFailed();
		} finally {
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
		}
		
		return httpEntity;
	}
	
	public void setTLSHttpClientProvider(TLSHttpClientProvider tlsHttpClientProvider) {
		this.tlsHttpClientProvider = tlsHttpClientProvider;
	}
	
	@Override
	protected String serviceName() {
		return "FileGetterService";
	}
	
}
