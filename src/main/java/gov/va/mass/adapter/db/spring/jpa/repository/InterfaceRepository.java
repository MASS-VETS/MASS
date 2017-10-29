package gov.va.mass.adapter.db.spring.jpa.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import gov.va.mass.adapter.db.spring.jpa.beans.Interface;

@Repository
public interface InterfaceRepository extends CrudRepository<Interface, Long> {
	
	Iterable<Interface> findAll();
	
	Interface findOne(UUID primaryKey);
	
	Interface findByNameAndDirection(String name, String direction);
                             
    boolean exists(UUID primaryKey);
    
    long count();
                                                                              
	@SuppressWarnings("unchecked")
	Interface save(Interface i);
	
    void delete(Interface i);
                                   
    void update(Interface i);
	
}