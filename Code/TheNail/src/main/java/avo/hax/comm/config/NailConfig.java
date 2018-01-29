package avo.hax.comm.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * @author avolkano
 */
@ConfigurationProperties(prefix = "nail")
@Component
public class NailConfig {
	static final Logger log = LoggerFactory.getLogger(NailConfig.class);
	
	@NestedConfigurationProperty
	private List<EndpointConfig> responses = new ArrayList<EndpointConfig>();
	
	public List<EndpointConfig> getResponses() {
		return this.responses;
	}
	
	public void setResponses(List<EndpointConfig> responses) {
		this.responses = responses;
	}
	
	public HashMap<String, Integer> dictionary = null;
	
	@PostConstruct
	private void postConstruct() {
		this.dictionary = createEndpointIndex();
	}
	
	private HashMap<String, Integer> createEndpointIndex() {
		log.info("indexing configuration...");
		HashMap<String, Integer> dict = new HashMap<String, Integer>();
		for (int i = 0; i < this.responses.size(); i++) {
			EndpointConfig c = this.responses.get(i);
			log.info("mapping " + c.toString());
			dict.put(c.getMethod() + "." + c.getUrl(), i);
		}
		return dict;
	}
	
	public EndpointConfig findConfig(String method, String urlParameter) {
		if (this.dictionary == null) {
			this.dictionary = createEndpointIndex();
		}
		if (this.dictionary.containsKey(method + "." + urlParameter)) {
			return this.getResponses().get(this.dictionary.get(method + "." + urlParameter));
		}
		return null;
	}
}
