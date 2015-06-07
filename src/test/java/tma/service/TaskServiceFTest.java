package tma.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tma.dao.TaskDao;
import tma.domain.model.Schedule;
import tma.domain.model.Task;
import tma.domain.model.Task.SchedulingStatus;
import tma.domain.model.Task.TaskType;
import tma.domain.model.User;
import tma.domain.model.Window;
import tma.test.FunctionalTest;
import tma.test.TestUtils;
import tma.util.Utils;

//@Ignore
public class TaskServiceFTest extends FunctionalTest {
	public static final Logger LOGGER = Logger.getLogger(TaskServiceFTest.class.getName());

	@Autowired
	private TaskService taskService;
	@Autowired
	private UserService userService;
	@Autowired
	private TaskDao taskDao;
	@Autowired
	private ScheduleService scheduleService;

	private long tempScheduleId;
	private long tempUserId;

	/**
	 * Create a "permanent" test user with a test schedule in db before running any tests. If that user already exists,
	 * delete it first. This user/schedule will be used by some of the tests and will not be deleted after the test run
	 * is over (hence permanent)
	 */
	@Test
	public void _setup() {
		User user = userService.findUser("taskServiceTestUserPerm");

		if (user != null) {
			userService.delete(user.getId());
		}

		user = new User();
		user.setUsername("taskServiceTestUserPerm");
		user.setPassword("xxxx");
		user.setEmail("example@example.com");
		Schedule schedule = new Schedule();
		schedule.setName("taskServiceTestSchedulePerm");
		user.addSchedule(schedule);
		userService.create(user);

	}
	
	/**
	 * Create a "temporary" test user with a test schedule in db before every single test. 
	 */
	@Before
	public void setup() {
		User user = userService.findUser("taskServiceTestUserTemp");
		if (user != null) {
			userService.delete(user.getId());
		}
		user = new User();
		user.setUsername("taskServiceTestUserTemp");
		user.setPassword("xxxx");
		user.setEmail("example@example.com");

		Schedule schedule = new Schedule();
		schedule.setName("taskServiceTestScheduleTemp");
		user.addSchedule(schedule);
		tempUserId = userService.create(user);
		tempScheduleId = user.getSchedules().get(0).getId();
	}

	/**
	 * Delete the temporary test user and schedule after every single test. 
	 */
	@After
	public void teardown() {
		userService.delete(tempUserId);
	}

	/**
	 * no assertions here; used to manually verify in db manually how the created task looks
	 */
	@Test
	public void testCreate() {
		Task t1 = new Task(TaskType.FLOATING);
		t1.setName("create test");
		long scheduleId = scheduleService.findSchedules("taskServiceTestSchedulePerm").get(0).getId();
		t1.setScheduleId(scheduleId);
		t1.setDuration(1L);
		taskService.create(t1);
		Long now = 0L;
		Window w1 = new Window(now - 60L, now + 60L);
		Window w2 = new Window(now + 100L, now + 300L);
		taskService.addWindow(t1.getId(), w1);
		taskService.addWindow(t1.getId(), w2);
	}

	@Test
	public void testCRUD() {

		Task t1 = new Task(TaskType.FLOATING);
		t1.setName("t1");
		t1.setScheduleId(tempScheduleId);
		t1.setDuration(1L);

		long taskId = taskService.create(t1);

		Window w1 = new Window(20L, 30L);
		Window w2 = new Window(0L, 10L);
		taskService.addWindow(taskId, w1);
		taskService.addWindow(taskId, w2);

		Task t1Read = taskService.read(taskId);
		assertTrue(t1Read.equals(t1));
		assertTrue(t1Read.getWindows().size() == 2);
		assertEquals(w1.getStart(), t1Read.getWindows().get(1).getStart());
		assertEquals(w2.getStart(), t1Read.getWindows().get(0).getStart());
		w1.setId(t1Read.getWindows().get(1).getId());
		w2.setId(t1Read.getWindows().get(0).getId());

		t1Read.setName("t1Updated");
		taskService.update(t1Read);
		t1Read = taskService.read(taskId);
		assertTrue(t1Read.getName().equals("t1Updated"));

		Window w3 = new Window(40L, 50L);
		taskService.addWindow(taskId, w3);
		t1Read = taskService.read(taskId);
		assertEquals(3, t1Read.getWindows().size());

		taskService.removeWindow(taskId, w1.getId());
		t1Read = taskService.read(taskId);
		assertEquals(2, t1Read.getWindows().size());
		assertTrue(t1Read.getWindows().get(0).equals(w2));

		taskService.delete(taskId);
		try {
			taskService.read(taskId);
			fail("Did not throw exception");
		} catch (Exception e) {
		}
	}

