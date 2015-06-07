tma.createController('app_cal_week', {
	// overrides 
	name : 'week', 
	path: ".selectedSub",
	active : false, 
	initModel : function() {
		var con = this;
		var periodStart = con.getPeriodStart(); 
		con.mod = {tasks : null};
		var deferred = tma.app.ajax({
				url : '/rs/task/findTasks/' + tma.app.mod.scheduleId,
				data : {start : periodStart, end : periodStart + tma.DAY*con.loc.numDays}, 
				type : "GET"
		}, null).then(function(d) {
			con.mod.tasks = d;
		});
		return deferred;
	}, 
	
	initLocal : function(){
		var con = this;
		con.loc = {numDays : 7};
	},
	view : function(){

		var con = this;
		
		/* constant */
		var tasks, pxPerHour, pxSnapY, dayHeight, allDayTaskHeight, headerHeight, pxPerMillis, numDays, periodStart, allDayBoxHeight, dayWidth;  
		
		/* final */
		var $html, $week, $header, $allDayBox, $hourly; 
		
		initWeekVars();
		/* gridlines must be written before week because absolute children of $html must come before relative children, 
		 * otherwise not rendered correctly*/
		writeHorizontalGridlines();
		/* week must be written before week contents in order to get week width and day width*/ 
		$html.append($week);
		con.$root.append($html);
		dayWidth = $week.width()/numDays;
		/* detach is an optimization to avoid repeated reflows */
		$week.detach();
		populateWeekContents();
		writeWeekContents();
		addOnClick();
		addTitles();
		$html.append($week);
		
		/*helpers*/
		function addOnClick(){
			tma.cclick($allDayBox, function(e){
				var pos = tma.subtractPosition({left: e.pageX, top: e.pageY}, $allDayBox.offset());
				var start = Math.floor(pos.left/dayWidth)*tma.DAY + periodStart;
				tma.app_cal_crudTask.show({allDay : true, start : start, duration : 24*tma.HOUR}, e);
			}); 
			tma.cclick($hourly, function(e){
				var pos = tma.subtractPosition({left: e.pageX, top: e.pageY}, $hourly.offset());
				var start = Math.floor(pos.left/dayWidth)*tma.DAY + periodStart + tma.snap(pos.top/dayHeight*tma.DAY, tma.HOUR*pxSnapY/pxPerHour, 'floor');  
				tma.app_cal_crudTask.show({allDay : false, start : start}, e);
			});
			
		}
		
		function addTitles(){
			$('.task', $hourly).attr('title', 'click to open\ndrag to move\ndrag bottom to resize');
			$('.task', $allDayBox).attr('title', 'click to open\ndrag to move');
			$hourly.attr('title', 'click to create event');
			$allDayBox.attr('title', 'click to create all-day event');
		}
		
		function populateWeekContents(){
			$.each(tasks[0], function(dayInd, dayTasks){

				/* constant */
				var dayStart, dayLeft, numDaysRemaining;
				
				initDayVars();
				writeColumnsAndLabels();

				writeTasks(dayTasks.multiDay); 
				writeTasks(dayTasks.allDay); 
				$.each(tma.app_cal.groupByOverlapping(dayTasks.hourly), function(groupInd, group){
					writeTasks(group);
				}); 
				
				
				/* helpers */
				function initDayVars(){
					dayStart = periodStart + dayInd*tma.DAY;
					dayLeft = dayWidth*dayInd + 1 ;
					numDaysRemaining = numDays - dayInd;
				}; 
				function writeColumnsAndLabels(){
					var $col = $("<div class='verticalGrid' style='position:absolute;width:" + dayWidth + "px;height:100%;left:" + dayLeft + "px;'/>");
					if(dayStart == tma.startOfDay(tma.getCurrentDate())){
						$col.addClass('style_currentDate');
					}
					
					$week.append($col);
					var $label = $("<div class='dayLabel' style='position:absolute;text-align:center;width:" + dayWidth + "px;height:100%;left:" + dayLeft + "px'/>").append(tma.dateToDateString(dayStart, true));
					$header.append($label);
				}
				
				function writeTasks(taskHolders){
					
					$.each(taskHolders, function(thInd, taskHolder){
						/* constant */
						var task, taskLeft, taskHeight, taskWidth, taskTop;
						
						task = taskHolder.task;
						

						if(taskHolder.type == 'multiDay'){
							taskWidth = dayWidth*tma.limit(tma.numDays(dayStart, task.start + task.duration), null, numDaysRemaining);
							taskLeft = dayLeft; 
							taskHeight = allDayTaskHeight;
							taskTop = allDayTaskHeight*taskHolder.index;
							$allDayBox.append($task);
						}
						else if(taskHolder.type == 'allDay'){
							taskWidth = dayWidth; 
							taskLeft = dayLeft; 
							taskHeight = allDayTaskHeight;
							taskTop = allDayTaskHeight*taskHolder.index;
							$allDayBox.append($task);
							var allDayTasksBegin = taskHolders[0].index*allDayTaskHeight;  
						}
						else if(taskHolder.type == 'hourly'){
							taskWidth = dayWidth/taskHolders.length;
							taskLeft = dayLeft + thInd*taskWidth;
							taskHeight = Math.max(task.duration, 30*tma.MINUTE) * pxPerMillis; 
							taskTop =  Math.round((task.start - dayStart) * pxPerMillis);
							$hourly.append($task); 
						}
						else {
							tma.assert(false); 
						}
						
						var $task = tma.app_cal.getTask(taskHolder, taskWidth, taskHeight, taskLeft, taskTop, periodStart);
						
						if(taskHolder.type == 'multiDay'){ 
							$allDayBox.append($task);
							widgetsMultiDay($task); 
						}
						else if(taskHolder.type == 'allDay'){
							$allDayBox.append($task);
							widgetsAllDay($task); 
						}
						else if(taskHolder.type == 'hourly'){
							$hourly.append($task); 
							widgetsHourly($task);
						}
						tma.app_cal.addClick($task, task);
					
						
						
						
						/* helpers */
						
						function widgetsMultiDay($task){
							/* why not use ui.position and ui.originalPosition: there are problems with those when using
							 * a grid. Basically, when dragging a little bit but not enough to move one unit on the grid, 
							 * the ui.position still changes (could be a bug in the widget). */
							var originalPosition;
							$task.draggable({
								start : function(event, ui){
									ui.helper.addClass('style_dragging'); 
									originalPosition = $task.position();
								}, 
								distance : tma.DRAG_TOLERANCE,
								stop: function(event, ui) {
									ui.helper.removeClass('style_dragging'); 
									var position = $task.position();
									var millisMoved = Math.round((position.left - originalPosition.left)/dayWidth)*tma.DAY; 
									if(millisMoved){
										task.start += millisMoved;
										tma.app_cal_crudTask.updateTask(task);
									}
								}, 
								grid : [dayWidth, 1], 
								drag : function(event, ui) {
									ui.position.top = originalPosition.top;
								},
							});
						}
						function widgetsAllDay($task){
							var originalPosition;
							$task.draggable({
								start : function(event, ui){
									ui.helper.addClass('style_dragging');
									originalPosition = $task.position();
								}, 
								distance : tma.DRAG_TOLERANCE,
								stop: function(event, ui) {
									ui.helper.removeClass('style_dragging');
									var position = $task.position();
									/* day changed: task always goes to end since we don't have info on the tasks in the target day */
									var millisMoved = Math.round((position.left - originalPosition.left)/dayWidth)*tma.DAY; 
									if(millisMoved){
										task.start += millisMoved;
										task.allDayOrder = new Date().getTime();
										tma.app_cal_crudTask.updateTask(task);
									}
									
									/* vertical movement: task order is rearranged */
									else{
										tma.app_cal_crudTask.fixAllDayOrder($.map(taskHolders,function(th){ 
											return th.task;
										}))
										.then(function(){
											/* calculate where the task got moved to */
											var indexOfPrevious = Math.floor((position.top - allDayTasksBegin)/taskHeight) ;
											var indexOfNext = indexOfPrevious + 1; 
											var orderOfPrevious, orderOfNext;
											if(indexOfPrevious < 0){
												orderOfPrevious = 0; 
												orderOfNext = taskHolders[0].task.allDayOrder;
											}
											else if(indexOfPrevious >= taskHolders.length - 1 ){
												orderOfPrevious = taskHolders[taskHolders.length -1].task.allDayOrder;
												orderOfNext = new Date().getTime();
											}
											else{
												orderOfPrevious = taskHolders[indexOfPrevious].task.allDayOrder;
												orderOfNext = taskHolders[indexOfNext].task.allDayOrder;
											}
											/* update order only if its order really changed */
											if((indexOfPrevious <= 0 && thInd == 0) ||  (indexOfPrevious >= taskHolders.length - 1 && thInd == taskHolders.length - 1)
													|| Math.abs(position.top - originalPosition.top) <= taskHeight){
												$task.css({top : originalPosition.top + 'px'});
											}
											else{
												task.allDayOrder = orderOfPrevious + Math.round((orderOfNext - orderOfPrevious)/2);
												tma.app_cal_crudTask.updateTask(task);
											}
										});
									}
									
								}, 
								grid : [dayWidth, 1],
								drag : function(event, ui) {
									ui.position.top = tma.limit(ui.position.top, allDayTasksBegin - 1, allDayBoxHeight - allDayTaskHeight + 1);
								},
							});
						
						}
						function widgetsHourly($task){
							var originalPosition;
							$task.draggable({
								start : function(event, ui){
									ui.helper.addClass('style_dragging');
									originalPosition = $task.position();
								}, 
								distance : tma.DRAG_TOLERANCE,
								stop: function(event, ui) {
									ui.helper.removeClass('style_dragging');
									var position = $task.position();
									var millisMovedX = Math.round((position.left - originalPosition.left)/dayWidth)*tma.DAY;
									/* extra snapping on top of the jquery ui grid is necessary because when zoomed 
									 * new position can be non-integer and therefore off by 1px when rounded 
									 * This is not needed for X direction because 1 px is less than a day, so if off by 1px won't be off by a day
									 * In the y direction 1px is more than a minute, so being off by 1 px matters. 
									 * */
									var millisMovedY = Math.round(tma.snap(position.top - originalPosition.top, pxSnapY)/pxPerMillis);
									var millisMoved = millisMovedX + millisMovedY; 
									if(millisMoved){
										task.start += millisMoved;
										tma.app_cal_crudTask.updateTask(task);
									}
									else{
										$task.css({top : originalPosition.top + 'px'});
									}
								
								}, 
								grid : [dayWidth, pxSnapY ], 
//								containment : '.hourlyBox', 
								drag : function(event, ui) {
									ui.position.top = tma.limit(ui.position.top, - 1, dayHeight - taskHeight + 1);
								},
							});
							
							/* on resize */
							 $task.resizable({
								start : function(event, ui){
									ui.helper.css('z-index', 1);
								}, 
								handles: "s",
								grid : [1000, pxSnapY],
								stop : function(event, ui){
									ui.helper.css('z-index', 0);
									/* extra snapping  on top of the jquery ui grid is necessary because when zoomed 
									 * new height can be non-integer and therefore off by 1px when rounded */
									var pxMoved = tma.snap(ui.size.height - ui.originalSize.height, pxSnapY);
									if(pxMoved){
										task.duration += (pxMoved/pxPerMillis);
										tma.app_cal_crudTask.updateTask(task);
									}
									
								}, 
								resize : function(event, ui) {
									ui.size.width = ui.originalSize.width;
								}, 
								containment : '.hourlyBox'
							});
						
						}
					}); 
				}
			}); 
			
		}

		function initWeekVars(){
			periodStart = con.getPeriodStart();
			numDays = con.loc.numDays; 
			tasks = tma.app_cal.groupByDay(periodStart, con.mod.tasks, 0, numDays, null);
			
			pxPerHour = 32;
			pxSnapY = pxPerHour/4;
			dayHeight = 24*pxPerHour; 
			allDayTaskHeight = 20; 
			headerHeight = 30;
			pxPerMillis = pxPerHour/(3600*1000);
			
			allDayBoxHeight = allDayTaskHeight*Math.max.apply(null, 
					$.map(tasks[0], function(day){
						var max = -1; 
						$.each(day.multiDay, function(thInd, th){
							max = th.index > max ? th.index : max;
						}); 
						$.each(day.allDay, function(thInd, th){
							max = th.index > max ? th.index : max;
						});
						return max + 1; 
					}
			));   
					
					
			
			allDayBoxHeight = Math.max(allDayBoxHeight, 3*allDayTaskHeight);
			
			$html = $("<div class='weekWrapper' style='position:relative' ></div>");
			$week = $("<div class='week' style='margin-left:50px;position:relative' ></div>");
			$header = $("<div class='header' style='position:relative;height:" + headerHeight + "px' ></div>");
			$allDayBox = $("<div class='allDayBox' style='position:relative;height:" + allDayBoxHeight + "px' ></div>");
			$hourly = $("<div class='hourlyBox'  style='position:relative;height:" + dayHeight + "px'></div>");	
			
		}
		
		function writeHorizontalGridlines(){
			var gridTop = headerHeight + allDayBoxHeight;
			for(var i = 0; i < 24; i++){
				$html.append($("<div class='horizontalGrid' style='position:absolute;width:100%;height:" + pxPerHour + "px;top:" + gridTop + "px'/>")
						.append(tma.dateToTimeString(i*tma.HOUR)));
				gridTop += pxPerHour; 
			}
		}
		
		function writeWeekContents(){
			$week.append($header, $allDayBox, $hourly);
		}
	},
	// actions 
	/** go to another page; direction is next or previous or now */
	page : function(direction){
		var con = this; 
		if(direction == 'next'){
			tma.app_cal.loc.start += tma.DAY*con.loc.numDays;
		}
		else if(direction == 'previous'){
			tma.app_cal.loc.start -= tma.DAY*con.loc.numDays;	
		}
		else if(direction == 'now'){
			tma.app_cal.loc.start = tma.startOfDay(tma.getCurrentDate());
		}
		else{
			tma.assert(false, 'invalid direction: ' + direction);
		}
		con.initModel()
		.then(function(){
			con.load();
		});
	} ,
	// helpers 
	/** returns the proper start day of the period. For a week or longer it's logical to start at start of week */
	getPeriodStart : function(){
		var con = this;
		var periodStart; 
		if(con.loc.numDays >= 7){
			periodStart = tma.startOfWeek(tma.app_cal.loc.start);
		}
		else if(con.loc.numDays < 7 && con.loc.numDays > 0){
			periodStart = tma.app_cal.loc.start;
		}
		else{
			tma.assert(false, 'invalid numDays');
		}
		return periodStart;
	} 
	
}); 


