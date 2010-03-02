// Provides Ajax functionalities for files that includes this file as a src

// Create XMLHttpRequest object of ActiveXObject for Ajax requests
function getXMLHttpRequestObject()
{
    if (window.XMLHttpRequest) {
        return new XMLHttpRequest();
    }
    else if (window.ActiveXObject) {
        try	{
            return new ActiveXObject("Microsoft.XMLHTTP");
        } 
        catch (e) { 
            try {
                return new ActiveXObject("Msxml2.XMLHTTP");
            } catch(e) {}
        }
    }
    else {
        alert("Your browser does not support AJAX.");
        return null;
    }
}

	function sendAjaxRequest(myAjax, myURL, handlerFunc)
	{
		// Initializes the XMLHttpObject based on the browser that the user is using.
		if( myAjax == null) {
			myAjax = getXMLHttpRequestObject();
		}
		
		if ( myAjax != null )
		{	
			//myURL = encodeURIComponent(myURL);		
			alert(myURL);
			myAjax.open("GET", myURL, true);
			myAjax.onreadystatechange = handlerFunc;    
			myAjax.send(null);	
		}
	}


// TEMPLATE FUNCTION FOR HANDLERS. DO NOT USE
function MOCK_AJAX_HANDLER(inmyAjax) {
    if(inmyAjax.readyState == 4) {
        if((inmyAjax.status == 200) || (inmyAjax.status == 304)) {
            alert("Called!");
        }
    }
}

