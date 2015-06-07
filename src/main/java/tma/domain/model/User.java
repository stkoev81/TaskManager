package tma.domain.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;

import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.util.Utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a user account of the application. A user has schedules and some basic information. Each schedule of the
 * user has a unique name.
 */
@Entity
public class User extends BaseEntity {
	@Column(unique = true)
	private String username;
	private String firstName;
	private String lastName;
	@JsonIgnore
	private String password;
	private String email;
	@JsonIgnore
	private Boolean isDemo; 



	/*
	 * Technically, this is a one to many relationship. However, there were problems in Hibernate implementing a one to
	 * many one-directional ordered mapping (details elsewhere) so a many to many used. Application logic should
	 * maintain the one to many constraint and remove orphans.
	 */
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "UserSchedule", joinColumns = { @JoinColumn(name = "userId") }, inverseJoinColumns = @JoinColumn(
			name = "scheduleId"))
	@OrderColumn
	private List<Schedule> schedules = new ArrayList<Schedule>();
	@JsonIgnore
	private Long createDate;

	/**
	 * Adds a new schedule to this user.
	 * 
	 * @throws BadInputException
	 *             if the schedule is null or not valid or if the user already has another schedule with the same name.
	 */
	public void addSchedule(Schedule schedule) {
		List<BadInput> errs = new LinkedList<BadInput>();
		if (schedule == null) {
			errs.add(BadInput.SCHEDULE_REQUIRED);
		}

		if (errs.isEmpty()) {
			errs.addAll(schedule.validate());
		}

		if (errs.isEmpty() && getScheduleByName(schedule.getName()) >= 0) {
			errs.add(BadInput.SCHEDULE_NAME_NOT_AVAILABLE);
		}
		if (!errs.isEmpty()) {
			throw new BadInputException(errs);
		}
		schedules.add(schedule);
	}

	/**
	 * Remove a schedule from this user
	 * 
	 * @throws BadInputException
	 *             if the schedule does not exist for this user
	 */
	public void removeSchedule(long scheduleId) {
		int index = getScheduleById(scheduleId);
		if (index < 0) {
			throw new BadInputException(BadInput.SCHEDULE_NOT_FOUND);
		}
		schedules.remove(index);
	}

	public void removeSchedules() {
		schedules.clear();
	}

	/**
	 * Update an existing schedule for this user
	 * 
	 * @throws BadInputException
	 *             if the schedule is null or its id is null or schedule does not exist for this user or it is not valid
	 *             or name property is being updated to a name that another schedule of this user already has.
	 * @param schedule
	 *            An object containing the data to be updated to.
	 */
	public void updateSchedule(Schedule schedule) {
		if (schedule == null) {
			throw new BadInputException(BadInput.SCHEDULE_REQUIRED);
		}
		if(schedule.getId() == null){
			throw new BadInputException(BadInput.OBJECT_ID_REQUIRED);
		}
		int index = getScheduleById(schedule.getId());
		if (index < 0) {
			throw new BadInputException(BadInput.SCHEDULE_NOT_FOUND);
		}

		List<BadInput> errs = schedule.validate();
		if (!errs.isEmpty()) {
			throw new BadInputException(errs);
		}
		Schedule existing = schedules.get(index);
		if (!existing.getName().equals(schedule.getName())) {
			int sameNameIndex = getScheduleByName(schedule.getName());
			if (sameNameIndex >= 0) {
				throw new BadInputException(BadInput.SCHEDULE_NAME_NOT_AVAILABLE);
			}
		}
		/*
		 * by doing this, we don't enforce the locking between service calls, just during a single service call
		 */
		schedule.setVersion(existing.getVersion());
		schedules.set(index, schedule);
	}

	private int getScheduleById(long scheduleId) {
		int index = -1, i = 0;
		for (Schedule s : this.schedules) {
			if (s.getId() == scheduleId) {
				index = i;
				break;
			}
			i++;
		}
		return index;
	}

	private int getScheduleByName(String name) {
		Utils.assertTrue(name != null);
		int index = -1, i = 0;
		for (Schedule s : this.schedules) {
			if (name.equals(s.getName())) {
				index = i;
				break;
			}
			i++;
		}
		return index;
	}

	public User() {
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@JsonIgnore
	public String getPassword() {
		return password;
	}

	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}

	public List<Schedule> getSchedules() {
		return schedules;
	}

	/**
	 * In general, this setter should not be used. Instead, the other provided methods for manipulating schedules should
	 * be used since the those methods perform special operations to enforce constraints.
	 * 
	 * @param schedules
	 */
	public void setSchedules(List<Schedule> schedules) {
		this.schedules = schedules;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * A valid user follows these rules: username is not empty and is at least 4 chars, password is not empty and is at
	 * least 4 chars, email is not empty and follows the right format for an email address.
	 */
	public List<BadInput> validate() {
		List<BadInput> errs = new LinkedList<BadInput>();
		if (username == null || username.length() < 4) {
			errs.add(BadInput.USER_USERNAME_INVALID);
		}
		if (password == null || password.length() < 4) {
			errs.add(BadInput.USER_PASSWORD_INVALID);
		}
		if (email == null || !email.matches("^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
			errs.add(BadInput.USER_EMAIL_INVALID);
		}
		return errs;
	}

	public Boolean getIsDemo() {
		return isDemo;
	}

	public void setIsDemo(Boolean isDemo) {
		this.isDemo = isDemo;
	}

	public Long getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Long createDate) {
		this.createDate = createDate;
	}
}
