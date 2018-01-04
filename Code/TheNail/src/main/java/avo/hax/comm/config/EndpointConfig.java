package avo.hax.comm.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author avolkano
 */
public class EndpointConfig {
	private String method;
	
	public String getMethod() {
		return this.method;
	}
	
	public void setMethod(String method) {
		this.method = method;
	}
	
	private String url;
	
	public String getUrl() {
		return this.url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	private String contentType;
	
	public String getContentType() {
		return this.contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	private String content;
	
	public String getContent() {
		return this.content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	private boolean isFile;
	
	public boolean getIsFile() {
		return this.isFile;
	}
	
	public void setIsFile(boolean isFile) {
		this.isFile = isFile;
	}
	
	public String getResponseTemplate() throws IOException {
		if (!isFile) {
			return content;
		} else {
			FileReader fileReader;
			try {
				fileReader = new FileReader(content);
			} catch (FileNotFoundException e) {
				throw e;
			}
			BufferedReader reader = new BufferedReader(fileReader);
			StringBuilder builder = new StringBuilder();
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					builder.append(line + "\r");
				}
			} catch (IOException e) {
				throw e;
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
			return builder.toString();
		}
	}
	
	@Override
	public String toString() {
		return String.format(
				"%s %s Content-Type: %s content(%s): %s",
				this.method,
				this.url,
				this.contentType,
				this.isFile ? "file" : "raw",
				this.content);
	}
}