	@Test
	public void testWindowAddition() {

		Task t1 = new Task(TaskType.FLOATING);
		t1.setName("t1");

		// int scheduleId = scheduleService.getSchedules("taskServiceTestSchedulePerm").get(0).getId();
		t1.setScheduleId(tempScheduleId);
		t1.setDuration(1L);

		long taskId = taskService.create(t1);
		Window w1 = new Window(20L, 30L);
		Window w2 = new Window(0L, 10L);
		taskService.addWindow(taskId, w1);
		taskService.addWindow(taskId, w2);

		t1 = taskService.read(taskId);
		assertEquals(w2.getStart(), t1.getWindows().get(0).getStart());
		w2.setId(t1.getWindows().get(0).getId());

		Window w3 = new Window(5L, 8L);
		taskService.addWindow(taskId, w3);

		t1 = taskService.read(taskId);
		assertEquals(2, t1.getWindows().size()); // test consolidation
		assertEquals(w2, t1.getWindows().get(0));
		assertEquals(Long.valueOf(10L), t1.getWindows().get(0).getEnd());
		assertEquals(Long.valueOf(20L), t1.getWindows().get(1).getStart());
	}

	@Test
	public void testGetTasks() { // todo 112: add some assertions for dates.

		Task t1 = new Task();
		t1.setName("t1");

		taskService.findTasks(1, null, null, null);
	}

	@Test
	public void testMoveTaskAfter() {
		Task t1 = new Task(TaskType.FLOATING);
		t1.setName("t1");
		t1.setDuration(1L);
		t1.setScheduleId(tempScheduleId);
		taskService.create(t1);

		Task t2 = new Task(TaskType.FLOATING);
		t2.setName("t2");
		t2.setDuration(1L);
		t2.setPreviousTaskId(t1.getId());
		t2.setScheduleId(tempScheduleId);
		taskService.create(t2);

		Task t3 = new Task(TaskType.FLOATING);
		t3.setName("t3");
		t3.setDuration(1L);
		t3.setPreviousTaskId(t2.getId());
		t3.setScheduleId(tempScheduleId);
		taskService.create(t3);

		taskService.moveAfter(t3.getId(), t1.getId());
		t1 = taskService.read(t1.getId());
		t2 = taskService.read(t2.getId());
		t3 = taskService.read(t3.getId());
		assertTrue(t1.getId().equals(t3.getPreviousTaskId()));
		assertTrue(t3.getId().equals(t2.getPreviousTaskId()));
	}

	@Test
	public void testOverlapsQuery() {
		Task t1 = new Task(TaskType.FLOATING);
		t1.setName("t1");
		t1.setDuration(1L);
		t1.setScheduleId(tempScheduleId);
		t1.setStart(0L);
		t1.setDuration(2L);
		t1.setSchedulingStatus(SchedulingStatus.VALID);
		taskService.create(t1);

		Task t2 = new Task(TaskType.FLOATING);
		t2.setName("t2");
		t2.setDuration(2L);
		t2.setSchedulingStatus(SchedulingStatus.UNSCHEDULED);
		t2.setScheduleId(tempScheduleId);
		taskService.create(t2);

		assertTrue(taskService.findConflictingTasks(tempScheduleId, null, null, false).size() == 0);

		t2.setSchedulingStatus(SchedulingStatus.VALID);
		t2.setStart(3L);
		t2.setDuration(2L);
		taskService.update(t2);
		t2 = taskService.read(t2.getId());
		assertTrue(taskService.findConflictingTasks(tempScheduleId, null, null, false).size() == 0);

		t2.setStart(1L);
		t2.setDuration(2L);
		taskService.update(t2);
		assertTrue(taskService.findConflictingTasks(tempScheduleId, null, null, false).size() == 1);
	}

