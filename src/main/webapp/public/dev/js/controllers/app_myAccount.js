tma.createController('app_myAccount', {

	// overrides 
	active : false, 
	path : ".page", 
	view : function(){

		var con = this;
		var user = tma.app.mod.user;
		var $html = tma.uni($("body>.app_myAccount")).clone().toggleClass('style_hidden');
		toDom();
		
		tma.cclick(tma.uni($('.submit', $html)), function(){
			var errors = fromDom(); 
			if(errors){
				tma.app.onMessage(errors, 'error');
				return;
			}
			con.updateAccount(user);
		});
		
		tma.cclick(tma.uni($('.delete', $html)), function(){
			var ok = confirm("Are you sure? Deletion cannot be undone."); 
			if(ok){
				con.deleteAccount();
			}
		});
		
		function fromDom(){
			var errors = '';
			user.firstName = tma.uni($('.firstName', $html)).val();
			user.lastName = tma.uni($('.lastName', $html)).val();
			user.email = tma.uni($('.email', $html)).val();
			user.password = tma.uni($('.password', $html)).val();
			if(user.password != tma.uni($('.repeatPassword', $html)).val()){
				errors += "Passwords don't match\n";
			}
			return errors;
		}
		
		function toDom(){
			tma.uni($('.username', $html)).append(user.username);
			tma.uni($('.firstName', $html)).val(user.firstName);
			tma.uni($('.lastName', $html)).val(user.lastName);
			tma.uni($('.email', $html)).val(user.email);
		}
		con.$root
		.append($html);
	}, 
	// actions
	/**
	 * delete account, i.e. delete user
	 */
	deleteAccount : function(){
		var con = this;
		var result = tma.app.ajax({url: '/rs/user/delete/' + tma.app.mod.userId, type : "DELETE"}, null)
		.then(function(){
			tma.app.onMessage('Account deleted', 'alert');
			return tma.app.switchSub('login');
		});
		return result;
	},
	/**
	 * update account, i.e. update user
	 */
	updateAccount : function(user){
		var con = this;
		var result = tma.app.ajax({url: '/rs/user/update', type : "PUT", data : user}, null)
		.then(function(){
			if(user.password){
				return tma.app.ajax({url: '/rs/user/updatePassword/' + user.id, type : "PUT", data : user.password}, null);
			}
			else{
				return null;
			}
		})
		.then(function(){
			tma.app.onMessage('Account updated. ' + (user.password ? '':'Password was not changed.') , 'alert');
			con.load();
		});
		return result;
	} 
}); 
