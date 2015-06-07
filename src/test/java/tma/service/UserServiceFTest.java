package tma.service;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tma.domain.model.Schedule;
import tma.domain.model.User;
import tma.test.FunctionalTest;

//@Ignore
public class UserServiceFTest extends FunctionalTest{
	public static final Logger LOGGER  = Logger.getLogger(UserServiceFTest.class.getName());
	
	@Autowired
	private UserService userService; 
	
	@Test
	public void testCRUD(){
		User user = userService.findUser("userServiceTestUserTemp"); 
		if(user != null){
			userService.delete(user.getId());
		}
		
		User u1 = new User();
		u1.setUsername("userServiceTestUserTemp");
		u1.setPassword("xxxx");
		u1.setEmail("example@example.com");
								
		long id = userService.create(u1);
		
		User u1Read = userService.read(id);
		
		assertTrue(u1Read.equals(u1));
		u1Read = userService.findUser("userServiceTestUserTemp");
		assertTrue(u1Read.equals(u1));
		
		u1Read.setFirstName("testName");
		userService.update(u1Read);
		u1Read = userService.read(id);
		assertTrue(u1Read.getFirstName().equals("testName"));
		
		Schedule schedule = new Schedule();
		schedule.setName("userServiceTestScheduleTemp");
		userService.addSchedule(id, schedule);
		
		u1Read = userService.read(id); 
		assertTrue(u1Read.getSchedules().size() == 1);
		assertTrue(u1Read.getSchedules().get(0).getName().equals("userServiceTestScheduleTemp"));
		schedule = u1Read.getSchedules().get(0);
		
		schedule.setName("userServiceTestScheduleTempUpdated");
		userService.updateSchedule(id, schedule);
		u1Read = userService.read(id); 
		assertTrue(u1Read.getSchedules().get(0).getName().equals("userServiceTestScheduleTempUpdated"));
		
		userService.removeSchedule(id, schedule.getId());
		u1Read = userService.read(id);
		assertTrue(u1Read.getSchedules().size() == 0);
		
		userService.delete(id);
		try{
			userService.read(id); 
			fail("Did not throw exception");
		}
		catch (Exception e){
		}
	}

	public void z_teardown(){
	}

	public void _setup() {
	}
}