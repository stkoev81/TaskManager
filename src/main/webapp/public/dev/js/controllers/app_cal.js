tma.createController('app_cal', {
	// overrides 
	active : false,
	path : ".page",
	initLocal : function() {
		var con = this; 
		con.loc = {selectedSub : null, selectedTimeFrame : 'week', start : null};
		con.loc.selectedSub = con.subs().week; 
		con.loc.selectedSub.active = true;
		con.loc.start = tma.startOfDay(tma.getCurrentDate());
	},
	view : function(){
		var con = this;
		var $html = tma.uni($("body>.app_cal")).clone().toggleClass('style_hidden');
		
		tma.cclick(tma.uni($(".paging", $html)).children(), function(e){
			con.loc.selectedSub.page(e.target.className);
		});
		var $buttons = tma.uni($(".viewSelector", $html)).children();
		tma.cclick($buttons, function(e){
			con.switchSub(e.target.className);
		});
		con.$root.append($html);
		con._markSelectedButton();
	},
 	// actions
	/**
	 * Switch to another sub of the main page, such as week, month, etc. 
	 */
	switchSub : function(selectedTimeFrame){
		var con = this;
		var subName = null; 
		if(selectedTimeFrame == 'day'){
			subName = 'week'; 
			con.subs().week.loc.numDays = 1;
		}
		else if(selectedTimeFrame == 'week'){
			subName = 'week'; 
			con.subs().week.loc.numDays = 7;
		}
		else if(selectedTimeFrame == 'fourDay'){
			subName = 'week'; 
			con.subs().week.loc.numDays = 4;
		}
		else{
			subName = selectedTimeFrame;
		}
		
		var sub = con.subs()[subName];
		tma.assert(sub);
		con.loc.selectedSub.active = false;
		con.loc.selectedSub = sub;
		sub.active = true;
		con.loc.selectedTimeFrame = selectedTimeFrame;
		/* reinitialize model because the start date may have changed through another sub*/
		/* todo 12x: possible optimization is to reinitialize only if we detect that the start date has changed */
		con._markSelectedButton();
		sub.load(false, true);
	} , 
	//helpers 
	/** sless, fless */
	getTask : function (taskHolder, taskWidth, taskHeight, taskLeft, taskTop){
		var task = taskHolder.task; 
		var $task = $("<div/>").addClass('task').css({width: taskWidth  - 1 + 'px', height: taskHeight - 1 + 'px', top : taskTop + 'px', left : taskLeft + 'px', position : 'absolute'});
		//if(taskHolder.type ==  'allDay'){
		if(task.allDay){
			$task.append(task.name);	
		}
		else{
			$task.append(tma.dateToTimeString(task.start) + " - "
					+ tma.dateToTimeString(task.start + task.duration) + " " + task.name);
		}
										
		if(taskHolder.extendsAfter){
			var arrow = ("<svg style='position:static;width:12px;height:" + taskHeight + "px;float:right;background-color:none;border-style:none' xmlns='http://www.w3.org/2000/svg' version='1.1'>")
			+ "<polygon points='0," + taskHeight/4 + " 10," + taskHeight/2 + " 0," + taskHeight*3/4 + "'  style='fill:black;stroke:none;stroke-width:0'/></svg>";
			$task.append(arrow);
		}
		if(taskHolder.extendsBefore){
			var arrow = ("<svg style='position:static;width:12px;height:" + taskHeight + "px;float:left;background-color:none;border-style:none' xmlns='http://www.w3.org/2000/svg' version='1.1'>")
			+ "<polygon points='0," + taskHeight/2 + " 10," + taskHeight/4 + " 10," + taskHeight*3/4 + "'  style='fill:black;stroke:none;stroke-width:0'/></svg>";
			$task.append(arrow);
		}
		return $task; 
	},
	/** sless */
	addClick : function($task, task){
		tma.cclick($task, function(e){
			tma.app_cal_crudTask.show(task, e); 
		});
	}, 
	_markSelectedButton : function markSelectedButton(){
		var con = this; 
		var $buttons = tma.uni($(".viewSelector", con.$root)).children();
		$.each($buttons, function(ind, button){
			var $button = $(button);
			if ($button.hasClass(con.loc.selectedTimeFrame)){
				$button.css('font-weight', 'bold');
			}
			else{
				$button.css('font-weight', 'normal');
			}
		}); 
	}, 
	/** sless, fless
	 * groups tasks from some period by day and by week. 
	 * returns a 2d array with length [numWeeks == 0 ? 1 : numWeeks][numWeeks == 0 ? numDays : 7] and with contents
	 * [][day] where day is the object returned by allocateDayTasks*/
	groupByDay : function(start, tasks, numWeeks, numDays, maxPerDay){
		tma.assert(numWeeks != null && numDays != null && (numWeeks == 0 || numDays == 0));
		var rows = [];
		/* month style */
		if(numWeeks > 0){
			for(var i = 0; i < numWeeks; i++){
				var periodStart = start + i*tma.WEEK; 
				var periodEnd = periodStart + tma.WEEK; 
				var row = [];
				var previousDay = null; 
				for(var j = 0; j < 7; j++){
					var dayStart = start + i*tma.WEEK + j*tma.DAY;
					var currentDay = tma.app_cal.allocateDayTasks(tasks, dayStart, periodStart, periodEnd, previousDay, true, maxPerDay);
					previousDay = currentDay;
					row.push(currentDay);
				}
				rows.push(row);
			}
		}
		/* week style */
		else if(numDays > 0){
			var periodStart = start; 
			var periodEnd = start + numDays*tma.DAY;
			var row = [];
			var previousDay = null; 
			for(var j = 0; j < numDays; j++){
				var dayStart = start + j*tma.DAY;
				var currentDay = tma.app_cal.allocateDayTasks(tasks, dayStart, periodStart, periodEnd, previousDay, false, maxPerDay);
				previousDay = currentDay;
				row.push(currentDay);
			}
			rows.push(row);
		}
		else{
			rows.push([]); 
		}
		return rows;
	}, 
	
	/**
	 * sless, fless
	 * groups tasks for a single day into various categories that are useful for display
	 * args: 
	 * tasks - array of tasks. The ones which are not for this day will be ignored. The ones that are for this day will be
	 * used in the result and deleted from the arg
	 * dayStart - start time 
	 * periodStart, periodEnd - the larger period which we might be viewing; typically it's a day, week, month, etc; this 
	 * info is used to put hints into the task to display it a certain way if it extends before or after the viweing period
	 * returns: day
	 * where day is an object of the form {allDay :[taskHolder],  multiDay : [taskHolder], hourly : [][{taskHolder}]}, numOverflows : ;}
	 * where taskHolder is an object of the form {task :, index:, type : , extendsAfter: , extendsBefore : , overflown : }
	 * 
	 * The hourly tasks are an array of arrays because they are grouped by overlapping. 
	 *   
	 * */
	allocateDayTasks : function(tasks, dayStart, periodStart, periodEnd, previousDay, monthStyle, maxPerDay){
		var day = {	multiDay : [], multiDay : [], allDay : [], hourly : [], numOverflows : 0, 
					_indices : {}, _toCarryOverIndices : {}
					};
		
		findTasksInDay(); 
		fixMultiDay();
		fixAllDay(); 
		fixHourly(); 
		fixOverflows(day.multiDay.concat(day.allDay).concat(day.hourly)); 
			
		return day;

		
		/* helpers */ 
		function findTasksInDay(){
			var dayPeriod = {start : dayStart, end : dayStart + tma.DAY};
			for(var j = 0; j < tasks.length; j++){
				var task = tasks[j];
				var taskPeriod = {start : task.start, end : task.start + task.duration};
				
				/* ignore tasks that are not in this day */
				if(!tma.periodsOverlap(dayPeriod, taskPeriod)){
					continue;
				}
				/* task is in this day */
				else{
					/* put task in the correct category */
					if(task.allDay){
						if(tma.numDays(task.start, task.start + task.duration) > 1){
							day.multiDay.push({task : task, type : 'multiDay'});
						}
						else{
							day.allDay.push({task : task, type : 'allDay' });
						}
					}
					else if (tma.numDays(task.start, task.start + task.duration) > 1){
						day.multiDay.push({task : task, type : 'multiDay'});
					}
					else{
						day.hourly.push({task : task, type : 'hourly'});
					}
				}
			}
		}
		
		
		/** multiday tasks need special treatment because they need to be rendered over several days */
		function fixMultiDay(){
			/* sort by end date descending */
			day.multiDay.sort(function(th1, th2){
				var t1 = th1.task;
				var t2 = th2.task;
				return - ((t1.start + t1.duration) - (t2.start + t2.duration));
			});
			day.multiDay = fixCarryOver(day.multiDay); 
			/* sort by index */
			day.multiDay.sort(function(th1, th2){
				return th1.index - th2.index;
			});
			
			
			/** Place multiday task only on the first day, while the rest of the days get an empty task as placeholder  
			 * to prevent overlaps with other tasks */
			function fixCarryOver(taskHolders){
				if(previousDay){
					day._indices = $.extend({}, previousDay._toCarryOverIndices); 
					day._toCarryOverIndices = $.extend({}, day._indices);
				}
				var result = []; 
				for(var j = 0; j < taskHolders.length; j++){
					var th = taskHolders[j];
					var task = th.task; 

					var taskPeriod = {start : task.start, end : task.start + task.duration};
					/* task is being carried over from a previous day */
					if(day._indices[task.id] !== undefined) {
						/* remove index taken entry if it's the last day of this task, otherwise let it carry over again */
						if(tma.startOfDay(taskPeriod.end - 1) == dayStart){
							delete day._toCarryOverIndices[task.id]; 
						}
					}
					/*it's the first day for this task*/
					else{
						result.push(th);
						
						/* add info whether this task extends beyond the viewing period */
						if(taskPeriod.start < periodStart){
							th.extendsBefore = true;
						}
						if(taskPeriod.end > periodEnd){
							th.extendsAfter = true;
						}
						
						th.index = getNextIndex(task.id);
						if(tma.startOfDay(taskPeriod.end - 1) != dayStart){
							day._toCarryOverIndices[task.id] = th.index;
						}
					}
				}
				return result; 
			}
		}
		
		function fixAllDay(){
			/* order all day tasks */
			day.allDay.sort(function(th1, th2){
				var t1 = th1.task, t2 = th2.task;
				return t1.allDayOrder - t2.allDayOrder;
			});
			for (var i = 0; i < day.allDay.length; i++){
				var th = day.allDay[i];
				if(monthStyle){
					th.index = getNextIndex(th.task.id);
				}
				else{
					th.index = getMaxIndex(th.task.id); 
				}
			}
		}
		
		function fixHourly(){
			if(monthStyle){
				for (var i = 0; i < day.hourly.length; i++){
					var th = day.hourly[i]; 
					th.index = getNextIndex(th.task.id); 
				}
			}
			else{
				/*index not used here*/
			}
		}
		
		/** find the smallest index that hasn't been used yet */ 
		function getNextIndex(taskId){
			
			
			var indices = []; 
			for (var key in day._indices){
				indices[day._indices[key]] = true;
			}
			var result; 
			for(var i = 0; i < indices.length; i++){
				if (!indices[i]){
					result = i;
					break; 
				}
			}
			if(result == null){
				result = indices.length;
			}
			
			day._indices[taskId] = result;
			return result;
		}
		/** find the index following the max index used*/
		function getMaxIndex(taskId){
			var max = -1, result;  
			for (var key in day._indices){
				var index =  day._indices[key]; 
				max = index > max ? index : max; 
			}
			result = max + 1;
			day._indices[taskId] = result; 
			return result;
		}
		function fixOverflows(taskHolders){
			if(maxPerDay != null){
				var maxIndex = 0, overflow = 0; 
				var maxIndexCarriedOver, overflowCarriedOver = 0; 
							
				$.each(day._indices, function(key, value){
					maxIndex = Math.max(maxIndex, value);
				});
				if(previousDay){
					$.each(previousDay._indices, function(key, value){
						maxIndexCarriedOver = Math.max(maxIndexCarriedOver, value);
					}); 
				}
				
				overflow = tma.limit(maxIndex + 1 - maxPerDay, 0, null);
				overflowCarriedOver = tma.limit(maxIndexCarriedOver + 1 - maxPerDay, 0, null);
				
				/* if overflow is 1, it can be accommodated by simply not showing the overflow message, whose height is 1 
				 * however if there are overflows from previous day, must show message even if it's just one 
				 * */
				if(overflow >=2 || overflowCarriedOver >= 1){
					$.each(taskHolders, function(thInd, taskHolder){
						var task = taskHolder.task;
						if(taskHolder.index  + 1 > maxPerDay){
							taskHolder.overflown = true;
						}
					}); 
					day.numOverflows = overflow; 
				}	
			}
		}
	}, 
	/** 
	 * sless, fless
	 * Returns an array of arrays. Each array is a group of overlapping task holders.
	 */
	groupByOverlapping : function(taskHolders){
		var result = [];
		for(var i = 0; i< taskHolders.length; i++){
			var th = taskHolders[i];
			if(th === undefined){
				continue;
			}
			var group = [];
			group.push(th);
			delete taskHolders[i];
			for(var j = i + 1; j < taskHolders.length; j++){
				var otherTaskHolder = taskHolders[j];
				if(otherTaskHolder === undefined){
					continue;
				}
				if(overlapsWithOthers(otherTaskHolder, group)){
					delete taskHolders[j];
					group.push(otherTaskHolder);
				}
			}
			result.push(group);
		}
		return result;
		
		/**
		 * returns true if the task overlaps wity any of the members of taskArray; false otherwise
		 */
		function overlapsWithOthers(th, ths){
			var task1 = th.task; 
			var result = false;
			for (var i = 0; i < ths.length; i++){
				var task2 = ths[i].task;
				tma.assert(task1 != null && task1.start != null && task1.duration != null && task2 != null && task2.start != null && task2.duration != null); 
				result = tma.tasksOverlap(task1, task2);
				if(result){
					break;
				}
			}
			return result;
		}
		
	}
	
}); 

