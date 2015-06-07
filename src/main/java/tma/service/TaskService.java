package tma.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tma.dao.ScheduleDao;
import tma.dao.TaskDao;
import tma.domain.model.Schedule;
import tma.domain.model.Task;
import tma.domain.model.Task.SchedulingStatus;
import tma.domain.model.Task.TaskType;
import tma.domain.model.Window;
import tma.domain.service.TaskGrouper;
import tma.exceptions.Assertion;
import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.util.Utils;

/**
 * Provides task related operations. <br/>
 * <br/>
 * Notes on the methods that mutate state (all public methods in this class except the ones that start with read, find,
 * or check):
 * <ul>
 * <li>
 * Locking. These methods potentially mutate something within the schedule aggregate (not necessarily the schedule
 * object, but the stuff that logically belongs to it, such as tasks). If calls on the same schedule overlap, this could
 * lead to non-repeatable reads and therefore data corruption. To prevent this, all these methods lock the schedule
 * optimistically by calling {@link ScheduleDao#lock(Object)}.</li>
 * <li>
 * Invalidation. These methods potentially cause some floating task scheduling time to become invalid (not only the task
 * being modified by the method, but also some other tasks that are dependent on it for example due to ordering). For
 * this reason, these methods call {@link #invalidateBadTasks(long)} to check for such cases and update the scheduling
 * status of the tasks.</li>
 * </ul>
 */
@Service
@Transactional
public class TaskService {

	@Autowired
	private TaskDao taskDao;
	@Autowired
	private ScheduleDao scheduleDao;

	/**
	 * Creates a new task. Task windows must be empty and task id must be null. Also, calls
	 * {@link #invalidateBadTasks(long)}.
	 * 
	 * @throws BadInputException
	 *             if task is null or task id is not null or task windows are not empty or task is not valid as defined
	 *             by {@link #checkValid(Task)}
	 */
	public long create(Task task) {
		if (task == null) {
			throw new BadInputException(BadInput.TASK_REQUIRED);
		}
		if (task.getId() != null) {
			throw new BadInputException(BadInput.OBJECT_NULL_ID_REQUIRED);
		}

		if (task.getWindows().size() > 0) {
			throw new BadInputException(BadInput.TASK_WINDOWS_NOT_ALLOWED_FOR_OPERATION);
		}
		checkValid(task);
		task.updateDerivedFields();
		lockSchedule(task.getScheduleId());
		long created = taskDao.create(task);
		invalidateBadTasks(task.getScheduleId());
		return created;
	}

	/**
	 * Moves a task to another schedule. If the task is an ordered task, takes care of fixing the order of other tasks
	 * that come after it. Also, calls {@link #invalidateBadTasks(long)}.
	 * 
	 * @throws BadInputException
	 *             if task or schedule do not exist
	 */
	public void moveToSchedule(long taskId, long newScheduleId) {
		Task task = taskDao.read(taskId);
		long scheduleId = task.getScheduleId();
		lockSchedule(scheduleId);
		lockSchedule(newScheduleId);

		Long taskOldPointee = task.getPreviousTaskId(); // could be null
		Task taskOldPointer = taskDao.findTaskAfter(taskId, scheduleId); // could be null

		if (taskOldPointer != null) {
			taskOldPointer.setPreviousTaskId(taskOldPointee);
		}
		task.setScheduleId(newScheduleId);
		if (TaskType.FLOATING.equals(task.getTaskType())) {
			task.setImmediatelyFollowsPrevious(null);
			task.setPreviousTaskId(null);
			task.setSchedulingStatus(SchedulingStatus.UNSCHEDULED);
			task.setStart(null);
		}
		invalidateBadTasks(scheduleId);
	}

