/**
 * Executes a test with provided args for ajax call and compares the response code to the expected one. 
 * Prints some info about request and response to screen
 */
tma.create('testCode', function (testName, jQuerySettings, addedSettings, expectedCode){
	if (expectedCode == null){
		expectedCode = 200;
	}
	tma.assert($("#tests") != null, "need a #tests div"); 
	$("#tests").append("------"+ testName + "------<br/>");
	
	var settings = tma.prepAjax(jQuerySettings, addedSettings);
	settings.async = false;
	
	$("#tests").append('request: ' + JSON.stringify(settings) + "<br/>");
	var result = $.ajax(settings);
	if(result.responseText.indexOf("Error:") != 0){
// 		result.responseText = "...";
	}
	
	$("#tests").append('result:' + JSON.stringify(result) + "<br/>");
	$("#tests").append("<br/>");
	tma.assert(result.status == expectedCode);
	return result;
});