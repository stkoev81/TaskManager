package tma.exceptions;

/**
 * Enumeraton of all the allowed messages about invalid input.
 * 
 */
public enum BadInput{
	
	LOGIN_USERNAME_REQUIRED("Username required"),
	LOGIN_PASSWORD_REQUIRED("Password required"),
	LOGIN_INVALID_USERNAME("Invalid username"), 
	LOGIN_INVALID_PASSWORD("Invalid password"),
	
	 
	TASK_TYPE_INVALID("The provided task type is not valid"),
	/*
	 * todo 13x: the type conversion will not work unless the user enters the right data type, so frontend validaton
	 * is needed too.
	 */
	TASK_DURATION_INVALID("The provided task duration is not valid"),   
	TASK_SCHEDULE_INVALID("The provided schedule for a task is not valid"),
	TASK_START_INVALID("The provided task start time is not valid"), 
	TASK_STATUS_INVALID("The provided task status is not valid"), 
	TASK_ORDER_INVALID("The provided ordering for a task is not valid"),
	TASK_ORDER_NOT_AVAILABLE("The provided ordering for a task is already taken by another task"), 
	
	TASK_WINDOWS_NOT_ALLOWED_FOR_TYPE("Task cannot have windows for this task type"),
	TASK_WINDOWS_NOT_ALLOWED_FOR_OPERATION("Task cannot have windows for this operation"),
	TASK_REQUIRED("Task is required"),
	TASK_NOT_FOUND("Task was not found. It may have been deleted, or you may not be authorized to view it. " ), 
	
	
	SCHEDULE_NAME_INVALID("The provided schedule name is not valid"),
	SCHEDULE_NAME_NOT_AVAILABLE("This schedule name is already in use"),
	SCHEDULE_REQUIRED("Schedule is required"),
	SCHEDULE_NOT_FOUND("Schedule was not found. It may have been deleted, or you may not be authorized to view it. " ),
	
	USER_USERNAME_INVALID("Username does not meet requirements"),
	USER_USERNAME_UPDATE_NOT_ALLOWED("Username cannot be changed"),
	USER_USERNAME_NOT_AVAILABLE("This username is already taken"),
	USER_PASSWORD_INVALID("Password does not meet requirements"),
	USER_EMAIL_INVALID("Not a valid email address"),
	USER_SCHEDULES_NOT_ALLOWED_FOR_OPERATION("User cannot have schedules for this operation"),
	USER_NOT_FOUND("User account was not found. It may have been deleted, or you may not be authorized to view it. " ),
	USER_REQUIRED("User is required"),
	
	WINDOW_START_END_INVALID("Start/end times invalid for a window"),
	WINDOW_REQUIRED("Window is required"),
	WINDOW_NOT_FOUND("Window was not found. It may have been deleted, or you may not be authorized to view it. " ),
	
	OBJECT_NULL_ID_REQUIRED("The id of the newly added object should be null"),
	OBJECT_ID_REQUIRED("Id of the object is required"),
	OBJECT_UPDATE_NOT_ALLOWED("One or more of the updated properties of this object is not allowed to be updated. See documentation for details. "),
	
	GENERIC_MESSAGE("Incorrect usage. Plase read the documentation and try again");
	
	
	private final String value; 

	
	private BadInput(String value){
		this.value = value;
	}
	public String getValue(){
		return value;
	}
}