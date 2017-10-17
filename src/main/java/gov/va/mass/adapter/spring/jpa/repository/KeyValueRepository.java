package gov.va.mass.adapter.spring.jpa.repository;

import org.springframework.data.repository.Repository;
import gov.va.mass.adapter.spring.jpa.beans.KeyValue;

interface KeyValueRepository extends Repository<KeyValue, Long> {
}