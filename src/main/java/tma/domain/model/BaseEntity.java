package tma.domain.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import tma.exceptions.Assertion;
import tma.exceptions.BadInput;
import tma.util.Utils;

/**
 * Common stuff for all entities. 
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable{
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Version
	private Long version;

	/**
	 * automatically set when persisted first time
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}
	
	
	public Long getId() {
		return id;
	}


	@Override
	public int hashCode() {
		if(id == null){
			return 0; 
		}
		else{
			return id.intValue();
		}
	}

	@Override
	/**
	 * equality based on id. 
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseEntity other = (BaseEntity) obj;
		if (!id.equals(other.id))
			return false;
		return true;
	}

	public Long getVersion() {
		return version;
	}
	/**
	 * automatically updated every time it entity is persisted
	 * @param version
	 */
	public void setVersion(Long version) {
		this.version = version;
	}
	/**
	 * Checks if the entity is in a valid state.
	 * @return a list of messages about invalid data in the entity; if the list is empty then the entity is valid.
	 */
	public abstract List<BadInput> validate();
	
	/**
	 * Requirements: the entity must be valid as determined by {@link #validate()}. 
	 * @throws Assertion if requirement are not met.  
	 */
	public void assertValid(){
		List<BadInput> err = validate();
		if(!err.isEmpty()){
			throw new Assertion(err.toString());
		}
	}
	
	/**
	 * Returns JSON representation of the entity
	 */
	public String toString() {
		return Utils.toJson(this);
	}

}
