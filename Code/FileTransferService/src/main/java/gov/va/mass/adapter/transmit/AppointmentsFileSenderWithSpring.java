package gov.va.mass.adapter.transmit;

/*
 * Class uses httpClient class
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
/*

import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
*/
import org.springframework.web.multipart.MultipartFile;

// TODO : test with curl : $ curl -F file=@"f:\\data.txt" http://localhost:8080/api/upload/

//9.3 cURL to upload a large file again.
//
//Terminal
//$ curl -F file=@"F://movies//300//Sample.mkv"  http://localhost:8080/api/upload/
//
//{"errCode":"0x000123","errDesc":"Attachment size exceeds the allowable limit! (10MB)"}

// exclude tomcat server, include jetty
// configure jetty tls invocation of ensemble
// mock ensemble

// TODO : Make hostname verification an environment specific parameter

//@RestController
// @Component
@PropertySource("classpath:application.properties")
public class AppointmentsFileSenderWithSpring {

	@Value("${destination.url.post}")
	private String DESTINATION_URL_POST;

	private static final Logger logger = LoggerFactory.getLogger(AppointmentsFileSenderWithSpring.class);

	@Value("${app.appointments.folder}")
	private String APPOINTMENTS_FOLDER; // = "C:\\work\\sprint7\\temp\\"; // TODO: Provide key_alias

	private TLSSpringTemplateProvider tlsTemplateProvider;
	//
	// @PostMapping("/adapter/audiocare/epicappointmentsnotworking")
	// public ResponseEntity<String>
	// postAppointmentsFileToEnsembleHttpClientBased(@RequestParam("file")
	// MultipartFile uploadedfile) {
	//
	// logger.debug("Received a post appointments request");
	//
	// if (uploadedfile.isEmpty()) {
	// return new ResponseEntity("File is empty", HttpStatus.OK);
	// }
	//
	// try {
	// File savedfile = saveUploadedFiles(uploadedfile);
	// sendApointmentFileToEnsemble(uploadedfile);
	// } catch (IOException e) {
	// return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	// }
	//
	// return new ResponseEntity<String>("Successfully uploaded file to Adapter and
	// from Adapter to Ensemble- "
	// + uploadedfile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);
	//
	// }

	// @PostMapping("/adapter/audiocare/epicappointments")
	// public ResponseEntity<String>
	// postAppointmentsFileToEnsembleHttpClientBased(@RequestParam("file")
	// MultipartFile file) {
	//
	// logger.debug("Received a post appointments request");
	//
	// LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
	// map.add("file", file ) ; // tempFile ); // new FileSystemResource(tempFile));
	// HttpHeaders headers = new HttpHeaders();
	// headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
	// HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new
	// HttpEntity<>(map, headers);
	//
	// String document = null;
	//
	// RestTemplate restTemplate = tlsTemplateProvider.getTLSSpringTemplate();
	// try {
	// ResponseEntity<String> responseEntity =
	// restTemplate.exchange(DESTINATION_URL_POST, HttpMethod.POST, requestEntity,
	// String.class);
	// document = responseEntity.getBody();
	// logger.debug("document " + document);
	// } catch (Exception e) {
	// e.getMessage();
	// }
	//
	// ResponseEntity<String> responseEntity = new ResponseEntity<>(document,
	// HttpStatus.OK);
	//
	// return responseEntity;
	//
	// }

	// Seems to be working, but receiver is unable to process the file

	@PostMapping("/adapter/audiocare/epicappointments")
	public ResponseEntity<String> postAppointmentsFileToEnsembleHttpClientBased(
			@RequestParam("file") MultipartFile file) {

		logger.debug("Received a post appointments request");

		byte[] fileBytes = null;
		try {
			fileBytes = file.getBytes();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
//		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
//
//		map.add("file", file); 
		
		// Main request
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA); // how to set for multipart mi
		List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
		acceptableMediaTypes.add(MediaType.TEXT_PLAIN);
		headers.setAccept(acceptableMediaTypes );
		
		MultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
		
		// creating an HttpEntity for the binary part
		HttpHeaders fileHeader = new HttpHeaders();
		fileHeader.setContentType(MediaType.APPLICATION_OCTET_STREAM ) ; //"application/octet_stream");
		fileHeader.setContentLength(file.getSize());
		fileHeader.set("Content-Disposition", "attachment; filename=" + file.getName());
		HttpEntity<byte[]> picturePart = new HttpEntity<>(fileBytes, fileHeader);
		 
		
		multipartRequest.add("file", picturePart);
		
		logger.debug("1");
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartRequest, headers);

