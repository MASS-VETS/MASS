package gov.va.mass.adapter.spring.jpa.repository;


import java.util.UUID;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gov.va.mass.adapter.spring.jpa.beans.MessageData;

/*
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
*/

public interface MessageDataRepository extends Repository<MessageData, UUID> {
//	@Query(nativeQuery=true)
//	public List<MessageData> findByDesc(@Param("description") String description);
	Iterable<MessageData> findAll();
	
	MessageData findOne(UUID msgID);
	
	Iterable<MessageData> findByInterfaceID(@Param("interfaceID") UUID iid);
                             
    boolean exists(UUID primaryKey);
    
    Long count();
                                                                              
    MessageData save(MessageData m);
	
    void delete(MessageData m);
                                   
    void update(MessageData m);
}