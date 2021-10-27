/**
*   simple sendmail  .. lg kahn kahn@lgk.com
*  You should have received a copy of the GNU General Public License
*  along with this program.  If not, see <https://www.gnu.org/licenses/>.
* 
* v 2.1 added desciprtive text and cleaned up debugging
* v 2.2 added hostname option and lookup ip if blank.
* v 2.3 added better checking for end of send to make sure it is closing cleanly
* v 2.4 add serial queueing so that a new connect waits while old finishes.. set timeout to 1 minute to wait. if email is not finished by
*       then close connection and start new one. Also add state.lastCommand to follow the attribute as that seems more reliable.
*       to this function telling me socket is closed but it obviously is not because responses are still asynchronously coming basck after!
*      This "Stream is closed" messages seems to always come out after the quit command so it appears to be a status message not really an error
* v 2.4.2. try to workaround the check i had for the goodbye/bye message .. I was checkiong the value string on code 221 which was not a good
*          approach as various servers can return differnent strings here. Unfortunately 221 just means remote server is closing so need to checkt
*          that the message was sent and it is 221,
* v 2.4.3 true concurrency and queue using a thread safe data structure concurrentlinkedqueue and also mutex (thanks to erktrek)
*         tested with up to 8 messages
*         When one message finishes it checks the queue, and delivers any other remaining.. also schedules a rerun when busy and one is added to the queue.
*         also serialize the setting and checking of the state.lastCommand as this is used to keep track of the async status of the commands
* 
*  v 2.5 changes were enough to signify new version. Found one bug in last one when reviewing code. a synchronize inside a synchronize on same semaphore.
*         Did not seem to be causing issues so I assume the system is smart enough to avoid it but fixed anyway.
* v 2.5.1 mine got stuck in weird state and kept re-running with failure.. added an unschedule to fix it in certain cases and reset states. Also removed a function no longer called.
*         Also reset state variable when it finds queue empty.
* v 2.5.2 change password input type to password from text
* v 2.5.3 auto turn off logging after 1/2 hour
* v 2.5.4 change formatting of date header
* v 2.5.5 change time formatting yet again.
* v 2.5.6 put extra lines in send to try to get around error in hubitat 2.2.5
* v 2.5.7 removed extra lines as bug is 2.2.5 and nothing to do with the lines.
* v 2.5.8 fixed email.. as summized version 2.2.5 telnet is stripping of extra lf or cr that are needed for email
* v 2.6 played with carriage return line feeds to get message back on first line..
* v 3.0 after recent release hubitat seems much slower getting the initial connect to the telnet port and therefore messages are stacking up .
* for this reason instead of queuing and trying 30 seconds later I have added a random component so it will queue and try 30 + 1-60 secs later.
* v 3.1 add desc. logging option default is on.. only a few lines of info come out when debugging is off. One for each email and a couple of others if queued up.
* this turns all that off.
* NOte: just noticed that if you send a bunch of message to the queue and also at the same time to another instance of the driver (ie another notification device) the queue
* is not distince. Meaining that all run in one process/thread.. The assumption is that each would be independent. Not the case . So the wrong device can get a message sent but it is rare.
* will look at somehow appending the device id to the queue and ignoring those not for you, but that is a big change .
* v 3.2 only one of the /r/n was needed before the body.. m
* v 3.3 got mms working from AT&T yeah.. so that we now can get all the msgs in one thread instead of the random number.
*
* V 3.4
* very very complicated. Spend about 6 hours on it.. here are my findings:
* 1. the date header must be there and in a specific format. It is so picky time zone must be in -0500 etc format not EST.
* 2. all FROM and TO emails must be in the form kahn@lgk.com <kahn@lgk.com>. It automatically constructs this so don't enter them that way.
* 3. there must be headers for Message-ID , also enclosed in <>, MIME-Version, Content-Type, and Content-Transfer-Encoding.
* If any of these are missing or wrong the mms doesn't work from AT&T and you get an error email back.
* This thing is so damn picky and there is no documentaton on it whatsoever.
* Send some Beers my way.. What a pain the Ass Hopefully in works for everyone on your existing servers. If not let me know!
* It was also complicated to generate the unique message id. GetHubUuid() did not work. 
* It works without a unique id, but I tried to generate one anyway. It uses a random UUID from a javascript function
* concatenated with the number of secs since midnight 1972 etc.
*
* v 3.5 Some email servers did not like the "email" <email> format, so trying just <email> in the recpt to and from headers.
*
* v 3.5.1 as someone pointed out the character map on hubitat is utf-8 not ascii so some special characters in emails were not being displayed correctly.
* v 3.6 noticed while i was having internet problems that the app kept spawning off retrying forever.. Notice the 2 attempts was not working.
* fixed it so it gives up after two failed attemps. also unchedules all pending and resets and tries again.
* v 4. add optional ehlo for servers that require it. Also need to handle the additional processing of EHLO informational messages coming back.
* v 4.01 bug gound with two typos with misspelling of lastCommand. Fix brought to light corresponding missing 250 checks.
* v 4 remove quit and close telnet coming out just for desc logging. Remove extaneous character at end of strings.

* Total rewrite here to scale much better for concurrenty!
* I create child devices (default is 5) but if you want more it is configurable.. Each child runs all its own data and can run totally concurrent with each other.

* Prior with the queuing it still sometimes would loose a message if two came in at exactly the right time. Even protected with semaphores the state variables could step on each other.
* This should never happend now. This also should scale really well say if you want to be able to send 50 concurrent messages very quickly you should be able to do that.

* There is also a new command called testConcurrancy which will start up a test email in each of your child simultaneously.
*       also, implemented the Send message as a call to deviceNotification
* new parent.. instead of queing.. create 5 child devices and an array/map of them and current message being processed and status.
* when a new message comes in go through the list.. and find an available child and pass the message to it to process. mark it as being processed and
* clean up any that havent finished (ie mark free) in the given time out... default 2 minutes.
*
* Not released to hubitat package manager yet but put out there in my git hub.. you will need to install both LGK SendMail V3 and SendMailV3 child.


* 4.1 slight change to make children components of the parent so that only way to delete is through parent because if you deleted child directly it would screw up the app when it tries to pass
* a message to it. Also calculated friendly child name based on parent name.

*/

