package tma.test;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Base class for functional test. Functional tests need to use the db. They are injected with resources from the spring 
 * context.     
 * 
 * Junit is not ideal for functional tests because it does not provide very good support for setting the tests. 
 * But it will do. 
 * 
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/root-context.xml")
@WebAppConfiguration("classpath:/mvc-context.xml")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class FunctionalTest {
	
	/**
	 * Workaround with name sorting of tests used to get setup and teardown behavior. Junit's @BeforeClass and 
	 * @AfterClass can't be used here because they require static method, which is incompatible with the Spring DI. 
	 *  
	 * This method should come alphabetically before all other methods, so it will be called first. It will be called only 
	 * once for the whole test run. In contrast, Junit's @Before will be called before the execution of every test method.
	 */
	public abstract void _setup();
	
	/**
	 * This method should come alphabetically after all other methods, so it will be called last. It will be called only 
	 * once for the whole test run. In contrast, Junit's @After will be called after the execution of every test method.
	 */
	public abstract void z_teardown();	
	
	
	
	
}