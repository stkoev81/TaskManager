package tma.dao;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import tma.domain.model.Schedule;
import tma.domain.model.User;
import tma.exceptions.Assertion;
import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.util.Utils;

/**
 * User related data access object. 
 */
@Repository
public class UserDao extends BaseDao{
	
	/**
	 * @throws Assertion if user is null or user id is not null
	 */
	public long create(User user) {
		Utils.assertTrue(user != null && user.getId() == null);
		getSession().save(user);
		return user.getId();
	}
	
	/**
	 * @throws Assertion if user or user id is null
	 * @throws BadInputException if user with this id does not exist
	 */
	public void update(User user) {
		Utils.assertTrue(user != null && user.getId() != null);
		read(user.getId());
		getSession().merge(user);
	}

	/**
	 * @throws BadInputException if it is not found
	 */
	public User read(long id){
		User result = (User) getSession().get(User.class, id);
	    if(result == null){
	    	throw new BadInputException(BadInput.USER_NOT_FOUND);
	    }
	    return result;
	}
	
	/**
	 * @throws BadInputException if it is not found
	 */
	public void delete(long id){
		User user = read(id);
		getSession().delete(user);
	}
	
	/**
	 * @throws BadInputException if it is not found
	 */
	public void deleteSchedule(long id){
		Schedule schedule = (Schedule) getSession().load(Schedule.class, id);
		if(schedule == null){
			throw new BadInputException(BadInput.SCHEDULE_NOT_FOUND);
		}
		getSession().delete(schedule);
	}
	
	/**
	 * Finds user by username (case insensitive).
	 * @throws Assertion if username is null 
	 * @return User if found, otherwise null 
	 */
	public User findUser(String username){
		Utils.assertTrue(username != null);
		StringBuilder queryBldr = new StringBuilder("select {user.*} from User user " );
	
		queryBldr.append(" where lower(user.username) = lower(:username)" ); 
		
		Query query =  getSession().createSQLQuery(queryBldr.toString())
		.addEntity("user", User.class);

		query.setString("username", username);
		
		List result = query.list();
		Utils.assertTrue(result.size() <= 1);
		if (result.size() == 1){
			return (User) result.get(0);  
		}
		else{
			return null;
		}
	}
	
	/**
	 * Check if user has task by id.  
	 * @return true if this combination of user and task exists, false otherwise
	 */
	public boolean hasTask(long userId, long taskId){
		String queryStr = "select 1 from Task t join UserSchedule us on us.scheduleId = t.scheduleId and us.userId = "
				+ ":userId and t.id = :taskId";
		
		Query query =  getSession().createSQLQuery(queryStr);
		
		query.setLong("userId", userId);
		query.setLong("taskId", taskId);
		
		List result = query.list();
		return result.size() > 0;
	}
	
	/**
	 * Check if user has schedule by id.
	 * @return true if this combination of user and schedule exists, false otherwise 
	 */
	public boolean hasSchedule(long userId, long scheduleId){
		String queryStr = "select 1 from UserSchedule us where us.scheduleId = :scheduleId and us.userId = :userId";
		
		Query query =  getSession().createSQLQuery(queryStr);
		
		query.setLong("userId", userId);
		query.setLong("scheduleId", scheduleId);
		
		List result = query.list();
		return result.size() > 0;
	}
	
	public List<BigInteger> getDemoUsers(Long createdBefore){
		StringBuilder queryBldr = new StringBuilder("select id from User user where isDemo = true");
		if(createdBefore != null){
			queryBldr.append(" and createDate < :createDate"); 
		}
		Query query =  getSession().createSQLQuery(queryBldr.toString());
		List<String> namedParams = Arrays.asList(query.getNamedParameters());			
		
		if (namedParams.contains("createDate")) {
			query.setLong("createDate", createdBefore);
		}
				
		List result = query.list();
		return result;
	}
}

