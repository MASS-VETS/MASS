package avo.hax.comm;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.yaml")
public class MessageCache {
	
	@Value("${send.source}")
	String messageSource;
	
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYYMMddHHmmssZ");
	
	private String messageContent;
	
	private boolean initialized = false;
	
	public String GetMessage(int msgNum) {
		if (!initialized) {
			return "";
		}
		
		return messageContent.replace("{NOW}", dateFormatter.format(new Date())).replace("{MSGNUM}", Integer.toString(msgNum)); 
	}
	
	public boolean IsInitialized() {
		return initialized;
	}
	
	@PostConstruct
	private void init() {
		FileReader fileReader;
		try {
			fileReader = new FileReader(messageSource);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		BufferedReader reader = new BufferedReader(fileReader);
		StringBuilder builder = new StringBuilder();
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line + "\r");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// don't even care.
					e.printStackTrace();
				}
			}
		}
		messageContent = builder.toString();
		initialized = true;
	}
}
