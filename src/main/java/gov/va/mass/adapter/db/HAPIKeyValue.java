package gov.va.mass.adapter.db.spring.jpa.beans;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.StoredProcedureQuery;


@NamedStoredProcedureQuery(
		name = "callStoreHAPIKeyValueStoreProcedure", 
		procedureName = "storeHAPIKeyValue", 
		parameters = { 
			@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class, name = "fieldList"),
			@StoredProcedureParameter(mode = ParameterMode.IN, type = UUID.class, name = "msgID")
		}
)

@Entity
public class HAPIKeyValue {
	
		@Id
		private UUID ID;
		
		static public StoredProcedureQuery createStoreHAPIKeyValueStoredProcedureQuery(EntityManager em, String fieldList,  UUID msgID) {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery("callStoreHAPIKeyValueStoreProcedure");
			query.setParameter("fieldList", fieldList);
			query.setParameter("msgID", msgID);
			return query;
		}
		
		@Override
		public String toString() {
			return "Key [ID=" + ID + " ]";
		}
	
}
