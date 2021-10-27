/**
*   simple sendmail  .. lg kahn kahn@lgk.com
*  child sendmail device v1
*/

attribute "lastCommand", "string"
attribute "myHostName", "string"

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import groovy.transform.Field    
import java.util.concurrent.ConcurrentLinkedQueue

@Field static java.util.concurrent.Semaphore mutex = new java.util.concurrent.Semaphore(1)
@Field static java.util.concurrent.Semaphore lastStateMutex = new java.util.concurrent.Semaphore(1)

@Field static lqueue = new java.util.concurrent.ConcurrentLinkedQueue()

preferences {
//	input("EmailServer", "text", title: "Email Server:", description: "Enter location of email server", required: true)
	//input("EmailPort", "integer", title: "Port #:", description: "Enter port number, default 25", defaultValue: 25)
//	input("From", "text", title: "From:", description: "", required: true)
 //	input("To", "text", title: "To:", description: "", required: true)
//	input("Subject", "text", title: "Subject:", description: "")
 //   input("myHostName", "text", title: "Your host name:", description: "Fully qualified domain/hostname (FQDN) to use in initial HELO/EHELO command. If blank you ip address will be used?", required: false)
 //   input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
  //  input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true)
  //  input("Authenticate", "bool", title: "Use Authentication on the server?", required: false, defaultValue: false)
  //  input("RequiresEHLO", "bool", title: "Does the server require the EHLO command instead of the std HELO?", required: false, defaultValue: false)
  //  input("Username", "text", title: "Username for Authentication - (base64 encoded)?", required: false, defaultValue: "")
 //  input("Password", "password", title: "Password for Authentication - (base64 encoded)?", required: false, defaultValue: "")
}

