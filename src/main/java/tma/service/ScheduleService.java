package tma.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tma.dao.ScheduleDao;
import tma.dao.TaskDao;
import tma.domain.model.Schedule;

/**
 * Provides schedule related operations. 
 */
@Service
@Transactional
public class ScheduleService {
	
	@Autowired
	private ScheduleDao scheduleDao;

	/**
	 * delegates to {@link ScheduleDao#read(long)}. See that for details. 
	 */
	public Schedule read(long id){
	    return scheduleDao.read(id);
	}
	
	/**
	 * delegates to {@link ScheduleDao#findSchedules(String)}. See that for details. 
	 */
	public List<Schedule> findSchedules(String name){
		return scheduleDao.findSchedules(name);
	}
}