	/**
	 * Updates an existing task. Any updates to task's windows are ignored, and other provided methods should be used
	 * for window manipulation. If this is a floating task and after the update does not fit in its windows, is is
	 * invalidated. Also, calls {@link #invalidateBadTasks(long)}.
	 * 
	 * @throws BadInputException
	 *             if task or task id is null or task doesn't exist or task is not valid or if any of the following
	 *             properties are being updated: task type, previousTaskId, scheduleId (other methods should be used for
	 *             updating those)
	 * @param task
	 *            contains the data to be updated to
	 */
	public void update(Task task) {
		if (task == null) {
			throw new BadInputException(BadInput.TASK_REQUIRED);
		}
		if (task.getId() == null) {
			throw new BadInputException(BadInput.OBJECT_ID_REQUIRED);
		}
		checkValid(task);
		task.updateDerivedFields();
		lockSchedule(task.getScheduleId());
		Task existing = taskDao.read(task.getId());

		/*
		 * some updates are not allowed or are ignored. The reason for this is that these updates are complex and
		 * require other tasks to be updated as well when they are done. there are dedicated methods for these updates.
		 * Instead of having to repeat logic from those methods here, just instruct client via doc and exceptions that
		 * those methods should be used instead.
		 */
		if (!task.getTaskType().equals(existing.getTaskType())
				|| !Utils.isNull(task.getPreviousTaskId(), 0L).equals(Utils.isNull(existing.getPreviousTaskId(), 0L))
				|| !task.getScheduleId().equals(existing.getScheduleId())) {
			throw new BadInputException(BadInput.OBJECT_UPDATE_NOT_ALLOWED);
		}

		task.setWindows(existing.getWindows());
		/*
		 * by doing this, we don't enforce the locking between service calls, just during a single service call.
		 */
		task.setVersion(existing.getVersion());
		/*
		 *if the duration changed, this task may have become invalid by not fitting in window
		 */
		invalidateUnfittingTask(task);
		taskDao.update(task);
		/* updating this task may have caused other tasks to be invalid */
		invalidateBadTasks(task.getScheduleId());
	}

	private void invalidateUnfittingTask(Task task) {
		if (task.getStart() != null && Task.TaskType.FLOATING.equals(task.getTaskType())) {
			if (!task.fitsInWindows()) {
				task.setSchedulingStatus(Task.SchedulingStatus.INVALID);
			}
		}
	}

	/**
	 * @return the start time of the first available time slot that is between start and end and in which duration can
	 *         fit; null if slot cannot be found
	 */
	/*
	 * todo 11x: think about if invalid tasks or done tasks should still block the availability or not.
	 */
	Long findFirstAvailableSlot(long scheduleId, long start, Long end, long duration) {
		List<Task> tasks = taskDao.findTasks(scheduleId, start, end, null);
		if (end != null && !(end - start >= duration)) {
			return null;
		}

		/*
		 * select all the tasks of which some part is in the time period; if none, then great, return the slot asap
		 */
		if (tasks.size() == 0) {
			return start;
		}

		/*
		 * if some tasks found, first consolidate them for where there are overlaps. Then, find the first gap that
		 * satisfies the req.
		 */
		Task previous = null;
		for (Task task : tasks) {
			if (previous != null) {
				Long currentEnd = task.getStart() + task.getDuration();
				Long previousEnd = previous.getStart() + previous.getDuration();
				/* slot not possible because overlaps with previous */
				if (task.getStart() <= previousEnd) {
					if (currentEnd > previousEnd) {
						previous.setDuration(currentEnd - previous.getStart());
					}
				}
				/* slot may be possible, task doesn't ovelap with previous */
				else {
					if (task.getStart() - previousEnd >= duration) {
						return previousEnd;
					}
					previous = task;
				}
			} else {
				/* case of first task */
				if (task.getStart() - start >= duration) {
					return start;
				}
				previous = new Task();
				previous.setStart(task.getStart());
				previous.setDuration(task.getDuration());
			}
		}

		/* could not find a slot in between tasks, maybe will find one after */
		Long previousEnd = previous.getStart() + previous.getDuration();
		/* slot possible */
		if (end == null || (end - previousEnd) >= duration) {
			return previousEnd;
		}
		/* slot not possible */
		else {
			return null;
		}
	}

