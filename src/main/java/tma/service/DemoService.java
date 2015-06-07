package tma.service;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tma.dao.UserDao;
import tma.domain.model.Schedule;
import tma.domain.model.Task;
import tma.domain.model.Task.TaskType;
import tma.domain.model.User;
import tma.util.Utils;

/**
 * Provides demo related operations.
 */
@Service
public class DemoService {
	@Autowired
	private TaskService taskService;
	@Autowired
	private UserService userService;
	@Autowired
	private ScheduleService scheduleService;
	@Autowired
	private UserDao userDao;
	
	public String createDemoUser(){
		long date = new Date().getTime(); 
		User user = new User();
		user.setIsDemo(true);
		user.setUsername("demo" + date);
		user.setPassword("xxxx");
		user.setEmail("a@b.c");
		
		createPrepopulatedUser(user);
		return user.getUsername() + "," + user.getPassword();
	}

	
	/**
	 * Creates a user pre-populated with tasks. Useful for demo and for testing. 
	 * @return
	 */
	public long createPrepopulatedUser(User user){
		final Long TODAY = Utils.truncateDate(new Date(), true).getTime(); 
		Long userId = userService.create(user);
		
		
		Schedule schedule = new Schedule();
		schedule.setName("schedule1");
		userService.addSchedule(userId, schedule);
		user = userService.read(userId);
		Long scheduleId = user.getSchedules().get(0).getId();
		
		/* some for today */
		Task task = new Task(TaskType.FIXED);
		task.setScheduleId(scheduleId);
		task.setStart(6*Utils.HOUR + TODAY);
		task.setDuration(30*Utils.MINUTE);
		task.setName("task1");
		taskService.create(task);
		
		task = new Task(TaskType.FIXED);
		task.setScheduleId(scheduleId);
		task.setStart(7*Utils.HOUR + TODAY);
		task.setDuration(1*Utils.HOUR);
		task.setName("task2");
		taskService.create(task);
		
		task = new Task(TaskType.FIXED);
		task.setScheduleId(scheduleId);
		task.setStart(TODAY);
		task.setDuration(25*Utils.HOUR);
		task.setAllDay(true);
		task.setName("task3");
		taskService.create(task);
		
		/*some for tomorrow*/
		task = new Task(TaskType.FIXED);
		task.setScheduleId(scheduleId);
		task.setStart(TODAY + Utils.DAY );
		task.setDuration(1*Utils.HOUR);
		task.setName("task4");
		task.setAllDay(true);
		taskService.create(task);
		
		task = new Task(TaskType.FIXED);
		task.setScheduleId(scheduleId);
		task.setStart(TODAY  + Utils.DAY);
		task.setDuration(2*Utils.DAY);
		task.setName("task5");
		task.setAllDay(true);
		taskService.create(task);
		
		task = new Task(TaskType.FIXED);
		task.setScheduleId(scheduleId);
		task.setStart(TODAY +  Utils.DAY);
		task.setDuration(20*Utils.DAY);
		task.setAllDay(true);
		task.setName("task6");
		taskService.create(task);
		return userId; 
		
	}
	
	
	
	@Transactional
	public void deleteDemoUsers(Long createdBefore){
		List<BigInteger> demoUsers =  userDao.getDemoUsers(createdBefore);
		for(BigInteger demoUser : demoUsers){
			userService.delete(demoUser.longValue());
		}
		 
	} 
}
