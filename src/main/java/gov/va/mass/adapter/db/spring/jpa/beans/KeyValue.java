package gov.va.mass.adapter.db.spring.jpa.beans;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;
import javax.persistence.TypedQuery;




@NamedStoredProcedureQuery(
		name = "callStoreKeyValueStoreProcedure", 
		procedureName = "storeKeyValue", 
		parameters = { 
			@StoredProcedureParameter(mode = ParameterMode.IN, type = UUID.class, name = "messageID"),
			@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class, name = "type"),
			@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class, name = "value")
		}
//		resultClasses = MessageData.class
)

@Entity
@Table(name = "keyValues")
public class KeyValue {

	@Id
	private UUID messageID;
	private String Type;
	private String Value;
	
	static public StoredProcedureQuery createStoreKeyValueStoredProcedureQuery(EntityManager em, UUID msgID, String type, String value) {
		StoredProcedureQuery query = em.createNamedStoredProcedureQuery("callStoreKeyValueStoreProcedure");
		query.setParameter("messageID", msgID);
		query.setParameter("type", type);
		query.setParameter("value", value);
		return query;
	}
	
	public KeyValue(UUID msgID, String type, String value) {
		this.messageID = msgID;
		this.Type = type;
		this.Value = value;
	}
	
	public KeyValue() {
	}

	public UUID getMessageId() {
		return this.messageID;
	}
	
	@Column(name = "MessageID", unique = true, nullable = false)
	public void setMessageId(UUID msgID) {
		this.messageID = msgID;
	}
	
	@Column(name = "Type", nullable = false, length = 20)
	public String getType() {
		return this.Type;
	}
	
	public void setType(String type) {
		this.Type = type;
	}
	
	@Column(name = "Value", nullable = true, length = 255)
	public String getValue() {
		return this.Value;
	}
	
	public void setValue(String value) {
		this.Value = value;
	}
	
	/*
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
	
	public void update(EntityManager em, String type, String value) {
		em.getTransaction().begin();
	    this.setType(type);
	    this.setValue(value);
	    em.getTransaction().commit();
	}
	
	static public List<KeyValue> getKeysByMessage(EntityManager em, UUID msgID) {
		TypedQuery<KeyValue> query = em.createQuery("SELECT k FROM KeyValue k where k.MessageID = :ID", KeyValue.class);
		 return (List<KeyValue>) query.setParameter("ID", msgID).getResultList();
	}
	
	static public List<KeyValue> getAll(EntityManager em) {
		TypedQuery<KeyValue> query = em.createQuery("SELECT k FROM KeyValue k", KeyValue.class);
		 return (List<KeyValue>) query.getResultList();
	}
	*/
	
	@Override
	public String toString() {
		return "Message [MessageId=" + messageID + ", Type="
				+ Type + ", Value=" + Value + "]";
	}

}
