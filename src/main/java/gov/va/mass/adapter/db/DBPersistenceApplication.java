package gov.va.mass.adapter.db;

import java.util.List;

/*
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.StoredProcedureQuery;
import gov.va.mass.adapter.spring.jpa.beans.Interface;
import gov.va.mass.adapter.spring.jpa.beans.MessageData;
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import gov.va.mass.adapter.db.spring.jpa.beans.Interface;
import gov.va.mass.adapter.db.spring.jpa.beans.MessageData;

@SpringBootApplication
//@EnableJpaRepositories
public class DBPersistenceApplication {

	private static final Logger logger = LoggerFactory.getLogger(DBPersistenceApplication.class);
//	@Autowired
//	private InterfaceRepository repository;

	public static void main(String[] args) {
		logger.info("DBPersistenceApplication running in environment: " + System.getenv("ENV"));
		SpringApplication.run(DBPersistenceApplication.class, args);
		
		DBPersistenceService dal = new DBPersistenceService();
		//dal.purgeMessages(1);
		//System.out.println("Messages purged");
		//dal.saveMessage("Scheduling - Vista to Epic", "Incoming", "Test Message 1");
		//System.out.println("Message saved");
		//Interface i = dal.queryInterfaceByNameAndDirection("Scheduling - Vista to Epic", "Incoming");
		//System.out.println(i.toString());
		//dal.updateInterface("Scheduling - Vista to Epic","Scheduling - Vista to Epic", "IN", "Incoming", 5);
		//dal.deleteAllMessagesOfInterface("Scheduling - Vista to Epic", "Incoming");
		/*
		MessageData m = dal.saveMessage("Scheduling - Vista to Epic", "Incoming","Test Message 3");
		List<MessageData> l = dal.queryMessagesByInterface("Scheduling - Vista to Epic", "Incoming");
		for (MessageData j : l) {
			System.out.println(j.toString());
		}	
		*/
		//System.out.println(m.toString());
		/*
		List<Interface> l = dal.queryAllInterfaces();
		for (Interface j : l) {
			System.out.println(j.toString());
		}	

		List<MessageData> msgs = dal.queryMessagesByInterface("Scheduling - Vista to Epic", "Incoming");
		for (MessageData m : msgs) {
			System.out.println(m.toString());
		}	
		*/
		
    /*  working named query
    Query nq = em.createNamedQuery("insertInterface")
    		.setParameter("interfID", UUID.fromString("00000001-0000-0000-0000-000000000000"))
    		.setParameter("name", "Test Interface")
    		.setParameter("direction","IN")
    		.setParameter("purgeDays", 1);
    em.getTransaction().begin();
    nq.executeUpdate();
    em.getTransaction().commit();
    working named query */
    
	//StoredProcedureQuery query = Interface.createPurgeMessagesStoredProcedureQuery(em, 1);
    
    /*
    UUID id = UUID.fromString("00000008-0000-0000-0000-000000000000");

    Interface i = Interface.get(em, id);
    */
    
    /*  working code
    Interface i = new Interface(id, "EPIC", "OUT", 1);
    i.save(em);

    System.out.println("Interface " + i.getInterfaceId() +  " successfully created");

    i.update(em, "EPIC", "OUT", 10);
    
   	Interface.updateName(em, i, "EPIC Cadence");
    System.out.println("Interface " + i +  " successfully updated");

    MessageData m = new MessageData("Test Bean creation", i.getInterfaceId());
    m.save(em);
    System.out.println("Message " + m.getmsgId() +  " successfully stored");

    Routing r = new Routing(m.getmsgId(), m.getmsgId());
    r.save(em);
    System.out.println("Routing " + r.getRelatedID() +  " successfully stored");
    
    KeyValue k = new KeyValue(m.getmsgId(),"MSH","|^~\\\\&|||||20170906131542.351-0500");
    k.save(em);
    System.out.println("Key " + k.getValue() + " for message " + k.getMessageId() + " successfully stored");
 		working code */
 
    /*
    StoredProcedureQuery query = MessageData.createStoreMessageStoredProcedureQuery(em, i.getInterfaceId(), "MSH|^~\\&|||||20170906131542.351-0500||ACK^A01^ACK|3901|T|2.5.1 MSA|AA|1401");
    em.getTransaction().begin();
    query.execute();
	//System.out.println("Messages successfully purged");
    UUID msgID = (UUID)query.getOutputParameterValue("msgID");
    em.getTransaction().commit();
    System.out.println("Message " + msgID +  " successfully stored");

    */

    /* working code  queryAll
	List<Interface> interfs = (List<Interface>) Interface.getAll(em);
	for (Interface i : interfs) {
		System.out.println(i.toString());
	}	
    
	List<Routing> interfs = (List<Routing>) Routing.getAll(em);
	for (Routing i : interfs) {
		System.out.println(i.toString());
	}	
	
	List<MessageData> msgs = (List<MessageData>) MessageData.getAll(em);
	for (MessageData i : msgs) {
		System.out.println(i.toString());
	}	

	List<KeyValue> keys = (List<KeyValue>) KeyValue.getAll(em);
	for (KeyValue i : keys) {
		System.out.println(i.toString());
	}	
			working code */ 
}
	/*
	@Bean
	public CommandLineRunner demo() {
		return (args) -> {
			// save a couple of customers
			/*
			repository.save(new Customer("Jack", "Bauer"));
			repository.save(new Customer("Chloe", "O'Brian"));
			repository.save(new Customer("Kim", "Bauer"));
			repository.save(new Customer("David", "Palmer"));
			repository.save(new Customer("Michelle", "Dessler"));

			
			// fetch all customers
			logger.info("Interfaces found with findAll():");
			logger.info("-------------------------------");
			for (Interface i : repository.findAll()) {
				logger.info(i.toString());
			}
			logger.info("");

			// fetch Interfaces by last name
			logger.info("Interface found with findByNameAndDirection('Vista to Epic'):");
			logger.info("--------------------------------------------");
			Interface vista = repository.findByNameAndDirection("Scheduling - Vista to Epic", "Incoming"); 
			logger.info(vista.toString());
			logger.info("");
		};
	}
*/
	
}
