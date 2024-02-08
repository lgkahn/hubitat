/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Author: LGKahn kahn-st@lgk.com
 *  version 2 user defineable timeout before checking if door opened or closed correctly. Raised default to 25 secs. You can reduce it to 15 secs. if you have custom simulated door with < 6 sec wait.
 * Version 3. add code to get name of device so messages don't alwasy says garage door for instance if you are using it on a fence gate etc.
 * Version 4 . Port to Hubitat. 
*  version 4.1 change doorcontrol to virtualgaragedoor for hubitat.

* lgk 2/24 v 4.2 add option to blink a light x times with y delay between each blink before closing
*/

definition(
    name: "LGK Virtual Garage Door",
    namespace: "lgkapps",
    author: "lgkahn kahn-st@lgk.com",
    description: "Sync the Simulated garage door device/gate/fence with 2 actual devices, either a tilt or contact sensor and a switch or relay. The simulated device will then control the actual garage door. In addition, the virtual device will sync when the garage door is opened manually, \n It also attempts to double check the door was actually closed in case the beam was crossed. ",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Choose the switch/relay that opens closes the garage?"){
		input "opener", "capability.switch", title: "Physical Garage Opener?", required: true
	}
	section("Choose the sensor that senses if the garage is open closed? "){
		input "sensor", "capability.contactSensor", title: "Physical Garage Door Open/Closed?", required: true
	}
    
	section("Choose the Virtual Garage Door Device? "){
		input "virtualgd", "capability.garageDoorControl", title: "Virtual Garage Door?", required: true
	}
    
	section("Choose the Virtual Garage Door Device sensor (same as above device)?"){
		input "virtualgdbutton", "capability.contactSensor", title: "Virtual Garage Door Open/Close Sensor?", required: true
	}
    
    section("Timeout before checking if the door opened or closed correctly?"){
		input "checkTimeout", "number", title: "Door Operation Check Timeout?", required: true, defaultValue: 25
	}
    
      
 section("Blinking light on closing options"){
		input "blink", "bool", title: "Enable a light to blink to signal closing?", required: true, defaultValue: false
  
     
		input "theLight", "capability.switch", title: "Choose a light?", required: false
		input "blinkTimes", "number", title: "Number of times to blink before closing door?", required: false, defaultValue: 6
	      
            input "blinkTime", "enum", title: "Blink on/off time in seconds?" , options: [
                                "1/2 second",
                                "1 second",
                                "2 seconds"
                              
			], required: false, defaultValue: "1/2 second"
	} 
    
     section( "Notifications" ) {
       // input("recipients", "contact", title: "Send notifications to") {
        input "sendPushMessage", "capability.notification", title: "Send Push Notifications? - Notification Devices: Hubitat PhoneApp or Other?", multiple: true, required: false

           }
         
       section("Logging" ) {
          input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
          input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true)
         }

    }

def installed()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact

	subscribe(sensor, "contact", contactHandler)
    subscribe(virtualgdbutton, "contact", virtualgdcontactHandler)
    
    // sync them up if need be set virtual same as actual
    if (realgdstate != virtualgdstate)
     {
        if (realgdstate == "open")
           {
             virtualgd.open()
            }
         else virtualgd.close()
      }
    
   if (descLog) log.info "Descriptive Text logging is on."
   else log.info "Description Text logging is off."
    
   if (debug)
    {
        log.info "Debug logging is on. Turning off debug logging in 1/2 hour."
        runIn(1800,logsOff)
    }
   else log.info "Debug logging is off."
      
    
 }

def updated()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact

	unsubscribe()
	subscribe(sensor, "contact", contactHandler)
    subscribe(virtualgdbutton, "contact", virtualgdcontactHandler)
    
    // sync them up if need be set virtual same as actual
    if (realgdstate != virtualgdstate)
     {
        if (realgdstate == "open")
           {
             log.info "opening virtual door"
             mysend("virtualgd.displayName Opened!")     
             virtualgd.open()
            }
         else {
              virtualgd.close()
              log.info "closing virtual door"
              mysend("$virtualgd.displayName Closed!")   
     		 }
      }
  // for debugging and testing uncomment  temperatureHandlerTest()
   if (descLog) log.info "Descriptive Text logging is on."
   else log.info "Description Text logging is off."
    
   if (debug)
    {
        log.info "Debug logging is on. Turning off debug logging in 1/2 hour."
        runIn(1800,logsOff)
    }
   else log.info "Debug logging is off."
           
}


