tma.createController('app_cal_month', {
	// overrides 
	path: ".selectedSub",
	active : false, 
	initModel : function() {
		var con = this;
		var firstWeekOfMonthStart = tma.firstWeekOfMonthStart(tma.app_cal.loc.start);
		var lastWeekOfMonthEnd = tma.lastWeekOfMonthEnd(tma.app_cal.loc.start);
		con.mod = {tasks : null};
		var deferred = tma.app.ajax({
				url : '/rs/task/findTasks/' + tma.app.mod.scheduleId,
				data : {start : firstWeekOfMonthStart, end : lastWeekOfMonthEnd},  
				type : "GET"
		}, null).then(function(d) {
			con.mod.tasks = d; 
		});
		return deferred;
	}, 
	
	 
	view : function(){
		var con = this;
		
		/* constant */
		var tasks, firstWeekOfMonthStart, startOfMonth, endOfMonth, numWeeks, monthHeight, headerHeight, weekHeight, dayHeight, taskHeight, dayWidth, maxTasksPerDay;  
		
		/* final */
		var $html, $monthLabel, $header, $monthBox;
		
		initMonthVars();
		writeMonthContents();
		/* this write must happen for $html.width() to be available. */
		writeMonth();
		dayWidth = $html.width()/7; 
		/* detach is an optimzation to avoid repeated reflow */
		detachMonth(); 
		populateMonthContents(); 
		addOnClicks(); 
		addTitles();
		writeMonth();
		
		/*helpers*/
		function addOnClicks(){
			tma.cclick($monthBox, function(e){
				var pos = tma.subtractPosition({left: e.pageX, top: e.pageY}, $monthBox.offset());
				var start = firstWeekOfMonthStart + Math.floor(pos.left/dayWidth)*tma.DAY +  Math.floor(pos.top/weekHeight)*tma.WEEK;  
				tma.app_cal_crudTask.show({allDay : true, start : start, duration : 24*tma.HOUR}, e);
			});
		}
		function addTitles(){
			$('.task', $monthBox).attr('title', 'click to open\ndrag to move');
		}
		
		function populateMonthContents(){
			$.each(tasks, function(wkInd, wkTasks){
				$.each(wkTasks, function(dayInd, dayTasks){
					/* constant */
					var dayTop, dayLeft, weekStart, dayStart, numDaysRemaining, taskLeft; 
					
					
					initDayVars();
					if(wkInd == 0){
						writeHeader();
					}
					writeDayBox(); 
					writeTasks(dayTasks.multiDay);
					writeTasks(dayTasks.allDay); 
					writeTasks(dayTasks.hourly); 
					writeOverflowMessage();
					
					
					/* helpers */
					function initDayVars(){
						dayTop = wkInd*dayHeight; 
						dayLeft = dayInd*dayWidth + 1;
						weekStart = firstWeekOfMonthStart + wkInd*tma.WEEK;
						dayStart =  weekStart + dayInd*tma.DAY; 
						numDaysRemaining = 7 - dayInd;
						taskLeft = dayLeft;
					}
					
					function writeHeader(){
						var $label = $("<div class='dowLabel' style='position:absolute;width:" + dayWidth + "px;height:100%;left:" + dayLeft + "px;'/>").append(tma.dayOfWeek(dayStart));
						$header.append($label);
					}
		 
					function writeDayBox(){
						var $dayBox = $("<div class='dayBox' style='position:absolute;z-index:-1;width:" + dayWidth + "px;height:" + dayHeight + "px;left:" + dayLeft + "px;top:" + dayTop + "px;'/>")
						.append($("<div class='dayLabel' style='height:" + taskHeight + "px;'/>").append(tma.dayOfMonth(dayStart)));
						if(!(dayStart >= startOfMonth && dayStart < endOfMonth)){
							$dayBox.addClass('style_notCurrentMonth');
						}
						if(dayStart == tma.startOfDay(tma.getCurrentDate())){
							$dayBox.addClass('style_currentDate');
						}
						$monthBox.append($dayBox);
					}
					
					function writeOverflowMessage(){
						if(dayTasks.numOverflows > 0){
							var $message = $("<div/>").addClass('overflowTasks').css({
								width: dayWidth  - 1 + 'px', 
								height: taskHeight - 1 + 'px',
								top : dayTop + (maxTasksPerDay + 1)*taskHeight  + 'px',
								left : dayLeft + 'px',
								position : 'absolute'
							});
							$message.append("+" + dayTasks.numOverflows + " more");
							tma.cclick($message, function(e){
								tma.app_cal.loc.start = dayStart;
								tma.app_cal.switchSub('day');
							});
							$monthBox.append($message);
						}
					}
					
					
					function writeTasks(taskHolders){
						$.each(taskHolders, function(thInd, taskHolder){
							/* constant */ 
							var task, taskWidth, taskTop; 
							
							if(!taskHolder.overflown){
								task = taskHolder.task;
								taskWidth = dayWidth; 
								if(taskHolder.type == 'multiDay'){
									taskWidth = dayWidth*tma.limit(tma.numDays(dayStart, task.start + task.duration), null, numDaysRemaining);		
								}
								taskTop = dayTop +  (1 + taskHolder.index)*taskHeight; 
								
								var $task = tma.app_cal.getTask(taskHolder, taskWidth, taskHeight, taskLeft, taskTop);
								
								widgets($task);
								tma.app_cal.addClick($task, task);
								$monthBox.append($task);
							}
							
							/*helpers*/
							function widgets($task){
								var position = $task.position();
								$task.draggable({
									start : function(event, ui){
										ui.helper.addClass('style_dragging');
										originalPosition = $task.position();
									}, 
									distance : tma.DRAG_TOLERANCE, 
									stop: function(event, ui) {
										var position = $task.position();
										ui.helper.removeClass('style_dragging');
										var millisMovedX = Math.round((position.left - originalPosition.left)/dayWidth)*tma.DAY;
										var millisMovedY = Math.round((position.top - originalPosition.top)/$monthBox.height()*numWeeks)*tma.WEEK;
										var millisMoved = millisMovedX + millisMovedY;
										if(millisMoved){
											task.start += millisMoved;
											if(taskHolder.type == 'allDay'){
												task.allDayOrder = new Date().getTime();
											}
											tma.app_cal_crudTask.updateTask(task);
										}
									}, 
									grid : [dayWidth, dayHeight]  
								});
							}
						});
					}
				}); 
			}); 
		}
		
		function initMonthVars(){
			firstWeekOfMonthStart = tma.firstWeekOfMonthStart(tma.app_cal.loc.start);
			numWeeks =  tma.numWeeksForMonth(tma.app_cal.loc.start); 
			
			startOfMonth = tma.startOfMonth(tma.app_cal.loc.start);
			endOfMonth = tma.endOfMonth(tma.app_cal.loc.start);
			monthHeight = 48*16; 
			headerHeight = 2*16;
			weekHeight = monthHeight/numWeeks;
			dayHeight = weekHeight;
			taskHeight = 20;
			$html = $("<div class='month' style='position:relative'></div>");
			$monthLabel = $("<div class='monthLabel'/>").append(tma.monthOfYear(tma.app_cal.loc.start) + ', ' + tma.getYear(tma.app_cal.loc.start))
			$header = $("<div class='header' style='position:relative;height:" + headerHeight + "px'></div>");
			$monthBox = $("<div class='monthBox' style='position:relative;height:" + monthHeight + "px'></div>");
			
			/*why -2: subtracting 1 for the label and 1 for the overflow message */
			maxTasksPerDay = Math.floor(dayHeight/taskHeight) - 2;  
			tasks = tma.app_cal.groupByDay(firstWeekOfMonthStart, con.mod.tasks, numWeeks, 0, maxTasksPerDay); 
		}
		
		function writeMonthContents(){
			$html
			.append($monthLabel)
			.append($header)
			.append($monthBox);
		}
		function detachMonth(){
			$html.detach();
		}
		
		function writeMonth(){
			con.$root.append($html);
		}
	}, 
	// actions 
	/** go to another page; direction is next or previous or now */
	page : function(direction){
		var con = this; 
		if(direction == 'next'){
			tma.app_cal.loc.start = tma.endOfMonth(tma.app_cal.loc.start);
		}
		else if(direction == 'previous'){
			tma.app_cal.loc.start = tma.startOfMonth(tma.startOfMonth(tma.app_cal.loc.start) - tma.DAY);	
		}
		else if(direction == 'now'){
			tma.app_cal.loc.start = tma.startOfDay(tma.getCurrentDate());
		}
		else{
			tma.assert(false, 'invalid direction: ' + direction);
		}
		con.initModel()
		.then(function() {
			con.load();
		});
	} ,
});

