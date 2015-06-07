package tma.web;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import tma.domain.model.User;
import tma.exceptions.BadInput;
import tma.exceptions.BadInputException;
import tma.service.UserService;
import tma.util.Utils;

/**
 * A filter that handles authentication and authorization. 
 */
public class SecurityFilter implements Filter {
	private UserService userService;


	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) 
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		HttpSession session = request.getSession(); 
		
		String path = request.getServletPath();  
		if(request.getPathInfo() != null){
			path += request.getPathInfo();
		}
		/*
		 * Public stuff. Don't authenticate, pass on to other filters and resources. 
		 */
		if(path.matches("/public/(.)*") || path.matches("/rs/user/create") || path.matches("/rs/user/createDemo")){
			chain.doFilter(request, response);	 
		}
		/*
		 * Login stuff. Check credentials and create session. 
		 */
		else if(path.equals("/login")){
			String username = request.getParameter("username"); 
			String password = request.getParameter("password");
			List<BadInput> errs = new LinkedList<BadInput>();
			
			if(Utils.isEmpty(username)){
				errs.add(BadInput.LOGIN_USERNAME_REQUIRED);
			}
			if(Utils.isEmpty(password)){
				errs.add(BadInput.LOGIN_PASSWORD_REQUIRED);
			}

			User user = null;
			if(errs.isEmpty()){
				user = userService.findUser(username);
				if(user == null){
					errs.add(BadInput.LOGIN_INVALID_USERNAME);
				}
			}
			
			if(errs.isEmpty()){
				if (Utils.hash(password).equals(user.getPassword())){
					session.setAttribute("userId", user.getId());
				}
				else{
					errs.add(BadInput.LOGIN_INVALID_PASSWORD);
				}
			}
			
			if(errs.isEmpty()){
				response.setStatus(HttpServletResponse.SC_OK);
			}
			else{
				throw new BadInputException(errs); 
			}
		}
		
		/*
		 * Log out stuff. Destroy session.  
		 */
		else if(path.equals("/logout")){
			session.invalidate();
			response.setStatus(HttpServletResponse.SC_OK);			
		}
		/*
		 * Private stuff. Check if logged in and allowed to access.  
		 */
		else {
			if(session.getAttribute("userId") != null){
				// if role based permissions become necessary, can be added here with an additional path check
				request.setAttribute("userId", session.getAttribute("userId"));
				chain.doFilter(request, response);
			}
			else{
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
		}
	}

	public void init(FilterConfig fConfig) throws ServletException {
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(fConfig.getServletContext());
		userService = (UserService) ctx.getBean("userService");
	}



	public void destroy() {
		userService = null;
	}

}