	private boolean autoSchedule(Task task, long earliestStartTime) {
		Utils.assertTrue(task != null);
		Utils.assertTrue(Task.TaskType.FLOATING.equals(task.getTaskType()));

		long scheduleId = task.getScheduleId();

		Long previousId = task.getPreviousTaskId();
		Task previous = null;
		/* if previous task exists, must be properly scheduled. */
		if (previousId != null) {
			previous = taskDao.read(previousId);
			/* todo 11x: think if done might also be ok previous task status */
			if (Task.SchedulingStatus.VALID.equals(previous.getSchedulingStatus())) {
				earliestStartTime = previous.getStart();
			/* not valid, cannot schedule this one */	
			} else { 
				task.setSchedulingStatus(Task.SchedulingStatus.FAILED);
				return false;
			}
		}
		Long availableStart = null;
		List<Window> windows = task.getWindows();
		for (Window window : windows) {
			Long windowStart = null;
			Long windowEnd = window.getEnd();
			if (window.getStart() == null || window.getStart() < earliestStartTime) {
				windowStart = earliestStartTime;
			} else {
				windowStart = window.getStart();
			}
			availableStart = findFirstAvailableSlot(scheduleId, windowStart, windowEnd, task.getDuration());
			if (availableStart != null) {
				break;
			}
		}
		if (windows.size() == 0) {
			availableStart = findFirstAvailableSlot(scheduleId, earliestStartTime, null, task.getDuration());
		}

		if (availableStart != null) {
			if (previous != null && previous.getImmediatelyFollowsPrevious()
					&& availableStart != previous.getStart() + previous.getDuration()) {
				task.setSchedulingStatus(Task.SchedulingStatus.FAILED);
			} else {
				task.setSchedulingStatus(Task.SchedulingStatus.VALID);
				task.setStart(availableStart);
			}
		} else {
			task.setSchedulingStatus(Task.SchedulingStatus.FAILED);
		}
		return Task.SchedulingStatus.VALID.equals(task.getSchedulingStatus());

	}

	/**
	 * Request the application to schedule (i.e. assign a start time to) the given floating tasks. The start time will
	 * be such that it is after earliestStartTime and the task fits in its windows. If such a time cannot be found, the
	 * task status will be updated to failed and the task will be returned in the list of results. If some of the tasks
	 * on which this method runs are already scheduled, they will first be unscheduled.
	 * 
	 * @throws BadInputException
	 *             if schedule does not exist or any of the tasks don't exist in the schedule or if any of the tasks are
	 *             not floating type
	 * 
	 * @param scheduleId
	 *            the id of the schedule in which tasks will be found
	 * @param taskIds
	 *            if null, all floating tasks that are not already scheduled will be used
	 * @param earliestStartTime
	 *            if null, current time is used.
	 * @return the list of taskId's that were attempted to be scheduled but could not be.
	 */
	public List<Long> autoSchedule(long scheduleId, List<Long> taskIds, Long earliestStartTime) { 
		lockSchedule(scheduleId);

		if (earliestStartTime == null) {
			earliestStartTime = new Date().getTime();
		}
		List<Task> tasks = null;
		/* tasks specified */
		if (taskIds != null) {
			tasks = new ArrayList<Task>();
			for (Long taskId : taskIds) {
				Task task = taskDao.read(taskId);
				if(task.getScheduleId() != scheduleId){
					throw new BadInputException(BadInput.TASK_SCHEDULE_INVALID);
				}
				if(TaskType.FLOATING.equals(task.getTaskType())){
					throw new BadInputException(BadInput.TASK_TYPE_INVALID);
				}
				tasks.add(task);
			}
		}
		/* tasks not specified - do all that need scheduling */
		else {
			tasks = taskDao.findFloatingTasks(scheduleId, true);
		}
		/* some of they may be presently scheduled, so unschedule. */
		_unschedule(scheduleId, tasks);

		/* if a task follows another one, the first one must be scheduled first */
		List<List<Task>> taskLists = TaskGrouper.buildOrderedTaskGroups(tasks);
		List<Long> failed = new ArrayList<Long>();
		for (List<Task> taskList : taskLists) {
			for (Task task : taskList) {
				if (!autoSchedule(task, earliestStartTime)) {
					failed.add(task.getId());
				}
			}
		}
		return failed;
	}

	private void _unschedule(long scheduleId, List<Task> tasks){
		for(Task task : tasks){
			task.setStart(null);
			task.setSchedulingStatus(Task.SchedulingStatus.UNSCHEDULED);
		}
		invalidateBadTasks(scheduleId);
	}
	
