package gov.va.mass.adapter.spring.jpa.beans;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.ParameterMode;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@NamedStoredProcedureQuery(
		name = "callStoreMessageStoreProcedure", 
		procedureName = "storeMessage", 
		parameters = { 
			@StoredProcedureParameter(mode = ParameterMode.IN, type = UUID.class, name = "interface"),
			@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class, name = "messageContent"),
			@StoredProcedureParameter(mode = ParameterMode.IN, type = UUID.class, name = "relatedID"),
			@StoredProcedureParameter(mode = ParameterMode.OUT, type = UUID.class, name = "msgID")
		}
//		resultClasses = MessageData.class
)

@Table(name = "messageData")
public class MessageData {
	
	@Id
	private UUID ID;
	private String MessageContent;
	private UUID InterfaceID;
	private Date Datetime;
	
	public MessageData() {
	}
	
	public MessageData(String messageContent, UUID interfaceID) {
		this.MessageContent = messageContent;
		this.InterfaceID = interfaceID;
		this.ID = UUID.randomUUID();
		this.Datetime = new Date();
		
	}
	
	static public StoredProcedureQuery createStoreMessageStoredProcedureQuery(EntityManager em, UUID interfaceID, String messageContent, UUID relatedMsgID) {
		StoredProcedureQuery query = em.createNamedStoredProcedureQuery("callStoreMessageStoreProcedure");
		query.setParameter("interface", interfaceID);
		//query.setParameter("msgID", msgID);
		query.setParameter("messageContent", messageContent);
		query.setParameter("relatedID", relatedMsgID);
		return query;
	}
	
	static public List<MessageData> getAll(EntityManager em) {
		TypedQuery<MessageData> query = em.createQuery("SELECT k FROM MessageData k", MessageData.class);
		 return (List<MessageData>) query.getResultList();
	}
	
	@Column(name = "ID", unique = true, nullable = false)
	public UUID getmsgId() {
		return this.ID;
	}
	
	public void setmsgId(UUID msgID) {
		this.ID = msgID;
	}
	
	@Column(name = "Datetime", unique = true, nullable = false)
	public Date getMsgDate() {
		return this.Datetime;
	}
	
	public void setmsgDate(Date datetime) {
		this.Datetime = datetime;
	}
	
	@Column(name = "MessageContent", unique = true, nullable = false)
	public String getMessageContent() {
		return this.MessageContent;
	}
	
	public void setMessageContent(String messageContent) {
		this.MessageContent = messageContent;
	}
	
	@Column(name = "InterfaceID", unique = true, nullable = false)
	public UUID getInterfaceId() {
		return this.InterfaceID;
	}
	
	
	public void setInterfaceId(UUID interfID) {
		this.InterfaceID = interfID;
	}
	
	public void save(EntityManager em) {
		em.getTransaction().begin();
	    em.persist(this);
	    em.getTransaction().commit();
	}
	
	public void delete(EntityManager em) {
		em.getTransaction().begin();
	    em.remove(this);
	    em.getTransaction().commit();
	}
	
	public void update(EntityManager em, String messageContent, UUID interfID) {
		em.getTransaction().begin();
		this.setMessageContent(messageContent);
		this.setInterfaceId(interfID);
		this.setmsgDate(new Date());
		em.getTransaction().commit();
	}
	
	static public MessageData get(EntityManager em, UUID ID) {
		return em.find(MessageData.class, ID);
	}
	
	static public void updateContent(EntityManager em, String content, UUID msgID) {
		MessageData m = em.find(MessageData.class, msgID);
		em.getTransaction().begin();
		m.setMessageContent(content);
	    em.getTransaction().commit();
	}
	
	static public void updateTimeStamp(EntityManager em, Date timestamp, UUID msgID) {
		MessageData m = em.find(MessageData.class, msgID);
		em.getTransaction().begin();
		m.setmsgDate(timestamp);
	    em.getTransaction().commit();
	}
	
	static public void updateInterface(EntityManager em, UUID interfID, UUID msgID) {
		MessageData m = em.find(MessageData.class, msgID);
		em.getTransaction().begin();
		m.setInterfaceId(interfID);
		em.getTransaction().commit();
	}
	
	@Override
	public String toString() {
		return "Message [" + "msgID = " + ID + " MessageContent= "
				+ MessageContent + " InterfaceID= " + InterfaceID + " Date = " + Datetime + " ]";
	}

}
