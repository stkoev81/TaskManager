package tma.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tma.dao.ScheduleDao;
import tma.dao.TaskDao;
import tma.dao.UserDao;
import tma.domain.model.Schedule;
import tma.domain.model.Task;
import tma.domain.model.User;
import tma.exceptions.Assertion;
import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.util.Utils;

/**
 * Provides user related operations.
 */

@Service
@Transactional
public class UserService {

	@Autowired
	private TaskDao taskDao;
	@Autowired
	private ScheduleDao scheduleDao;
	@Autowired
	private UserDao userDao;

	/**
	 * Creates a new user. The id of the passed in user must be null, and it will be assigned upon creation.
	 * 
	 * @throws BadInputException
	 *             if user is null or id of user is not null or if user with this username already exists or if user is
	 *             not valid as defined by {@link #confirmValid(User)}
	 * @return the id of the newly created user.
	 */
	public long create(User user) {
		if (user == null) {
			throw new BadInputException(BadInput.USER_REQUIRED);
		}
		if (user.getId() != null) {
			throw new BadInputException(BadInput.OBJECT_NULL_ID_REQUIRED);
		}
		user.setCreateDate(new Date().getTime());
		/* if any schedules are already added in user, they cannot be trusted. Removing them and re-adding them using the addSchedule
		 * method assures that the constraints are met because that method does validation */
		List<Schedule> schedules = new	ArrayList<Schedule>(user.getSchedules());
		user.removeSchedules();
		for(Schedule s : schedules){
			user.addSchedule(s);
		}
		
		user.setPassword(Utils.hash(user.getPassword()));
		if (userDao.findUser(user.getUsername()) != null) {
			throw new BadInputException(BadInput.USER_USERNAME_NOT_AVAILABLE);
		}
		confirmValid(user);
		long result = userDao.create(user);
		return result;
	}

	/**
	 * Update an existing user. Username change is not allowed. Provided password and schedules are ignored (there are
	 * other methods for changing those).
	 * 
	 * @throws Assertion
	 *             if user is null
	 * @throws BadInputException
	 *             if user or user id is null or a user with this id does not exist or if username is being changed or
	 *             if user is not valid as defined by {@link UserService#confirmValid(User)}.
	 */
	public void update(User user) {
		Utils.assertTrue(user != null);
		if (user == null) {
			throw new BadInputException(BadInput.USER_REQUIRED);
		}
		if (user.getId() == null) {
			throw new BadInputException(BadInput.OBJECT_ID_REQUIRED);
		}
		User existing = userDao.read(user.getId());
		if (!existing.getUsername().equals(user.getUsername())) {
			throw new BadInputException(BadInput.USER_USERNAME_UPDATE_NOT_ALLOWED);
		}
		user.setPassword(existing.getPassword());
		user.setSchedules(existing.getSchedules());
		/*
		 * by doing this, we don't enforce the locking between service calls, just during a single service call.
		 */
		user.setVersion(existing.getVersion());

		confirmValid(user);
		userDao.update(user);
	}

	/**
	 * @throws BadInputException
	 *             if user does not exist or if password is not valid as defined by {@link User#validate()}
	 */
	public void updatePassword(long userId, String newPassword) {
		User user = userDao.read(userId);
		user.setPassword(newPassword);
		confirmValid(user);
		/*
		 * validity check is not cleartext password but only the hashed password gets saved.
		 */
		user.setPassword(Utils.hash(newPassword));
	}

	/**
	 * delegates to {@link UserDao#read(long)}. See that for details.
	 */
	public User read(long userId) {
		User user = userDao.read(userId);
		return user;
	}

	/**
	 * Deletes user by id. Also, deletes all of the users's schedules and tasks.
	 * 
	 * @throws BadInputException
	 *             if user does not exist.
	 */
	public void delete(long userId) {
		User user = userDao.read(userId);
		/*
		 * can't iterate directly on user.getSchedules because of concurrent modification
		 */
		List<Schedule> schedules = new ArrayList<Schedule>(user.getSchedules());
		for (Schedule schedule : schedules) {
			removeSchedule(userId, schedule.getId());
		}
		userDao.delete(userId);
	}

	/**
	 * Confirms the user is in a valid state. 
	 * 
	 * @throw Assertion if user is null
	 * @throws BadInputException
	 *             if user not valid as determined by {@link User#validate()}.
	 */
	public void confirmValid(User user) {
		Utils.assertTrue(user != null);
		List<BadInput> errs = user.validate();
		if (errs.size() > 0) {
			throw new BadInputException(errs);
		}
	}

	/**
	 * Adds a schedule to a user. The schedule must have null id, and it gets assigned on addition. Delegates to
	 * {@link User#addSchedule(Schedule)} . See that for more details and exceptions.
	 * 
	 * @throws BadInputException
	 *             if the schedule is null or if its id is not null or if the user does not exist.
	 */
	public void addSchedule(long userId, Schedule schedule) {
		if (schedule.getId() != null) {
			throw new BadInputException(BadInput.OBJECT_NULL_ID_REQUIRED);
		}
		User user = userDao.read(userId);
		user.addSchedule(schedule);
	}

	/**
	 * Removes an existing schedule from user.
	 * 
	 * @throws BadInputException
	 *             if either the user or the schedule don't exist.
	 */
	public void removeSchedule(long userId, long scheduleId) {
		User user = userDao.read(userId);
		user.removeSchedule(scheduleId);
		Collection<Task> tasks = taskDao.findTasks(scheduleId, null, null, null);
		for (Task task : tasks) {
			taskDao.delete(task.getId());
		}
		userDao.deleteSchedule(scheduleId);
	}

	/**
	 * Updates an existing schedule for a user. Delegates to {@link User#updateSchedule(Schedule)}. See that for more
	 * details and exceptions.
	 * 
	 * @throws BadInputException
	 *             if user does not exist
	 */
	public void updateSchedule(long userId, Schedule schedule) {
		User user = userDao.read(userId);
		user.updateSchedule(schedule);
		/*
		 * required to merge so updating explicitly; the implicit update won't work because it does update and not merge
		 * and the schedule is already in session and that fails.
		 */
		userDao.update(user);
	}

	/**
	 * Delegates to {@link UserDao#findUser(String)}. See that for details.  
	 */
	public User findUser(String username) {
		return userDao.findUser(username);
	}

	/**
	 * Delegates to {@link UserDao#hasTask(long, long)}. See that for details. 
	 */
	public boolean hasTask(long userId, long taskId) {
		return userDao.hasTask(userId, taskId);
	}

	/**
	 * Delegates to {@link UserDao#hasSchedule(long, long)}. See that for details. 
	 */
	public boolean hasSchedule(long userId, long scheduleId) {
		return userDao.hasSchedule(userId, scheduleId);
	}

}