def contactHandler(evt) 
{
def virtualgdstate = virtualgd.currentContact
// how to determine which contact

  if("open" == evt.value)
    {
    // contact was opened, turn on a light maybe?
    if (debug) log.info "Contact is in ${evt.value} state"
    // reset virtual door if necessary
    if (virtualgdstate != "open")
      {
        mysend("$virtualgd.displayName Opened. Manually syncing with Virtual Device!")   
        virtualgd.open()
      }
     }  
  if("closed" == evt.value)
   {
   // contact was closed, turn off the light?
    if (debug) log.debug "Contact is in ${evt.value} state"
    //reset virtual door
     if (virtualgdstate != "closed")
      {    
       mysend("$virtualgd.displayName Closed. Manually syncing with Virtual Device!")   
       virtualgd.close()
      }
   }
}

def virtualgdcontactHandler(evt) {
// how to determine which contact
def realgdstate = sensor.currentContact
    
    if (debug) log.debug "in virtualgd contact handler check timeout = $checkTimeout"

  if("open" == evt.value)
    {
    // contact was opened, turn on a light maybe?
    if (debug) log.debug "Contact is in ${evt.value} state"
    // check to see if door is not in open state if so open
    if (realgdstate != "open")
      {
        if (desclog) log.debug "Opening real gd to correspond with button press."
         mysend("$virtualgd.displayName Opened. Syncing with Actual Device!")   
         opener.on()
         if (debug) log.debug "scheduling checkifActuallyOpened via runin for $checkTimeout seconds."
         runIn(checkTimeout, checkIfActuallyOpened)
        
      }
     }
  if("closed" == evt.value)
   {
    // contact was closed, turn off the light?
    if (debug) log.debug "Contact is in ${evt.value} state"
    if (realgdstate != "closed")
      {
        if (descLog) log.info "closing real gd to correspond with button press."
        mysend("$virtualgd.displayName Closed. Syncing with Actual Device!")   
        closeTheDoor()
       
        if(debug) log.debug "Schedulng checkIfActuallyClosed via runIn for $checkTimeout seconds."
        runIn(checkTimeout, checkIfActuallyClosed)
      }
   }
}


private mysend(msg) {
    
     // check that contact book is enabled and recipients selected
    if (location.contactBookEnabled && recipients) {
        if (debug) log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    } else {
        if (sendPushMessage) {
         if (debug) log.debug("Sending Push Notification...")
            sendPushMessage.deviceNotification(msg)
        } 
        
    }
}

def checkIfActuallyClosed()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact
 
    if (debug)
    {
      log.debug "In checkIfActuallyClosed."
      log.debug "in checkifopen ... current state=  $realgdstate"
      log.debug "in checkifopen ... gd state= $virtualgd.currentContact"
    }
   
    // sync them up if need be set virtual same as actual
    if (realgdstate == "open" && virtualgdstate == "closed")
     {
             log.debug "opening virtual door as it didnt close.. beam probably crossed"
             mysend("Resetting $virtualgd.displayName to Open as real device didn't close (beam probably crossed)!")   
             virtualgd.open()
    }   
}

def checkIfActuallyOpened()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact

    if (debug) 
    {
        log.debug "In checkIfActuallyOpened."
        log.debug "in checkifopen ... current state=  $realgdstate"
        log.debug "in checkifopen ... gd state= $virtualgd.currentContact"
    }
  
   
    // sync them up if need be set virtual same as actual
    if (realgdstate == "closed" && virtualgdstate == "open")
     {
             log.debug "opening virtual door as it didnt open... track blocked?"
             mysend("Resetting $virtualgd.displayName to Closed as real device didn't open! (track blocked?)")   
             virtualgd.close()
    }   
}

def closeTheDoor()
{  
    // close the door 
    // if option to blink lights blink
    
    log.debug "blink = $blink"
    
    if (blink)
    {
      //  log.debug "will blink"
        def delaytime = 1000
        
        if (blinkTime == "1/2 second")
          delaytime = 500
        else if (blinkTime == "1 second")
          delaytime = 1000
        else delaytime = 2000
        
       // log.debug "blink times = $blinktimes delay = $delaytime"
            
      for(int i = 1;i<blinkTimes;i++) {
         theLight.on()
          pauseExecution(delaytime)
         theLight.off()
          pauseExecution(delaytime)
      }
      
    }
    
     opener.on()
}

def logsOff()
{
    log.info "Turning off Logging!"
    device.updateSetting("debug",[value:"false",type:"bool"])
}

