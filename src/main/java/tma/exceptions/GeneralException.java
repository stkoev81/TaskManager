package tma.exceptions;

/**
 * A catch-all type of exception.
 * 
 * 
 * 
 */
public class GeneralException extends RuntimeException implements ServiceException {

	public static final String MESSAGE = "There was an internal error "
			+ "that could not be identified at this time. Try again. If problem persists, contact support.";

	public String getServiceExceptionMessage() {
		return MESSAGE;
	}

	public int getStatusCode() {
		return 500;
	}
}