		String document = null;
		logger.debug("2");
		RestTemplate restTemplate = tlsTemplateProvider.getTLSSpringTemplate();
		
		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
		logger.debug("40");
		try {

			ResponseEntity<String> responseEntity = restTemplate.exchange(DESTINATION_URL_POST, HttpMethod.POST,
					requestEntity, String.class);
			logger.debug("50");
			document = responseEntity.getBody();
		} catch (Exception e) {
			logger.error(e.getMessage() + e.toString());
			e.printStackTrace();
		}
		// String resp
		ResponseEntity<String> responseEntity = new ResponseEntity<>(document, HttpStatus.OK);

		return responseEntity;

	}

	// @PostMapping("/adapter/audiocare/epicappointments")
	// public ResponseEntity<String>
	// postAppointmentsFileToEnsembleHttpClientBased(@RequestParam("file")
	// MultipartFile file) {
	//
	// MultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
	//
	// // creating an HttpEntity for the JSON part
	// HttpHeaders jsonHeader = new HttpHeaders();
	// jsonHeader.setContentType(MediaType.APPLICATION_JSON);
	// HttpEntity<JsonObject> jsonHttpEntity = new HttpEntity<>(jsonObject,
	// jsonHeader);
	//
	// // creating an HttpEntity for the binary part
	// HttpHeaders pictureHeader = new HttpHeaders();
	// pictureHeader.setContentType(MediaType.IMAGE_PNG);
	// HttpEntity<ByteArrayResource> picturePart = new HttpEntity<>(pngPicture,
	// pictureHeader);
	//
	// // putting the two parts in one request
	// multipartRequest.add("myAwesomeJsonData", jsonPart);
	// multipartRequest.add("file", picturePart);
	//
	// HttpEntity<MultiValueMap<String, Object>> requestEntity = new
	// HttpEntity<>(multipartRequest, header);
	// ResultObject result = restTemplate.postForObject(UPLOAD_URL, requestEntity,
	// ResultObject.class);
	//
	//
	// }

	// TODO: Change parameter to regular File type or change message converter

	private ResponseEntity<String> sendApointmentFileToEnsemble(MultipartFile uploadedfile) {

		ResponseEntity<String> httpresp = null;

		RestTemplate restTemplate = tlsTemplateProvider.getTLSSpringTemplate();
		// restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
		// httpresp =
		finalpostTest2(restTemplate, uploadedfile);
		// finalExecPostAppointmentsFile(restTemplate, uploadedfile);

		return httpresp;
	}

	private ResponseEntity<String> finalpostTest2(RestTemplate restTemplate, MultipartFile savedfile) {

		ArrayList<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>(Arrays.asList( // new
																												// MappingJackson2HttpMessageConverter(),
				new ResourceHttpMessageConverter()));

		// RestTemplate restTemplate = new RestTemplate(converters);
		// restTemplate.getMessageConverters().add(new
		// MappingJackson2HttpMessageConverter());
		// restTemplate.getMessageConverters().add(new ResourceHttpMessageConverter ());

		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", "text/plain");
		// headers.setAccept(acceptableMediaTypes);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("file", savedfile);

		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
				map, headers);
		ResponseEntity<String> result = restTemplate.exchange(DESTINATION_URL_POST, HttpMethod.POST, requestEntity,
				String.class);

		return result;
	}

	public void setTemplateProvider(TLSSpringTemplateProvider tlsTemplateProvider) {
		this.tlsTemplateProvider = tlsTemplateProvider;
	}

	private File saveUploadedFiles(MultipartFile file) throws IOException {
		logger.debug("In saveUploadedFiles " + file.getOriginalFilename());

		String filePathInAdapter = APPOINTMENTS_FOLDER + "/" + file.getOriginalFilename();
		// Get the file and save it somewhere
		byte[] bytes = file.getBytes();
		Path path = Paths.get(filePathInAdapter);
		Files.write(path, bytes);
		logger.debug("Saving file to local " + file.getSize() + " " + path);
		File savedfile = new File(filePathInAdapter); // TODO : Cleanup don't need another pointer savedfile.
		logger.debug("length of saved file " + savedfile.length());
		return savedfile;

	}

}
