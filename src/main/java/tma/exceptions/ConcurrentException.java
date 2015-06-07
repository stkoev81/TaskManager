package tma.exceptions;

/**
 * This type of exception happens if an object is modified by two processes at the same time. <br/>
 * 
 * This is is extremely unlikely in this application since optimistic locking is not maintained between service calls
 * (only during service calls), but technically is still possible.
 */
public class ConcurrentException extends RuntimeException implements ServiceException {
	public static final String MESSAGE = "The update you requested failed "
			+ "since it conflicted with another update being done at the same " + "time. Try again.";

	public String getServiceExceptionMessage() {
		return MESSAGE;
	}

	public int getStatusCode() {
		return 500;
	}

}
