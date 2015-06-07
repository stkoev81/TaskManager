
// ajax related 
/**
 * sless, fless
 * Prepares settings for $.ajax(settings). The settings are created as follows: default settings are overridden with
 * jQuerySettings parameter and with addedSettings parameter.
 * 
 * Default settings: contentType "application/json", async true, cache false.
 * 
 * Parameters: 
 * jQuerySettings - object that can be passed to $.ajax() 
 * addedSettings - object used to figure out how to override the jQuerySettings object. Contains the following keys: 
 * 	ct - content type. Value should be 'j' for json (default) or 'f' for form. If 'j' and the request type is anything 
 * 		but 'GET', the jQuerySettings.data is stringified.
 * 	str - stringify (default is true). If false, will prevent the stringification of jQuerySettings.data described above
 * 	pre - prefix url (default is true). If true, will prefix the url with "/tma". This way urls can be specified
 * 		concisely with respect to the servlet context path, and using the prefix they will be repointed properly with respect
 * 		to the domain. This assumes that the application is deployed with a context path of "/tma".
 * 
 * todo 112: determine the context path dynamically
 * 
 */
tma.create('prepAjax', function(jQuerySettings, addedSettings) {
	addedSettings = addedSettings || {};
	var addedSettingsResult = {};

	var defaultSettings = {
		contentType : "application/json",
		async : true,
		cache : false
	};

	if (addedSettings.ct == null || addedSettings.ct == 'j') {
		addedSettingsResult.contentType = "application/json";
		if (jQuerySettings.type != null && jQuerySettings.type != 'GET' && (!(typeof jQuerySettings.data === 'string'))) {
			addedSettingsResult.data = JSON.stringify(jQuerySettings.data);
		}
	} else if (addedSettings.ct == 'f') {
		addedSettingsResult.contentType = "application/x-www-form-urlencoded; charset=utf-8";
	}

	if (!(addedSettings.str == null || addedSettings.str == true)) {
		addedSettingsResult.data = undefined;
	}

	if (addedSettings.pre == null || addedSettings.pre == true) {
		addedSettingsResult.url = "/tma" + jQuerySettings.url;
	}

	var result = $.extend({}, defaultSettings, jQuerySettings, addedSettingsResult);
	return result;
});

/**
 * fless
 * Calls $.ajax() by using tma.prepAjax first
 */
tma.create('ajax', function(jQuerySettings, addedSettings) {
	var settings = tma.prepAjax(jQuerySettings, addedSettings);
	return $.ajax(settings);
});

// date related
/**
 * sless, fless
 */
tma.create('dateToDateString', function(dateMillis, includeDayOfWeek) {
	var date = new Date(dateMillis);
	var result = (date.getUTCMonth() + 1) + "/" + date.getUTCDate() + "/" + date.getUTCFullYear();
	if(includeDayOfWeek){
		result = tma.dayOfWeek(dateMillis) + " " + result;
	}
	return result;
});

/**
 * sless, fless
 */
tma.create('dateToTimeString', function(dateMillis) {
	var date = new Date(dateMillis);
	var hours, minutes;
	hours = date.getUTCHours();
	minutes = date.getUTCMinutes();
	if (hours < 10) {
		hours = '0' + hours;
	}
	if (minutes < 10) {
		minutes = '0' + minutes;
	}
	return hours + ":" + minutes;
});

/**
 * sless, fless
 */
tma.create('dateToDateTimeString', function(dateMillis) {
	return tma.dateToDateString(dateMillis) + " " + tma.dateToTimeString(dateMillis);
});

/**
 * sless, fless
 */
tma.create('startOfDay', function(dateMillis) {
	var date = new Date(dateMillis);
	date.setUTCHours(0);
	date.setUTCMinutes(0);
	date.setUTCSeconds(0);
	date.setUTCMilliseconds(0);
	return date.getTime();
});

/**
 * sless, fless
 */
tma.create('startOfMonth', function(dateMillis){
	var date = new Date(dateMillis);
	date.setUTCDate(1); 
	date.setUTCHours(0);
	date.setUTCMinutes(0);
	date.setUTCSeconds(0);
	date.setUTCMilliseconds(0);
	return date.getTime();
});

/**
 * sless, fless
 */
tma.create('startOfWeek', function(dateMillis){
	dateMillis = tma.startOfDay(dateMillis);
	var date = new Date(dateMillis);
	dateMillis -= date.getUTCDay()*tma.DAY; 
	return dateMillis;
});

/**
 * sless, fless
 */
tma.create('firstWeekOfMonthStart', function(dateMillis){
	return tma.startOfWeek(tma.startOfMonth(dateMillis)); 
}); 

/**
 * sless, fless
 */
tma.create('endOfWeek',  function(dateMillis){
	return tma.startOfWeek(dateMillis) + tma.WEEK;
});

/**
 * sless, fless
 */
