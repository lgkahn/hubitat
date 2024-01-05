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
  definition (name: "LGK Virtual Email Notification Device", namespace: "lgkapps", author: "Larry Kahn kahn@lgk.com") {
    capability "Notification"
    capability "Switch"
    capability "Configuration"
    
    command "onPhysical"
	command "offPhysical"
     attribute "device", "string"
     attribute "status", "string"
     attribute "description", "string"
  
   
  }

}

def parse(String description) {
log.debug "in parse desc = $description"
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def installed() {
log.debug "in installed"
sendEvent(name: "status" , value: "none")
sendEvent(name: "device" , value: "None")
sendEvent(name: "description", value: "none")

}

def deviceNotification(String desc)
{
log.debug "in device notification"
log.debug "desc = $desc"

def currentTime = new Date(now())
log.debug "got current time= $currentTime"
    
def parts = desc.split(":")

if (parts.length == 6)
{

    // shoudl be desc:value:device:value:status:value
    //id desc:living room switch:on:
  def command1 = parts[0]
  def value1 = parts[1]
  log.debug "desc = *$value1*"

    def command2 = parts[2]
  def value2 = parts[3]
    
  log.debug "device = *$command2* name = *$value2*"
    def command3 = parts[4]
  def value3 = parts[5]
  log.debug "status = *$command3* value = *$value3*" 
 
  sendEvent(name: "status", value:value3)
  sendEvent(name: "description", value: value1)
  sendEvent(name: "device", value: value2)
  on()
   	
}
}

def configure()  
{
    updated()
}

def updated()
{
    
    def currentTime = new Date(now())
   // log.debug "in updated"
    sendEvent(name: "status" , value: "none")
    sendEvent(name: "device" , value: "None")
    sendEvent(name: "description", value: "none")
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

