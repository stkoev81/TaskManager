package tma.util;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.logging.Logger;

import org.junit.Test;

import tma.util.Utils;

public class UtilsUTest {
	public static final Logger LOGGER  = Logger.getLogger(UtilsUTest.class.getName());

	@Test
	public void testDateParse() {
		Date now = new Date();
		LOGGER.fine(Utils.dateToDateTimeString(now, false));
		//todo 123: add an assertion
		Date parsed = Utils.dateTimeStringToDate("4/17/14 17:38", false);
		LOGGER.fine(Utils.dateToDateTimeString(parsed, false));
	} 

}
