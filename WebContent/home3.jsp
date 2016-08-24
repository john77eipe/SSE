<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Weather Data</title>
<style type="text/css">
#start, #send, #stop{
    width: 100px;
    margin-right: 100%;
    font-size: medium;
}
input {
 	margin-bottom: 2%;
}
ul {
	list-style: none;
	color: maroon;
}
ul li:before {
 content: "\0BB \020";
}
</style>
</head>
<body>
	<h3>Climate Temperature Toolbox</h3>
	<input type="button" value="start" id="start" />
	Send Weather Data for your home city: 
	<br /><br />
	City: <input type="text" id="home-city" />
	Celsius: <input type="text" id="home-temperature" />
	<input type="button" value="send" id="send" />
	<br />
	Weather Data from other cities:
	<ul id="cities">
	</ul>
<script>

//self executing function here
(function() {
	// your page initialization code here
	// the DOM will be available here
	document.getElementById("start").addEventListener("click", startEventStream);
	document.getElementById("send").addEventListener("click", sendHomeTemperature);
})();

function startEventStream() {
	console.log("startEventStream()");
	// Check that browser supports EventSource 
	if (window.EventSource) {
	    // Subscribe to url to listen
	    var source = new EventSource('Weather/v3');
		window.evntSrc = source;
		// The pre-existing events defined by EventSource API - message, open, error
	    
		// This code listens for incoming messages from the server that do not have an event field on them
	    // If you are not planning to  just data then this event listener alone will suffice
	    source.addEventListener("message", function(e) {
		    console.log("Invalid message without event field:" + e.data);
	    }, false);

	    source.addEventListener("open", function(e) {
		    console.log("Event stream connection opened");
	    }, false);

	    source.addEventListener("error", function(e) {
		    console.log("Event stream connection error");
	    }, false);
	    
	    // Define what to do when server sent custom event of type city
	    source.addEventListener("city", function(e) {
		    console.log(e.data);
	        var el = document.getElementById("cities"); 
	        el.innerHTML += "<li>" + e.data + "</li>";
	    }, false);
	    
	} else {
	    alert("Your browser does not support EventSource!");
	}
}

function sendHomeTemperature(){
    // Init http object
    var http = createXHRConnection();

    if(http) {
	    // Prepare data
	    var parameters = "city=" + encodeURIComponent(document.getElementById("home-city").value.trim()) + 
	    	"&temp="+encodeURIComponent(document.getElementById("home-temperature").value.trim());
	
	    http.open("POST", "Weather/v3", true);
	    http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	    http.send(parameters);
    }
    return false;
}

function createXHRConnection() { 
	var http = false;
    if (typeof ActiveXObject != "undefined") {
        try {
            http = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (ex) {
            try {
                http = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (ex2) {
                http = false;
            }
        }
    } else if (window.XMLHttpRequest) {
        try {
            http = new XMLHttpRequest();
        } catch (ex) {
            http = false;
        }
    }

    if (!http) {
        alert("Unable to connect!");
        return;
    }
    return http;
}

</script>
</body>
</html>