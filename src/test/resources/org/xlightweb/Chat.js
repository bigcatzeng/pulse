

registerChatTextReceiver(onChatTextReceived); 


function onChatTextReceived(msg) {
	var chat_div = document.getElementById('chat');
	chat_div.innerHTML += msg;
	chat_div.scrollTop = chat_div.scrollHeight;					
} 


function resetChatText() {
	document.getElementById('chat').innerHTML = "";
}

var sendReq = getXmlHttpRequestObject();


    



function getXmlHttpRequestObject() {
	if (window.XMLHttpRequest) {
		return new XMLHttpRequest();
	} else if(window.ActiveXObject) {
		return new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		document.getElementById('status').innerHTML = 
		'Status: Cound not create XmlHttpRequest Object.' +
		'Consider upgrading your browser.';
	}
}


function sendChatText() {
	
	if (sendReq.readyState == 4 || sendReq.readyState == 0) {
		sendReq.open("POST",  'service/' + document.getElementById('channel').value + '/addMessage/' + document.getElementById('user').innerHTML, true);
		sendReq.setRequestHeader('Content-Type','text/plain');
		sendReq.send(document.getElementById('message').value);
		document.getElementById('message').value = '';
	}	
    				
} 


function registerChatTextReceiver(callback) {
	var listen = new pi.comet;
    listen.environment.setUrl('service/'  + document.getElementById('channel').value + '/receiveMessages/' + document.getElementById('user').innerHTML);
    listen.event.push = callback;
    
    listen.send();					
} 
