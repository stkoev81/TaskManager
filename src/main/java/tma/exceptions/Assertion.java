package tma.exceptions;

/**
 * Thrown in cases when some basic assumption is violated. Should not be used for validating client input. <br/>
 * 
 * It is a common practice to use the java "assert" keyword for this purpose, and to turn on the "assert" checking only
 * in development but not in production for performance reasons. However, assertions are very useful in production too
 * because they catch bugs closest to the place where they occur. This makes debugging easy and also prevents data from
 * being corrupted. Yet the "assert" keyword cannot be relied on to be always turned on depending on environment
 * configuration. In this application, throwing Assertion explicitly is favored over using the "assert" keyword. <br/>
 * 
 */
public class Assertion extends AssertionError implements ServiceException {
	public static final String MESSAGE = "An internal check failed. This "
			+ "indicates a possible bug or use of the application in " + "an unexpected way.";

	public Assertion() {
	}

	public Assertion(String message) {
		super(message);
	}

	public String getServiceExceptionMessage() {
		return MESSAGE;
	}

	public int getStatusCode() {
		return 500;
	}
}
