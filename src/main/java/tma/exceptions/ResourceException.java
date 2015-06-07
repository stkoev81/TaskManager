package tma.exceptions;

/**
 * This type of exception is for errors related to resources not available. Such as db is down.
 * 
 * 
 * 
 */

public class ResourceException extends RuntimeException implements ServiceException {
	public static final String MESSAGE = "A required resource is currently "
			+ "not available. This may be a temporary condition. Try again. If problem persists, contact support.";

	public String getServiceExceptionMessage() {
		return MESSAGE;
	}

	public int getStatusCode() {
		return 500;
	}
}
