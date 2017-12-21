package gov.va.mass.adapter.monitoring.stats;

import java.io.IOException;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * @author avolkano
 */
public class RestJsonHelper {
	
	public static JsonObject getObjectAtPath(CloseableHttpClient client, String baseUri, String path) {
		HttpGet get = new HttpGet(baseUri + path);
		try {
			HttpResponse response = client.execute(get);
			String respString = new BasicResponseHandler().handleResponse(response);
			JsonReader jsonReader = Json.createReader(new StringReader(respString));
			JsonObject resp = jsonReader.readObject();
			jsonReader.close();
			return resp;
		} catch (HttpClientErrorException | HttpServerErrorException | IOException e) {
			e.printStackTrace();
			JsonObjectBuilder builder = Json.createObjectBuilder();
			return builder.build(); // leave it empty. Caller should account for this by checking!
		}
	}
}
