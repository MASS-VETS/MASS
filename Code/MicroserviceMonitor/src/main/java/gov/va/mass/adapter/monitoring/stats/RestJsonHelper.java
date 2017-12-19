package gov.va.mass.adapter.monitoring.stats;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class RestJsonHelper {
	public static JsonObject getObjectAtPath(RestTemplate restTemplate, String baseUri, String path)
			throws URISyntaxException {
		URI uri = new URI(baseUri + path);
		String responseString = "";
		try {
			responseString = restTemplate.getForObject(uri, String.class);
		} catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			return builder.build(); // leave it empty. Caller should account for this by checking!
		}
		JsonReader jsonReader = Json.createReader(new StringReader(responseString));
		JsonObject resp = jsonReader.readObject();
		jsonReader.close();
		return resp;
	}
}
