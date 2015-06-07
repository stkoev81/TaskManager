package tma.dao;

import javax.annotation.Resource;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Common stuff for dao's.
 */
public class BaseDao { 
	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

	protected Session getSession(){
		return sessionFactory.getCurrentSession();
	}
	
	/**
	 * Evicts the object as defined by Hibernate, i.e. makes it detached. 
	 * @param obj
	 */
	public void evict(Object obj) {
		getSession().evict(obj);
	}

	/**
	 * Sets a "optimistic force increment lock" on the object for the current transaction. This means that when
	 * committing the transaction, the version field is incremented to indicate that the object has been tampered with;
	 * if meanwhile another transaction has tampered with the object, this one fails. 
	 */
	public void lock(Object obj) {
		getSession().buildLockRequest(LockOptions.NONE).setLockMode(LockMode.OPTIMISTIC_FORCE_INCREMENT).lock(obj);
	}
	


}
