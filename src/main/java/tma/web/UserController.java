package tma.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import tma.domain.model.Schedule;
import tma.domain.model.Task;
import tma.domain.model.User;
import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.service.DemoService;
import tma.service.TaskService;
import tma.service.UserService;
import tma.util.Utils;

/**
 * Exposes user related web services. Delegates calls to {@link UserService}. See that for information on contracts. In
 * addition, it does a check to ensure that the userId passed in is that of the logged in user in order to prevent
 * forgery.
 */
@RestController
public class UserController {
	@Autowired
	private UserService userService;
	@Autowired
	private DemoService demoService;
	
	
	/**
	 * Returns the id of the currently logged in user
	 */
	@RequestMapping(value = "/user/getId", method =RequestMethod.GET)
	public long getId(HttpServletRequest request){
		Long authenticatedId = (Long) request.getAttribute("userId");
		Utils.assertTrue(authenticatedId != null);
		return authenticatedId;
	}
	
	/**
	 * Delegates to {@link UserService#create(User)}
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "/user/create", method = RequestMethod.POST)
	public long create(@RequestBody User user) {
		return userService.create(user);
	}
	/**
	 * Delegates to {@link DemoService#createUser()}
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "/user/createDemo", method = RequestMethod.POST)
	public String createDemo() {
		return demoService.createDemoUser();
	}
	
	
	/**
	 * Delegates to {@link UserService#read(User)}
	 */
	@RequestMapping(value = "/user/read/{id}", method = RequestMethod.GET)
	public User read(@PathVariable("id") int id, HttpServletRequest request) {
		authorize(request, id);
		User result = userService.read(id);
		return result;
	}
	
	/**
	 * Delegates to {@link UserService#update(User)}
	 */
	@RequestMapping(value = "/user/update", method = RequestMethod.PUT)
	public void update(@RequestBody User user, HttpServletRequest request) {
		authorize(request, user.getId());
		userService.update(user);
	}
	/**
	 * Delegates to {@link UserService#delete(long)}
	 */
	@RequestMapping(value = "/user/delete/{id}", method = RequestMethod.DELETE)
	public void delete(@PathVariable("id") int id, HttpServletRequest request) {
		authorize(request, id);
		userService.delete(id);
	}

	private void authorize(HttpServletRequest request, long userId) {
		Long authenticatedId = (Long) request.getAttribute("userId");
		Utils.assertTrue(authenticatedId != null);
		if (!authenticatedId.equals(userId)) {
			throw new BadInputException(BadInput.USER_NOT_FOUND);
		}
	}
	/**
	 * Delegates to {@link UserService#addSchedule(long, Schedule)}
	 */
	@RequestMapping(value = "/user/addSchedule/{userId}", method = RequestMethod.POST)
	public void addSchedule(@PathVariable("userId") int userId, @RequestBody Schedule schedule,
			HttpServletRequest request) {
		authorize(request, userId);
		userService.addSchedule(userId, schedule);
	}
	/**
	 * Delegates to {@link UserService#updateSchedule(long, Schedule)}
	 */
	@RequestMapping(value = "/user/updateSchedule/{userId}", method = RequestMethod.PUT)
	public void updateSchedule(@PathVariable("userId") int userId, @RequestBody Schedule schedule,
			HttpServletRequest request) {
		authorize(request, userId);
		userService.updateSchedule(userId, schedule);
	}
	/**
	 * Delegates to {@link UserService#removeSchedule(long, long))}
	 */
	@RequestMapping(value = "/user/removeSchedule/{userId}/{scheduleId}", method = RequestMethod.DELETE)
	public void removeSchedule(@PathVariable("userId") int userId, @PathVariable("scheduleId") int scheduleId,
			HttpServletRequest request) {
		authorize(request, userId);
		userService.removeSchedule(userId, scheduleId);
	}
	/**
	 * Delegates to {@link UserService#updatePassword(long, String)}
	 */
	@RequestMapping(value = "/user/updatePassword/{userId}", method = RequestMethod.PUT)
	public void updatePassword(@PathVariable("userId") int userId, @RequestBody String newPassword,
			HttpServletRequest request) {
		authorize(request, userId);
		userService.updatePassword(userId, newPassword);
	}

}
