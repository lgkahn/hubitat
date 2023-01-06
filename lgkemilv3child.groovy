/**
*   simple sendmail  .. lg kahn kahn@lgk.com
*  child sendmail device 
*/

attribute "lastCommand", "string"
attribute "myHostName", "string"

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import groovy.transform.Field   
import groovy.xml.XmlUtil

@Field static java.util.concurrent.Semaphore lastStateMutex = new java.util.concurrent.Semaphore(1)

preferences {

}

metadata {
    definition (name: "LGK Sendmail V3 Child", namespace: "lgkapps", author: "larry kahn kahn@lgk.com") {
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

def forceFailure()
{
    
    // used for testing for a bad state
            synchronized (lastStateMutex) { state.lastCommand = "Failed" }
               sendEvent(name: "lastCommand", value: "Failed")  
    
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
    
    if (state.debug || state.descLog)
    {
      if (state.descLog)  log.info "Descriptive Text logging is on."
        else log.info "Description Text logging is off."
    }
    
   if (state.debug) 
    {
        log.info "Debug logging is on. Turning off debug logging in 1/2 hour."
        runIn(1800,logsOff)
    }
   else if (state.debug) log.info "Debug logging is off."
   
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
                if (state.debug) log.debug "In cleanup ... called from parent!"
                unschedule()
                sendEvent(name: "lastCommand", value: "Force Closed")
                synchronized (lastStateMutex) { state.lastCommand = "Force Closed" }
                initialize()
}

def resetData()
{
                unschedule()
                sendEvent(name: "lastCommand", value: "Force Closed")
                synchronized (lastStateMutex) { state.lastCommand = "Force Closed" }
             
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
} 

def deviceNotification(String message) {

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
           log.info "Existing state ($oldState) is incorrect for processing. Resetting and aborting!"
           resetData() 
           goOn = false
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
                    closeConnection()
                    telnetConnect(EmailServer, EmailPort.toInteger(), null, null) 
                  
                } catch(e) {
                       log.error "Connect failed. Either your internet is down or check the ip address of your Server:Port: $EmailServer:$EmailPort !"
                       resetData() 
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
   def boolean Authenticate = parent.getAuthenticate()
    
    synchronized (lastStateMutex) { lastCommand = state.lastCommand }
    
      if (state.debug) {
        log.debug "In parse - ${msg}"
        log.debug "lastCommand = $lastCommand"
    }
    
    def first4 = msg.substring(0,4)
    if (first4 == "250-")
    {
        if (state.debug) log.debug "Skipping informational command: $msg after ehlo!"
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
                     if (parent.getRequiresEHLO())
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
             if (state.debug) log.debug "Either got 250 for helo or 235 for password, Now Sending Message or ehlo"
               
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
            def toReplaced = false 
            def cc = ""
            def ccFound = false
            
               
          if(msgData.substring(0,1) == "{") {
	             	
		        def slurper = new groovy.json.JsonSlurper()
		        def result = slurper.parseText(msgData)	      
                emlBody = result.Body
		        emlSubject = (result.Subject != null ? result.Subject : "")
	        } else {
	           	emlBody = msgData
	        	emlSubject = (Subject != null ? "${Subject}" : "")
	        } 
        
          // note order is important here from, to, subject,cc  
          if (state.debug) log.debug "before check for header replacment, subject = $emlSubject, body = $emlBody, from = $From, To = $To"
          
          if (emlBody.startsWith("rh-From:") && emlBody.indexOf(",") > -1)
               {
                   def io = emlBody.indexOf(",")
                   def len = emlBody.length()                             
                   if (state.debug) log.debug "found replace header for From! index = $io, len = $len" 
                   def newFrom = emlBody.substring(0,io)
                   def messageSplit = emlBody.substring(io+1,len)           
                   From = newFrom.replace("rh-From: ", "").replace("rh-From:", "")
                   emlBody = messageSplit.trim()
                }  
               
             if (emlBody.startsWith("rh-To:") && emlBody.indexOf(",") > -1)
               {
                   def io = emlBody.indexOf(",")
                   def len = emlBody.length()                                   
                   if (state.debug) log.debug "found replace header for To! index = $io, len = $len" 
                   def newTo = emlBody.substring(0,io)
                   def messageSplit = emlBody.substring(io+1,len)
                   To = newTo.replace("rh-To: ", "").replace("rh-To:", "")
                   emlBody = messageSplit.trim()
                   toReplaced = true
                } 
               
            if (emlBody.startsWith("rh-Subject:") && emlBody.indexOf(",") > -1)
               {
                   def io = emlBody.indexOf(",")
                   def len = emlBody.length()                  
                   if (state.debug) log.debug "found replace header for Subject!"                  
                   def newSubject = emlBody.substring(0,io)
                   def messageSplit = emlBody.substring(io+1,len)
                   emlSubject = newSubject.replace("rh-Subject: ", "").replace("rh-Subject:", "")
                   emlBody = messageSplit.trim()
                }   
               
              if (emlBody.startsWith("Subject:") && emlBody.indexOf(",") > -1)
               {
                   def io = emlBody.indexOf(",")
                   def len = emlBody.length()                  
                   if (state.debug) log.debug "found replace header for Subject!"                  
                   def newSubject = emlBody.substring(0,io)
                   def messageSplit = emlBody.substring(io+1,len)
                   emlSubject = newSubject.replace("Subject: ", "").replace("Subject:", "")
                   emlBody = messageSplit.trim()
                } 
               
            if (emlBody.startsWith("rh-CC:") && emlBody.indexOf(",") > -1)
               {
                   def io = emlBody.indexOf(",")
                   def len = emlBody.length() 
                   if (state.debug) log.debug "found replace header for CC!" 
                   def newCC = emlBody.substring(0,io)
                   def messageSplit = emlBody.substring(io+1,len)
                   cc = newCC.replace("rh-CC: ", "")replace("rh-CC:", "")
                   emlBody = messageSplit.trim()
                   ccFound = true 
                }    
                             
               if (state.debug) log.debug "After check new subject = *${emlSubject}*, new body = $emlBody, newTo = *${To}*, new From = *${From}*, CC = *${cc}"
               
            def toList = To.split(",")
            def toListSize = toList.size()
                             
             if (state.debug) log.debug "Number of To addresses = $toListSize"
             if (state.debug) log.debug "From = ${From}, To = ${To}, Subject = ${emlSubject}"
               
	              def sndMsg =[
                    "MAIL FROM: <${From}>" ]
               
                  if (toListSize > 1)
                   {
                     for (ctr= 0; ctr < toListSize; ctr++)
                       {
                        def oneToAddress = toList[ctr].replaceAll(' ','')
                        if (state.debug) log.debug "one email address = *$oneToAddress*"
                        sndMsg = sndMsg + [  "RCPT TO: <${oneToAddress}>" ]
                       }
                   }
                   else
                       {
                        sndMsg = sndMsg + [ "RCPT TO: <${To}>" ]
                       }
               
               if (ccFound)
               {
                 sndMsg = sndMsg + [ "RCPT TO: <${cc}>" ]   
                   
               sndMsg = sndMsg +
                    [ "DATA"
                    , "From: ${From}"
                    , "To: ${To}" 
                    , "CC: ${cc}"
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
                   
               }
               else
               {
                   sndMsg = sndMsg +
                    [ "DATA"
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
                   
               }
               
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
    if (state.debug) log.debug "in sendData, message = ${msgs}"
  
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