	/**
	 * Unschedules the specified tasks. This involves setting start time to null and setting status to unscheduled; also
	 * calling {@link #invalidateBadTasks(long)} in case the unscheduling of these tasks affected others to become
	 * invalid (e.g. if they have an order dependency on one of these).
	 * 
	 * @throws BadInputException
	 *             if schedule does not exist or any of the tasks don't exist in the schedule or taskIds is null or if
	 *             any of the tasks are not floating type
	 */
	public void unschedule(long scheduleId, List<Long> taskIds) {
		lockSchedule(scheduleId);
		
		if(taskIds == null){
			throw new BadInputException(BadInput.GENERIC_MESSAGE);
		}
		List<Task> tasks = new ArrayList<Task>();
		for (Long taskId : taskIds) {
			Task task = taskDao.read(taskId);
			if (!Task.TaskType.FLOATING.equals(task.getTaskType())) {
				throw new BadInputException(BadInput.TASK_TYPE_INVALID);
			}
			if(task.getScheduleId() != scheduleId){
				throw new BadInputException(BadInput.TASK_SCHEDULE_INVALID);
			}
			
		}
		_unschedule(scheduleId, tasks);
	}

	/**
	 * Toggles the task to the opposite type (fixed vs floating). Delegate to {@link Task#changeType()}. See that for
	 * details. In addition to that, fixes the order of other tasks that come after it, if any. Also, calls
	 * {@link #invalidateBadTasks(long)}.
	 * 
	 * @throws BadInputException
	 *             if task does not exist.
	 */
	public void changeType(long taskId) {
		Task task = taskDao.read(taskId);
		long scheduleId = task.getScheduleId();
		lockSchedule(scheduleId);
		/* if it is ordered, then preceding and follwoing will need to be updated */
		if (Task.TaskType.FLOATING.equals(task.getTaskType())) {
			Long taskOldPointee = task.getPreviousTaskId(); // could be null
			Task taskOldPointer = taskDao.findTaskAfter(task.getId(), scheduleId); // could be null
			if (taskOldPointer != null) {
				taskOldPointer.setPreviousTaskId(taskOldPointee);
			}
		}
		task.changeType();
		invalidateBadTasks(scheduleId);
	}

	/**
	 * Confirms the task is in a valid state. Requirements: task is valid as determined by {@link Task#validate()},
	 * scheduleId property refers to an existing schedule, previousTaskId property refers to an existing task on same
	 * schedule, and no other task refers to same previousId.
	 * 
	 * @throws Assertion
	 *             if task is null.
	 * @throws BadInputException
	 *             if task is not valid as determined by {@link Task#validate()}, or scheduleId doesn't refer to an
	 *             existing schedule, or previousTaskId doesn't refers to an existing task on same schedule, or some
	 *             other task refers to same previousId.
	 */
	public void checkValid(Task task) {
		List<BadInput> errs = task.validate();
		// make sure the schedule exists
		scheduleDao.read(task.getScheduleId());
		

		if (task.getPreviousTaskId() != null) {
			Task otherTask = taskDao.findTaskAfter(task.getPreviousTaskId(), task.getScheduleId());
			if (!(otherTask == null || otherTask.equals(task))) {
				errs.add(BadInput.TASK_ORDER_NOT_AVAILABLE);
			}
			Task previousTask = taskDao.read(task.getPreviousTaskId());
			if (!(previousTask != null && previousTask.getScheduleId().equals(task.getScheduleId()))) {
				throw new BadInputException(BadInput.TASK_ORDER_INVALID);
			}
		}
		if (errs.size() > 0) {
			throw new BadInputException(errs);
		}
	}

	/**
	 * Delegates to {@link TaskDao#delete(long)}. See that for details. In addition, fixes the order of other tasks that
	 * come after it, if any, and also calls {@link #invalidateBadTasks(long)}
	 */
	public void delete(long taskId) {
		Task task = this.read(taskId);
		lockSchedule(task.getScheduleId());
		Long taskOldPointee = task.getPreviousTaskId(); // could be null
		long scheduleId = task.getScheduleId();
		Task taskOldPointer = taskDao.findTaskAfter(taskId, scheduleId); // could be null

		if (taskOldPointer != null) {
			taskOldPointer.setPreviousTaskId(taskOldPointee);
		}
		taskDao.delete(taskId);
		invalidateBadTasks(scheduleId);
	}

