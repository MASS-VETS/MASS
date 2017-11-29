package gov.va.mass.adapter.transmit;


import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.ByteArrayHttpMessageConverter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
//@Component // TODO - take out
@PropertySource("classpath:application.properties")
public class AudioResponseFileGetter {

	@Value("${destination.url.get}")
	private String DESTINATION_URL_GET;

	@Value("${app.responses.file.storage}")
	private String RESPONSES_FILE_STORAGE_FOLDER;

	private TLSSpringTemplateProvider tlsTemplateProvider;

	private static final Logger logger = LoggerFactory.getLogger(AudioResponseFileGetter.class);

	private ResponseEntity<byte[]> finalExecGetAudioResponsesFile(RestTemplate restTemplate) {

		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		HttpEntity<Object> reqEntity = new HttpEntity<Object>(headers);

		ResponseEntity<byte[]> response = restTemplate.exchange(DESTINATION_URL_GET, HttpMethod.GET, reqEntity,
				byte[].class);

		return response;
	}

	private void saveByteFile(byte[] file, String pathStr) {

		try {
			Path path = Paths.get(pathStr);
			Files.write(path, file);
			logger.debug("File saved of size " + file.length);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// TODO: Add mm-dd-yyyy & TS to make file name unique or retain the filename given originally
	
	@GetMapping("/adapter/audiocare/responses")
	public ResponseEntity<byte[]> getAudiocareResponseFileFromEnsemble() {
		logger.debug("Connecting to Ensemble to obtain Audiocare responses for the last file");

		ResponseEntity<byte[]> httpresp = null;
		RestTemplate restTemplate = tlsTemplateProvider.getTLSSpringTemplate(); // new RestTemplate(requestFactory);

		httpresp = finalExecGetAudioResponsesFile(restTemplate);

		if (httpresp.getStatusCode().equals(HttpStatus.OK)) {
			LocalDateTime curDateTime = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
			String formatDateTime = curDateTime.format(formatter);
			saveByteFile(httpresp.getBody(), RESPONSES_FILE_STORAGE_FOLDER   + "/AudioCareResponses_"+formatDateTime+".csv"); 
		}
		return httpresp;
	}

	public void setTemplateProvider(TLSSpringTemplateProvider tlsTemplateProvider) {
		this.tlsTemplateProvider = tlsTemplateProvider;
	}

}
