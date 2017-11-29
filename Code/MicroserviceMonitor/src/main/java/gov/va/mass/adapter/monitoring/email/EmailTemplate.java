package gov.va.mass.adapter.monitoring.email;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailTemplate
{
	public String address;
	public String password;
	private Properties _props;
	
	public EmailTemplate(String address, String password, String host, String port)
	{
		this.address = address;
		this.password = password;
        this._props = new Properties();
        _props.put("mail.smtp.auth", "true");
        _props.put("mail.smtp.host", host);
        _props.put("mail.smtp.socketFactory.port", port); 
        _props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        _props.put("mail.smtp.port", port);
	}
	public void SendMail(String toEmail, String subject, String text)
	{
        Session session = Session.getInstance(_props,
                new javax.mail.Authenticator() {
                  protected PasswordAuthentication getPasswordAuthentication() {
                      return new PasswordAuthentication(address, password);
                  }
                });

              try {

                  Message message = new MimeMessage(session);
                  message.setFrom(new InternetAddress(address));
                  message.setRecipients(Message.RecipientType.TO,
                          InternetAddress.parse(toEmail));
                  message.setSubject(subject);
                  message.setText(text);

                  Transport.send(message);

                  System.out.println("Done");

              } catch (MessagingException e) {
                  throw new RuntimeException(e);
              }
	}
}
