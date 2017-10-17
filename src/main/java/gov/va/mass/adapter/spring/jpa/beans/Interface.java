package gov.va.mass.adapter.spring.jpa.beans;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;
import javax.persistence.TypedQuery;


@NamedNativeQueries({
	@NamedNativeQuery(
	name = "insertInterface",
	query = "insert into dbo.interfaces values(:interfID,:name,:direction,:purgeDays)"
	//resultClass = KeyValue.class
	)
})

@NamedStoredProcedureQuery(
		name = "callPurgeMessagesStoreProcedure", 
		procedureName = "purgeMessageData", 
		parameters = { 
			@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class, name = "systemPurgeDays")
		}
)

@Entity
@Table(name = "interfaces")
public class Interface {
	@Id
	private UUID ID;
	private String Name;
	private String Direction;
	private int PurgeDays;
	
	static public StoredProcedureQuery createPurgeMessagesStoredProcedureQuery(EntityManager em, int purgeDays) {
		StoredProcedureQuery query = em.createNamedStoredProcedureQuery("callPurgeMessagesStoreProcedure");
		query.setParameter("systemPurgeDays", purgeDays);
		return query;
	}
	
	public Interface() {
	}
	
	public Interface(UUID interfID, String name, String direction, int purgeDays) {
		this.ID = interfID;
		this.Name = name;
		this.Direction = direction;
		this.PurgeDays = purgeDays;
	}

	@Column(name = "ID", unique = true, nullable = false)
	public UUID getInterfaceId() {
		return this.ID;
	}
	
	public void setInterfaceId(UUID interfID) {
		this.ID = interfID;
	}
	
	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return this.Name;
	}
	
	public void setName(String name) {
		this.Name = name;
	}
	
	@Column(name = "Direction", nullable = true, length = 20)
	public String getDirection() {
		return this.Direction;
	}
	
	public void setDirection(String direction) {
		this.Direction = direction;
	}
	
	@Column(name = "PurgeDays", nullable = true, length = 4)
	public int PurgeDays() {
		return this.PurgeDays;
	}
	
	public void setPurgeDays(int purgeDays) {
		this.PurgeDays = purgeDays;
	}
	
	public void save(EntityManager em) {
		em.getTransaction().begin();
	    em.persist(this);
	    em.getTransaction().commit();
	}
	
	public void delete(EntityManager em) {
		em.getTransaction().begin();
	    em.remove(this);
	    em.getTransaction().commit();
	}
	
	public void update(EntityManager em, String name, String direction, int purgeDays) {
		em.getTransaction().begin();
	    this.setName(name);
	    this.setDirection(direction);
	    this.setPurgeDays(purgeDays);
	    em.getTransaction().commit();
	}
	
	static public Interface get(EntityManager em, UUID ID) {
		return em.find(Interface.class, ID);
	}
	
	static public List<Interface> getAll(EntityManager em) {
		TypedQuery<Interface> query = em.createQuery("SELECT i FROM Interface i", Interface.class);
		 return (List<Interface>) query.getResultList();
	}
	
	static public void updateName(EntityManager em, UUID interfID, String name) {
		Interface i = em.find(Interface.class, interfID);
		em.getTransaction().begin();
		i.setName(name);
		em.getTransaction().commit();
	}
	
	static public void updateDirection(EntityManager em, UUID interfID, String direction) {
		Interface i = em.find(Interface.class, interfID);
		em.getTransaction().begin();
		i.setDirection(direction);
		em.getTransaction().commit();
	}
	
	static public void updatePurgeDays(EntityManager em, UUID interfID, Integer days) {
		Interface i = em.find(Interface.class, interfID);
		em.getTransaction().begin();
		i.setPurgeDays(days);
		em.getTransaction().commit();
	}
	
	@Override
	public String toString() {
		return "Message [InterfaceID=" + ID + ", Name="
				+ Name + ", Direction=" + Direction + "PurgeDays=" + PurgeDays + "]";
	}

}
