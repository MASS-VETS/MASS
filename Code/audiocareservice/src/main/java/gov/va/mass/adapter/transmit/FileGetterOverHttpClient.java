package gov.va.mass.adapter.transmit;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.util.FileCopyUtils;

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
	
	@Value("${keystore.location}")
	private String KEYSTORE_LOCATION;

	@Value("${keystore.password}")
	private String KEYSTORE_PASSWORD;

	@Value("${keystore.type}")
	private String KEYSTORE_TYPE;


	private TLSSpringTemplateProvider tlsTemplateProvider;

	private static final Logger logger = LoggerFactory.getLogger(FileGetterOverHttpClient.class);


	private void saveByteFile(byte[] file, String pathStr) {

		try {
			Path path = Paths.get(pathStr);
			Files.write(path, file);
			logger.debug("File saved of size " + file.length);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	@RequestMapping(value = "/adapter/audiocare/responses/apache", method = RequestMethod.GET , produces =  "text/csv")  
		public @ResponseBody HttpEntity<byte[]> getAudiocareResponseToLastAppointmentFile() throws IOException {

	
			
			logger.debug("Connecting to Ensemble to obtain Audiocare responses for the last file");

			CloseableHttpClient httpClient = null; 
			HttpResponse response = null;
			HttpEntity<byte[]> httpEntity = null;
			
			SSLContext sslContext = configureSSLContext();
			HttpClientBuilder builder = prepareHttpClientBuilder(sslContext);

		
	        try {
	        	 httpClient = builder.build();
	            HttpGet request = new HttpGet( DESTINATION_URL_GET ) ;

	            request.setHeader(HttpHeaders.ACCEPT, "text/csv");
	            response = httpClient.execute(request);
	            org.apache.http.HttpEntity entity = response.getEntity();
	 
	            int responseCode = response.getStatusLine().getStatusCode();
	 
	            System.out.println("Request Url: " + request.getURI());
	            System.out.println("Response Code: " + responseCode);
	 
	            InputStream is = entity.getContent();

	            LocalDateTime curDateTime = LocalDateTime.now();
	    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
	    		String formatDateTime = curDateTime.format(formatter);
	    		String localStorePath = RESPONSES_FILE_STORAGE_FOLDER + "/AudioCareResponses_" + formatDateTime + ".csv";
	            File file = new File(localStorePath) ;
	            FileOutputStream fos = new FileOutputStream(file );
	 
	            int inByte;
	            while ((inByte = is.read()) != -1) {
	                fos.write(inByte);
	            }
	 
	            is.close();
	            fos.close();

	            httpClient.close();
	            System.out.println("File Download Completed!!!");
	            
	            
				byte[] filebytes = FileCopyUtils.copyToByteArray(file);
				System.out.println(" Sending file to EPIC " + file.getAbsolutePath());
				
				HttpHeaders headers = new HttpHeaders();
				headers.set("Content-Type","application/octet_stream");
				headers.set("Content-Disposition", "attachment; filename=" + file.getName());	
				headers.setContentLength(file.length());
				httpEntity = new HttpEntity<byte[]>(filebytes, headers);
	            
	            
	    
	        } catch (ClientProtocolException e) {
	            e.printStackTrace();
	        } catch (UnsupportedOperationException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }	

			
			
			
			
			return httpEntity;
		}

	
	
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


}
