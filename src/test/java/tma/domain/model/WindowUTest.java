package tma.domain.model;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.Test;

import tma.test.UnitTest;

public class WindowUTest extends UnitTest{
	public static final Logger LOGGER  = Logger.getLogger(WindowUTest.class.getName());

	@Test
	public void testFit() {
		Long now = 0L;
		
		Window win = new Window(now, now + 60); 
		assertTrue(win.fits(now, now + 60));
		assertTrue(!win.fits(now, now + 61));
		
		win = new Window(null, null);
		assertTrue(win.fits(-1000L, 1000L));
		
	} 
	
}
	