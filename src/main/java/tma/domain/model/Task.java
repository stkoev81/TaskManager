package tma.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;

import com.fasterxml.jackson.annotation.JsonIgnore;

import tma.domain.service.TaskGrouper;
import tma.exceptions.BadInputException;
import tma.exceptions.BadInput;
import tma.util.Utils;
import tma.exceptions.Assertion;

/**
 * Represents a task, i.e. something on your schedule that you have to do.
 * 
 * @see TaskType
 * @see SchedulingStatus
 **/

@Entity
public class Task extends BaseEntity {
	public static final Logger LOGGER = Logger.getLogger(Task.class.getName());

	private Long scheduleId;

	private String name;
	/*
	 * Technically, this is a one to many relationship. However, there were problems in Hibernate implementing that.
	 * 
	 * Hibernate requires that one to many one-directional use a join table, not a join column (if not, some things work
	 * but others are broken). And one to many with an order does not work with a join table (due to a unique constraint
	 * violation on element deletion; this may be a bug of Hibernate).
	 * 
	 * For this reason, a many to many mappings is used here. Application logic should still maintain the one to many
	 * constraint and remove orphans.
	 * 
	 * @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, orphanRemoval=true)
	 * 
	 * @JoinColumn(name="taskId")
	 */
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "TaskWindow", joinColumns = { @JoinColumn(name = "taskId") }, inverseJoinColumns = @JoinColumn(
			name = "windowId"))
	@OrderColumn
	private List<Window> windows = new ArrayList<Window>();
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "dependsOnTaskId")
	private Task dependency;

	private Long previousTaskId;

	private Boolean immediatelyFollowsPrevious;

	private Long duration;
	private Long start;
	@JsonIgnore
	private Long end; 
	
	
