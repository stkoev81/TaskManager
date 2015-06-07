package tma.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tma.exceptions.BadInputException;
import tma.exceptions.GeneralException;
import tma.exceptions.ServiceException;
import tma.util.Utils;

/**
 * An error handling servlet. Should be called by the container if any servlet throws an exception. This servlet 
 * logs the exception  and returns to the client a client-friendly error message and http status code.   
 */
public class ErrorHandler extends HttpServlet {
	public static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
		Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
		String servletName = (String) request.getAttribute("javax.servlet.error.servlet_name");
		String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
		
		/*
		 * Log the exception but only if it is not a suspected client error. A client error is not noteworthy 
		 * because it's not an application error. 
		 * 
		 * todo 12x: Check on duplication of logging. Seems that the spring mvc servlet itself logs all
		 * exceptions -- need to eliminate that.
		 */
		if (!(throwable instanceof BadInputException)){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			throwable.printStackTrace(pw);
			LOGGER.severe("Calling " + requestUri + " caused the following exception \n" + sw.toString());
			pw.close();
		}
		PrintWriter out = response.getWriter();

		String message;
		/*
		 * todo 12x: check for a few other types of likely exceptions that have not been wrapped as ServiceExceptions
		 * so far but can be wrapped as such.   
		 * todo 12x: figure out if the errror page is called by the container for things like page not found, or
		 * it is just called when there is an exception in the servlet
		 */
		if (throwable instanceof ServiceException) {
			ServiceException se = (ServiceException) throwable;
			response.setStatus(se.getStatusCode());
			message = se.getServiceExceptionMessage();
		} else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			message = GeneralException.MESSAGE;
		}
		out.println(message);
	}

}