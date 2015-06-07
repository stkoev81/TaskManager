package tma.test;

import tma.domain.model.Task;

public class TestUtils {
	
	public static Task[] createTaskObjects(Task template, int number) {
		Task[] tasks = new Task[number];
		for (int i = 0; i < number; i++) {
			Task task = new Task();
			task.setDuration(template.getDuration());
			task.setScheduleId(template.getScheduleId());
			task.setTaskType(template.getTaskType());
			task.setStart(template.getStart());
			task.setSchedulingStatus(template.getSchedulingStatus());
			if (template.getName() == null) {
				task.setName("t" + i);
			} else {
				task.setName(template.getName() + i);
			}
			tasks[i] = task;
		}
		return tasks;
	}

}
