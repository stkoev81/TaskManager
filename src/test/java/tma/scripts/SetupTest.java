package tma.scripts;

import java.util.logging.Logger;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tma.domain.model.User;
import tma.service.DemoService;
import tma.service.UserService;
import tma.test.Script;

/**
 * This is a not really a test, but it is named test so that it is executed automatically on build. 
 * It sets up a sample user in db. 
 * 
 * @author skoev
 *
 */
public class SetupTest extends Script {
	public static final Logger LOGGER = Logger.getLogger(SetupTest.class.getName());

	
	@Autowired
	private DemoService demoService;
	@Autowired
	private UserService userService;

	@Test
	public void createSetupUser() {
		User user = userService.findUser("setup");

		if (user != null) { 
			userService.delete(user.getId());
		}
		user = new User();
		user.setUsername("setup");
		user.setPassword("xxxx");
		user.setEmail("a@b.c");
		demoService.createPrepopulatedUser(user); 
	}

}
