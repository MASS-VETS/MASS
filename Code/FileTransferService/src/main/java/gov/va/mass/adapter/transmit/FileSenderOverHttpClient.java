package gov.va.mass.adapter.transmit;

/*
 * Class uses httpClient class
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import gov.va.mass.adapter.core.MicroserviceBase;

// TODO : Make hostname verification an environment specific parameter
@RestController
@RequestMapping("/adapter/filetransferservice/sender")
@PropertySource("classpath:application.properties")
public class FileSenderOverHttpClient extends MicroserviceBase {
	
	// Keystore properties here are used when connecting to ensemble
	
	@Value("${keystore.enabled}")
	private boolean TLS_ENABLED = false;
	
	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION;// = "C:/work/1twowayssl/adapterkeys/adapterks.jks"
	
	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD;
	
	@Value("${keystore.type}")
	private String KEYSTORE_TYPE;
	
	@Value("${destination.url.post}")
	private String DESTINATION_URL_POST;
	
	@Value("${interface.id}")
	private String interfaceId;
	
	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;
	
	@Value("${jms.databaseQ}")
	private String databaseQueue;
	
	private static final Logger logger = LoggerFactory.getLogger(FileSenderOverHttpClient.class);
	
	private TLSHttpClientProvider setTLSHttpClientProvider;
	
	@PostMapping("/send") // , consumes="text/csv")
	public ResponseEntity<String> postAppointmentsFileToEnsembleHttpClientBased(
			@RequestParam("file") MultipartFile uploadfile) {
		
		logger.debug("Single file upload!");
		this.state.serviceCalled();
		
		if (uploadfile.isEmpty()) {
			return new ResponseEntity<String>("Please select a file.", HttpStatus.OK);
		}
		
		try {
			File savedfile = saveUploadedFiles(uploadfile);
			prepareAndPost(savedfile);
		} catch (IOException e) {
			this.state.serviceFailed();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		this.state.serviceSucceeded();
		return new ResponseEntity<String>("Successfully uploaded file to Adapter and from Adapter to Ensemble- "
				+ uploadfile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);
		
	}
	
	private File saveUploadedFiles(MultipartFile file) throws IOException {
		logger.debug("In saveUploadedFiles " + file.getOriginalFilename());
		
		byte[] bytes = file.getBytes();
		
		// Provided that this executed log to the database that this happened.
		// Get current date time for later.
		String dateTime = String.format("%1$tF %1$tT", new Date());
		
		// Create the HashMap for MapMessage JMS queue.
		HashMap<String, Object> mmsg = new HashMap<String, Object>();
		
		// Build the MapMessage
		mmsg.put("messageContent", new String(bytes));
		mmsg.put("fieldList", ""); // There are not fields to be stored for this interface.
		mmsg.put("interfaceId", interfaceId);
		mmsg.put("dateTime", dateTime);
		
		// Send to the database
		if (databaseQueue != null && !databaseQueue.isEmpty()) {
			jmsMsgTemplate.convertAndSend(databaseQueue, mmsg);
			logger.info("Forwarded to queue = " + databaseQueue);
		}
		
		File tempFile = stream2file(file.getInputStream());
		logger.debug("length of saved file " + tempFile.length());
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
	
	private void finalPostFile(CloseableHttpClient httpClient, File savedfile) {
		
		HttpPost httpPost = new HttpPost(DESTINATION_URL_POST);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		
		builder.addBinaryBody("file", savedfile, ContentType.create("text/csv"), savedfile.getName());
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE); // change mode
		
		HttpEntity entity = builder.build();
		
		httpPost.setEntity(entity);
		try {
			logger.debug("Posting");
			HttpResponse response = httpClient.execute(httpPost);
			logger.debug("Posted file of the type text/csv");
			logger.debug("Response " + response.toString());
		} catch (IOException e) {
			logger.error(" Could not execute post method on httpclient " + e.toString());
		} finally {
			httpPost.releaseConnection();
		}
	}
	
	private void prepareAndPost(File savedfile) {
		CloseableHttpClient httpClient = setTLSHttpClientProvider.getTLSHttpClient();
		finalPostFile(httpClient, savedfile);
	}
	
	public void setTLSHttpClientProvider(TLSHttpClientProvider tlsHttpClientProvider) {
		this.setTLSHttpClientProvider = tlsHttpClientProvider;
	}
	
	@Override
	protected String serviceName() {
		return "FileGetterService";
	}
	
}
