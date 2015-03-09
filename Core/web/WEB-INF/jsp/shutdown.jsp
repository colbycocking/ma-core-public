<%--
    Copyright (C) 2014 Infinite Automation Software. All rights reserved.
    @author Terry Packer
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>




<tag:page dwr="StartupDwr">
    <div style="width: 100%; padding: 1em 2em 1em 1em;"
            data-dojo-type="dijit/layout/ContentPane"
            data-dojo-props="region:'center'">
    <div id="startingMessage" class='bigTitle'></div>
    <div id="startupProgress"></div>
    <div id="startupMessage"></div>
    <div id="startupConsole"
            style=" height: 200px; margin: 1em 3em 1em 1em; border: 2px; padding: .2em 1em 1em 1em; overflow:auto; border: 2px solid; border-radius:10px; border-color: lightblue;"
            data-dojo-type="dijit/layout/ContentPane"></div>
    </div>
    
    <script type="text/javascript">
var lastMessage; //Holds the last recieved log message
var pollLock = false; //Ensure we don't overpoll mango

require(["dojo/_base/xhr", "dojo/topic","dijit/ProgressBar", "dojo/_base/window", "dojo/domReady!"], 
        function(xhr, topic, ProgressBar, win){

    //Setup the console messages target
    topic.subscribe("startupTopic", function(message) {
        //Message has members:
        // duration - int
        // message - string
        // type - string
        var startupConsole = dijit.byId("startupConsole");
        if (message.type == 'clear')
            startupConsole.set('content', "");
        else {
            startupConsole.set('content', message.message + startupConsole.get('content'));
        }
    });
    
    //Initialize Existing info
    getStatus(0);
    
    var pollPeriodMs = 500;
    var i = 0;
    var myProgressBar = new ProgressBar({
        style: "width: 300px"
    },"startupProgress");
    
    /**
     * Set the method to poll for updates
     */
    setInterval(function(){
        var timestamp = new Date().getTime() - pollPeriodMs;
        getStatus(timestamp);
    }, pollPeriodMs);
    
    /**
     * Get the status from the server
     **/
    function getStatus(timestamp){
        if(pollLock)
            return;
        else
            pollLock = true;
        xhr.get({
            url: "/status/mango.json?time=" + timestamp,
            handleAs: "json",
            load: function(data){
 	            
                //Update my messages
                var startupMessageDiv = dojo.byId("startupMessage");
                startupMessageDiv.innerHTML = data.state;

                for(var i=0; i<data.messages.length; i++){
                    dojo.publish("startupTopic",[{
                        message:data.messages[i] + "<br>",
                        type: "message",
                        duration: -1, //Don't go away
                        }]
                    );
                }

	
	            var redirect = false;
	            
	            var progress = 0;
	            //We don't care if we are starting up or shutting down, just need to know which one
	            if((data.startupProgress >= 100) && (data.shutdownProgress > 0)){
	                //Dirty hack for now to show that the restart has happened, once the web server is off no more messages.
	                progress = 98; //This looks like its almost restarted, then if it does it will flip over to 'Starting' messages
	            }
	
	            if(data.startupProgress < 100)
	                progress = data.startupProgress;
	

	            
	            //If the interval is > 100 then we should redirect, just remember at this point we could be shutting down
	             if((data.startupProgress >= 100) && (data.shutdownProgress == 0)){
	                 progress = 100; //Ready for start, redirect now
	                 redirect = true;
	             }
	            
	            //Do redirect?
	            if(redirect){
	                setTimeout(function(){
	                    window.location.href = data.startupUri;
	                }, 500);
	               
	            }
                //Update the progress bar
                myProgressBar.set("value", progress + "%");
	            
	            pollLock = false; 
            },
            error: function(error){
                pollLock = false;           
            }
        });
    }
    
});
</script>
</tag:page>
