package gov.va.mass.adapter.db;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import gov.va.mass.adapter.db.spring.jpa.beans.HAPIKeyValue;
import gov.va.mass.adapter.db.spring.jpa.beans.Interface;
import gov.va.mass.adapter.db.spring.jpa.beans.KeyValue;
import gov.va.mass.adapter.db.spring.jpa.beans.MessageData;


@Component
@EnableAutoConfiguration
@PropertySource("classpath:application.properties")
public class DBPersistenceService {
	
	@Value("${jpa.string}")
	private static String JPA_STRING ;

	private static EntityManagerFactory emfactory = Persistence.createEntityManagerFactory("MASS-Adapter_JPA"); 
	private EntityManager em;
	
	public DBPersistenceService() {
		em = emfactory.createEntityManager();
	}
	
	/* Read Operations */
	
	public List<Interface> queryAllInterfaces() {
		TypedQuery<Interface> query = em.createQuery("SELECT i FROM Interface i", Interface.class);
		return (List<Interface>) query.getResultList();
		//return (List<Interface>)iRepository.findAll();
	}
	
	
	public Interface queryInterfaceByNameAndDirection(String name, String direction) {
		TypedQuery<Interface> query = em.createQuery("SELECT i FROM Interface i where i.Name = :name and i.Direction = :direction", Interface.class);
		query.setParameter("name", name);
		query.setParameter("direction", direction);
		return query.getSingleResult();
		//return iRepository.findByNameAndDirection(name, direction);
	}
	
	
	public List<MessageData> queryAllMessages() {
		TypedQuery<MessageData> query = em.createQuery("SELECT i FROM MessageData i", MessageData.class);
		return (List<MessageData>) query.getResultList();
		//return (List<MessageData>)msgRepository.findAll();
	}
	
	
	public List<MessageData> queryMessagesByInterface(String interfaceName, String interfaceDirection) {
		Interface i = queryInterfaceByNameAndDirection( interfaceName, interfaceDirection);
		TypedQuery<MessageData> query = em.createQuery("SELECT i FROM MessageData i where i.interfaceID = :iid", MessageData.class);
		query.setParameter("iid", i.getInterfaceId());
		return query.getResultList();
		//return (List<MessageData>)msgRepository.findByInterfaceID(i.getInterfaceId());
	}
	
	public MessageData queryMessageByID(UUID msgID) {
		//Interface i = queryInterfaceByNameAndDirection( interfaceName, interfaceDirection);
		TypedQuery<MessageData> query = em.createQuery("SELECT i FROM MessageData i where i.ID = :iid", MessageData.class);
		query.setParameter("iid", msgID);
		return query.getSingleResult();
		//return (List<MessageData>)msgRepository.findByInterfaceID(i.getInterfaceId());
	}
	
	
	public List<KeyValue> queryKeysByMessage(UUID msgID) {
		TypedQuery<KeyValue> query = em.createQuery("SELECT i FROM KeyValue i where i.messageID = :iid", KeyValue.class);
		query.setParameter("iid", msgID);
		return query.getResultList();
		//return (List<KeyValue>)keyValRepository.findByMessageID(msgID);
	}
	
	public KeyValue queryKeyValueByTypeAndMessageID(UUID msgID, String type) {
		TypedQuery<KeyValue> query = em.createQuery("SELECT i FROM KeyValue i where i.Type = :type and i.messageID = :msgID", KeyValue.class);
		query.setParameter("type", type);
		query.setParameter("msgID", msgID);
		return query.getSingleResult();
	}
	
	/* Create Operations */
	
	public MessageData saveMessage(String interfaceName, String interfaceDirection, String content) {
		Interface i = queryInterfaceByNameAndDirection( interfaceName, interfaceDirection);
		StoredProcedureQuery query = MessageData.createStoreMessageStoredProcedureQuery(em, i.getInterfaceId(), content);
	    em.getTransaction().begin();
	    query.execute();
	    UUID msgID = (UUID)query.getOutputParameterValue("msgID");
	    em.getTransaction().commit();
	    return queryMessageByID(msgID);
		//MessageData m = new MessageData(content, i.getInterfaceId());
		//msgRepository.save(m);
	}
	
