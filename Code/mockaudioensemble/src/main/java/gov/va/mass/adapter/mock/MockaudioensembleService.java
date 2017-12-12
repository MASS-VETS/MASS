package gov.va.mass.adapter.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

// TODO: Have the server insist on client authentication
// TODO : Verify content type and content disposition are as expected
// TODO : Add from and to date parameters?

@RestController
@PropertySource("classpath:application.properties")
public class MockaudioensembleService {


	//TODO : Try to send a text/csv content type
	// TODO : What is the difference between octet stream and octet stream value
	// Note: OCTET_STREAM_VALUE and OCTET_STREAM constants both do not work well.They appears to be missing a '/' that is expected by the api
    //produces = "MediaType.APPLICATION_OCTET_STREAM_VALUE") // APPLICATION_OCTET_STREAM_VALUE do not work

	@Value("${app.appointments.folder}")
	private String ENSEMBLE_FROM_ADAPTER;

	@RequestMapping(value = "/mockensemble/audiocare/responses", method = RequestMethod.GET , produces =  "text/csv") //,
// working @RequestMapping(value = "/mockensemble/audiocare/responses", method = RequestMethod.GET 
	public @ResponseBody HttpEntity<byte[]> getAudiocareResponseToLastAppointmentFile() throws IOException {

		String respfile_path = System.getenv("RESPFILE");
		File file = new File(respfile_path);
		byte[] filebytes = FileCopyUtils.copyToByteArray(file);
		System.out.println(" In mock service. Sending file " + file.getAbsolutePath());
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type","application/octet_stream");
		headers.set("Content-Disposition", "attachment; filename=" + file.getName());	
		headers.setContentLength(file.length());
		HttpEntity<byte[]> httpEntity = new HttpEntity<byte[]>(filebytes, headers);

		return httpEntity;
	}

	

	@PostMapping(path="/mockensemble/audiocare/appointments", consumes = "multipart/form-data", produces =  "text/plain")
 
	public ResponseEntity<String> receiveFile(@RequestParam("file") MultipartFile file) {

		System.out.println("Received a post appointments request");
		
	    String document = "File received";
		
	    saveUploadedFiles( file) ;
	    
	    ResponseEntity<String> responseEntity = new ResponseEntity<>(document,
                HttpStatus.OK);
	   
	    return responseEntity;

	}
	
	
	
	private void saveUploadedFiles(MultipartFile file) {
		System.out.println ("Received file ofcontentType " + file.getContentType());
		System.out.println ( "saving UploadedFile " + file.getOriginalFilename() + " file size " + file.getSize());
		
		// Get the file and save it somewhere
		byte[] bytes;
		try {
			bytes = file.getBytes();
			Path path = Paths.get(ENSEMBLE_FROM_ADAPTER + "/" + file.getOriginalFilename());
			Files.write(path, bytes);

		} catch (IOException e) {
			System.out.println ( "Could NOT save uploadedFile " + file.getOriginalFilename());
			e.printStackTrace();
		}
	}

}
