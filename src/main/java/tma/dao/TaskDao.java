package tma.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import tma.domain.model.Task;
import tma.domain.model.Window;
import tma.exceptions.Assertion;
import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.util.Utils;

/**
 * Task related data access object.
 */
@Repository
@Transactional
public class TaskDao extends BaseDao {

	/*
	 * This is needed for optimization since there can be large #'s of tasks resulting from a query. When querying for
	 * tasks, even if eager fetching is used in mapping, there will be secondary db queries, one for each task, to get
	 * the windows. To prevent this it is necessary to use a join that brings everything in one query.
	 * 
	 * The expected join query is this, but hibernate fails due to the join table mapping. Possibly a bug. "select
	 * {task.*}, {window.*} from Task task left join TaskWindow tw on task.id = tw.taskId join Window window on
	 * tw.windowId = window.id "
	 * 
	 * When using the following query, hibernate works fine.
	 */
	private static final String EAGER_FETCH_TASK = "select {task.*}, {window.*} from Task task "
			+ "left join (select * from TaskWindow tw join Window w on tw.windowId = w.id ) window on task.id = "
			+ "window.taskId  where 1=1 ";

	private static final String LAZY_FETCH_TASK = "select {task.*} from Task task where 1=1 ";

	/**
	 * @throws Assertion
	 *             if task is null
	 */
	public long create(Task task) {
		Utils.assertTrue(task != null);
		getSession().save(task);
		return task.getId();
	}

	/**
	 * Update existing task.
	 * 
	 * @throws Assertion
	 *             if task is null.
	 * @throws BadInputException
	 *             if task with this id doesn't exist
	 */
	public Task update(Task task) {
		Utils.assertTrue(task != null);
		/*
		 * make sure task exists
		 */
		read(task.getId());
		/*
		 * getSession().update throws exception if session already contains persistent instance with this id, so it is
		 * necessary to use getSession().merge instead since in the general case there might be an instance
		 */
		Task persistent = (Task) getSession().merge(task);
		return persistent;
	}

	/**
	 * @throws BadInputException
	 *             if does not exist
	 */
	public Task read(long taskId) {
		Task result = (Task) getSession().get(Task.class, taskId);
		if (result == null) {
			throw new BadInputException(BadInput.TASK_NOT_FOUND);
		}
		return result;
	}

	/**
	 * @throws BadInputException
	 *             if does not exist
	 */
	public void delete(long taskId) {
		Task task = read(taskId);
		getSession().delete(task);
	}

	/**
	 * @throws BadInputException
	 *             if does not exist
	 */
	public void deleteWindow(long windowId) {
		Window window = (Window) getSession().get(Window.class, windowId);
		if (window == null) {
			throw new BadInputException(BadInput.WINDOW_NOT_FOUND);
		}
		getSession().delete(window);
	}

	/**
	 * Finds tasks that satisfy the requirements specified by the arguments. The tasks are sorted by start date ascending. 
	 * 
	 * @param scheduleId
	 *            which schedule the tasks belong to
	 * @param start
	 *            earliest start time of the tasks. If null, it means negative infinity in time
	 * @param end
	 *            latest start time of the tasks. If null, it means positive infinity in time.
	 * @param name
	 *            the task name (case insensitive). Can have sql-style wildcard characters. If null, it means any name. 
	 * @return
	 */
	public List<Task> findTasks(long scheduleId, Long start, Long end, String name) {

		/*
		 * todo 12x: if implementing paging, the join for proper eager fetching complicates things a bit. Because if
		 * we put a limit, it would be putting it on the windows and not the tasks. This can be solved by putting the
		 * limit in a task sub-query that doesn't join window. The following limits in subqueries were tested:
		 * 
		 * "select * from Task where id in(select id from Task limit 1)" -- error message, doesn't work
		 * "select * from Task t, (select id from Task limit 0, 1) t1 where t.id = t1.id" -- works fine
		 */

		StringBuilder queryBldr = new StringBuilder(EAGER_FETCH_TASK);

		queryBldr.append(" and task.scheduleId = :scheduleId");
		if (name != null) {
			queryBldr.append(" and lower(task.name) like lower(:name)");
		}

		if (start != null) {
			if (end != null) {
				queryBldr.append(" and ((task.start >= :start and task.start < :end) or (task.end >"
						+ " :start and task.end <= :end)  or (task.start <= :start and "
						+ "task.end >= :end))");
			} else {
				queryBldr.append(" and task.end > :start");
			}
		} else {
			if (end != null) {
				queryBldr.append(" and task.start < :end");
			}
		}
		queryBldr.append(" order by task.start asc ");

		Query query = getSession().createSQLQuery(queryBldr.toString()).addEntity("task", Task.class)
				.addJoin("window", "task.windows");

		List<String> namedParams = Arrays.asList(query.getNamedParameters());

		if (namedParams.contains("scheduleId")) {
			query.setLong("scheduleId", scheduleId);
		}
		if (namedParams.contains("start")) {
			query.setLong("start", start);
		}
		if (namedParams.contains("end")) {
			query.setLong("end", end);
		}
		if (namedParams.contains("name")) {
			query.setString("name", name);
		}

		List result = query.list();
		/*
		 * using the eager fetch query causes duplicate results due to the join
		 */
		List<Task> unique = new ArrayList<Task>();
		Set<Task> set = new HashSet<Task>();
		for (Object res : result) {
			Task task = (Task) ((Object[]) res)[0];
			if (!set.contains(task)) {
				unique.add(task);
				set.add(task);
			}
		}
		return unique;
	}