attribute "lastCommand", "string"
attribute "myHostName", "string"
import java.util.ArrayList; 
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import groovy.transform.Field

@Field static java.util.concurrent.Semaphore mutex = new java.util.concurrent.Semaphore(1)
@Field static java.util.ArrayList<Map<Integer,String,String,Long,Integer>> messageStatus = new ArrayList<Map<Integer,String,String,Long,Integer>>();
//List<Map<Integer,String,String,Integer>> maps = new ArrayList<Map<Integer,String,String,Integer>>()

preferences {
	input("EmailServer", "text", title: "Email Server:", description: "Enter location of email server", required: true)
	input("EmailPort", "number", title: "Port #:", description: "Enter port number, default 25", defaultValue: 25)
	input("From", "text", title: "From:", description: "", required: true)
 	input("To", "text", title: "To:", description: "", required: true)
	input("Subject", "text", title: "Subject:", description: "")
    input("myHostName", "text", title: "Your host name:", description: "Fully qualified domain/hostname (FQDN) to use in initial HELO/EHELO command. If blank you ip address will be used?", defaultValue: " ", required: false)
    input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
    input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true)
    input("Authenticate", "bool", title: "Use Authentication on the server?", required: false, defaultValue: false)
    input("RequiresEHLO", "bool", title: "Does the server require the EHLO command instead of the std HELO?", required: false, defaultValue: false)
    input("Username", "text", title: "Username for Authentication - (base64 encoded)?", required: false, defaultValue: "")
    input("Password", "password", title: "Password for Authentication - (base64 encoded)?", required: false, defaultValue: "")
    input("ConcurrentChildren", "number", title: "How many concurrent messages should we allow at once (NOTE: This many child devices will be created)?", defaultValue: 5, required: true)
}

