package tma.exceptions;

import java.util.LinkedList;
import java.util.List;

/**
 * Thrown to indicate that a method has been given inputs that are not allowed by its contract.<br/>
 * 
 * Input here is used in a very general sense. It could be method arguments, instance variables, or some data that is
 * looked up by the method. The bad input ultimately can be caused by service client input or by bugs. <br/>
 * 
 * If this exception comes out of a service method, checks should have been made to ensure that the bad input was caused
 * by client input for this call. This ensures that the exception makes sense to the service client and the client can
 * avoid the exception by providing different input. <br/>
 * 
 */
public class BadInputException extends RuntimeException implements ServiceException {

	private List<BadInput> errors;

	/**
	 * @param errors
	 *            messages about the invalid pieces of data that were encountered
	 */
	public BadInputException(List<BadInput> errors) {
		this.errors = errors;
	}

	/**
	 * @param errors
	 *            message about the invalid piece of data that was encountered
	 */
	public BadInputException(BadInput error) {
		errors = new LinkedList<BadInput>();
		errors.add(error);
	}

	public BadInputException() {
		errors = new LinkedList<BadInput>();
	}

	/**
	 * Returns a message listing the invalid pieces of data, each on a new line.
	 */
	public String getServiceExceptionMessage() {
		StringBuilder sb = new StringBuilder();
		for (BadInput error : errors) {
			sb.append(error.getValue()).append("\n");
		}
		if (sb.length() == 0) {
			sb.append(BadInput.GENERIC_MESSAGE);
		}
		return sb.toString();
	}

	public String getMessage() {
		return errors.toString();
	}

	public int getStatusCode() {
		return 400;
	}

}
