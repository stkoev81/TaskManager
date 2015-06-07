
/**
 */
tma.createController('app_message', {
	//overrides
	path : ".message", 
	
	initLocal : function() {
		var con = this; 
		con.loc = {messages : {error : '', alert : '', notification : ''}, timeouts : {notification : null}};
	},
	view : function(){
		var con = this; 
		/*constant*/
		var error, alert, notification; 
		/*final*/
		var $html, $error, $alert, $notification, $blocker, $window;
		
		var blocker;
		
		initialize();
		/* why append now: append is needed to calculate sizes used later*/
		con.$root.append($html);
		showError();
		showAlert();
		showNotification();
		showBlocker();
		tma.emptyClick($html); 
		
		/* helpers */
		function initialize(){
			$html =  tma.uni($("body>.app_message")).clone().toggleClass('style_hidden'); 
			
			$error = tma.uni($('.error', $html));
			$alert = tma.uni($('.alert', $html));
			$notification = tma.uni($('.notification', $html)); 
			$blocker = tma.uni($('.blocker', $html));
			$window = $(window);
			
			error = con.loc.messages['error'];
			alert = con.loc.messages['alert'];
			notification = con.loc.messages['notification'];
			blocker = false;
			
		}
		
		function showError(){
			tma.uni($(".contents", $error)).html(fixNewline(error));
			if(error){
				blocker = true;
				$error.removeClass('style_hidden');
				tma.cclick(tma.uni($(".ack", $error)), function(e){
					con.loc.messages['error'] = ''; 
					con.load();
				});
				if(!$error.hasClass('ui-draggable')){
					$error.draggable();
				}
				$error.css('top', Math.max(($window.height() - $error.height())/2, 0) + 'px');
				$error.css('left', Math.max(($window.width() - $error.width())/2, 0) + 'px');
			}
			else{
				$error.addClass('style_hidden');	
			}
		}
		function showAlert(){
			tma.uni($(".contents", $alert)).html(fixNewline(alert));
			if(alert){
				blocker = true;
				$alert.removeClass('style_hidden');
				tma.cclick(tma.uni($(".ack", $alert)), function(e){
					con.loc.messages['alert'] = ''; 
					con.load();
				});
				if(!$alert.hasClass('ui-draggable')){
					$alert.draggable();
				}
				$alert.css('top', Math.max(($window.height() - $alert.height())/2, 0) + 'px');
				$alert.css('left', Math.max(($window.width() - $alert.width())/2, 0) + 'px');
			}
			else{
				$alert.addClass('style_hidden');	
			}
		}
		
		function showNotification(){
			tma.uni($(".contents", $notification)).html(fixNewline(notification));
			if(notification){
				$notification.removeClass('style_hidden');
				
				if(con.loc.timeouts.notification){
					window.clearTimeout(con.loc.timeouts.notification);
					con.loc.timeouts.notification = null; 
				}
				
				con.loc.timeouts.notification = window.setTimeout(function(){
					con.loc.messages['notification'] = '';
					con.load();
				}, 1000);
				
				if(!$notification.hasClass('ui-draggable')){
					$notification.draggable();
				}
				$notification.css('top', '0px');
				$notification.css('left', '0px');
				$notification.css('min-width', $window.width() + 'px');
			}
			else{
				$notification.addClass('style_hidden');	
			}
		}
		
		function showBlocker(){
			if(blocker){
				$blocker.removeClass('style_hidden');
			}
			else{
				$blocker.addClass('style_hidden');
			}
		}
		
		function fixNewline(message){
			return message.replace(/\n/g, '<br/>');
		}
		 
	}, 
	//actions
	/**
	 * Method to be called when it's necessary to display a message to the user. There are 3 kinds of messages in order of 
	 * decreasing  importance:  error, alert, and notification. The less important ones are shown in a less obtrusive way. 
	 */
	onMessage : function(message, type){
		var con = this;
		tma.assert(message && con.loc.messages[type] != null);
		if(message.charAt(message.length - 1) != '\n'){
			message += '\n';
		}
		con.loc.messages[type] += message;
		con.load(); 
	} 
}); 