metadata {
    definition (name: "LGK Sendmail V3", namespace: "lgkapps", author: "larry kahn kahn@lgk.com",  singleThreaded: false) {
    capability "Notification"
    capability "Actuator"
	capability "Telnet"
	capability "Configuration"
        
    command "testConcurrency"
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

String IdToDni(Integer which) {
    
  String pid = device.getId().concat("-") + String.valueOf(which)

  return pid;
    
}
  
String IdToLabel(Integer which) {
    
  def String label = device.getLabel()
    
  if (label == null)
    label = device.getName()
    
  String pid = label.concat(" - ") + String.valueOf(which)

  return pid;
    
}

def initialize() {
    
    if (debug) log.debug "in initialize"
    
    state.lastMsg = ""
	state.LastCode = 0
	state.EmailBody = ""
    state.lastCommand = "quit"
    def version = "4.1"
       
    log.debug "-------> In lgk sendmail Version ($version)"
      
    def long lasttime = now()
    mutex.release()
    
    if (state.messageStatus)
    messageStatus = state.messageStatus
 
   if (debug) log.debug "hostname = $myHostName"
    
    if (myHostName == " ")
    {
        log.info "User specified hostname is blank using IP instead."
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
    
   if (descLog) log.info "Descriptive Text logging is on."
   else log.info "Description Text logging if off."
    
   if (debug)
    {
        log.info "Debug logging is on. Turning off debug logging in 1/2 hour."
        runIn(1800,logsOff)
    }
   else log.info "Debug logging is off."
    
    // clear array if it exists
    
    
    // check if we already have children devices
     def List<com.hubitat.app.ChildDeviceWrapper> childlist = getChildDevices();   
     def int currentChildren = 0

    if (childlist)
    {
        currentChildren = childlist.size()   
    }
    // clean up children if necessary
    if ((currentChildren > 0) && (currentChildren != ConcurrentChildren))
    {
        log.debug "$currentChildren != Requested: $ConcurrentChildren so cleaning up/deleting and re-creating!"
        childenGarbageCollect()
    }
    
    else
    {
        log.debug "Current children match requested at $currentChildren ... no changes made!"

    }
    
     // recheck numbeer to make sure cleanup worked
      childlist = getChildDevices(); 
      currentChildren = childlist.size()
    
     // if (debug) log.debug "After cleanup: Children = $currentChildren"
     // now create children if neccesasry
     if ((currentChildren == 0) || (currentChildren != ConcurrentChildren))
      {
      
        if (messageStatus)
         {
           // log.debug "message size = $messageStatus.size()"
            messageStatus.clear()
            state.messageStatus = messageStatus
         }
          
        log.debug "Creating $ConcurrentChildren Child device handlers!"
    
        for (int childNum = 1; childNum <= ConcurrentChildren ; childNum++)
         {
      
           def String dni = IdToDni(childNum);
           def childName = "LGK Sendmail V3 Child - " + childNum.toString()
           def long ctime = now()
           def String childLabel = IdToLabel(childNum)
        
            messageStatus.add([childNum,"","Complete",ctime,1])
             
           //  log.debug "after add size = ${messageStatus.size()}"
             
             def ArrayList element = messageStatus.get(childNum-1)
             // now get element 
             log.debug "element $childNum retrieved = $element"
           
             def int index = element.get(0)
             def String message = element.get(1)
             def String status = element.get(2)
             ctime = element.get(3)
             def int tries = element.get(4)
            
             log.debug "index = $index, message = $message, status = $status time = $ctime, tries = $tries"
             // test
           
            log.debug "Creating child: $childName"     
            child = addChildDevice("LGK Sendmail V3 Child", dni, [name: childName, label: childLabel, isComponent: true]);
    }
 
    }
   
       // restore array
    if (Debug) log.debug "Before commit message status = $messageStatus"
    
     state.messageStatus = messageStatus
 
}


 int findFirstFreeElement()
{  
   
    def num = messageStatus.size()
   
    if (messageStatus && (num > 0))
    {
      if (debug) log.debug "Looping through all $num elements in the array to find first free one (!= In Process)"
   
     if (messageStatus)
        {
            
      for (ctr= 0; ctr < num; ctr++)
        {
           def ArrayList element2 = messageStatus.get(ctr)
   
      //  if (debug) log.debug "got element $element2"
        def status = element2.get(2)
        
            
      //  if (debug) log.debug " status = $status"
         if  (status != "In Process")
            {
                if(debug) log.debug "Found Free Element id: $ctr, $element2!"
                return ctr
            }
        }
        return -1
    }
}
 
}
    
void checkArrayForTimedoutMessages(long numSecs)
{
    if (debug) "Checking message array for timed out messages: timeout = $numSecs !"
 
 //  if (state.messageStatus)
//    {
//    messageStatus = state.messageStatus
     def num = messageStatus.size()
   // log.debug "num = $num"
    
    
    // loop through
   
    if (messageStatus && num > 0)
    {
      if (debug) log.debug "Looping through all $num elements in the array so see if any timed out"
      def long ctime = now()
         
     if (messageStatus) 
        {
      for (ctr= 0; ctr < num; ctr++)
        {
         def ArrayList element2 = messageStatus.get(ctr)
  
        // if (debug) log.debug "got element $element2"
            
         def long startTime = element2.get(3)
         def status = element2.get(2)

        // if (debug) log.debug "ctime = $ctime start time = $startTime, status = $status"
         def long diff = ctime - startTime
            
         def int seconds = (diff / 1000.0) 
         //if (debug) log.debug "diff in secs = $seconds"
         if  ((seconds > numSecs) && (status == "In Process"))
            {
                log.error "Element $element2 has timed out at $seconds seconds!"
                log.debug "Resetting Status"
                reinitElement(ctr+1,"Reinitialized")
            }
        }
        }
    }
}
        
void incrementTries(int index)
{
 if (index < 1)
    log.error "Incorrect index value pased to incrementTries!"
 else
 {

    def ArrayList element = messageStatus.get(index-1)
  
    // now get element 
    if (debug) log.debug "element retrieved = $element"
    def int currentTries = element.get(4)
     log.debug "current tries = $currentTries"
     // change status
    element.set(4,++currentTries)
    messageStatus.set(index-1,element)
    
    if (debug) 
     {
         def ArrayList newelement = messageStatus.get(index-1)
         log.debug "element after change = $newelement"  
     }      
 }
      state.messageStatus = messageStatus
}

void updateStatus(int index, String newStatus)
{

    if (index < 1)
    log.error "Incorrect index value pased to updateStatus!"
 else
 {

    def ArrayList element = messageStatus.get(index-1)
  
    // now get element 
    if (debug) log.debug "element retrieved = $element"
    
   // change status
    // get email for log
    def String sentMsg = element.get(1)
    element.set(2,newStatus)
    messageStatus.set(index-1,element)
    log.info "Sent Message via child($index): $sentMsg" 
     
 }
      state.messageStatus = messageStatus
}


void reinitElement(int index, String newStatus)
{
    
    if (index < 1)
    log.error "Incorrect index value pased to reinitElement!"
 else
 {

    def ArrayList element = messageStatus.get(index-1)
  
    // now get element 
    if (debug) log.debug "element retrieved = $element"
    
   // change status
    // get email for log
    def String sentMsg = element.get(1)
    element.set(2,newStatus)
    messageStatus.set(index-1,element)
     
 }
      state.messageStatus = messageStatus
}



void updateForProcessingStart(int index, String Message, long startTime)
{
 if (index < 0)
    log.error "Incorrect index value pased to UpdateForProcessingStart!"
 else
 {

     if (debug) log.debug "in update for processing start indeix = $index message = $Message start time = $startTime"
     
    def ArrayList element = messageStatus.get(index-1)
  
    // now get element 
    if (debug) log.debug "element retrieved = $element"
    
   // change data
    element.set(1,Message)
    element.set(2,"In Process")
    element.set(3,startTime)
    element.set(4,1)
    messageStatus.set(index-1,element)
    
 }
}

 Boolean getDebug()
{
    return debug
}

Boolean getDescLog()
{
    return descLog
}

Boolean getAuthenticate()
{
    return Authenticate
}

Boolean getRequiresEJLO()
{
    return RequiresEHLO
}

String getMyHostName()
{
    return myHostName
}

String getUsername()
{
    return Username
}

String getPassword()
{
    return Password
}

String getEmailServer()
{
  return EmailServer
}

Integer getEmailPort()
{
    return EmailPort
}

String getTo()
{
    
    return To
}

String getFrom()
{
    return From
}

String getSubject()
{
    return Subject
}

private void childenGarbageCollect() 
    {
     def List<com.hubitat.app.ChildDeviceWrapper> childlist = getChildDevices();
         
     if (childlist) childlist.each
        {
         String dni = it.getDeviceNetworkId();
         if(debug) log.debug "Deeleting device name = $it , id = $dni"
            
         deleteChildDevice(dni);
        }
    }

def sendMsg(String message)
{
    deviceNotification(message)
}

def deviceNotification(String message)
{
    mutex.release()
    
    if (state.messageStatus)
    messageStatus = state.messageStatus
 
     def Integer waitTime = 30000
    // linearize this code with a mutex.
     
     if (debug) log.debug "in process queue queue = $lqueue, fromRunIn = $fromRunIn"
      if (mutex.tryAcquire(waitTime,TimeUnit.MILLISECONDS))
       {  
          def version = "4.1"
          def Boolean goOn = true
          state.messageSent = false  
          sendEvent(name: "telnet", value: "Ok")
         
          if (debug) log.debug "-------> In lgk sendmail Version ($version)"
    
          if (debug) 
             {
                 log.debug "Getting free child for processing"
     	         log.debug "Got mutex"
             }
           
         // find free child for processing
         def freeChild = findFirstFreeElement()
         if (freeChild != -1)
           {
               if (debug) log.debug "Got free child: $freeChild for processing!"
               
              // now call the child to process it..
              def String dni = IdToDni(freeChild+1);
              if (debug) log.debug "dni = $dni"       
              com.hubitat.app.ChildDeviceWrapper theChild = getChildDevice(dni);
              def long time1 = now()
               
              if (debug) log.debug "got the child now calling function to send the mail now = $time1"
           
               //put stuff in array here with time 
               updateForProcessingStart(freeChild+1, message, time1)
               
              theChild.sendMessage(message)
              def long time2 = now()
           }
          else 
          {
              log.error "No Free Child found for processing ... calling timeout/clieanup function!"
          }
            
           checkArrayForTimedoutMessages(120)
           mutex.release()   
           doProcess = true
         }
     
     else
     {
        log.error "Lock Acquire failed ... Aborting!"
        mutex.release()
        unschedule()
        exit
    }
     
    state.messageStatus = messageStatus
    mutex.release()   
 
}  

void testConcurrency()
{
    log.debug "Testing Concurrency"
  
    if (state.messageStatus)
    messageStatus = state.messageStatus
 
    def numChildren = messageStatus.size()
    
    log.debug "Your currently have $numChildren child devices so we will test that many concurrent send Messages!"
    
    for (i=1; i<=numChildren; i++)
    {
        def MessageText = "Test Message $i"
        log.debug "Starting up Child $i!"
        deviceNotification(MessageText)
    }
    state.messageStatus = messageStatus 
    
}
    
void uninstalled() {
  //
  // Called once when the driver is deleted
  //
    log.debug "in uninstalled"
    
  try {
    // Delete all children
    List<com.hubitat.app.ChildDeviceWrapper> list = getChildDevices();
      if (list) list.each { log.debug "calling uninstall on a child - $it id = ${it.getDeviceNetworkId()}" 
                         deleteChildDevice(it.getDeviceNetworkId()); }

    log.debug("Uninstalled()!");
  }
  catch (Exception e) {
    logError("Exception in uninstalled(): ${e}");
  }
}



void uninstalledChildDevice(String dni) {
  //
  // Called by the children to notify the parent they are being uninstalled
  //
}
