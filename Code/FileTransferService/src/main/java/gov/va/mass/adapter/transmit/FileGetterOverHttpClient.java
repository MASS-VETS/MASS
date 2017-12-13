package gov.va.mass.adapter.transmit;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

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

import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.util.FileCopyUtils;

import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PropertySource("classpath:application.properties")
public class FileGetterOverHttpClient {

	@Value("${destination.url.get}")
	private String DESTINATION_URL_GET;

	@Value("${app.responses.file.storage}")
	private String RESPONSES_FILE_STORAGE_FOLDER;

	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;
	
	@Value("${jms.databaseQ}")
	private String databaseQueue;
	
	@Value("${interface.id}")
	private String interfaceId;
	
	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION;

	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD;

	@Value("${keystore.type}")
	private String KEYSTORE_TYPE;

	private TLSHttpClientProvider tlsHttpClientProvider;

	private static final Logger logger = LoggerFactory.getLogger(FileGetterOverHttpClient.class);

	private byte[] saveStreamFile(InputStream is, File file) {
		
		byte[] filebytes = null;
		
		try {
			filebytes = FileCopyUtils.copyToByteArray(file);
			FileOutputStream fos = new FileOutputStream(file);

			int inByte;
			while ((inByte = is.read()) != -1) {
				fos.write(inByte);
			}
			fos.close();
			
			logger.debug("File saved of size " + filebytes.length);

			// Send to the database
			if(databaseQueue != null && !databaseQueue.isEmpty()) {
				//Provided that this executed, log to the database that this happened.
				// Get current date time for later.
				String dateTime = String.format("%1$tF %1$tT", new Date());
				
				// Create the HashMap for MapMessage JMS queue.
				HashMap<String, Object> mmsg = new HashMap<String, Object>();
	
				// Build the MapMessage
				mmsg.put("messageContent", new String(filebytes));
				mmsg.put("fieldList", ""); //There are not fields to be stored for this interface.
				mmsg.put("interfaceId", interfaceId);
				mmsg.put("dateTime", dateTime);
	
				jmsMsgTemplate.convertAndSend(databaseQueue, mmsg);
				logger.info("Forwarded to queue = " + databaseQueue);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return filebytes;
	}

	@RequestMapping(value = "/adapter/audiocare/responses", method = RequestMethod.GET, produces = "text/csv")
	public @ResponseBody HttpEntity<byte[]> getAudiocareResponseToLastAppointmentFile() throws IOException {

		logger.debug("Connecting to Ensemble to obtain Audiocare responses for the last file");

		CloseableHttpClient httpClient = null;
		HttpResponse response = null;
		HttpEntity<byte[]> httpEntity = null;
		
		HttpGet httpGet = null;

		try {
			httpClient =  tlsHttpClientProvider.getTLSHttpClient(); //builder.build();
			httpGet = new HttpGet(DESTINATION_URL_GET);

			httpGet.setHeader(HttpHeaders.ACCEPT, "text/csv");
			response = httpClient.execute(httpGet);
			org.apache.http.HttpEntity entity = response.getEntity();

			int responseCode = response.getStatusLine().getStatusCode();

			System.out.println("Request Url: " + httpGet.getURI());
			System.out.println("Response Code: " + responseCode);


			LocalDateTime curDateTime = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
			String formatDateTime = curDateTime.format(formatter);
			String localStorePath = RESPONSES_FILE_STORAGE_FOLDER + "AudioCareResponses_" + formatDateTime + ".csv";
			logger.debug("Saving file " + localStorePath);
			File file = new File(localStorePath);

			InputStream is = entity.getContent();
			byte[] filebytes = saveStreamFile(is, file); 
			is.close();
						
			HttpHeaders headers = new HttpHeaders();
			headers.set("Content-Type", "application/octet_stream");
			headers.set("Content-Disposition", "attachment; filename=" + file.getName());
			headers.setContentLength(file.length());
			httpEntity = new HttpEntity<byte[]>(filebytes, headers);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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

}