//	private Long repeat; 
//	private List<Long> repeatExclusions; 
//	private Integer repeatMonthly;
//	private boolean repeatMonthlyDayOfMonth = true; //if false, it is like "1st Friday every month"
	
	private boolean allDay = false; 
	private Long allDayOrder; 
	private boolean includeTime = true;
	
	//todo 3xx: add annotation to make it a known length. 
	private String description; 
	
	@Enumerated(EnumType.STRING)
	private TaskType taskType;

	@Enumerated(EnumType.STRING)
	private SchedulingStatus schedulingStatus;
	
	

	public enum TaskType {
		/**
		 * A fixed task is like an appointments on a calendar. It has a known start time that is set explicitly by the
		 * user.
		 */
		FIXED,
		/**
		 * A floating task has an unknown start time. Its start time is not set by the user but is managed by the
		 * application. At some point after the task is created by the user, the application assigns a start time to it
		 * (i.e. it schedules it). Later, this start time may be changed by the application again.
		 */
		FLOATING
	}

	/**
	 * Only applies to floating tasks.
	 */
	public enum SchedulingStatus {
		/**
		 * Task start time is present and obeys all constraints.
		 */
		VALID,
		/**
		 * Task has a start time but it does not obey all constraints.
		 */
		INVALID,
		/**
		 * Task does not have a start time. Application has not tried to find such a time yet.
		 */
		UNSCHEDULED,
		/**
		 * Task does not have a start time. Application tried to find such a time but could not.
		 */
		FAILED,
		/**
		 * Task has a start time, and task has been completed. Application will not try to check its validity or
		 * re-schedule it again.
		 */
		DONE
	}

	public Task() {
	}

	public Task(TaskType taskType) {
		this.taskType = taskType;
		if (TaskType.FLOATING.equals(taskType)) {
			this.schedulingStatus = SchedulingStatus.UNSCHEDULED;
		}
	}

	/**
	 * Add a new window of time during which this task can be done. Internally, windows are maintained in an ascending
	 * order and they are consolidated so that there are no overlaps. Thus, calling this method in some cases may result
	 * in 1) the window is not actually added because its period of time is already included; or 2) the addition of the
	 * window caused some existing window to be removed or resized.
	 * 
	 * @throws BadInputException
	 *             if window is null or invalid or the task is not of the floating type.
	 * @throws Assertion
	 *             if task is not valid
	 * 
	 * @return the windows that were removed due to consolidation.
	 */
	public List<Window> addWindow(Window window) {
		List<BadInput> errs = new LinkedList<BadInput>();
		if (!Task.TaskType.FLOATING.equals(taskType)) {
			errs.add(BadInput.TASK_TYPE_INVALID);
		}
		if (window == null) {
			errs.add(BadInput.WINDOW_REQUIRED);
		} else {
			errs.addAll(window.validate());
		}
		if (!errs.isEmpty()) {
			throw new BadInputException(errs);
		}
		assertValid();

		/*
		 * windows must be ordered by time
		 */
		windows.add(window);
		Comparator<Window> comp = new Comparator<Window>() {
			public int compare(Window w1, Window w2) {
				int result;
				if (w1.getStart() != null && w2.getStart() != null) {
					result = w1.getStart().compareTo(w2.getStart());
				} else if (w1.getStart() == null && w2.getStart() != null) {
					result = -1;
				} else if (w1.getStart() != null && w2.getStart() == null) {
					result = 1;
				} else {
					/*
					 * order by id just so it is ordered by something consistently 
					 * at most one of the id's can be null (if that window was just added and hasn't bee persisted yet).
					 */
					if (w1.getId() != null && w2.getId() != null) {
						result = w1.getId().compareTo(w2.getId());
					}

					else if (w1.getId() == null && w2.getId() != null) {
						result = -1;
					} else if (w1.getId() != null && w2.getId() == null) {
						result = 1;
					} else {
						Utils.assertTrue(false);
						result = 0;
					}
				}
				return result;
			}
		};
		Collections.sort(windows, comp);
		List<Window> removed = new ArrayList<Window>();

		/*
		 * windows must be consolidated.
		 */
		Window previous = null;
		ListIterator<Window> it = windows.listIterator();
		while (it.hasNext()) {
			Window current = it.next();
			if (previous != null) {
				/*
				 * merge current with previous and remove current.
				 */
				if (current.getStart() == null || previous.getEnd() == null || current.getStart() <= previous.getEnd()) {
					/*
					 *  take whatever the later end is.
					 */
					if (current.getEnd() == null || (previous.getEnd() != null && current.getEnd() > previous.getEnd())) {
						previous.setEnd(current.getEnd());
					}
					if (current != window) {
						removed.add(current);
					}
					it.remove();
				}
				/*
				 * don't merge
				 */
				else {
					previous = current;
				}
			} else {
				/*
				 *  case of first
				 */
				previous = current;
			}
		}

		return removed;
	}

	/**
	 * Determines if this task overlaps with another task.
	 * 
	 * @throws Assertion
	 *             if the following is not true: both tasks are valid and have start != null
	 * 
	 */
	public boolean overlaps(Task task) {
		Utils.assertTrue(task != null);
		Utils.assertTrue(start != null && task.start != null);
		assertValid();
		task.assertValid();

		Long t1s = this.start;
		Long t1e = this.start + this.duration;

		Long t2s = task.start;
		Long t2e = task.start + task.duration;

		if ((t1s <= t2s && t2s < t1e) || (t1s < t2e && t2e <= t1e) || (t2s <= t1s && t1s < t2e)
				|| (t2s < t1e && t1e <= t2e)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check if this task is scheduled in such a way that it fits in one of its windows.
	 * 
	 * @throws Assertion
	 *             if following not true: task is valid, it is of the floating type, and start property != null
	 */
	public boolean fitsInWindows() {
		assertValid();
		Utils.assertTrue(TaskType.FLOATING.equals(taskType));
		Utils.assertTrue(start != null);
		
		long end = start + duration;
		for (Window window : windows) {
			if (window.fits(start, end)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes the window with the given id from the task.
	 * 
	 * @throws BadInputException
	 *             if the window does not exist in this task or this task is not of floating type. 
	 */
	public void removeWindow(long windowId) {
		if (!Task.TaskType.FLOATING.equals(taskType)) {
			throw new BadInputException(BadInput.TASK_TYPE_INVALID);
		}
		int index = -1, i = 0;
		for (Window w : this.windows) {
			if (w.getId() == windowId) {
				index = i;
				break;
			}
			i++;
		}
		if (index < 0) {
			throw new BadInputException(BadInput.WINDOW_NOT_FOUND);
		}
		this.windows.remove(index);
	}

	/**
	 * Removes all windows of this task
	 */
	private void removeWindows() {
		windows.clear();
	}

	public Long getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(Long scheduleId) {
		this.scheduleId = scheduleId;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public TaskType getTaskType() {
		return taskType;
	}

	public void setTaskType(TaskType taskType) {
		this.taskType = taskType;
	}

	public Long getStart() {
		return start;
	}

	public void setStart(Long start) {
		this.start = start;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getPreviousTaskId() {
		return previousTaskId;
	}

	/**
	 * Applies to floating tasks. Used to define an order of tasks. If this is not null, when the task is scheduled by
	 * the application it will be given a time slot after the specified previous task. If null, then the time slot is
	 * not constrained by order. <br/>
	 * 
	 * By using this property, a group of ordered tasks can be specified where one follows the other an so on. See
	 * {@link TaskGrouper#buildOrderedTaskGroups(java.util.Collection)}
	 * 
	 * @param previousTaskId
	 */
	public void setPreviousTaskId(Long previousTaskId) {
		this.previousTaskId = previousTaskId;
	}

	public List<Window> getWindows() {
		return windows;
	}

	/**
	 * Windows of opportunity during which this task can be done. In general, this setter should not be used. Instead,
	 * the other provided methods for adding/removing windows should be used since they perform special operations on
	 * the windows to enforce constraints.
	 */
	public void setWindows(List<Window> windows) {
		this.windows = windows;
	}

	public SchedulingStatus getSchedulingStatus() {
		return schedulingStatus;
	}

	public void setSchedulingStatus(SchedulingStatus schedulingStatus) {
		this.schedulingStatus = schedulingStatus;
	}

	public Boolean getImmediatelyFollowsPrevious() {
		return immediatelyFollowsPrevious;
	}

	/**
	 * Only applies to floating tasks that are also ordered. If true, the task must be scheduled immediately after its
	 * previous task, not just at any time after. 
	 * 
	 * @param immediatelyFollowsPrevious
	 */
	public void setImmediatelyFollowsPrevious(Boolean immediatelyFollowsPrevious) {
		this.immediatelyFollowsPrevious = immediatelyFollowsPrevious;
	}
	


	/**
	 * A valid task follows these rules: type is populated; duration is positive, scheduleId is populated; if type is
	 * fixed, it has start, but no status and no previouisTaskId, and no windows; if type is floating, it has status; if
	 * status is valid or invalid or done, it has start, otherwise it does not.
	 */
	public List<BadInput> validate() {

		List<BadInput> errs = new LinkedList<BadInput>();
		if (taskType == null || !(taskType.equals(Task.TaskType.FIXED) || taskType.equals(Task.TaskType.FLOATING))) {
			errs.add(BadInput.TASK_TYPE_INVALID);
		}
		if (duration == null || !(duration > 0)) {
			errs.add(BadInput.TASK_DURATION_INVALID);
		}
		if (scheduleId == null) {
			errs.add(BadInput.TASK_SCHEDULE_INVALID);
		}

		if (TaskType.FIXED.equals(taskType)) {
			if (start == null) {
				errs.add(BadInput.TASK_START_INVALID);
			}
			if (schedulingStatus != null) {
				errs.add(BadInput.TASK_STATUS_INVALID);
			}
			if (previousTaskId != null) {
				errs.add(BadInput.TASK_ORDER_INVALID);
			}
			if (windows.size() > 0) {
				errs.add(BadInput.TASK_WINDOWS_NOT_ALLOWED_FOR_TYPE);
			}

		} else {
			if (schedulingStatus == null) {
				throw new BadInputException(BadInput.TASK_STATUS_INVALID);
			}

			if (SchedulingStatus.VALID.equals(schedulingStatus) || SchedulingStatus.INVALID.equals(schedulingStatus)
					|| SchedulingStatus.DONE.equals(schedulingStatus)) {
				if (start == null) {
					errs.add(BadInput.TASK_START_INVALID);
				}
			} else {
				if (start != null) {
					errs.add(BadInput.TASK_START_INVALID);
				}
			}

		}
		return errs;
	}

	/**
	 * Changes the type of this task to the opposite type (fixed vs floating). In the case of floating to fixed, some
	 * information about the task is lost (windows, order, scheduling status); also if it doesn't have start time, the
	 * current time is assigned as start.
	 * 
	 * @throws Assertion
	 *             if the task is not valid.
	 */
	public void changeType() {
		assertValid();
		if (TaskType.FLOATING.equals(taskType)) {
			removeWindows();
			taskType = TaskType.FIXED;
			if (start != null) {
				start = new Date().getTime();
			}
			schedulingStatus = null;
			immediatelyFollowsPrevious = null;
			previousTaskId = null;
		} else {
			taskType = TaskType.FLOATING;
			schedulingStatus = SchedulingStatus.VALID;
		}
	}

	public boolean isAllDay() {
		return allDay;
	}

	public void setAllDay(boolean allDay) {
		this.allDay = allDay;
	}

	public Long getAllDayOrder() {
		return allDayOrder;
	}

	public void setAllDayOrder(Long allDayOrder) {
		this.allDayOrder = allDayOrder;
	}

	public boolean isIncludeTime() {
		return includeTime;
	}

	public void setIncludeTime(boolean includeTime) {
		this.includeTime = includeTime;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Derived field from start and duration. Needed for efficient db queries.
	 */
	public Long getEnd() {
		return end;
	}

	/** update the fields that are derived from others */
	public void updateDerivedFields(){
		if (start != null && duration != null){
			end = start + duration;
		}
		else{
			end = null; 
		}
	}
	
	

}