tma.create('endOfMonth', function(dateMillis){
	var date = new Date(dateMillis);
	date.setUTCDate(1); 
	date.setUTCHours(0);
	date.setUTCMinutes(0);
	date.setUTCSeconds(0);
	date.setUTCMilliseconds(0);
	
	if(date.getUTCMonth() == 11){
		date.setUTCFullYear(date.getUTCFullYear() + 1);
		date.setUTCMonth(0);
	}
	else{
		date.setUTCMonth(date.getUTCMonth() + 1);
	}
	return date.getTime();
}); 

/**
 * sless, fless
 */
tma.create('numWeeksForMonth', function(dateMillis){
	return Math.ceil((tma.lastWeekOfMonthEnd(dateMillis) - tma.firstWeekOfMonthStart(dateMillis))/tma.WEEK);
});

/**
 * sless, fless
 */
tma.create('numDays', function(start, end){
	start = tma.startOfDay(start);
	return Math.ceil((end - start)/tma.DAY);
}); 

/**
 * sless, fless
 */
tma.create('lastWeekOfMonthEnd', function(dateMillis){
	return tma.endOfWeek(tma.endOfMonth(dateMillis) - 1); 
}); 


/**
 * sless, fless
 * Converts string to date assuming utc timezone. Any timzone provided in the string is ignored.  
 * */
tma.create('dateStringToDate', function(dateString) {
	var date = new Date(dateString);
	date = new Date(date.getTime() - date.getTimezoneOffset()*60*1000);
	return date.getTime();
});

/**
 * fless
 */
tma.create('getCurrentDate', function(){
	var date = new Date();
	return date.getTime() - date.getTimezoneOffset()*60*1000;
});

/**
 * constant
 */
tma.create('MINUTE', 60*1000); 

/**
 * constant
 */
tma.create('HOUR', 60*tma.MINUTE);

/**
 * constant
 */
tma.create('DAY', 24*tma.HOUR);

/**
 * constant
 */
tma.create('WEEK', 7*tma.DAY);


/**
 * sless, fless
 * Returns true if the periods represented by the arguments overlap. p1 and p2 are objects of the type {start: ..., end: ...}. 
 * */
tma.create('periodsOverlap',  function(p1, p2){
	result = (tma.isBetween(p2.start, p1.start, p1.end, true) && tma.isBetween(p2.end, p1.start, p1.end, true)) || 
	tma.isBetween(p2.start, p1.start, p1.end, false) || 
	tma.isBetween(p2.end, p1.start, p1.end, false) ||
	(tma.isBetween(p1.start, p2.start, p2.end, true) && tma.isBetween(p1.end, p2.start, p2.end, true));
	return result;
});

/**
 * sless, fless
 */
tma.create('tasksOverlap', function(task1, task2){
	var p1 = {start : task1.start, end : task1.start + task1.duration}; 
	var p2 = {start : task2.start, end : task2.start + task2.duration}; 
	result = tma.periodsOverlap(p1, p2);
	return result;
});

 
/**
 * sless, fless 
 */
tma.create('dayOfWeek', function(dateMillis){
	var date = new Date(dateMillis); 
	var days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
	return days[date.getUTCDay()];
	
}); 

/**
 * sless, fless
 */
tma.create('monthOfYear', function(dateMillis){
	var date = new Date(dateMillis);
	var months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
	return months[date.getUTCMonth()];
});

/**
 * sless, fless
 */ 
tma.create('getYear', function(dateMillis){
	var date = new Date(dateMillis);
	return date.getUTCFullYear(); 
});

/**
 * sless, fless
 */ 
tma.create('dayOfMonth', function(dateMillis){
	var date = new Date(dateMillis);
	return date.getUTCDate();
});

/**
 * sless, fless
 */
tma.create('isAllDaySingleDay', function(task){
	return task.allDay && tma.numDays(task.start, task.start + task.duration) <= 1;
});

/**
 * sless, fless
 */
tma.create('durationToComponents', function(duration){
	tma.assert(duration != null);
	var result = {days : 0, hrs : 0, mins : 0}; 
	if(duration < 0){
		result = tma.durationToComponents(-duration);
		result.days = -result.days; 
		result.hrs = -result.hrs;
		result.mins = -result.mins;
	}
	else{
		result.days = Math.floor(duration/tma.DAY); 
		duration = duration % tma.DAY;
		result.hrs = Math.floor(duration/tma.HOUR); 
		duration = duration % tma.HOUR; 
		result.mins = Math.round(duration/tma.MINUTE);
	}
	return result; 
}); 

/**
 * sless, fless
 */
tma.create('durationFromComponents', function(days, hrs, mins){
	days = parseInt(days);  
	hrs = parseInt(hrs); 
	mins = parseInt(mins);
	var result; 
	days = isNaN(days) ? 0 : days; 
	hrs = isNaN(hrs) ? 0 : hrs; 
	mins = isNaN(mins) ? 0 : mins; 
	result = days*tma.DAY + hrs*tma.HOUR + mins*tma.MINUTE;
	return result;
});



// math related
/**
 * sless, fless
 * Cuts off a number to be withing min and max
 */
