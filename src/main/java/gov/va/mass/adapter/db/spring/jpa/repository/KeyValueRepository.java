package gov.va.mass.adapter.db.spring.jpa.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import gov.va.mass.adapter.db.spring.jpa.beans.KeyValue;
import org.springframework.stereotype.Repository;

@Repository

public interface KeyValueRepository extends CrudRepository<KeyValue, Long> {
	Iterable<KeyValue> findAll();
	
	KeyValue findOne(UUID primaryKey);
	
	Iterable<KeyValue> findByMessageID(@Param("messageID")UUID msgID);
	
	KeyValue findByTypeAndMessageID(UUID msgID, String type);
                             
    boolean exists(UUID primaryKey);
    
    long count();
                                                                              
    KeyValue save(KeyValue k);
	
    void delete(KeyValue k);
                                   
    void update(KeyValue k);
}