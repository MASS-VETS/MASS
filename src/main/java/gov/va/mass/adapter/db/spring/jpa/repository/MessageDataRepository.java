package gov.va.mass.adapter.db.spring.jpa.repository;


import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import gov.va.mass.adapter.db.spring.jpa.beans.MessageData;

/*
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
*/
@Repository
public interface MessageDataRepository extends CrudRepository<MessageData, Long> {
//	@Query(nativeQuery=true)
//	public List<MessageData> findByDesc(@Param("description") String description);
	Iterable<MessageData> findAll();
	
	MessageData findOne(UUID msgID);
	
	Iterable<MessageData> findByInterfaceID(@Param("interfaceID") UUID iid);
                             
    boolean exists(UUID primaryKey);
    
    long count();
                                                                              
    MessageData save(MessageData m);
	
    void delete(MessageData m);
                                   
    void update(MessageData m);
}