	/**
	 * Finds tasks of type floating
	 * 
	 * @param scheduleId
	 *            which schedule they belong to
	 * @param onlyNeedingScheduling
	 *            whether to coniser only tasks that still need scheduling
	 */
	public List<Task> findFloatingTasks(long scheduleId, boolean onlyNeedingScheduling) {
		StringBuilder queryBldr = new StringBuilder(EAGER_FETCH_TASK);
		queryBldr.append(" and task.taskType = :floating and task.scheduleId = :scheduleId ");

		if (onlyNeedingScheduling) {
			queryBldr.append(" and task.schedulingStatus in (:unscheduled, :failed, :invalid)");
		}

		Query query = getSession().createSQLQuery(queryBldr.toString()).addEntity("task", Task.class)
				.addJoin("window", "task.windows");
		query.setLong("scheduleId", scheduleId);
		query.setString("floating", Task.TaskType.FLOATING.toString());
		List<String> namedParams = Arrays.asList(query.getNamedParameters());
		if (namedParams.contains("unscheduled")) {
			query.setString("unscheduled", Task.SchedulingStatus.UNSCHEDULED.toString());
			query.setString("failed", Task.SchedulingStatus.FAILED.toString());
			query.setString("invalid", Task.SchedulingStatus.INVALID.toString());
		}

		List result = query.list();
		/*
		 * using the eager fetch query causes duplicate results due to the join
		 */
		List<Task> unique = new ArrayList<Task>();
		Set<Task> set = new HashSet<Task>();
		for (Object res : result) {
			Task task = (Task) ((Object[]) res)[0];
			if (!set.contains(task)) {
				unique.add(task);
				set.add(task);
			}
		}
		return unique;
	}

