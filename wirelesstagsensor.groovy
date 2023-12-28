/**
 *  Wireless Sensor Tag Motion Device Driver
 *
 *  author: Bart Hazer
 *  
 *  Modified from https://github.com/st-swanny/smartthings/tree/master/WirelessTags which had the following license:
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
 */
metadata {
    definition (name: "Wireless Sensor Tag", namespace: "bhazer", author: "Bart Hazer") {
        capability "Presence Sensor"
        capability "Acceleration Sensor"
        capability "Motion Sensor"
        capability "Sensor"
        capability "Tone"
        capability "Relative Humidity Measurement"
        capability "Temperature Measurement"
        capability "Signal Strength"
        capability "Battery"
        capability "Refresh"
        capability "Polling"
        capability "Switch"
        capability "Contact Sensor"
        capability "Illuminance Measurement"

        command "generateEvent"
        // lgk arm and disarm motion are not implemented in parent so comment out
        //command "armMotion"
        //command "disarmMotion"
        
        command "setDoorClosedPosition"
        command "initialSetup"

        attribute "tagType","string"
        attribute "motionMode", "string"
        attribute "motion", "string"
        attribute "motionArmed", "string"
        attribute "signaldBm" , "number"
    }

    simulator {
        // TODO: define status and reply messages here
    }


    preferences {
            input "motionDecay", "number", title: "Motion Rearm Time", description: "Seconds (min 60 for now)", defaultValue: 60, required: true, displayDuringSetup: true
            input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
    }
}

// parse events into attributes
def parse(String description) {
    if (debug) log.debug "Parsing '${description}'"
}

// handle commands
def beep() {
   if (debug) log.debug "Executing 'beep'"
    parent.beep(this, 3)
}

def on() {
  if (debug)  log.debug "Executing 'on'"
    parent.light(this, true, false)
    sendEvent(name: "switch", value: "on")
}

def off() {
   if (debug) log.debug "Executing 'off'"
    parent.light(this, false, false)
    sendEvent(name: "switch", value: "off")
}

void poll() {
    log.debug "poll"
    parent.pollChild(this)
}

def refresh() {
    log.debug "refresh"
    parent.refreshChild(this)
}

def armMotion() {
    log.trace "armMotion"
    parent.armMotion(this)
}

def disarmMotion() {
    log.trace "disarmMotion"
    parent.disarmMotion(this)
}

def setDoorClosedPosition() {
    log.debug "set door closed pos"
    parent.disarmMotion(this)
    parent.armMotion(this)
}

def initialSetup() {
}

def getMotionDecay() {
    def timer = (settings.motionDecay != null) ? settings.motionDecay.toInteger() : 60
    return timer
}

def updated() {
    log.trace "updated"
}

void generateEvent(Map results) {
   if (debug) log.debug "parsing data $results"

    if(results) {
        results.each { name, value ->
            if (name == "water") {
                // Doesn't have this capability. Do nothing.
            } else if (name == "temperature") {
                sendEvent(name: name, value: getTemperature(value), unit: getTemperatureScale())
            } else {
                //log.debug "name = $name, value = $value"
                sendEvent(name: name, value: value)
            }
        }
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