	public void saveKeyValue(UUID msgID, String type, String value) {
		StoredProcedureQuery query = KeyValue.createStoreKeyValueStoredProcedureQuery(em, msgID, type, value);
	    em.getTransaction().begin();
	    query.execute();
	    em.getTransaction().commit();
		//KeyValue k = new KeyValue(msgID, type, value);
		//keyValRepository.save(k);
	}
	
	public void saveKeyValues(UUID msgID, String content) {    
	    EntityManager em = emfactory.createEntityManager( );
	    StoredProcedureQuery query = HAPIKeyValue.createStoreHAPIKeyValueStoredProcedureQuery(em, content, msgID);
	    em.getTransaction().begin();
	    query.execute();
	    em.getTransaction().commit();
		System.out.println("Keys successfully saved");
	}
	
	
	public void saveInterface(String name, String direction, int purgeDays) {
		Interface i = new Interface(UUID.randomUUID(), name, direction, purgeDays);
		em.getTransaction().begin();
	    em.persist(i);
	    em.getTransaction().commit();
		//iRepository.save(i);
	}
	
	/* Delete Operations */
	
	public void deleteMessage(UUID id) {
		MessageData m = queryMessageByID(id);
		em.getTransaction().begin();
		em.remove(m);
		em.getTransaction().commit();
		//msgRepository.delete(m);
	}
	
	
	public void deleteAllMessagesOfInterface(String interfaceName, String interfaceDirection) {
		List<MessageData> l = queryMessagesByInterface(interfaceName, interfaceDirection);
		for (MessageData m : l) {
			deleteKeyValuesByMessage(m.getmsgId());
			deleteMessage(m.getmsgId());
		}
	}
	
	
	public void deleteKeyValuesByMessage(UUID id) {
		List<KeyValue> l = queryKeysByMessage(id);
		for (KeyValue k : l) {
			em.getTransaction().begin();
			em.remove(k);
			em.getTransaction().commit();
		}
	}
	
	
	public void deleteInterface(String name, String direction) {
		Interface i = queryInterfaceByNameAndDirection(name, direction);
		deleteAllMessagesOfInterface(name, direction);
		em.getTransaction().begin();
		em.remove(i);
		em.getTransaction().commit();
		//iRepository.delete(i);
	}
	
	public void purgeMessages(int purgeDays) {
	    EntityManager em = emfactory.createEntityManager();
	    StoredProcedureQuery query = Interface.createPurgeMessagesStoredProcedureQuery(em, purgeDays);
	    em.getTransaction().begin();
	    query.execute();
	    em.getTransaction().commit();
	}
	
	/* Update Operations */
	
	public void updateInterface(String oldName, String newName, String oldDirection, String newDirection, int purgeDays) {
		Interface i = queryInterfaceByNameAndDirection(oldName, oldDirection);
		em.getTransaction().begin();
		i.setName(newName);
		i.setDirection(newDirection);
		i.setPurgeDays(purgeDays);
		em.getTransaction().commit();
		//iRepository.update(i);
	}
	
	
	public void updateKeyValue(UUID msgID, String newValue, String oldType, String newType) {
		//KeyValue k = keyValRepository.findByTypeAndMessageID(msgID, oldType);
		KeyValue k = queryKeyValueByTypeAndMessageID(msgID, oldType);
		em.getTransaction().begin();
		k.setType(newType);
		k.setValue(newValue);
		em.getTransaction().commit();

		//keyValRepository.update(k);
	}
	
	
	public void updateMessage(UUID msgID, String content) {
		MessageData m = queryMessageByID(msgID);
		em.getTransaction().begin();
		m.setMessageContent(content);
		m.setmsgDate(new Date());
		em.getTransaction().commit();
		//msgRepository.update(m);
	}
	
	
	public void updateMessageInterface(UUID msgID, String interfaceName, String direction) {
		MessageData m = queryMessageByID(msgID);
		Interface i = queryInterfaceByNameAndDirection(interfaceName, direction);
		em.getTransaction().begin();
		m.setInterfaceId(i.getInterfaceId());
		em.getTransaction().commit();
	}
	
}
