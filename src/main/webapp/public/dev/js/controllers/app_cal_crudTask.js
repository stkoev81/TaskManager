tma.createController('app_cal_crudTask', {
	//overrides
	path : ".crudTask" ,
	active : false,
	/**
	 * Params are optional. Task may be an existing one or a newly created one.  
	 */
	initLocal : function(task, clickEvent){
		var con = this;
		task = task || {};
		/* new task being created, possibly with some parameters already set upstream */
		if(task.id == null){
			var defaultTask = {	start : tma.getCurrentDate(), 
								duration : 1*tma.HOUR , 
								name : 'new task',  
								taskType : 'FIXED', 
								scheduleId : tma.app.mod.scheduleId
								};  
			task = $.extend(defaultTask, task);
		}
		con.loc = {task : task, clickEvent : clickEvent};
	},
	view : function(){
		var con = this;
		/*final*/
		var task, $html, $startTime, $endTime;
		
		var end; 
		
		initialize();
		onClicks();
		widgets();
		onChange();
		hide(); 
		toDom();
		//$html.draggable();
		con.$root.append($html);
		position();
		
		
		/*helpers*/

		function initialize(){
			task = con.loc.task;
			$html = tma.uni($("body>.app_cal_crudTask")).clone().toggleClass('style_hidden');
			end = task.start + task.duration; 
		}
		
		function onClicks(){
			tma.cclick(tma.uni($('.save', $html)), function(){
				var errors = fromDom(true); 
				if(errors){
					tma.app.onMessage(errors, 'error');
					return;
				};
				if(task.id != null){
					con.updateTask(task);
				}
				else{
					con.createTask(task);	
				}
			});
			tma.cclick(tma.uni($('.delete', $html)), function(){
				con.deleteTask(task.id);	
			});
			tma.cclick(tma.uni($('.cancel', $html)), function(){
				con.cancel();
			});
			tma.cclick($html, function(){
				tma.hideDatePickers(); 
			}); 
		}
		
		function widgets(){
			var $datepicker = tma.uni($('.startDate', $html)).datepicker({
				showOn: "both", 
				showButtonPanel : false, 
				buttonImage: "images/calendar.gif",
				buttonImageOnly: true,
				buttonText: "Select date"
			});
			tma.emptyClick($datepicker); 
			prepTimes(tma.uni($('.startTime', $html)));
			tma.uni($('.startTime', $html)).combobox();
			$startTime = tma.uni($('input', tma.uni($('.startTime', $html)).next()));
			
			$datepicker = tma.uni($('.endDate', $html)).datepicker({
				showOn: "both",
				buttonImage: "images/calendar.gif",
				buttonImageOnly: true,
				buttonText: "Select date"
			});
			tma.emptyClick($datepicker); 
			prepTimes(tma.uni($('.endTime', $html)));
			tma.uni($('.endTime', $html)).combobox();
			$endTime = tma.uni($('input', tma.uni($('.endTime', $html)).next()));
			
			function prepTimes($select){
				var time = 0; 
				for (var i = 0; i<48; i++){
					var timeString = tma.dateToTimeString(time); 
					$select.append("<option value='"+timeString+"'>"+timeString+"</option>");
					time += 0.5*tma.HOUR; 
				}
			};
		}
		
		function onChange(){
			$('[class^=duration],[class^=startDate],[class^=endDate]', $html).change(function(e){synchronizeStartEndDuration(e.target.className);});
			$startTime.change(function(){synchronizeStartEndDuration('startTime');});
			$endTime.change(function(){synchronizeStartEndDuration('endTime');});
			$('.allDay', $html).change(toggleAllDay);
			
			function synchronizeStartEndDuration(inputType){
				var errors = fromDom();
				if(errors){
					tma.app.onMessage(errors, 'error'); 
				}
				if(inputType.indexOf('start') > -1){
					end = task.start + task.duration;
				}
				else if(inputType.indexOf('end') > -1){
					task.duration = end - task.start;
				}
				else if(inputType.indexOf('duration') > -1){
					end = task.start + task.duration; 
				}
				else{
					tma.assert(false); 
				}
				toDom();
			};
			
			function toggleAllDay(e){
				$('.startTimeContainer, .endTimeContainer', $html).toggleClass('style_hidden');
				var errors = fromDom();
				if(errors){
					tma.app.onMessage(errors, 'error'); 
				}
				if(e.target.checked){
					task.start = tma.startOfDay(task.start);
					end = task.start + task.duration;
				}
				toDom();
			}
		}
		
		function hide(){
			if(task.id == null){
				tma.uni($('.delete', $html)).toggleClass('style_hidden');
			}
			if(task.allDay){
				$('.startTimeContainer, .endTimeContainer', $html).toggleClass('style_hidden'); 
			}
		}

		function position(){
			var clickEvent = con.loc.clickEvent;
			if(clickEvent){
				var positionOnScreen = tma.stayOnScreen(tma.positionOnScreen({left: clickEvent.pageX, top: clickEvent.pageY}), {width:$html.width(), height: $html.height()});
				$html.css('left', positionOnScreen.left +  'px');
				$html.css('top', positionOnScreen.top + 'px');
			}
		}
		
		
		function fromDom(isFinalSubmit){
			var errors = ''; 
			var startDateTime = tma.dateStringToDate(tma.uni($('.startDate', $html)).val() + " " +  $startTime.val());
			if(isNaN(startDateTime)){
				errors += 'Start date or time is badly formatted\n';
			}
			else{
				task.start = startDateTime; 
			}
			var endDateTime = tma.dateStringToDate(tma.uni($('.endDate', $html)).val() + " " + $endTime.val());
			if(isNaN(endDateTime)){
				errors += 'End date or time is badly formatted\n';
			}
			else{
				end = endDateTime; 
			}
			
			task.name = tma.uni($('.name', $html)).val();
			task.description = tma.uni($('.description', $html)).val();
			task.allDay = tma.uni($('.allDay', $html)).prop('checked');
			task.duration = tma.durationFromComponents(tma.uni($('.durationDays', $html)).val(), tma.uni($('.durationHrs', $html)).val(), tma.uni($('.durationMins', $html)).val());
			
			if(isFinalSubmit){
				if(task.start + task.duration != end){
					errors += "Start end and duration don't match\n";
				}
				if(task.duration <= 0){
					errors += 'Start date must be before end date\n';
				}
			}
			return errors;
		}
		
		function toDom(){
			tma.uni($('.startDate', $html)).val(tma.dateToDateString(task.start));
			$startTime.val(tma.dateToTimeString(task.start));
			
			var durationComponents = tma.durationToComponents(task.duration);
			tma.uni($('.durationDays', $html)).val(durationComponents.days);
			tma.uni($('.durationHrs', $html)).val(durationComponents.hrs);
			tma.uni($('.durationMins', $html)).val(durationComponents.mins);
			tma.uni($('.endDate', $html)).val(tma.dateToDateString(end));
			$endTime.val(tma.dateToTimeString(end));
			tma.uni($('.name', $html)).val(task.name);
			tma.uni($('.description', $html)).val(task.description);
			/* why .attr insted of .prop : using .prop triggers the onchange again*/
			tma.uni($('.allDay', $html)).attr('checked', task.allDay);
		}; 

	},
	//actions
	/**
	 * Shows or hides the crud task window. Params are optional.
	 */
	show : function(task, clickEvent){
		var con = this;
		con.initLocal(task, clickEvent);
		
		/* show */
		if(!con.$root.contents().length){
			con.active = true;
			con.load();
			con.active = false;
		}
		/* hide */
		else{
			con.hide(); 
		}
	},  
	hide : function(){
		var con = this; 
		if(con.$root.contents().length){
			con.active = false; 
			tma.hideDatePickers();
			con.load();
		}
	}, 
	/**
	 * Creates a new task in db and refreshes view. 
	 */
	createTask : function(task){
		var con = this;
		con._prepTask(task);
		tma.app.ajax({url: '/rs/task/create', type : "POST", data : task}, null)
		.then(function(){
			con._completeCrud('Task created');
		});
		
	},
	/**
	 * Closes the crud task window
	 */
	cancel : function(){
		var con = this; 
		con.hide();
	}, 
	/**
	 * deletes a task and refreshes the view
	 */
	deleteTask : function(id){
		var con = this;
		tma.app.ajax({url: '/rs/task/delete/' + id, type : "DELETE"}, null)
		/* reload model but not local of the tasks view */
		.then(function(){
			con._completeCrud('Task deleted');
		});
		
	}, 
	/**
	 * updates a task and refreshes the view
	 */
	updateTask : function(task){
		var con = this;
		con._prepTask(task);
		tma.app.ajax({url: '/rs/task/update', type : "PUT", data : task}, null)
		/* reload model but not local of the tasks view */
		.then(function(){
			con._completeCrud('Task updated');
		});
	}, 
	_completeCrud : function(message){
		var con = this;
		/* close the crud popup */   
		con.hide();
		/* show success message */
		if(message){
			tma.app.onMessage(message, 'notification');
		}
		/* reload model of the tasks con to show updated tasks*/
		tma.app_cal.loc.selectedSub.load(false, true);
	},
	//helpers
	
	/** After multiple reorderings, the tasks' allDayOrder can become too closely spaced and the reordering won't work
	 * properly. This method checks for this condition and fixes it. This condition will occur very rarely, so this
	 * method will do task updates very rarely. 
	 */  
	fixAllDayOrder : function(tasks){
		var con = this;
		var tooCloselySpaced = false; 
		for (var  i = 1; i < tasks.length; i++){
			if(tasks[i].allDayOrder - tasks[i-1].allDayOrder <= 2){
				tooCloselySpaced = true;
			}
		}
		var deferred = $.Deferred();
		deferred.resolve();
		if(tooCloselySpaced){
			var currentOrder = new Date().getTime();
			for(var i = tasks.length - 1; i >= 0; i --){
				(function(){
					tasks[i].allDayOrder = currentOrder;  
					currentOrder -= 10000; 
					deferred = deferred.then(function(){
						return tma.app.ajax({url: '/rs/task/update', type : "PUT", data : task}, null);
					});
				})();
			}
		}
		return deferred; 
	},
	_prepTask : function(task){
		/* start and duration may not be rounded to the nearest minute due to division when converting pixels to millis */
		task.start = tma.snap(task.start, tma.MINUTE);
		task.duration = tma.snap(task.duration, tma.MINUTE);
		/* if a task is beng converted to all day it needs some changes */
		if(task.allDay){
			task.start = tma.startOfDay(task.start);
			/* all day single day tasks need an order */
			if(task.allDayOrder == null && tma.numDays(task.start, task.start + task.duration) <= 1){
				task.allDayOrder = new Date().getTime();	
			}
		}
	} 
	
});