	/**
	 * Find task that follows (i.e. is ordered after) specified task in specified schedule.
	 * 
	 * @param previousTaskId
	 * @param scheduleId
	 * @return the task found (null if nothing found). 
	 */
	public Task findTaskAfter(long previousTaskId, long scheduleId) {
		StringBuilder queryBldr = new StringBuilder("select {task.*} from Task task "
				+ " where task.taskType = :taskType and task.scheduleId = :scheduleId");
		queryBldr.append(" and task.previousTaskId = :previousTaskId");

		Query query = getSession().createSQLQuery(queryBldr.toString()).addEntity("task", Task.class);

		query.setString("taskType", "FLOATING");
		query.setLong("scheduleId", scheduleId);
		query.setLong("previousTaskId", previousTaskId);

		List<Task> result = query.list();
		if (result.size() > 1) {
			Utils.assertTrue(false, "only one task can be after another task");
			return null;
		} else if (result.size() == 1) {
			return result.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Find floating type tasks that are conflicting, i.e. they overlap in time.
	 * 
	 * @param scheduleId
	 * @param start
	 *            earliest start time of the tasks. If null, it means negative infinity in time
	 * @param end
	 *            latest end time of the tasks. If null, it means positive infinity in time
	 * @param validOnly
	 *            whether to consider only the tasks that are in valid status.
	 * @return a list of unique pairs of conflicting tasks. The list consists of object arrays of length 2, where the
	 *         objects are taskId's as BigInteger.
	 * 
	 */
	public List<Object[]> findConflictingTasks(long scheduleId, Long start, Long end, boolean validOnly) {
		/*
		 * they dont' have the same id, and they have the same schedule id, and they are over this date range, and they
		 * aren't both "FIXED".
		 */
		StringBuilder queryBldr = new StringBuilder("SELECT t1.id id1, t2.id id2  FROM  Task t1 join Task t2 on \n"
				+ "		((t1.start <= t2.start and t2.start < t1.end)\n"
				+ "		or (t1.start < t2.end and t2.end <= t1.end)\n"
				+ "		or (t2.start <= t1.start and t1.start < t2.end) \n"
				+ "		or (t2.start < t1.end and t1.end <= t2.end)\n"
				+ "		)\n" + "		and t1.id != t2.id\n" + "		and t1.scheduleId = t2.scheduleId\n"
				+ "	    and t1.scheduleId = :scheduleId "
				+ "		and not( t1.taskType = 'FIXED' and t2.taskType = 'FIXED') " + "		and t1.start <= t2.start "
				+ "		and t1.id < t2.id ");
		if (start != null) {
			if (end != null) {
				/*
				 * not checking for t2.start and t2.end. If one of the overlapping tasks is in the time range, the other
				 * will be included anyway even if it is outside the time range
				 */
				queryBldr
						.append(" and ((t1.start >= :start and t1.start < :end) or (t1.end > :start and t1.end <= :end)"
								+ "  or (t1.start <= :start and t1.end >= :end))");
			} else {
				queryBldr.append(" and t1.end > :start");
			}
		} else {
			if (end != null) {
				queryBldr.append(" and t1.start < :end");
			}
		}
		if (validOnly) {
			queryBldr.append(" and (t1.schedulingStatus = 'VALID' or t2.schedulingStatus = 'VALID')");
		}

		Query query = getSession().createSQLQuery(queryBldr.toString());

		List<String> namedParams = Arrays.asList(query.getNamedParameters());
		query.setLong("scheduleId", scheduleId);

		if (namedParams.contains("start")) {
			query.setLong("start", start);
		}
		if (namedParams.contains("end")) {
			query.setLong("end", end);
		}
		List result = query.list();
		return result;
	}

	/**
	 * Find floating tasks that are badly ordered, i.e. they are ordered after a task that is not valid, or they are
	 * supposed to be ordered after a task by they are not actually scheduled after it. 
	 * 
	 * @param scheduleId
	 * @return a list of the id's of the badly ordered tasks as BigInteger.
	 */
	public List<BigInteger> findBadlyOrderedTasks(long scheduleId) {
		/*
		 * and they dont' have the same id, and they have the same schedule id, and they are over this date range, and
		 * they aren't both "FIXED".
		 */
		StringBuilder queryBldr = new StringBuilder(
				"select t2.id id2\n"
						+ "from\n" + "    Task t1\n" + "        join\n"
						+ "    Task t2 ON t2.previousTaskId = t1.id and \n"
						+ "    (t1.schedulingStatus not in ('VALID' , 'DONE') \n"
						+ "        or t1.start = null or not t2.start >= t1.end \n"
						+ "        or (t2.immediatelyFollowsPrevious and t2.start != t1.end)\n"
						+ "    ) \n" + "    and t2.schedulingStatus = 'VALID' "
						+ "	 and t1.scheduleId = t2.scheduleId\n" + "	 and t1.scheduleId = :scheduleId ");

		Query query = getSession().createSQLQuery(queryBldr.toString());

		query.setLong("scheduleId", scheduleId);

		List result = query.list();
		return result;
	}

	/**
	 * Find floating tasks that are scheduled to start before earliestStartTime argument. 
	 * @return a list of the task id's as BigInteger. 
	 */
	public List<BigInteger> findTasksScheduledForPast(long scheduleId, long earliestStartTime) {
		// select * from Task where schedulingStatus = 'VALID' and task.start < :earliestStartTime
		StringBuilder queryBldr = new StringBuilder(
				"select id from Task where schedulingStatus = 'VALID' and start < :earliestStartTime ");
		queryBldr.append(" and scheduleId = :scheduleId ");
		Query query = getSession().createSQLQuery(queryBldr.toString());
		query.setLong("earliestStartTime", earliestStartTime);
		query.setLong("scheduleId", scheduleId);
		List result = query.list();
		return result;
	}

}
