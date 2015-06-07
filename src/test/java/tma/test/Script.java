package tma.test;



/**
 * This is not really a test but an admin script that is used to set up/clean up things in database, etc. It is 
 * executed through Junit for convenience because Junit is already has the Spring container.  
 * 
 * This will not execute with regular Junit tests. It can be executed by: 
 * 
 * mvn surefire:test -Dtest=Class#method 
 *   
 *
 */
public abstract class Script extends FunctionalTest {
	@Override
	public void _setup() {
	}

	@Override
	public void z_teardown() {
	}
	
}