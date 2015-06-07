
"use strict"; 
/*
 * File defines core objects needed by the application. These objects must be loaded before the rest of 
 * the application.  			
 */

/**
 * Name space for the application. The following conventions apply to this name space.  
 * 
 * These are labels used in comments on variables, functions, object fields, and object methods. Missing label implies default.
 * Labels on variable/field/method apply to the behavior once initialization is complete, i.e. the labels may be
 * violated during initialization.  
 * - optional - variable/field/method may be null. Default is not optional (must not be null). 
 * - final - variable/field/method must not be re-assigned. Default is non-final. 
 * - immutable - variable/field points to object that must not be mutated. Immutable does not mean final. Default is mutable. 
 * - constant - variable/field is both final and immutable. 
 * - private - field/method is such that only methods of same object may access it. Default is public. 
 * - sless - function/method is stateless, i.e. it does not read any state other than local variables and arguments. 
 * - fless - function/method is effectless, i.e. it does not write any state other than local variables. 
 *  
 */
var tma = {};

tma.assert = function(condition, message) {
	if (!condition) {
		throw new Error(message);
	}
};


/** Creates an object and checks for name collisions. All objects in namespace must be created using this function. */	
tma.create = function(objectName, object){
	tma.assert(tma[objectName] === undefined, 'name collision');
	tma[objectName] = object; 
}; 



/**
 * Base object from which all controllers in the application are extended.
 * uses: sub.load() for each sub in this.subs()   
 */
tma.create('baseController', {
	/** 
	 * final
	 * Fully qualified name of the controller, which is short names of the hierarchy of owning controllers
	 * joined by "_" 
	 **/
	name : undefined,

	/**
	 * final 
	 * The last part of the full name. 
	 */
	shortName : undefined,  
	/**
	 * final
	 * The jquery element to which the rendered view from the controller will be appended.
	 */
	$root : undefined,

	/**
	 * final
	 * Contains the selector path relative to parent's root where the sub controller view will be rendered.
	 */
	path : undefined,

	/** If false, the initialization and rendering will do nothing */
	active : true,

	/**
	 * final
	 * Returns map of subcontrollers, i.e. controllers that this one owns
	 */
	subs : function(){
		if(this._cachedSubs){
			return this._cachedSubs; 
		}
		
		var names = this.name.split('_'); 
		var currentMap = tma.controllerMap;
		$.each(names, function(ind, name ){
			currentMap = currentMap[name];
		});
		
		var subs = {}; 
		for(var key in currentMap){
			var subName = this.name + "_" + key;
			var sub = tma[subName];
			tma.assert(sub, "Subcontroller is present in controller mappings but hasn't been created. " +
					"Make sure all controllers and subcontrollers implied in controller names are created"); 
			subs[key] = sub;
		}
		this._cachedSubs = subs; 
		
		return subs; 
	}, 
	
	/**
	 * Model, also known as domain model. This is persistent state of the application accessed via web services. 
	 */
	mod : undefined,
	/**
	 * Local. This is the state of the UI itself. It is not persistent. 
	 */
	loc : undefined,
	/**
	 * Function used for rendering this controller to dom. Does not render the subcontrollers. 
	 */
	view : function() {
	},

	/**
	 * final
	 * Method that loads controller. This means: initialize local, initialize model, render view,
	 * recurse into subcontrollers. If controller not active, skip these steps and just render an empty view.
	 *  
	 * By default, the initialization of local and model is skipped if they are not null. The parameters reinitializeLocal 
	 * and reinitializeModel override this behavior if they are true. 
	 * 
	 * If the parameter skipRendering is true, then the view is not rendered. 
	 */
	load : function(reinitializeLocal, reinitializeModel, skipRendering) { 
		var con = this;
		var result;  
		var deferreds = []; 
		
		con._initAll(reinitializeLocal, reinitializeModel, deferreds);
		result = $.when.apply(null, deferreds);
		if(!skipRendering){
			result = result.always(function(){
				con._renderAll(); 
			});
		}
		return result;
	},
	
	/** 
	 * Initializes the local state associated with the controller to an empty object. Overrides will initialize to something
	 * controller-specific */
	initLocal : function() {
		this.loc = {};
	},
	/**
	 * Initializes the model with and empty object and returns a dummy promise. Overrides will do
	 * ajax calls to fetch model.
	 */
	initModel : function() {
		this.mod = {};
		var deferred = $.Deferred();
		return deferred.resolve(null);
	},  
	/**
	 * final, private
	 */
	_initAll : function(reinitializeLocal, reinitializeModel, deferreds){
		var con = this; 
		if(con.active){
			var subs = con.subs();
			if(reinitializeLocal || !con.loc){
				con.initLocal();
			}
			if(reinitializeModel || !con.mod){
				deferreds.push(con.initModel());
			} 
			for ( var subShortName in subs) {
				var sub = subs[subShortName];
				sub._initAll(reinitializeLocal, reinitializeModel, deferreds);
			}
		}
		return deferreds; 
	}, 
	
	_renderAll : function(){
		var con = this; 
		if(con.$root){
			con.$root.empty(); 
		}
		if(con.active){
			var subs = con.subs();
			con.view();
			for ( var subShortName in subs) {
				var sub = subs[subShortName];
				if(sub.path && con.$root){
					sub.$root = tma.uni($(sub.path, con.$root)); 
				}
				if(sub.active){
					sub._renderAll();
				}
			}
		}
	}
	
	
}); 

/**
 * constant 
 * A hierarchical mapping of controllers to subcontrollers */
tma.create('controllerMap', {}); 

/** Creates a controller by extending the base controller with the contObject. The name is used to figure out if this
 * controller is a sub of any other controllers and the controllerMap is updated accordingly. 
 * 
 * The name must be a path separated by "_" that shows the controller's hierarchy. It should follow this format:  
 *  ownersOwnerShortName_ownerShortName_controllerShortName
 * 
 * Example  name : 
 * 	app_calendar_week
 *     
 * */
tma.create('createController', function(contName, contObject){
	var names = contName.split('_');
	var shortName = names[names.length - 1]; 

	var con = $.extend(true, {}, tma.baseController, {name : contName, shortName : shortName}, contObject);
	tma.create(contName, con); 
	var currentMap = tma.controllerMap; 
	$.each(names, function(ind, name){
		if(!currentMap[name]){
			currentMap[name] = {};
		}
		currentMap = currentMap[name];
	}); 
}); 

/**
 * Prints out all controllers in the applications hierarchically for debugging purposes. 
 */
tma.create('showControllers', function(con, depth) {
	if (depth == null) {
		depth = 0;
	}
	if (con == null) {
		con = tma.app;
	}
	for ( var key in con.subs()) {
		var indent = '-';
		for (var i = 0; i < depth; i++) {
			indent += '-';
		}
		console.log(indent + key);
		console.debug(con.subs()[key]);
		tma.showControllers(con.subs()[key], depth + 1);
	}
});

/**
 * used to assert that the a jquery element exists and is unique  
 */
tma.create('uni', function($) {
	tma.assert($.length != 0, 'jquery elemement does not exist');
	tma.assert($.length == 1, 'jquery elemement is not unique');
	return $;
}); 

