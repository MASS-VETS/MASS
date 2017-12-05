package gov.va.mass.adapter.transmit;

/*
 * Class uses httpClient class
 */

import java.io.File;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import org.apache.http.client.methods.HttpPost;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.PropertySource;


import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

// TODO : In PROD, disable the save file method

// TODO : Make hostname verification an environment specific parameter
@RestController
@PropertySource("classpath:application.properties")
public class FileSenderOverHttpClient {

	// Keystore properties here are used when connecting to ensemble

	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION;// = "C:/work/1twowayssl/adapterkeys/adapterks.jks"

	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD;

	@Value("${keystore.type}")
	private String KEYSTORE_TYPE;

	@Value("${destination.url.post}")
	private String DESTINATION_URL_POST;

	@Value("${app.appointments.file.storage}")
	private String APPOINTMENTS_FILE_STORAGE_FOLDER;

	private TLSHttpClientProvider setTLSHttpClientProvider;

	private static final Logger logger = LoggerFactory.getLogger(FileSenderOverHttpClient.class);

	@PostMapping("/adapter/audiocare/epicappointments") // , consumes="text/csv")
	public ResponseEntity<String> postAppointmentsFileToEnsembleHttpClientBased(
			@RequestParam("file") MultipartFile uploadfile) {

		logger.debug("Single file upload!");

		if (uploadfile.isEmpty()) {
			return new ResponseEntity("Please select a file.", HttpStatus.OK);
		}

		try {
			File savedfile = saveUploadedFiles(uploadfile);
			CloseableHttpClient httpClient = setTLSHttpClientProvider.getTLSHttpClient();
			finalPostFile(httpClient, savedfile);
			
//TODO: Pass on the multipart file without saving
			//finalPostFile(httpClient, uploadfile);
			
		} catch (IOException e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<String>("Successfully uploaded file to Adapter and from Adapter to Ensemble- "
				+ uploadfile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);

	}

	private File saveUploadedFiles(MultipartFile file) throws IOException {
		logger.debug("In saveUploadedFiles " + file.getOriginalFilename());

		// create file name and path for storage in adapter
		LocalDateTime curDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
		String formatDateTime = curDateTime.format(formatter);
		String localStorePath = APPOINTMENTS_FILE_STORAGE_FOLDER + "/AudioCareAppointments_" + formatDateTime + ".csv";
		
		
		byte[] bytes = file.getBytes();
		Path path = Paths.get(localStorePath);
		Files.write(path, bytes);
		logger.debug("Saving file to local " + file.getSize() + " " + path);

		File savedfile = new File(localStorePath); // TODO : Cleanup don't need another pointer savedfile.
		logger.debug("length of saved file " + savedfile.length());
		return savedfile; 

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


	public void setTLSHttpClientProvider(TLSHttpClientProvider tlsHttpClientProvider) {
		this.setTLSHttpClientProvider = tlsHttpClientProvider;
	}

}