	private void createTasks(Task[] tasks, boolean useDao) {
		for (int i = 0; i < tasks.length; i++) {
			Task task = tasks[i];
			if (!useDao) {
				taskService.create(task);
			} else {
				task.updateDerivedFields();
				taskDao.create(task);
			}
		}
		/* why read them back: so that they can be updated without throwing optimistic lock exception */
		for (int i = 0; i < tasks.length; i++) {
			Task task = tasks[i];
			if (!useDao) { 
				tasks[i] = taskService.read(task.getId());
			} else {
				tasks[i] = taskDao.read(task.getId());
			}
		}
	}

	private void updateTasks(Task[] tasks, boolean useDao) {
		for (int i = 0; i < tasks.length; i++) {
			Task task = tasks[i];
			if (!useDao) {
				taskService.update(task);
			} else {
				task.updateDerivedFields();
				taskDao.update(task);
			}
		}
		/* why read them back: so that they can be updated without throwing  optimistic lock exception */
		for (int i = 0; i < tasks.length; i++) {
			Task task = tasks[i];
			if (!useDao) {
				tasks[i] = taskService.read(task.getId());
			} else {
				tasks[i] = taskDao.read(task.getId());
			}
		}
	}

	@Test
	public void testGetFloatingTasks() {
		Task template = new Task();
		template.setDuration(1L);
		template.setTaskType(TaskType.FLOATING);
		template.setSchedulingStatus(SchedulingStatus.UNSCHEDULED);
		template.setScheduleId(tempScheduleId);

		Task[] tasks = TestUtils.createTaskObjects(template, 3); 
		createTasks(tasks, false);

		taskService.moveAfter(tasks[0].getId(), tasks[2].getId());
		taskService.moveAfter(tasks[1].getId(), tasks[2].getId()); // 3 2 1 is the expected order.

		List<List<Task>> lists = taskService.findFloatingTasksOrdered(tempScheduleId);
		assertTrue(lists.size() == 1);
		List<Task> list = lists.get(0);
		assertTrue(list.size() == 3);
		assertTrue(list.get(0).getId().equals(tasks[2].getId()));
		assertTrue(list.get(1).getId().equals(tasks[1].getId()));
		assertTrue(list.get(2).getId().equals(tasks[0].getId()));


	}

	@Test
	public void testGetFirstAvailableSlot() {
		Task template = new Task();
		template.setDuration(1L);
		template.setTaskType(TaskType.FLOATING);
		template.setScheduleId(tempScheduleId);
		template.setStart(0L);
		template.setSchedulingStatus(SchedulingStatus.VALID);

		Task[] tasks = TestUtils.createTaskObjects(template, 3);
		createTasks(tasks, false);
		tasks[0].setStart(0L);
		tasks[1].setStart(2L);
		tasks[2].setStart(3L);
		updateTasks(tasks, false);

		long result = taskService.findFirstAvailableSlot(tempScheduleId, 0L, null, 1L);
		assertEquals(1L, result);
		result = taskService.findFirstAvailableSlot(tempScheduleId, 3L, null, 1L);
		assertEquals(4L, result);
		result = taskService.findFirstAvailableSlot(tempScheduleId, 0L, null, 2L);
		assertEquals(4L, result);
		result = taskService.findFirstAvailableSlot(tempScheduleId, -10L, null, 2L);
		assertEquals(-10L, result);
		assertEquals(null, taskService.findFirstAvailableSlot(tempScheduleId, 0L, 4L, 3L));
	}

	@Test
	public void testAutoschedule() {
		Task template = new Task();
		template.setDuration(1L);
		template.setTaskType(TaskType.FLOATING);
		template.setSchedulingStatus(SchedulingStatus.UNSCHEDULED);
		// int scheduleId = scheduleService.getSchedules("taskServiceTestSchedulePerm").get(0).getId();

		template.setScheduleId(tempScheduleId);

		Task[] tasks = TestUtils.createTaskObjects(template, 3);
		createTasks(tasks, false);

		taskService.autoSchedule(tempScheduleId, null, 0L);

		// todo 11x: add some more tests here
	}

