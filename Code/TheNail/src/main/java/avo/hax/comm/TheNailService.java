package avo.hax.comm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import avo.hax.comm.config.EndpointConfig;
import avo.hax.comm.config.NailConfig;

@RestController
@PropertySource("classpath:application.yaml")
public class TheNailService {
	static final Logger log = LoggerFactory.getLogger(TheNailService.class);
	
	@Autowired
	NailConfig config;
	
	@RequestMapping("**")
	public ResponseEntity<String> receive(@RequestBody(required = false) String body, HttpServletRequest request) {
		String method = request.getMethod();
		String uri = request.getRequestURI();
		log.info(method + " " + uri);
		EndpointConfig endpoint = config.findConfig(method, uri);
		if (endpoint == null) {
			log.error("undefined: " + method + " " + uri);
			return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
		}
		return createResponse(endpoint, body);
	}
	
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYYMMddHHmmssZ");
	
	private ResponseEntity<String> createResponse(EndpointConfig config, String body) {
		String retVal;
		try {
			retVal = config.getResponseTemplate();
		} catch (IOException e) {
			log.error("Error reading response", e);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String stackTrace = sw.toString();
			return new ResponseEntity<>("Error reading response file: " + stackTrace, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if (body != null) {
			retVal = retVal.replace("{REQ}", body);
		}
		retVal = retVal.replace("{NOW}", dateFormatter.format(new Date()));
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", config.getContentType());
		ResponseEntity<String> resp = new ResponseEntity<String>(retVal, headers, HttpStatus.OK);
		return resp;
	}
}
