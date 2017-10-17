package gov.va.mass.adapter.spring.jpa.repository;


import org.springframework.data.repository.Repository;
import gov.va.mass.adapter.spring.jpa.beans.MessageData;

/*
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
*/

interface MessageDataRepository extends Repository<MessageData, Long> {
//	@Query(nativeQuery=true)
//	public List<MessageData> findByDesc(@Param("description") String description);
}