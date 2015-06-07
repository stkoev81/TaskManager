package tma.domain.model;
import java.util.logging.Logger;

import org.junit.Test;

import tma.domain.model.Task.SchedulingStatus;
import tma.domain.model.Task.TaskType;
import tma.test.UnitTest;
import static org.junit.Assert.assertTrue;

public class TaskUTest extends UnitTest{
	public static final Logger LOGGER  = Logger.getLogger(TaskUTest.class.getName());

	
	@Test
	public void testFit(){
		Long now = 0L;
		Window win1 = new Window(now - 60L , now + 60L);
		Window win2 = new Window(now + 100L, now + 300L);
		
		Task task = new Task(TaskType.FLOATING);
		task.setScheduleId(0L);
		task.setStart(now);
		task.setSchedulingStatus(SchedulingStatus.VALID);
		task.setDuration(61L);
		task.addWindow(win1); 
		assertTrue(!task.fitsInWindows());
		task.addWindow(win2);
		assertTrue(!task.fitsInWindows());
		task.setStart(100L);
		assertTrue(task.fitsInWindows());
	}
}
