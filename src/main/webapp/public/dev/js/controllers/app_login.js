tma.createController('app_login', {
	// overrides 
	active : false, 
	path : ".page", 
	initModel : function(){
		var con = this;
		con.mod = {};
		/* visiting login page logs you out first */
		return tma.app.ajax({url : '/logout', type : "GET"}, null); 
	},
	
	view : function(){

		var con = this;
		/* automatic login for debugging */
		//		con.login();
		var $html = tma.uni($("body>.app_login")).clone().toggleClass('style_hidden');
		var username, password;
		tma.cclick(tma.uni($('.submit', $html)), function(){
			fromDom();
			con.login(username, password);
		});
		tma.cclick(tma.uni($('.createAccount', $html)), function(e){
			tma.app.switchSub(e.target.className);
		});
		tma.cclick(tma.uni($('.demoLogin', $html)), function(e){
			con.demoLogin();
		});
		con.$root
		.append($html);
		
		function fromDom(){
			username = tma.uni($('.username', $html)).val();
			password = tma.uni($('.password', $html)).val();
		}
	},
	//actions
	login : function(username, password){ 
		var con = this;
		tma.app.ajax({
			url : '/login',
			type : "POST",
			data : {
				/* pre-populated for debugging */
				//username : username || 'setup',
				//password : password || 'xxxx'
				username : username,
				password : password
			}
		}, {
			ct : 'f'
		}).then(function(){
			tma.app.completeLogin();
		});
	}, 
	demoLogin : function(){
		var con = this;
		var result = tma.app.ajax({url: '/rs/user/createDemo', type : "POST"}, null)
		.then(function(d){
			var creds = d.split(",");
			con.login(creds[0], creds[1]);
		});
		return result;
	}
	
}); 
