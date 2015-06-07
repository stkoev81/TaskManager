tma.createController('app_navbar', {

	// overrides 
	active : false,
	path : '.navbar', 
	view : function(){

		var con = this;
		var $html = tma.uni($("body>.app_navbar")).clone().toggleClass('style_hidden');
		var $buttons = 	tma.uni($('.selector', $html)).children();
		tma.cclick($buttons, function(e){
			tma.app.switchSub(e.target.className);
		});
		
		$.each($buttons, function(ind, button){
			var $button = $(button);
			if ($button.hasClass(tma.app.loc.selectedSub.shortName)){
				$button.css('font-weight', 'bold');
			}
			else{
				$button.css('font-weight', 'normal');
			}
		}); 
		
		
		con.$root.append($html);
		
		
		

	}

}); 
