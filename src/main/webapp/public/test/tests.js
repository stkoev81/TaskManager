/**
 * Container for all the automated js tests. 
 */
tma.reserve('tests');
tma.create('tests',{
	/**
	 * tests basic crud functionality
	 */
	crud : function(){

		var result, userId;
		var username = "wsTestUser1";
		var password = "xxxx";
		var newPassword = "yyyy";
		
		/* cleanup: delete user in case it already exists; try both passwords for this since depending on where test failed
		previously it might be either password.
		*/
		try{
			tma.ajax({url: '/login', async: false, type : "POST", data : {username: username, password : password }}, {ct : 'f'}).fail(function(x, s, e){ 
				throw e;
			});
			tma.ajax({url: '/rs/user/getId' ,  async: false, type : "GET"}, null).done(function(d, s, x){userId = d;}).fail(function(x, s, e){
				throw e;
			});
			tma.ajax({url: '/rs/user/delete/' + userId,  async: false, type : "DELETE"}, null);
		}
		catch (e){}
		try{
			tma.ajax({url: '/login', async: false, type : "POST", data : {username: username, password : newPassword }}, {ct : 'f'}).fail(function(x, s, e){ 
				throw e;
			});
			tma.ajax({url: '/rs/user/getId' ,  async: false, type : "GET"}, null).done(function(d, s, x){userId = d;}).fail(function(x, s, e){
				throw e;
			});
			tma.ajax({url: '/rs/user/delete/' + userId,  async: false, type : "DELETE"}, null);
		}
		catch(e){}
		
		/* clean up end; start test */

		result = tma.testCode("create user", {url: '/rs/user/create', type : "POST", data : {username: username, email : "a@b.c", password : password }}, null, null);
		userId = result.responseJSON;
		tma.assert(result.responseJSON != null); 

		result = tma.testCode("login", {url: '/login', type : "POST", data : {username: username, password : password }}, {ct : 'f'}, null);

		result = tma.testCode("read user", {url: '/rs/user/read/' + userId, type : "GET"}, null, null);
		tma.assert(result.responseJSON != null);

		result = tma.testCode("update user", {url: '/rs/user/update', type : "PUT", data : ({id: result.responseJSON.id, username: result.responseJSON.username, email : "c@b.d"})}, null, null);

		result = tma.testCode("read user again", {url: '/rs/user/read/' + userId, type : "GET"}, null, null);
		tma.assert(result.responseJSON != null);
		tma.assert(result.responseJSON.email == 'c@b.d');

		result = tma.testCode("add schedule", {url: '/rs/user/addSchedule/' + userId, type : "POST", data : {name : "test1", createDate : "2"}}, null, null);
		result = tma.testCode("add schedule", {url: '/rs/user/addSchedule/' + userId, type : "POST", data : {name : "test2"}}, null, null);

		result = tma.testCode("read user's schedules", {url: '/rs/user/read/' + userId, type : "GET"}, null, null);
		tma.assert(result.responseJSON != null);
		tma.assert(result.responseJSON.schedules.length == 2);
		var scheduleId = result.responseJSON.schedules[1].id;

		result = tma.testCode("update schedule", {url: '/rs/user/updateSchedule/' + userId, type : "PUT", data : {id:scheduleId, name : "test2", createDate : "1"}}, null, null);

		result = tma.testCode("read user's schedules", {url: '/rs/user/read/' + userId, type : "GET"}, null, null);
		tma.assert(result.responseJSON.schedules[1].createDate == "1");

		result = tma.testCode("remove schedule", {url: '/rs/user/removeSchedule/' + userId + "/" + scheduleId, type : "DELETE"}, null, null);

		result = tma.testCode("read user's schedules again", {url: '/rs/user/read/' + userId, type : "GET"}, null, null);
		tma.assert(result.responseJSON != null);
		tma.assert(result.responseJSON.schedules.length == 1);
		scheduleId = result.responseJSON.schedules[0].id;

		result = tma.testCode("create task", {url: '/rs/task/create', type : "POST", data : {duration: 1, name : "wsTest1", scheduleId : scheduleId, start: 3, taskType : "FIXED"}}, null, null);
		tma.assert(result.responseJSON != null); 
		var taskId = result.responseJSON;

		result = tma.testCode("read task", {url: '/rs/task/read/' + taskId , type : "GET"}, null, null);

		result = tma.testCode("change password", {url: '/rs/user/updatePassword/' + userId, type : "PUT", data : newPassword, dataType : "json"}, null, null);

		result = tma.testCode("old passsword fails", {url: '/login', type : "POST", data : {username: username, password : password }}, {ct : 'f'}, 400);

		result = tma.testCode("new passsword works", {url: '/login', type : "POST", data : {username: username, password : newPassword }}, {ct : 'f'}, null);

		result = tma.testCode("delete user", {url: '/rs/user/delete/' + userId, type : "DELETE"}, null, null);

		result = tma.testCode("logout", {url: '/logout', type : "GET"}, null, null);

		result = tma.testCode("login fails for deleted user", {url: '/login', type : "POST", data : {username: username, password : newPassword }}, {ct : 'f'}, 400);
	}
	
}); 






