/*  Copyright 2015 lgkahn
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *   version 1.0
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
metadata {
  definition (name: "Hubitat Smart Alarm Notification Device", namespace: "lgkapps", author: "Larry Kahn kahn@lgk.com") {
    capability "Notification"
    capability "Switch"
    capability "Configuration"
    
    command "onPhysical"
	command "offPhysical"
     attribute "armDisarm", "string"
     attribute "armStay", "string"
     attribute "armAway", "string"
     attribute "lastAlert", "string"
     attribute "lastAlertTime", "string"
     attribute "status", "string"
     attribute "armStatus", "string"
     attribute "statusText", "string"
     attribute "lastAlertType", "string"
     attribute "lastAlertDateTime", "string"
    
   
  }

}

def parse(String description) {
log.debug "in parse desc = $description"
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def installed() {
log.debug "in installed"
sendEvent(name: "status" , value: "All Ok")
sendEvent(name: "lastAlert" , value: "None")
sendEvent(name: "lastAlertType" , value: "None")
sendEvent(name: "armStatus" , value: "Unknown")
sendEvent(name: "statusText", value:"Unknown") 
sendEvent(name: "armDisarm", value: "unlite")
sendEvent(name: "armStay", value: "unlite")
sendEvent(name: "armAway", value: "unlite")
sendEvent(name: "lastAlertDateTime", value: "None")


}


def deviceNotification(String desc)
{
log.debug "in device notification"
log.debug "desc = $desc"

def currentTime = new Date(now())
//log.debug "got current time= $currentTime"
    
def parts = desc.split(":")

if (parts.length == 2)
{

  def command = parts[0]
  def value = parts[1]
  log.debug "command = *$command* value = *$value*"

switch (command)
{
    case "Status":
    log.debug "got status thestatus = *$value*"
    sendEvent(name: "armStatus", value: value)
    
    switch (value)
    {
    case " Disarmed":
    	log.debug "Setting disarm tile"
        sendEvent(name: "armDisarm", value: "lite")
        sendEvent(name: "armStay", value: "unlite")
        sendEvent(name: "armAway", value: "unlite")
        break;

    case " Armed - Away":
    	log.debug "setting arm away tile"
        sendEvent(name: "armDisarm", value: "unlite")
        sendEvent(name: "armStay", value: "unlite")
        sendEvent(name: "armAway", value: "lite")
        break;

    case " Armed - Stay":
    	log.debug "setting arm stay tile"
        sendEvent(name: "armDisarm", value: "unlite")
        sendEvent(name: "armStay", value: "lite")
        sendEvent(name: "armAway", value: "unlite")
        break;
    default: log.debug "in default case value = $value"
    }
    
    sendEvent(name: "status" , value: "All Ok")
    sendEvent(name:"statusText", value: value) 
    break;

    case "Alert":
        log.debug "got Alert message value = $value"
        break;

    case "Zones":
      log.debug "Got Zone Message zone = $value"
      sendEvent(name: "lastAlert", value: value)
      break;  

    case "AlertType":
      log.debug "Got alert type Message = *$value*"
	  onPhysical()
      
      switch (value) {
         case " smoke":
          sendEvent(name: "status" , value: "Smoke/CO/Gas Alert")
          sendEvent(name: "lastAlertType" , value: "Smoke/CO/Gas Alert")
          sendEvent(name: "lastAlertDateTime" , value: currentTime)
          break;
      
         case " water":
          sendEvent(name: "status" , value: "Water Alert")
          sendEvent(name: "lastAlertType" , value: "Water Alert")
          sendEvent(name: "lastAlertDateTime" , value: currentTime)
          break;
     
          
          case " temp":
          sendEvent(name: "status" , value: "Temperature Alert")
          sendEvent(name: "lastAlertType" , value: "Temperature Alert")
          sendEvent(name: "lastAlertDateTime" , value: currentTime)
          break;
          
          default:
          sendEvent(name: "status" , value: "Intrusion Alert")
          sendEvent(name: "lastAlertType" , value: "Intrusion Alert")
          sendEvent(name: "lastAlertDateTime" , value: currentTime)
          break;
          }
      
      break;
  
      default:
       log.debug "Got unknown Message!"
       }
  
 } // have valid command
	
}

def configure()  
{
    updated()
}

def updated()
{
    
    def currentTime = new Date(now())
   // log.debug "in updated"
    sendEvent(name: "status" , value: "All Ok")
    sendEvent(name: "lastAlert" , value: "None")
    sendEvent(name: "lastAlertType" , value: "None")
    sendEvent(name: "armStatus" , value: "Unknown")

// for testing onPhysical()
    sendEvent(name: "armDisarm", value: "unlite")
    sendEvent(name: "armStay", value: "unlite")
    sendEvent(name: "armAway", value: "unlite")
  
    sendEvent(name: "lastAlertDateTime", value: currentTime)
    sendEvent(name: "statusText", value: "None") 
}

def on() { 
 	sendEvent(name: "switch", value: "on") 
 } 
 
 
 def off() { 
 log.debug "in off"
 	sendEvent(name: "switch", value: "off") 
 } 


def onPhysical() {
	sendEvent(name: "switch", value: "on", type: "physical")
}

def offPhysical() {
log.debug " in off physical"
	sendEvent(name: "switch", value: "off", type: "physical")
}

