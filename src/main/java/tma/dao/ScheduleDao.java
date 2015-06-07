package tma.dao;

import java.util.List;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import tma.domain.model.Schedule;
import tma.exceptions.Assertion;
import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.util.Utils;

/**
 * Schedule related data access object. 
 */
@Repository
public class ScheduleDao extends BaseDao {
	/**
	 * @throws BadInputException if it is not found
	 */
	public Schedule read(long id){
		Schedule result = (Schedule) getSession().get(Schedule.class, id);
	    if(result == null){
	    	throw new BadInputException(BadInput.SCHEDULE_NOT_FOUND);
	    }
	    return result;
	}
	
	/**
	 * Checks if schedule exists by id. 
	 */
	public boolean exists(long id){
		Schedule result = (Schedule) getSession().get(Schedule.class, id);
	    return result != null; 
	}
	
	/**
	 * Finds schedules by name (case insensitive). 
	 * @throws Assertion if name is null
	 */
	public List<Schedule> findSchedules(String name){
		Utils.assertTrue(name != null);
		StringBuilder queryBldr = new StringBuilder("select {schedule.*} from Schedule schedule " );
		queryBldr.append(" where lower(schedule.name) = lower(:name)" ); 
		Query query =  getSession().createSQLQuery(queryBldr.toString())
				.addEntity("schedule", Schedule.class);
		
		
		query.setString("name", name);
		List result = query.list();
		return result;
	}
	
	


}