	/**
	 * Delegates to {@link TaskDao#read(long)}. See that for details. 
	 */
	public Task read(long taskId) {
		return taskDao.read(taskId);
	}

	/**
	 * Delegates to {@link Task#addWindow(Window)}. See that for more details and exceptions. Window id must be null; it
	 * will be assigned on creation.
	 * 
	 * @throws BadInputException
	 *             if task does not exist or window id is not null.
	 * 
	 */
	public void addWindow(long taskId, Window window) {
		Task task = this.read(taskId);
		lockSchedule(task.getScheduleId());
		if (window.getId() != null) {
			throw new BadInputException(BadInput.OBJECT_NULL_ID_REQUIRED);
		}
		/*
		 * evicting it just in case so multiple operations in the addwindow don't generate db calls, although they
		 * should not if hibernate behaves properly.
		 */
		taskDao.evict(task); 
		List<Window> removed = task.addWindow(window);
		/* this call needed because task was evicted so won't be automatically updated */
		taskDao.update(task); 
		for (Window w : removed) {
			/* clean up orphans */
			taskDao.deleteWindow(w.getId());  
		}
	}

	/**
	 * Delegates to {@link Task#removeWindow(long)}. See that for more details and exceptions. Also invalidates this
	 * task if it no longer fits in its windows calls {@link #invalidateBadTasks(long)} in case other tasks became
	 * invalid due to this.
	 * 
	 * @throws BadInputException
	 *             if task does not exist
	 */
	public void removeWindow(long taskId, long windowId) {
		Task task = this.read(taskId);
		lockSchedule(task.getScheduleId());
		task.removeWindow(windowId);
		taskDao.deleteWindow(windowId); // need to do this since no automatic orphan deletion for many to many
		// removing the window may have caused it not to fit.
		invalidateUnfittingTask(task);
		// other tasks may have become invalid
		invalidateBadTasks(task.getScheduleId());
	}

	/**
	 * Changes the order of a task by moving it (and the group of tasks that are specified to follow it, if any) after
	 * another task. If another task is already ordered after the afterTaskId, then that task becomes  ordered after the
	 * last member of the ordered group that starts with taskIdToMove. This method also calls
	 * {@link #invalidateBadTasks(long)}
	 * 
	 * @throws BadInputException
	 *             if either of the tasks does not exist or if the tasks are not in the same schedule or if both the
	 *             tasks are not floating.
	 * @see Task#setPreviousTaskId(Long)
	 */
	public void moveAfter(long taskIdToMove, long afterTaskId) {
		Task taskToMove = this.read(taskIdToMove);
		Task afterTask = this.read(afterTaskId);
		lockSchedule(taskToMove.getScheduleId());

		if(!taskToMove.getScheduleId().equals(afterTask.getScheduleId())){
			throw new BadInputException(BadInput.TASK_SCHEDULE_INVALID);
		}
		
		if(!(Task.TaskType.FLOATING.equals(taskToMove.getTaskType()) 
				&& Task.TaskType.FLOATING.equals(afterTask.getTaskType()))){
			throw new BadInputException(BadInput.TASK_TYPE_INVALID);
		}
		

		Long taskToMoveOldPointee = taskToMove.getPreviousTaskId(); // could be null
		long scheduleId = taskToMove.getScheduleId();
		Task taskToMoveOldPointer = taskDao.findTaskAfter(taskIdToMove, scheduleId); // could be null
		Task afterTaskOldPointer = taskDao.findTaskAfter(afterTaskId, scheduleId); // could be null

		taskToMove.setPreviousTaskId(afterTaskId);
		if (taskToMoveOldPointer != null) {
			taskToMoveOldPointer.setPreviousTaskId(taskToMoveOldPointee);
		}
		if (afterTaskOldPointer != null) {
			afterTaskOldPointer.setPreviousTaskId(taskToMove.getId());
		}
		invalidateBadTasks(scheduleId);
	}

