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
public class CleanUp extends Script {
	public static final Logger LOGGER = Logger.getLogger(CleanUp.class.getName());
	
	@Autowired
	private DemoService demoService;
	@Autowired
	private UserService userService;

	@Test
	public void deleteDemoUsers() {
		demoService.deleteDemoUsers(null); 
	}

}
