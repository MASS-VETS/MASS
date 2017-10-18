package gov.va.mass.adapter.spring.jpa.repository;

import java.util.UUID;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gov.va.mass.adapter.spring.jpa.beans.KeyValue;

public interface KeyValueRepository extends Repository<KeyValue, UUID> {
	Iterable<KeyValue> findAll();
	
	KeyValue findOne(UUID primaryKey);
	
	Iterable<KeyValue> findByMessageID(@Param("messageID")UUID msgID);
	
	KeyValue findByTypeAndMessageID(UUID msgID, String type);
                             
    boolean exists(UUID primaryKey);
    
    Long count();
                                                                              
    KeyValue save(KeyValue k);
	
    void delete(KeyValue k);
                                   
    void update(KeyValue k);
}