metadata {
    definition (name: "LGK Sendmail V3 Child", namespace: "lgkapps", author: "larry kahn kahn@lgk.com") {
   // capability "Notification"
  //  capability "Actuator"
	//capability "Telnet"
//	capability "Configuration"
        
        
    command "sendMessage", ["String"]
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()   
}

def configure()
{
    initialize()
}

def logsOff()
{
    log.debug "Turning off Logging!"
    device.updateSetting("debug",[value:"false",type:"bool"])
}

def initialize() {

    state.debug = false
    state.descLog = false
    state.lastMsg = ""
	state.LastCode = 0
	state.EmailBody = ""
    state.lastCommand = "quit"
   
    def ld = parent.getDebug()
    state.debug = ld
    state.descLog = parent.getDescLog()  
    if (state.debug) log.debug "in child initialize"
    
    mutex.release()
    if (state.debug) 
    { 
        log.debug "parent debug = ${parent.getDebug()}"
        log.debug "parent host = ${parent.getMyHostName()}"
    }
    
    def myHostname = parent.getMyHostName()

    if ((!myHostName) || (myHostName == ""))
    {
        if (state.debug) log.info "User specified hostname is blank using IP instead."
        def hub = location.hubs[0]
	    def myName =  "[" + hub.getDataValue("localIP") + "]"
    
        state.myHostName = myName
        sendEvent(name: "myHostName", value: myName)
    }
    
    else
    {
        state.myHostName = myHostName
        sendEvent(name: "myHostName", value: myHostName) 
    }
    
   if (state.descLog)  log.info "Descriptive Text logging is on."
   else log.info "Description Text logging if off."
    
   if (state.debug) 
    {
        log.info "Debug logging is on. Turning off debug logging in 1/2 hour."
        runIn(1800,logsOff)
    }
   else log.info "Debug logging is off."
   
}

def restartFromRunIn()
{  
    processQueue(true)
}

def processQueue(Boolean fromRunIn = false)
{
    def Integer waitTime = 30000
    def Boolean doprocess  = false
    def String msg = []
    
       if (state.debug)  log.debug "in process queue queue = $lqueue, fromRunIn = $fromRunIn"
      if (mutex.tryAcquire(waitTime,TimeUnit.MILLISECONDS))
       {  
        def isempty = lqueue.isEmpty()
         if (state.debug)  log.debug "in process queue current empty = $isempty"

        if (!isempty)
         {
         if (state.debug) 
             {
                 log.debug "Getting item to process"
     	         log.debug "Got mutex"
             }
             
           msg = lqueue.poll()
            if (state.debug)  log.debug "Got item $msg to process."
           mutex.release()   
           doProcess = true
         }
       else
           {
               // queue is empty so reset state just in case
               synchronized (lastStateMutex) { state.lastCommand = "Sent Ok" }
               sendEvent(name: "lastCommand", value: "Sent Ok")  
           }
       }        
   
    else
    {
        log.debug "Lock Acquire failed ... Aborting!"
        mutex.release()
        unschedule()
        exit
    }
    
    mutex.release()  
    if (doProcess) deviceNotification(msg,fromRunIn) 
    
     if (state.debug) log.debug "after run queue = $lqueue"
}     

def addToQueue(String message)

{
     if (state.debug) 
    {
        log.debug "in add to queue current queue = $lqueue"
        log.debug("Acquiring semaphore.")
    }
    
    def Integer  waitTime = 30000
    
    if (mutex.tryAcquire(waitTime,TimeUnit.MILLISECONDS))
    {    
        
       def isempty = lqueue.isEmpty()
       if (state.debug) log.debug "in process queue current empty = $empty"
       lqueue.add(message)
    }
    else
    {
        log.debug "Lock Acquire failed ... Aborting"
        mutex.release()
        unschedule()
        exit
    }
    
    mutex.release()
     if (state.debug)  log.debug "after queue = $lqueue"
}

int DnitoID()
{
    // get my id and store in the state to use to past back status to parent.
    
  String dni = device.deviceNetworkId
 
    // no parse it
    def pair = dni.split("-")
    def String myId = pair[1]
  
    
    def int intid = myId.toInteger()

    return intid
}  
    
    
def sendMessage(String message)
{
    def ld = parent.getDebug()
    state.debug = ld
    state.descLog = parent.getDescLog()
    
   deviceNotification(message)
}

def cleanUp()
{
                if(state.debug) log.debug "In cleanup ... called from parent!"
                unschedule()
                sendEvent(name: "lastCommand", value: "Force Closed")
                synchronized (lastStateMutex) { state.lastCommand = "Force Closed" }
                initialize()
}


def deviceNotification(String message, Boolean fromRunIn = false) {

def version = parent.getVersion()
def Boolean goOn = true
state.messageSent = false  
sendEvent(name: "telnet", value: "Ok")
 
synchronized (lastStateMutex)
    {
     if (state.lastCommand == null) state.lastCommand = "quit"
    }
    
    if (state.debug) log.debug "-------> In lgk sendmail Version ($version)"
    
    // lgk now check if we are already in middle of a message and if so wait for a minute.. if after that we still are force close the connection and resume this
    // message
    def oldState
    synchronized (lastStateMutex) { oldState = state.lastCommand }
    
     if (state.debug)  log.debug "Initial state found: ($oldState)"
    
    if (!((oldState == "Sent Ok") || (oldState == "Send Failed") || (oldState == "Connection Closed") || (oldState == "quit") || (oldState == "Force Closed")))   
      { 
          if (fromRunIn == true)
            {  
                
                def Integer addAmount = Math.floor((Math.random() * 60) + 1)
                def Integer waitTime = 10 + addAmount
                    
                // lgk dont re-add to queue as it will try this forever.
                log.debug "2nd attempt to run failed after sleeping... Clearing scheduling, resetting states and aborting!"
                unschedule()
                sendEvent(name: "lastCommand", value: "Force Closed")
              // addToQueue(message)
                synchronized (lastStateMutex) { state.lastCommand = "Force Closed" }
                goOn = false
                
                //uppdate status of parent.
                def int intId =  DnitoID()
                if (state.debug) log.debug "My internal id = $intId"
                parent.updateStatus(intId,"Failed")
                
                // if debuging on redo the job to turn off
                if (state.debug) 
                 {
                  log.info "Debug logging is on. Turning off debug logging in 1/2 hour."
                  runIn(1800,logsOff)
                 }
                     
             runIn(waitTime,"restartFromRunIn", [overwrite: false])   
            }
          
         else
              {
                goOn = false
                  
                 // lgk hubitat slower starting up with connections and multi mails are pilling up.
                 // so to avoid make wait time a random number added to 30 seconds
    
                def Integer addAmount = Math.floor((Math.random() * 60) + 1)
                def Integer waitTime = 30 + addAmount
          
                  
                  if (state.debug || state.descLog) log.info "Existing state ($oldState) indicates last run did not complete. Adding to queue, Waiting $waitTime secs. then trying again!"
                  sendEvent(name: "lastCommand", value: "Force Closed")
                  addToQueue(message)
  
                // now reschedule this queue item.
                runIn(waitTime,"restartFromRunIn", [overwrite: false]) 
              }
         }

       if (goOn)
        { 
             if (state.debug)  log.debug "Found ok initial state ($oldState) ... going on!"
            
           synchronized (lastStateMutex) { state.lastCommand = "initialConnect"
                                           sendEvent(name: "lastCommand", value: "initialConnect")
                                         }
            
            if (state.debug) synchronized (lastStateMutex) { log.debug "set last command to $state.lastCommand" }
    
	       state.EmailBody = "${message}"
	       state.LastCode = 0
           def String EmailServer = parent.getEmailServer()
           def String EmailPort = parent.getEmailPort()
            
            if (state.debug)  log.debug "Connecting to ${EmailServer}:${EmailPort}"
	
            // handle failed connect probabaly due to bad ip address
              try {
                    telnetConnect(EmailServer, EmailPort.toInteger(), null, null) 
                  
                } catch(e) {
                       log.error "Connect failed. Either your internet is down or check the ip address of your Server:Port: $EmailServer:$EmailPort !"
                       // force clean here by running immediately rather than duplicating code
                       addToQueue(message)
                      // now reschedule this queue item.
                     runIn(2,"restartFromRunIn", [overwrite: false]) 
              }
        }
}

def sendData(String msg, Integer millsec) {
    
      if (state.debug)  log.debug "$msg"
	
	def hubCmd = sendHubCommand(new hubitat.device.HubAction("${msg}", hubitat.device.Protocol.TELNET))
	pauseExecution(millsec)
	
	return hubCmd
}

def parse(String msg) {  
  
   def lastCommand
   def String Username = parent.getUsername()
   def String Password = parent.getPassword()
    
    synchronized (lastStateMutex) { lastCommand = state.lastCommand }
    
      if (state.debug) {
        log.debug "In parse - ${msg}"
        log.debug "lastCommand = $lastCommand"
    }
    
    def first4 = msg.substring(0,4)
    if (first4 == "250-")
    {
        log.debug "Skipping informational command: $msg after ehlo!"
    }
    else
    {
        
    def pair = msg.split(" ")
    def response = pair[0]
    def value = pair[1]
    
    if (state.debug) log.debug "Got server response $response value = $value lastCommand = ($lastCommand)"
    
   if (lastCommand == "initialConnect")
        {
           if (state.debug) log.debug "In initialConnect case"
             if (response == "220")
                 { 
                     sendEvent([name: "telnet", value: "Ok"])
                     if (RequiresEHLO)
                     {
                        if (state.debug) log.debug "Using EHLO instead of HELO!"
                        synchronized (lastStateMutex) { state.lastCommand = "ehlo" }
                        sendEvent(name: "lastCommand", value: "ehlo")      
                        def res1 = sendData("ehlo $state.myHostName",500)
                     }
                     else
                     {
                       synchronized (lastStateMutex) { state.lastCommand = "helo" }
                       sendEvent(name: "lastCommand", value: "helo") 
                       def res1 = sendData("helo $state.myHostName",500)
                     }
                 }
                 else
                 {
                    closeOnError()
                 }
                }
    
   else if (lastCommand == "Auth")
    {
     if (state.debug)  log.debug "In auth response looking for 334"
     if (response == "334")
     {
          if (state.debug)  log.debug "Got auth response now sending username"
 
       synchronized (lastStateMutex) { state.lastCommand = "Username" }
       sendEvent(name: "lastCommand", value: "Username")
        
       def res1 = sendData("$Username",500)
     }
    else  
    {
         log.debug "Got bad response for auth = $response"
         synchronized (lastStateMutex) { state.lastCommand = "Send Failed" }
         sendEvent(name: "lastCommand", value: "Send Failed")  
         closeConnection()
     }
    }   

   else if (lastCommand == "Username")
    {
        
        if (state.debug) log.debug "In Username response looking for 334"
        
     if (response == "334")
     {
        if (state.debug) log.debug "Got username response now sending password"
       synchronized (lastStateMutex) { state.lastCommand = "Password" }
       sendEvent(name: "lastCommand", value: "Password")
       def res1 = sendData("$Password",500)
     }
    else  
    {
         log.debug "Got bad response for Username = $response"
         synchronized (lastStateMutex) { state.lastCommand = "Send Failed" }
         sendEvent(name: "lastCommand", value: "Send Failed")  
         closeConnection()
     }
    }   
     
   
    else if ((lastCommand == "helo") || (lastCommand == "ehlo") || (lastCommand == "Password"))
        {
            
           if (state.debug) log.debug "In helo/ehlo/Password case"
            
         if ((response == "250") || (response == "235"))
         {
        
          if (((lastCommand == "helo") || (lastCommand == "ehlo")) && (Authenticate) && (Username) && (Password))
          {
              if (state.debug) log.debug "Trying authentication"
              
           synchronized (lastStateMutex) { state.lastCommand = "Auth" }
           sendEvent(name: "lastCommand", value: "Auth")
           def res1 = sendData("auth login",500)
          }

         else
           {
             if (state.debug) "Either got 250 for helo or 235 for password, Now Sending Message or ehlo"
               
            synchronized (lastStateMutex) { state.lastCommand = "sendmessage" }
            sendEvent(name: "lastCommand", value: "sendmessage") 
                    
            def msgData = "${state.EmailBody}"
	        def emlBody = ""
	        def emlSubject = ""
            def emlDateTime = new Date().format('EEE, dd MMM YYYY H:mm:ss Z',location.timeZone)
               
            def String idP1 = now()
            def String idP2 = java.util.UUID.randomUUID().toString().replaceAll('-', '')
            def String msgId = '<' + idP2 + "." + idP1 + "@2LGKSendmailV3>"
           
               def String Subject = parent.getSubject()
               def String From = parent.getFrom()
               def String To = parent.getTo()
            
               
          if(msgData.substring(0,1) == "{") {
	             	
		        def slurper = new groovy.json.JsonSlurper()
		        def result = slurper.parseText(msgData)	      
                emlBody = result.Body
		        emlSubject = (result.Subject != null ? result.Subject : "")
	        } else {
	           	emlBody = msgData
	        	emlSubject = (Subject != null ? "${Subject}" : "")
	        } 
        
	              def sndMsg =[
                      "MAIL FROM: <${From}>"
                    , "RCPT TO: <${To}>"
	        		, "DATA"
                    , "From: ${From}"
                    , "To: ${To}"
                    , "Date: ${emlDateTime}"
                    , "Message-ID: ${msgId}"
                    , "Subject: ${emlSubject}"  
                    , "MIME-Version: 1.0"
                    , 'Content-Type: text/plain; charset="utf-8"'
                    , "Content-Transfer-Encoding: quoted-printable\r\n"
                    , ""
	        		, "${emlBody}"
                    , ""
	        		, "."
		        	, "quit"
	            ]  
                   
                 def res1 = seqSend(sndMsg,500) 
                 state.messageSent = true  
                 if (state.debug || state.descLog) log.info "Sent Message: $emlBody" 
               
                // call parent to update status
                 
                def int intId =  DnitoID()
                if (state.debug) log.debug "My internal id = $intId"
                parent.updateStatus(intId,"Completed")      
         }
         }
         else
         {
            closeOnError()
         }
        }
    
    else if (lastCommand == "sendmessage")
        {
          if (state.debug)  log.debug "In send message case"
             if ((response == "220") || (response == "250"))
                 {
                      if (state.debug)  log.info "sending quit"
                     synchronized (lastStateMutex) { state.lastCommand = "quit" } 
                     sendEvent(name: "lastCommand", value: "quit")     
                     def res1 = sendData("quit",500)
                 }
                 else
                 {
                    closeOnError()
                 }
                }
       else if ((lastCommand == "quit") || ((response == "221") && (lastCommand == "other")))
                                            //&& ((value == "bye") || (value == "2.0.0") || (value = "Goodbye"))))
        {
             if (state.debug) log.debug "In quit case"
               if (response == "220" || response == "221" || response == "250")
                 { 
                   if (state.messageSent)
                     {
                      synchronized (lastStateMutex) { state.lastCommand = "Sent Ok" }
                      sendEvent(name: "lastCommand", value: "Sent Ok")  
                     }
                   closeConnection()
                   processQueue()
                 }
                 else
                 {
                      closeOnError()
                 }
           } 
        else 
        {
            if (response == "250" || response == "354")
            //|| response == "221")
            {
               synchronized (lastStateMutex) { state.lastCommand = "other" }
               sendEvent(name: "lastCommand", value: "other")
            }
            else
            {
              closeOnError()
            }
        }         
    }                 
}

def telnetStatus(status) {
    // comment out telent status as it is not working correctly and the system is telling me the socket is closed and other
    // response are still returned on the socket after. ignore stream is closed errors.
    if (status != "receive error: Stream is closed") 
    {    
          if (state.debug) synchronized (lastStateMutex) { log.debug "telnetStatus: ${status} lastcommand is ($state.lastCommand)" }
        sendEvent([name: "telnet", value: "${status}"])
    }
}

def closeConnection()
{
    if (closeTelnet){
                try {
                    log.error "Calling telnet close now!!!!"
                    telnetClose()
                    synchronized (lastStateMutex) { state.lastCommand = "Connection Closed" }
                    sendEvent(name: "lastCommand", value: "Connection Closed")
                } catch(e) {
                       if (state.debug) log.debug("Connection Closed")
                }
                
			}
}
    
boolean seqSend(msgs, Integer millisec)
{
     if (state.debug) log.debug "in sendData"
  
			msgs.each {
				sendData("${it}",millisec)
			}
			seqSent = true
	return seqSent
}

def closeOnError()
{
     log.debug "Got bad response = $response"
     synchronized (lastStateMutex) { state.lastCommand = "Send Failed" }
     sendEvent(name: "lastCommand", value: "Send Failed")  
     closeConnection()    
}


void uninstalled() {
  try {
 
      log.debug("Child: ${device.getName()} - (${device.getDeviceNetworkId()})  Deleted.");
  }
  catch (Exception e) {
    log.error("Exception in uninstalled(): ${e}");
  }
}