	@Test
	public void testInvalidateBadTasks() {
		// todo 11x: create tasks that are obviously in violation and see if the methods will catch them.
		// violaitons: overlapping, windows, past, bad order.
		// for now, just testing that the method can be called.
		Task template = new Task();
		template.setDuration(1L);
		template.setTaskType(TaskType.FLOATING);
		template.setScheduleId(tempScheduleId);
		template.setStart(0L);
		template.setSchedulingStatus(SchedulingStatus.VALID);

		Task[] tasks = TestUtils.createTaskObjects(template, 3);
		createTasks(tasks, true);
		tasks[0].setStart(0L);
		tasks[1].setStart(0L);
		tasks[2].setStart(3L);
		updateTasks(tasks, true);
		assertEquals(2, taskService.invalidateBadTasks(tempScheduleId));
		assertEquals(0, taskService.invalidateBadTasks(tempScheduleId));
	}

	@Test
	public void testInvalidatePastTasks() {
		// todo 11x: create tasks that are obviously in violation and see if the methods will catch them.
		// violaitons: overlapping, windows, past, bad order.
		// for now, just testing that the method can be called.
		Task template = new Task();
		template.setDuration(1L);
		template.setTaskType(TaskType.FLOATING);
		template.setScheduleId(tempScheduleId);
		template.setStart(0L);
		template.setSchedulingStatus(SchedulingStatus.VALID);

		Task[] tasks = TestUtils.createTaskObjects(template, 2);
		createTasks(tasks, true);
		tasks[0].setStart(0L);
		tasks[1].setStart(new Date().getTime() + Utils.DAY);

		updateTasks(tasks, true);
		assertEquals(1, taskService.invalidatePastTasks(tempScheduleId));
		assertEquals(0, taskService.invalidatePastTasks(tempScheduleId));
	}

	@Test
	@Ignore
	public void testIsolation() {
		// manual test to test the transaction isolation using debugger
		// read some things
		// put breakpoint; then modify it from another transaction
		// try to commit and see if fails (just that thing; something else that was read; range query)

		Task template = new Task();
		template.setName("isolationTest");
		template.setDuration(1L);
		template.setTaskType(TaskType.FLOATING);
		template.setSchedulingStatus(SchedulingStatus.UNSCHEDULED);
		long scheduleId = scheduleService.findSchedules("taskServiceTestSchedulePerm").get(0).getId();
		template.setScheduleId(scheduleId);

		Task[] tasks = TestUtils.createTaskObjects(template, 2);
		createTasks(tasks, false);
		// taskService.doStuff(tasks); // just locks the tasks

	}

	@Test
	public void testMove() {
		Task task = new Task(TaskType.FLOATING);
		task.setDuration(1L);
		task.setName("moveTest");
		Long to = tempScheduleId;
		Long from = scheduleService.findSchedules("taskServiceTestSchedulePerm").get(0).getId();
		task.setScheduleId(from);
		taskService.create(task);
		taskService.moveToSchedule(task.getId(), to);
		task = taskService.read(task.getId());
		assertEquals(to, task.getScheduleId());
	}

	@Test
	public void testFind() {
		// see if a task with multiple windows is properly fetched eagerly, i.e. if it results in multiple db calls or
		// just one.

		Task t1 = new Task(TaskType.FLOATING);
		t1.setName("t1");

		// int scheduleId = scheduleService.getSchedules("taskServiceTestSchedulePerm").get(0).getId();
		long scheduleId = tempScheduleId;
		t1.setScheduleId(scheduleId);
		t1.setDuration(1L);

		t1.setName("taskServiceTestFindTask");
		taskService.create(t1);

		Window w1 = new Window(0L, 10L);
		Window w2 = new Window(20L, 30L);
		Window w3 = new Window(40L, 50L);
		taskService.addWindow(t1.getId(), w1);
		taskService.addWindow(t1.getId(), w2);
		taskService.addWindow(t1.getId(), w3);

		List<Task> tasks = taskDao.findTasks(scheduleId, null, null, "taskServiceTestFindTask");
		assertEquals(1, tasks.size());
		t1 = tasks.get(0);
		assertEquals(w1.getStart(), t1.getWindows().get(0).getStart());
		assertEquals(w3.getStart(), t1.getWindows().get(2).getStart());

		LOGGER.info(">> start: check # of queries in hibernate log to see if eager fetch is proeperly done");
		tasks = taskDao.findTasks(scheduleId, null, null, null);
		LOGGER.info(">> end. Used tasks:" + tasks.size());

	}

	public void z_teardown() {
	}

}
