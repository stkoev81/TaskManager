/**
 * Application services are stateless objects that expose the functionality of the application in an easy to use manner.
 * Internally, they are simple and delegate as much of the work as possible to domain objects and data access objects.<br/>
 * 
 * The services expose general CRUD operation of domain objects and also more specific operations for manipulating
 * domain objects. All changes in the domain objects that are caused by the call are persisted (using dao's) unless
 * there is an error, in which case all changes are rolled back.<br/>
 * 
 * All service methods are transactional and use optimistic locking to ensure the domain objects remain in a consistent
 * state in database. <br/>
 * 
 * Create operations expect that there is no object id provided; they assign an id when the object is first persisted.
 * All operations ignore the version property provided. This means that the optimistic locking is performed inside
 * service calls but not across service calls.
 * 
 * @author skoev
 * 
 */
package tma.service;