tma.create('limit', function(num, min, max){
	if(min != null && num < min){
		return min;
	}
	else if(max != null && num > max){
		return max;
	}
	else{
		return num;
	}
});


/**
 * sless, fless
 * Test if number is within inverval
 */

tma.create('isBetween', function(number, intervalStart, intervalEnd, inclusive){
	if(inclusive){
		return number >= intervalStart && number <= intervalEnd; 
	}
	else{
		return number > intervalStart && number < intervalEnd;
	}
});

/**
 * sless, fless
 * Rounding with adjustable threshold
 */
tma.create('round', function(n, threshold){
	var result; 
	if(n >= 0){
		result = Math.floor(n);
		if(n - result > threshold){
			result ++; 
		}
	}
	else{
		result = - tma.round(-n, threshold); 
	}
	return result;
});

/**
 * sless, fless
 * Rounds number to the nearest multiple of factor
 */
tma.create('snap', function(number, factor, threshold){
	threshold = threshold || 'round'; 
	switch (threshold){
		case 'round' : return Math.round(number/factor)*factor;
		case 'ceil' : return Math.ceil(number/factor)*factor;
		case 'floor' : return Math.floor(number/factor)*factor;
		default : assert(false); 
	}
});

// display related
/**
 * sless, fless
 * takes position on document and returns position on screen. These are the same if scrolled to to the top left corner, 
 * but different otherwise. 
 */
tma.create('positionOnScreen', function(positionOnDocument){
	var result = {left: null, top: null};
	var $html = tma.uni($('html'));
	result.left = positionOnDocument.left - $html.scrollLeft();
	result.top = positionOnDocument.top - $html.scrollTop();
	return result;
}); 


/**
 * sless, fless
 */
tma.create('subtractPosition', function(position1, position2){
	var result = {left: null, top: null};
	result.left = position1.left - position2.left; 
	result.top = position1.top - position2.top;
	return result;
}); 	

/**
 * sless, fless
 * takes a position of a click on screen and dimensions of a div. Returns the position on screen such that the box is 
 * as close as possible to the click without any part of it being hidden.  
 */

tma.create('stayOnScreen', function(position, dimensions){
	var result = {left : null, top : null}; 
	$window = $(window);
	dimensions.width = tma.limit(dimensions.width, 0, $window.width());
	dimensions.height = tma.limit(dimensions.height, 0, $window.height());
	
	result.left = tma.limit(position.left, 0, $window.width() - dimensions.width);
	result.top = tma.limit(position.top, 0, $window.height() - dimensions.height);
	return result;
}); 

/**
 * sless
 * tolerance and propagate params are optional.  
 **/
tma.create('cclick', function($element, callback, tolerance, propagate){
	var md; 
	if(tolerance == null){
		tolerance = tma.CLICK_TOLERANCE; 
	} 
	$element.mousedown(function(e){
		md = e;
		propagate || e.stopPropagation(); 
	});
	$element.mouseup(function(e){
		if(md && e.target == md.target && Math.abs(e.pageX - md.pageX) <= tolerance && Math.abs(e.pageY - md.pageY) <= tolerance){
			/* why not stop propagation here: the drag doesn't release if so. Seems that drag widget is listening to 
			 * window's mouseup instead of exact element mouseup */
			//propagate || e.stopPropagation();
			callback(e);
		}
		md = null; 
	});
}); 

/**
 * sless
 */ 
tma.create('emptyClick', function($element){
	tma.cclick($element, function(){});
}); 

/**
 * This is a bit ugly but it's necesssary because the jQueryUI datepicker widget inserts an element at the global level. If 
 * it's not hidden when switching controllers, then it would still show up when the next controller renders. 
 **/
tma.create('hideDatePickers', function(){
	$.each($('.root .hasDatepicker'), function(id, elem){
		$(elem).datepicker("hide"); 
	});
});

// settings related

/** 
 * final 
 * User accessible settings 
 * todo 22x any: add settngs and getting of user settings
 * todo 22x any: modify certain functions to use settings 
 * */
tma.create('settings',  {
	weekStartsOn : 0, 
	hourFormat: 24, 
	defaultView: 'week', 
	keyboardShortcuts : true, 
	dateFormat: 'MM/DD/YY', 
	taskDuration: 60, 
	hideWeekends: false, 
	hideNights : false, 
	keyboardShorcuts: true, 
	timeZone : 0, 
	largeButtons: true
}); 

/**
 * constant
 */
tma.create('CLICK_TOLERANCE', 10); 

/**
 * constant
 */
tma.create('DRAG_TOLERANCE', 5); 

/**
 * constant
 */
tma.create('DEMO', true);

/**
 * constant
 */ 
tma.create('DEBUG', false); 

/**
 * constant
 */ 
tma.create('DELAY', 0);



// debugging related
/**
 * sless
 * Print timestamp
 */
tma.create('stamp', function(message){
	if(tma.DEBUG){
		message = message || ""; 
		console.log(">>" + new Date() + " : " +  message); 
	}
});


