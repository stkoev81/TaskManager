tma.createController('app_createAccount', {

	// overrides 
	active : false, 
	path : ".page", 
	
	view : function(){

		var con = this;
		var $html = tma.uni($("body>.app_createAccount")).clone().toggleClass('style_hidden'); 
		var user = {};
		tma.cclick(tma.uni($('.login', $html)), function(e){
			tma.app.switchSub(e.target.className);
		});
		tma.cclick(tma.uni($('.submit', $html)), function(){
			var errors = fromDom(); 
			if(errors){
				tma.app.onMessage(errors, 'error');
				return;
			}
			con.create(user);
		});
		
		con.$root
		.append($html);
		
		
		function fromDom(){
			var errors = '';
			user.username = tma.uni($('.username', $html)).val();
			user.firstName = tma.uni($('.firstName', $html)).val();
			user.lastName = tma.uni($('.lastName', $html)).val();
			user.email = tma.uni($('.email', $html)).val();
			user.password = tma.uni($('.password', $html)).val();
			if(user.password != tma.uni($('.repeatPassword', $html)).val()){
				errors += "Passwords don't match\n";
			}
			return errors;
		}
	},
	// actions
	/** create a new account, i.e. a new user */
	create : function(user){
		var con = this;
		
		user.schedules = [{name : 'default'}]; 
		
		var result = tma.app.ajax({url: '/rs/user/create', type : "POST", data : user}, null)
		.then(function(){
			tma.app.onMessage('Account created', 'alert');
			con.load();
		});
		return result;
	}

	
}); 
