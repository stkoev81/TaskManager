package tma.domain.service;

import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import tma.domain.model.Task;
import tma.domain.model.Task.SchedulingStatus;
import tma.domain.model.Task.TaskType;
import tma.test.TestUtils;
import tma.test.UnitTest;

public class TaskGrouperUTest extends UnitTest{
	@Test
	public void testGrouping(){
		Task template = new Task();
		template.setDuration(1L);
		template.setTaskType(TaskType.FLOATING);
		template.setSchedulingStatus(SchedulingStatus.UNSCHEDULED);
		
		Task[] tasks = TestUtils.createTaskObjects(template, 6);
		tasks[0].setId(0L);
		tasks[1].setId(1L);
		tasks[2].setId(2L);
		tasks[3].setId(3L);
		tasks[4].setId(4L);
		tasks[5].setId(5L);
		
		
		tasks[1].setPreviousTaskId(tasks[0].getId());
		tasks[2].setPreviousTaskId(tasks[1].getId());
		
		tasks[4].setPreviousTaskId(tasks[3].getId());
		
		List<List<Task>> lists = TaskGrouper.buildOrderedTaskGroups(Arrays.asList(tasks));
		assertEquals(3, lists.size());
		assertEquals(3, lists.get(0).size());
		assertEquals(2, lists.get(1).size());
		assertEquals(1, lists.get(2).size());
		
	}
}