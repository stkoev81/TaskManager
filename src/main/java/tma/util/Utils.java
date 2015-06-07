package tma.util;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

import tma.domain.model.Schedule;
import tma.exceptions.Assertion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class Utils {
	public static final Logger LOGGER  = Logger.getLogger(Utils.class.getName());
	//4/17/14 5:38 PM
	public static final Long MINUTE = 60*1000L;
	public static final Long HOUR = 60*MINUTE;
	public static final Long DAY = 24*HOUR;
	public static final String DATE_TIME_FORMAT = "MM/dd/yy HH:mm";
	public static final String DATE_FORMAT = "MM/dd/yy";
	
	private static SimpleDateFormat getDateFormat(String pattern, boolean utc){
		SimpleDateFormat df = new SimpleDateFormat(pattern);
		TimeZone timeZone;
		if(!utc){
			timeZone = TimeZone.getDefault();
		}
		else{
			timeZone = TimeZone.getTimeZone("GMT+0");
		}
		df.setTimeZone(timeZone);
		return df;
	}
	
	public static String dateToDateTimeString(Date date, boolean utc){
		if(date == null) return ""; 
		return getDateFormat(DATE_TIME_FORMAT, utc).format(date);
	}

	public static String dateToDateString(Date date, boolean utc){
		if(date == null) return "";
		return getDateFormat(DATE_FORMAT, utc).format(date);
		
	}
	
	public static Date dateTimeStringToDate(String date, boolean utc){
		SimpleDateFormat df = getDateFormat(DATE_TIME_FORMAT, utc);
		Date result = null;
		try {
			result = df.parse(date); 
		}
		catch (Exception e){ throw new RuntimeException(e);}
		return result;
	}
	
	public static Date dateStringToDate(String date, boolean utc){
		SimpleDateFormat df = getDateFormat(DATE_FORMAT, utc);
		Date result = null;
		try {
			result = df.parse(date); 
		}
		catch (Exception e){ throw new RuntimeException(e);}
		return result;
	}
		
	
	public static Date truncateDate(Date date, boolean utc){
		return dateStringToDate(dateToDateString(date, utc), utc);
	}
		
	
	public static String toJson(Object obj){
		Gson gson = new GsonBuilder().setDateFormat(DATE_TIME_FORMAT).setPrettyPrinting().create();
		return gson.toJson(obj);
	}
	
	public static Schedule fromJson(String s){
		Gson gson = new Gson();
		Schedule schedule = gson.fromJson(s, Schedule.class);
		return schedule;
	}
	
	public static void assertTrue(boolean condition){
		assertTrue(condition, ""); 
	}
	public static void assertTrue(boolean condition, String message){
		if(!condition){
			throw new Assertion(message);
		}
	}
	
	public static String hash(String input){
		StringBuilder result = null;
		try{
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(input.getBytes());
			byte[] hashed = md.digest();
			// this conversion seems to truncate bytes, so converting to hex instead
			// result = new String(hashed, Charset.forName("US-ASCII"));
			result = new StringBuilder();
			for (byte b : hashed){
				result.append(toHex(b));
			}
		}
		catch (Exception e){
			throw new RuntimeException(e); 
		}
		// todo 133: enable the hashing of password again; temporarily disabled for debugging
//		return result.toString();
		return input;
	}
	
	public static String toHex(byte input){
		String result = String.format("%02x", input);
		return result;
	}
	public static boolean isEmpty(String string){
		return "".equals(string) || string == null;
	}
	
	public static String emptyIfNull(String string){
		if(string == null){
			return "";
		}
		else{
			return string;
		}
	}

	public static String isNull(String input, String replacementIfNull){
		if(input == null){
			return replacementIfNull;
		}
		else{
			return input;
		}
	}
	
	public static Long isNull(Long input, Long replacementIfNull){
		if(input == null){
			return replacementIfNull;
		}
		else{
			return input;
		}
	}

}
