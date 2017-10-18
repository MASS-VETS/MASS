package gov.va.mass.adapter.db;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.StoredProcedureQuery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import gov.va.mass.adapter.spring.jpa.beans.HAPIKeyValue;
import gov.va.mass.adapter.spring.jpa.beans.Interface;
import gov.va.mass.adapter.spring.jpa.beans.KeyValue;
import gov.va.mass.adapter.spring.jpa.beans.MessageData;
import gov.va.mass.adapter.spring.jpa.repository.InterfaceRepository;
import gov.va.mass.adapter.spring.jpa.repository.KeyValueRepository;
import gov.va.mass.adapter.spring.jpa.repository.MessageDataRepository;

@Component
@PropertySource("classpath:application.properties")
public class DBPersistenceService {
	
	@Value("${jpa.string}")
	private static String JPA_STRING ;
	
	private InterfaceRepository iRepository;
	private KeyValueRepository keyValRepository;
	private MessageDataRepository msgRepository;
	private static EntityManagerFactory emfactory = Persistence.createEntityManagerFactory("MASS-Adapter_JPA"); 

	/* Read Operations */
	public List<Interface> queryAllInterfaces() {
		return (List<Interface>)iRepository.findAll();
	}
	
	public Interface queryInterfaceByNameAndDirection(String name, String direction) {
		return iRepository.findByNameAndDirection(name, direction);
	}
	
	public List<MessageData> queryAllMessages() {
		return (List<MessageData>)msgRepository.findAll();
	}
	
	public List<MessageData> queryMessagesByInterface(String interfaceName, String interfaceDirection) {
		Interface i = queryInterfaceByNameAndDirection( interfaceName, interfaceDirection);
		return (List<MessageData>)msgRepository.findByInterfaceID(i.getInterfaceId());
	}
	
	public List<KeyValue> queryKeysByMessage(UUID msgID) {
		return (List<KeyValue>)keyValRepository.findByMessageID(msgID);
	}
	
	/* Create Operations */
	public MessageData saveMessage(String interfaceName, String interfaceDirection, String content) {
		EntityManager em = emfactory.createEntityManager();
		Interface i = queryInterfaceByNameAndDirection( interfaceName, interfaceDirection);
		StoredProcedureQuery query = MessageData.createStoreMessageStoredProcedureQuery(em, i.getInterfaceId(), content);
	    em.getTransaction().begin();
	    query.execute();
	    UUID msgID = (UUID)query.getOutputParameterValue("msgID");
	    em.getTransaction().commit();
	    return msgRepository.findOne(msgID);
		//MessageData m = new MessageData(content, i.getInterfaceId());
		//msgRepository.save(m);
	}
	
	public void saveKeyValue(UUID msgID, String type, String value) {
		EntityManager em = emfactory.createEntityManager( );
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
		iRepository.save(i);
	}
	
	/* Delete Operations */
	public void deleteMessage(UUID id) {
		MessageData m = msgRepository.findOne(id);
		msgRepository.delete(m);
	}
	
	public void deleteAllMessagesOfInterface(String interfaceName, String interfaceDirection) {
		List<MessageData> l = queryMessagesByInterface(interfaceName, interfaceDirection);
		for (MessageData m : l) {
			deleteKeyValuesByMessage(m.getmsgId());
			msgRepository.delete(m);
		}
	}
	
	public void deleteKeyValuesByMessage(UUID id) {
		List<KeyValue> l = queryKeysByMessage(id);
		for (KeyValue k : l)
			keyValRepository.delete(k);
	}
	
	public void deleteInterface(String name, String direction) {
		Interface i = queryInterfaceByNameAndDirection(name, direction);
		deleteAllMessagesOfInterface(name, direction);
		iRepository.delete(i);
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
		i.setName(newName);
		i.setDirection(newDirection);
		i.setPurgeDays(purgeDays);
		iRepository.update(i);
	}
	
	public void updateKeyValue(UUID msgID, String newValue, String oldType, String newType) {
		KeyValue k = keyValRepository.findByTypeAndMessageID(msgID, oldType);
		k.setType(newType);
		k.setValue(newValue);
		keyValRepository.update(k);
	}
	
	public void updateMessage(UUID msgID, String content) {
		MessageData m = msgRepository.findOne(msgID);
		m.setMessageContent(content);
		m.setmsgDate(new Date());
		msgRepository.update(m);
	}
	
	public void updateMessageInterface(UUID msgID, String interfaceName, String direction) {
		MessageData m = msgRepository.findOne(msgID);
		Interface i = queryInterfaceByNameAndDirection(interfaceName, direction);
		m.setInterfaceId(i.getInterfaceId());
	}
	
}
