/**
 * Top level controller for the application. 
 */
tma.createController('app', {
	// overrides 
	$root : tma.uni($("body>.root")), 
	initLocal : function() {
		var con = this; 
		con.loc = {selectedSub : null, pendingResize : null, numPendingRequests : 0};
		con.loc.selectedSub = con.subs().login; 
		con.loc.selectedSub.active = true;
	},
	view : function(){
		var con = this;
		var $html = tma.uni($("body>.app")).clone().toggleClass('style_hidden');
		
		tma.cclick($html, function(e){
			$(".popup", con.$root).remove();
		});
		$(window).resize(function(e){
			if(!$(e.target).hasClass('ui-resizable')){
				if(con.loc.pendingResize != null){
					window.clearTimeout(con.loc.pendingResize);
				}
				con.loc.pendingResize = window.setTimeout(function(){
					tma.hideDatePickers();
					con.load();
				}, 200);
			}
		});
		con.$root
		.append($html);
	}, 
	
	// actions
	/**
	 * Switches to another page
	 */
	switchSub : function(subName){
		var con = this; 
		var sub = con.subs()[subName];
		tma.assert(sub, 'no such sub');
		con.loc.selectedSub.active = false;
		con.loc.selectedSub = sub;
		sub.active = true;
		tma.hideDatePickers();
		if(subName == 'login'){
			/* there is no navbar on login page */
			tma.app.subs().navbar.active = false;
			/* going to the login does logout because model loading on login page does logout 
			 * reloading location resets memory so no private data is in browser after logout */  
			location.reload(); 
			//con.load(false, true)
		}
		else{
			return sub.load();
		}
	}, 

	onMessage : function(message, type){
		this.subs().message.onMessage(message, type); 
	}, 
	
	/**
	 * Makes a ajax call. Does some extra things like showing errors, simulating network delay (e.g. for debugging),
	 * showing hourglass while waiting for return. Should be used by all subcontrollers instead of using tma.ajax
	 * directly 
	 */
	ajax : function(jQuerySettings, addedSettings){
		var con = this; 
		showWaiting();
		/* used for simlating delays for debugging */
		var result =  tma.ajax(jQuerySettings, addedSettings)
			.then(delaySuccess, delayFailure)
			.then(success, failure);
		return result;
		
		function delaySuccess(d, s, x){
			var deferred = $.Deferred();
			 window.setTimeout(function(){
				 	deferred.resolve(d, s, x);
			 }, tma.DELAY);
			 return deferred;
		}
		
		function delayFailure(x, s, e){
			var deferred = $.Deferred();
			 window.setTimeout(function(){
				 	deferred.reject(x, s, e);
			 }, tma.DELAY);
			 return deferred;
		}
		
		function success(d, s, x){
			var deferred = $.Deferred();
			hideWaiting();
			deferred.resolve(d, s, x);
			//tma.stamp("done with" + JSON.stringify(jQuerySettings)); 
			return deferred;
		}
		function failure(x, s, e){
			hideWaiting(); 
			tma.app.onMessage(x.responseText.trim(), 'error');
		}
		
		function showWaiting(){
			con.loc.numPendingRequests = tma.limit(con.loc.numPendingRequests + 1, 0, null); 
			$('body').css('cursor', 'wait');
		}
		
		function hideWaiting(){
			con.loc.numPendingRequests = tma.limit(con.loc.numPendingRequests - 1, 0, null); 
			if(!con.loc.numPendingRequests){
				$('body').css('cursor', 'auto');
			}
		}
	}, 
	
	/**
	 * Sets the user data and switches to calendar page. 
	 */
	completeLogin : function() {
		var con = this;
		con.mod = {userId : null, scheduleId : null};
		tma.app.ajax({
				url : '/rs/user/getId',
				type : "GET"
		})
		.then(function(d) {
			con.mod.userId = d;
			return tma.app.ajax({
				url : '/rs/user/read/' + d,
				type : "GET"
			});
		}).then(function(d) {
			con.mod.user = d;
			con.mod.scheduleId = d.schedules[0].id;
			con.subs().navbar.active = true;
			return con.subs().navbar.load();
		}).then(function(){
			return con.switchSub('cal');
		}).then(function(){
			if(tma.DEMO){
				tma.app.onMessage(
					'Click to create new tasks.\n' +
					'Click on task to open.\n' +
					'Drag task to reschedule.' 
					, 'alert'); 
			}
		});
	}, 
	
	
}); 
