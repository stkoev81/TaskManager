package tma.domain.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import tma.domain.model.Task;
import tma.exceptions.Assertion;
import tma.util.Utils;

/**
 * Contains methods for manipulating groups of tasks.
 */
public class TaskGrouper {
	/**
	 * Returns a list of groups (lists) of ordered tasks. Floating tasks can be ordered (but don't have to be) by
	 * specifying that they follow another task. This method takes a collection of floating tasks, and separates it into
	 * into a bunch of groups, where each group is ordered internally but is not ordered with respect to the other
	 * groups. In special cases, all tasks can be ordered (resulting in a single group) or none of the tasks can be
	 * ordered (resulting in single-task groups).
	 * 
	 * @throws Assertion
	 *             if any of the tasks in the list is not of floating type.
	 */
	public static List<List<Task>> buildOrderedTaskGroups(Collection<Task> tasks) {

		LinkedHashMap<Long, List<Task>> listsPointing = new LinkedHashMap<Long, List<Task>>();
		LinkedHashMap<Long, List<Task>> listsPointedTo = new LinkedHashMap<Long, List<Task>>();

		for (Task task : tasks) {
			Utils.assertTrue(Task.TaskType.FLOATING.equals(task.getTaskType()));
			Long previous = task.getPreviousTaskId();
			List<Task> listPointedTo = listsPointedTo.get(previous);
			List<Task> listPointing = listsPointing.get(task.getId());
			if (listPointedTo != null && listPointing != null) {
				listPointedTo.add(task);
				listPointedTo.addAll(listPointing);
				listsPointedTo.remove(previous);
				listsPointing.remove(task.getId());
				listsPointedTo.put(listPointedTo.get(listPointedTo.size() - 1).getId(), listPointedTo);
			}

			else if (listPointedTo != null) {
				listPointedTo.add(task);
				listsPointedTo.remove(previous);
				listsPointedTo.put(task.getId(), listPointedTo);
			} else if (listPointing != null) {
				listPointing.add(0, task);
				listsPointing.remove(task.getId());
				if (previous != null) {
					listsPointing.put(previous, listPointing);
				}
			}

			else {
				listPointedTo = new LinkedList<Task>();
				listPointedTo.add(task);
				listsPointedTo.put(task.getId(), listPointedTo);
				listPointing = listPointedTo;

				if (previous != null) {
					listsPointing.put(previous, listPointing);
				}
			}

		}
		return new LinkedList(listsPointedTo.values());

	}
}
