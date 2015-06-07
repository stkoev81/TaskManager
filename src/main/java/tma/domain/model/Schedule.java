package tma.domain.model;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;

import tma.exceptions.BadInput;

/**
 * Represents a schedule, which conceptually consists of a bunch of tasks. However, the tasks are not included as
 * associations of the schedule, so they are not reachable directly from the schedule. This is for performance reasons
 * as a schedule probably contains large numbers of tasks. Service methods are available elsewhere for finding the tasks
 * of a schedule.
 */
@Entity
public class Schedule extends BaseEntity {

	private String name;
	private Long createDate;

	public Schedule() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Long createDate) {
		this.createDate = createDate;
	}

	/**
	 * A valid schedule obeys the following rule: name is not null.
	 */
	public List<BadInput> validate() {
		List<BadInput> errs = new LinkedList<BadInput>();
		if (name == null) {
			errs.add(BadInput.SCHEDULE_NAME_INVALID);
		}
		return errs;
	}

}