	/**
	 * Delegates to {@link TaskDao#findTasks(long, Long, Long, String)}. See that for details
	 */
	public List<Task> findTasks(long scheduleId, Long start, Long end, String name) {
		return taskDao.findTasks(scheduleId, start, end, name);
	}

	
	/**
	 * Delegates to {@link TaskDao#findConflictingTasks(long, Long, Long, boolean)}. See that for details.  
	 */
	public List<Object[]> findConflictingTasks(long scheduleId, Long start, Long end, boolean validOnly) {
		return taskDao.findConflictingTasks(scheduleId, start, end, validOnly);
	}

	/**
	 * Finds flotings tasks as defined by {@link TaskDao#findFloatingTasks(long, boolean)} and groups them as 
	 * defined by {@link TaskGrouper#buildOrderedTaskGroups(Collection)}. 
	 */
	public List<List<Task>> findFloatingTasksOrdered(long scheduleId) {
		List<Task> tasks = taskDao.findFloatingTasks(scheduleId, false);
		return TaskGrouper.buildOrderedTaskGroups(tasks);
	}

	/**
	 * Finds floating tasks that are conflicting as defined by
	 * {@link TaskDao#findConflictingTasks(long, Long, Long, boolean)} and tasks that are badly ordered as defined by
	 * {@link TaskDao#findBadlyOrderedTasks(long)} and updates their status to invalid. Since the some more tasks can 
	 * become invalid as a result of the first call, keeps calling itself recursively until the call returns 0. 
	 * 
	 * @throws BadInputException
	 *             if schedule with this id does not exist.
	 * @return the number of tasks that were invalidated.
	 */
	public int invalidateBadTasks(long scheduleId) {
		lockSchedule(scheduleId);
		int count = 0;
		count += invalidateConflictingTasks(scheduleId);
		count += invalidateBadlyOrderedTasks(scheduleId, 0);
		return count;
	}

	private int invalidateConflictingTasks(long scheduleId) {
		int count = 0;
		Collection<Object[]> taskPairIds = taskDao.findConflictingTasks(scheduleId, null, null, true);
		for (Object[] taskPairId : taskPairIds) {
			count += invalidateTask(((BigInteger) taskPairId[0]).longValue());
			count += invalidateTask(((BigInteger) taskPairId[1]).longValue());
		}
		return count;

	}
	
	private int invalidateBadlyOrderedTasks(long scheduleId, int startingInvalidCount) {
		taskDao.findBadlyOrderedTasks(scheduleId);
		Collection<BigInteger> taskIds = taskDao.findBadlyOrderedTasks(startingInvalidCount);
		int singleRunCount = 0;
		for (BigInteger taskId : taskIds) {
			singleRunCount += invalidateTask(taskId.longValue());
		}
		startingInvalidCount += singleRunCount;
		if (singleRunCount > 0) {
			/*
			 * called recursively because invalidating one ordered task can cause the ivalidations of others
			 */
			invalidateBadlyOrderedTasks(scheduleId, startingInvalidCount);
		}
		return startingInvalidCount;
	}

	/**
	 * Finds floating tasks that are scheduled such that their start time is in the past and updates their status to
	 * invalid.
	 * 
	 * @param scheduleId
	 * @return
	 */
	public int invalidatePastTasks(long scheduleId) {
		lockSchedule(scheduleId);
		int count = 0;
		Collection<BigInteger> taskIds = taskDao.findTasksScheduledForPast(scheduleId, new Date().getTime());
		for (BigInteger taskId : taskIds) {
			count += invalidateTask(taskId.longValue());
		}
		return count;
	}
	
	
	private int invalidateTask(long taskId) {
		Task task = read(taskId);
		if (Task.SchedulingStatus.VALID.equals(task.getSchedulingStatus())) {
			task.setSchedulingStatus(Task.SchedulingStatus.INVALID);
			return 1;
		} else {
			return 0;
		}

	}

	/**
	 * @throws BadInputException
	 *             if schedule does not exist.
	 */
	private void lockSchedule(long scheduleId) {
		Schedule schedule = scheduleDao.read(scheduleId);
		scheduleDao.lock(schedule);
	}

}
