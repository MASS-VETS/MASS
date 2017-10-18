package gov.va.mass.adapter.spring.jpa.repository;

import java.util.UUID;
import org.springframework.data.repository.Repository;
import gov.va.mass.adapter.spring.jpa.beans.Interface;

public interface InterfaceRepository extends Repository<Interface, UUID> {
	
	Iterable<Interface> findAll();
	
	Interface findOne(UUID primaryKey);
	
	Interface findByNameAndDirection(String name, String direction);
                             
    boolean exists(UUID primaryKey);
    
    Long count();
                                                                              
	Interface save(Interface i);
	
    void delete(Interface i);
                                   
    void update(Interface i);
	
}