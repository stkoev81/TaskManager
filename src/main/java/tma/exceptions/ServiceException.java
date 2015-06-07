package tma.exceptions;

/**
 * An enhanced exception type that contains easy-to-understand information about the failure that can be shown to
 * service clients. <br/>
 * 
 * The functionality of the application is exposed through services. The client using the services should not need to
 * know about how they work internally and should not be shown obscure internal exceptions. <br/>
 * 
 * All exceptions thrown by the services should be of this type, and should contain enough information to indicate to
 * client if it was the client's fault or not and if the client can correct something to avoid the failure.
 * 
 * 
 */
public interface ServiceException {
	/**
	 * Returns a message about the failure that makes sense to the service client.
	 */
	public String getServiceExceptionMessage();

	/**
	 * Returns the HTTP status code that that should be returned if this exception reaches the web layer.
	 */
	public int getStatusCode();
}
