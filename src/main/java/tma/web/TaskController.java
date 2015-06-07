package tma.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tma.domain.model.Task;
import tma.domain.model.Window;
import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.service.TaskService;
import tma.service.UserService;
import tma.util.Utils;

/**
 * Exposes task related web services. Delegates calls to {@link TaskService}. See that for information on contracts. In
 * addition, it does a check to ensure that the logged in user really owns the task or schedule being accessed to
 * prevent forgery.
 */
@RestController
public class TaskController {
	@Autowired
	private TaskService taskService;
	@Autowired
	private UserService userService;

	/**
	 * Delegates to {@link TaskService#create(Task)} 
	 */
	@RequestMapping(value = "/task/create", method = RequestMethod.POST)
	public long create(@RequestBody Task task, HttpServletRequest request) {
		authorize(request, null, task.getScheduleId());
		return taskService.create(task);
	}
	/**
	 * Delegates to {@link TaskService#read(long)} 
	 */
	@RequestMapping(value = "/task/read/{id}", method = RequestMethod.GET)
	public Task read(@PathVariable("id") long id, HttpServletRequest request) {
		authorize(request, id, null);
		Task task = taskService.read(id);
		return task;
	}
	
	private void authorize(HttpServletRequest request, Long taskId, Long scheduleId) {
		Utils.assertTrue(taskId != null || scheduleId != null);
		Long userId = (Long) request.getAttribute("userId");
		Utils.assertTrue(userId != null);

		if (scheduleId != null && !userService.hasSchedule(userId, scheduleId)) {
			throw new BadInputException(BadInput.SCHEDULE_NOT_FOUND);
		}
		if (taskId != null && !userService.hasTask(userId, taskId)) {
			throw new BadInputException(BadInput.TASK_NOT_FOUND);
		}

	}

	/**
	 * Delegates to {@link TaskService#update(Task)}
	 */
	@RequestMapping(value = "/task/update", method = RequestMethod.PUT)
	public void update(@RequestBody Task task, HttpServletRequest request) {
		authorize(request, null, task.getScheduleId());
		taskService.update(task);
	}
	/**
	 * Delegates to {@link TaskService#delete(long)}
	 */
	@RequestMapping(value = "/task/delete/{id}", method = RequestMethod.DELETE)
	public void delete(@PathVariable("id") long id, HttpServletRequest request) {
		authorize(request, id, null);
		taskService.delete(id);
	}
	/**
	 * Delegates to {@link TaskService#changeType(long)}
	 */
	@RequestMapping(value = "/task/changeType/{taskId}", method = RequestMethod.PUT)
	public void changeType(@PathVariable long taskId, HttpServletRequest request) {
		authorize(request, taskId, null);
		taskService.changeType(taskId);
	}
	/**
	 * Delegates to {@link TaskService#moveToSchedule(long, long)} 
	 */
	@RequestMapping(value = "/task/moveToSchedule/{taskId}", method = RequestMethod.PUT)
	public void moveToSchedule(@PathVariable long taskId, @RequestBody long scheduleId, HttpServletRequest request) {
		authorize(request, taskId, null);
		authorize(request, null, scheduleId);
		taskService.moveToSchedule(taskId, scheduleId);
	}
	/**
	 * Delegates to {@link TaskService#moveAfter(long, long)}
	 */
	@RequestMapping(value = "/task/moveAfter/{taskIdToMove}", method = RequestMethod.PUT)
	public void moveAfter(@PathVariable long taskIdToMove, @RequestBody long afterTaskId, HttpServletRequest request) {
		authorize(request, taskIdToMove, null);
		taskService.moveAfter(taskIdToMove, afterTaskId);
	}
	
	/**
	 * Delegates to {@link TaskService#addWindow(long, Window)}
	 */
	@RequestMapping(value = "/task/addWindow/{taskId}", method = RequestMethod.PUT)
	public void addWindow(@PathVariable long taskId, @RequestBody Window window, HttpServletRequest request) {
		authorize(request, taskId, null);
		taskService.addWindow(taskId, window);
	}
	/**
	 * Delegates to {@link TaskService#removeWindow(long, long)}
	 */
	@RequestMapping(value = "/task/removeWindow/{taskId}/{windowId}", method = RequestMethod.DELETE)
	public void removeWindow(@PathVariable long taskId, @PathVariable long windowId, HttpServletRequest request) {
		authorize(request, taskId, null);
		taskService.removeWindow(taskId, windowId);
	}
	
	/**
	 * Delegates to {@link TaskService#findConflictingTasks(long, Long, Long, boolean)} 
	 */
	@RequestMapping(value = "/task/findConflictingTasks/{scheduleId}", method = RequestMethod.GET)
	public List<Object[]> findConflictingTasks(@PathVariable long scheduleId, @RequestParam Long start,
			@RequestParam Long end, @RequestParam boolean validOnly, HttpServletRequest request) {
		authorize(request, null, scheduleId);
		return taskService.findConflictingTasks(scheduleId, start, end, validOnly);
	}
	/**
	 * Delegates to {@link TaskService#findFloatingTasksOrdered(long)}
	 */
	@RequestMapping(value = "/task/findFloatingTasksOrdered/{scheduleId}", method = RequestMethod.GET)
	public List<List<Task>> findFloatingTasksOrdered(@PathVariable long scheduleId, HttpServletRequest request) {
		authorize(request, null, scheduleId);
		return taskService.findFloatingTasksOrdered(scheduleId);
	}
	/**
	 * Delegates to {@link TaskService#findTasks(long, Long, Long, String)}
	 */
	@RequestMapping(value = "/task/findTasks/{scheduleId}", method = RequestMethod.GET)
	public List<Task> findTasks(@PathVariable long scheduleId, @RequestParam(required = false) Long start, @RequestParam(required = false) Long end,
			@RequestParam(required = false) String name, HttpServletRequest request) {
		authorize(request, null, scheduleId);
		return taskService.findTasks(scheduleId, start, end, name);
	}
	/**
	 * Delegates to {@link TaskService#autoSchedule(long, List, Long)}
	 */
	@RequestMapping(value = "/task/autoSchedule/{scheduleId}", method = RequestMethod.PUT)
	public List<Long> autoSchedule(@PathVariable long scheduleId, @RequestBody List<Long> taskIds,
			@RequestParam Long earliestStartTime, HttpServletRequest request) {
		authorize(request, null, scheduleId);
		return taskService.autoSchedule(scheduleId, taskIds, earliestStartTime);
	}
	/**
	 * Delegates to {@link TaskService#unschedule(long, List)}
	 */
	@RequestMapping(value = "/task/unschedule/{scheduleId}", method = RequestMethod.PUT)
	public void unschedule(@PathVariable long scheduleId, @RequestBody List<Long> taskIds, HttpServletRequest request) {
		authorize(request, null, scheduleId);
		taskService.unschedule(scheduleId, taskIds);
	}
	/**
	 * Delegates to {@link TaskService#invalidatePastTasks(long)}
	 */
	@RequestMapping(value = "/task/invalidatePastTasks/{scheduleId}", method = RequestMethod.PUT)
	public int invalidatePastTasks(@PathVariable long scheduleId, HttpServletRequest request) {
		authorize(request, null, scheduleId);
		return taskService.invalidatePastTasks(scheduleId);
	}

}
