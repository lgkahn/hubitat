/**
 *  Wireless Tag Water
 *
 *  Copyright 2014 Dave Swanson (swanny)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.,
 * lgk remove smartthings crap and add  capability water sensor and set wet if greater than user specified level and dry otherwise.
 * also add last update and signal dbm attr and set tagtype
 * also add debug and text logging and remove unused capilities.
 */
metadata {
	definition (name: "Wireless Tag Water", namespace: "bhazer", author: "swanny") {
		capability "Water Sensor"
       // capability "Presence Sensor"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Sensor"
		capability "Battery"
        capability "Refresh"
        capability "Polling"
        
        command "generateEvent"
        command "initialSetup"
               
        attribute 'tagType', 'string'
        attribute "lastUpdate", "string"
        attribute "signaldBm", "number"
	}

	preferences {
       input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
       input("triggerLevel", "number", title: "Humidity level at which to trigger wet, and dry below?", required: true, defaultValue: 75)
    } 

	
}


// parse events into attributes
def parse(String description) {
	if (debug) log.debug "Parsing '${description}'"
}

void poll() {
	log.debug "poll"	
    parent.pollChild(this)
}

def refresh() {
	log.debug "refresh"
    parent.refreshChild(this)
}

def initialSetup() {

}

def updated() {
	log.trace "updated"
     def tl = triggerLevel
     log.debug "TriggerLevel = $tl"
     sendEvent(name: "tagType", value: "water")
}

void generateEvent(Map results)
{
	if (debug) log.debug "parsing data $results"
    def tl = triggerLevel
   	if(results)
	{
		results.each { name, value ->
            
            if (name=="temperature") {
            	def tempValue = getTemperature(value)
				sendEvent(name: name, value: tempValue, unit: getTemperatureScale())                                  									 
            }
            else {
              //  log.debug "name = $name , value = $value"
                
                if (name =="humidity")
                   {
                       if (debug)
                       {
                           log.debug "TrigerLevel = $tl"
                           log.debug "humidity value = $value"
                       }
                       
                       if (value >= tl)
                       {
                        sendEvent(name: "water", value: "wet")
                        log.debug "Alert Device $name is WET, humidity >= $triggerLevel!"
                       }
                       else {
                           sendEvent(name: "water", value: "dry")
                           log.debug "Device $name is now dry!"
                              
                       }
                   }
             	sendEvent(name: name, value: value)       
            }
		}
        
       def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
       sendEvent(name: "lastUpdate", value: now)
       sendEvent(name: name, value: tempValue, unit: measure, isStateChange: isChange, displayed: isChange)   
       sendEvent(name: "tagType", value: "water") 
        
	}
}

def getTemperature(value) {
	def celsius = value
	if(getTemperatureScale() == "C"){
		return celsius
	} else {
		return celsiusToFahrenheit(celsius).toDouble().round(1)
	}
}
