package gov.va.mass.adapter.transmit;

/*
 * Class uses httpClient class
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.xml.ws.Response;

import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.PropertySource;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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


// TODO : Make hostname verification an environment specific parameter
@RestController
@PropertySource("classpath:application.properties")
public class AppointmentsFileSender {

	// Keystore properties here are used when connecting to ensemble

	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION;// = "C:/work/1twowayssl/adapterkeys/adapterks.jks"

	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD;

	@Value("${keystore.type}")
	private String KEYSTORE_TYPE;

	@Value("${destination.url.post}")
	private String DESTINATION_URL_POST;
	
	@Value("${interface.interfaceId}")
	private String interfaceId;

	@Value("${app.appointments.file.storage}")
	private String APPOINTMENTS_FILE_STORAGE_FOLDER;

	@Autowired
	private JmsMessagingTemplate jmsMsgTemplate;
	
	@Value("${jms.databaseQ}")
	private String databaseQueue;
	
	private static final Logger logger = LoggerFactory.getLogger(AppointmentsFileSender.class);

	private TLSSpringTemplateProvider tlsTemplateProvider;

	@PostMapping("/adapter/audiocare/epicappointments")
	public ResponseEntity<String> postAppointmentsFileToEnsembleHttpClientBased(
			@RequestParam("file") MultipartFile uploadfile) {

		logger.debug("Single file upload!");

		if (uploadfile.isEmpty()) {
			return new ResponseEntity("please select a file!", HttpStatus.OK);
		}

		try {
			File savedfile = saveUploadedFiles(uploadfile);
			prepareAndPost(savedfile);
			// sendApointmentFileToEnsemble(uploadfile);
		} catch (IOException e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<String>("Successfully uploaded file to Adapter and from Adapter to Ensemble- "
				+ uploadfile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);

	}

	private File saveUploadedFiles(MultipartFile file) throws IOException {
		logger.debug("In saveUploadedFiles " + file.getOriginalFilename());

		LocalDateTime curDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
		String formatDateTime = curDateTime.format(formatter);
		String localStorePath = APPOINTMENTS_FILE_STORAGE_FOLDER + "/AudioCareAppointments_" + formatDateTime + ".csv";// +
																														// file.getOriginalFilename();

		byte[] bytes = file.getBytes();
		Path path = Paths.get(localStorePath);
		Files.write(path, bytes);
		
		//Provided that this executed log to the database that this happened.
		// Get current date time for later.
		String dateTime = String.format("%1$tF %1$tT", new Date());

		// Create the HashMap for MapMessage JMS queue.
		HashMap<String, Object> mmsg = new HashMap<String, Object>();

		// Build the MapMessage
		mmsg.put("messageContent", new String(bytes));
		mmsg.put("fieldList", ""); //There are not fields to be stored for this interface.
		mmsg.put("interfaceId", interfaceId);
		mmsg.put("dateTime", dateTime);

		// Send to the database
		jmsMsgTemplate.convertAndSend(databaseQueue, mmsg);
		logger.info("Forwarded to queue = " + databaseQueue);
		
		logger.debug("Saving file to local " + file.getSize() + " " + path);

		File savedfile = new File(localStorePath); // TODO : Cleanup don't need another pointer savedfile.
		logger.debug("length of saved file " + savedfile.length());
		return savedfile; // TODO: return the inmemory file object instead of hte savedfile

	}

	// Create an SSL context with our private key store.
	// We are only loading the key-material here, but if your server uses a
	// self-signed certificate,
	// you will need to load the trust-material (a JKS key-store containing the
	// server's public SSL
	// certificate) as well.

	private SSLContext configureSSLContext() {
		KeyStore keyStore = null;

		try {
			keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
			InputStream keyStoreInput = new FileInputStream(KEYSTORE_LOCATION);
			keyStore.load(keyStoreInput, KEYSTORE_PASSWORD.toCharArray());
			logger.debug("Key store has " + keyStore.size() + " keys");
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error("Problem with keystore " + e.toString());
		}

		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, KEYSTORE_PASSWORD.toCharArray())
					.loadTrustMaterial(new TrustSelfSignedStrategy()).setProtocol("TLSv1") // TODO: TLS version needs to
																							// be uniform
					.build();
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
			logger.error("Could not create SSLContext " + e.toString());
		}

		return sslContext;
	}

	// = new SSLConnectionSocketFactory(sslContext,
	// SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

	// TODO: sslconnectionfactory is deprecated
	private HttpClientBuilder prepareHttpClientBuilder(SSLContext sslContext) {
		// Prepare the HTTPClient.
		HttpClientBuilder builder = HttpClientBuilder.create();

		SSLConnectionSocketFactory sslConnectionFactory = null;
		String env = System.getenv("ENV") ; 
		logger.debug("ssl conn fact. creation " + env);
		if (env.equals("prod") || env.equals("preprod") || env.equals("accept") ) {
			sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
					BrowserCompatHostnameVerifier.INSTANCE); 
		} else {
			sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
					NoopHostnameVerifier.INSTANCE);
		}

		builder.setSSLSocketFactory(sslConnectionFactory);

		// TODO : Need to disable the http socket factory? Or is this used after ssl is stripped.
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslConnectionFactory) // .register("http", new PlainConnectionSocketFactory())
				.build();
		HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
		builder.setConnectionManager(ccm);
		return builder;
	}


	private void finalPostFile(CloseableHttpClient httpClient, File savedfile) {

		HttpPost post = new HttpPost(DESTINATION_URL_POST);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addBinaryBody("file", savedfile, ContentType.create("text/csv"), savedfile.getName());
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE); // change mode

		HttpEntity entity = builder.build();

		post.setEntity(entity);
		try {
			logger.debug("posting");
			HttpResponse response = httpClient.execute(post);
			
			logger.debug(response.toString());
		} catch (IOException e) {
			logger.error(" Could not execute post method on httpclient " + e.toString());
		}
	}

	private void prepareAndPost(File savedfile) {

		SSLContext sslContext = configureSSLContext();
		HttpClientBuilder builder = prepareHttpClientBuilder(sslContext);

		//TODO: test it with using the shared class. 
//		CloseableHttpClient httpClient = tlsTemplateProvider.getTLSHttpClient();
//		finalPostFile(httpClient, savedfile);
		
		try (CloseableHttpClient httpClient = builder.build()) {
			finalPostFile(httpClient, savedfile);
		} catch (IOException e) {
			logger.error("Unable to create httpclient " + e.toString());
		}
	}

	public void setTemplateProvider(TLSSpringTemplateProvider tlsTemplateProvider) {
		this.tlsTemplateProvider = tlsTemplateProvider;
	}

}
