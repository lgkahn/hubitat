/**
 *  Wireless Tag Motion
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
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Wireless Tag Motion", namespace: "lgkahn", author: "swanny") {
		capability "Presence Sensor"
		capability "Acceleration Sensor"
        capability "Motion Sensor"
		capability "Tone"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Signal Strength"
		capability "Battery"
        capability "Refresh"
        capability "Polling"
        capability "Switch"
        capability "Contact Sensor"
        
        command "generateEvent"
        command "setMotionModeAccel"
        command "setMotionModeDoor"
        command "setMotionModeDisarm"
        command "setDoorClosedPosition"
        command "initialSetup"
        
        attribute "tagType","string"
        attribute "motionMode", "string"
        attribute "lastUpdate", "string"
        attribute "lastTemperature", "number"
        attribute "temperatureChange", "number"
        attribute "signaldBm", "number"
        attribute "motionArmed", "string"
        attribute "signaldBm" , "number"
	}


    
    preferences {
    	   input "motionDecay", "number", title: "Motion Rearm Time", description: "Seconds (min 60 for now)", defaultValue: 60, required: true, displayDuringSetup: true
           input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
           input("descText", "bool", title: "Enable descriptionText logging?", required: true, defaultValue: false) 
  
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

// handle commands
def beep() {
	log.debug "Executing 'beep'"
    parent.beep(this, 3)
}

def on() {
	log.debug "Executing 'on'"
    parent.light(this, true, false)
    sendEvent(name: "switch", value: "on")
}

def off() {
	log.debug "Executing 'off'"
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

def setMotionModeAccel() {
	log.debug "set to door"
    def newMode = "door"
    parent.setMotionMode(this, newMode, getMotionDecay())
    sendEvent(name: "motionMode", value: newMode)
}

def setMotionModeDoor() {
	log.debug "set to disarm"
    def newMode = "disarmed"
    parent.setMotionMode(this, newMode, getMotionDecay())
    sendEvent(name: "motionMode", value: newMode)
}

def setMotionModeDisarm() {
	log.debug "set to accel"
    def newMode = "accel"
    parent.setMotionMode(this, newMode, getMotionDecay())
    sendEvent(name: "motionMode", value: newMode)
}

def setDoorClosedPosition() {
	log.debug "set door closed pos"
    parent.disarmMotion(this)
    parent.armMotion(this)
}

def initialSetup() {
	sendEvent(name: "motionMode", value: "accel")
    parent.setMotionMode(this, "accel", getMotionDecay())
      sendEvent(name: "temperatureChange", value: 0.00) 
      sendEvent(name: "motion", value: 'inactive')
     if (debug)
    {
        log.debug "Turning off logging in 1/2 hour!"
        runIn(1800,logsOff)
    }      
}

def getMotionDecay() {
	def timer = (settings.motionDecay != null) ? settings.motionDecay.toInteger() : 60
    return timer
}

def updated() {
	log.trace "updated"
    parent.setMotionMode(this, device.currentState("motionMode")?.stringValue, getMotionDecay())
    sendEvent(name: "temperatureChange", value: 0.00) 
    if (debug)
    {
        log.debug "Turning off logging in 1/2 hour!"
        runIn(1800,logsOff)
    }        
}

void generateEvent(Map results)
{
	def devName = device.getLabel()
    
    if (debug) log.debug "parsing data $results"
    
   	if (results)
	{
		results.each { name, value ->
            def isDisplayed = true
      
            switch (name) {
                case 'battery':
                case 'humidity':
                    valueUnit = '%'
                    break
                case 'illuminance':
                    valueUnit = 'Lux'
                    break
            }
            
            if (name == "signaldBm")
            {
              sendEvent(name: "signaldBm", value: value)  
            }
                    
            else if (name=="temperature") {
                
                double tempValue = getTemperature(value)
                boolean isChange = isStateChange(device, name, tempValue.toString())
                
                BigDecimal degrees = tempValue.toBigDecimal();
                  
                def lastTemp = (device.currentValue("temperature") as BigDecimal)
                BigDecimal change = 0.00
                
                if (lastTemp != null) 
                  change = (degrees - lastTemp as BigDecimal)
                else lastTemp = 0.00
       
                //if (debug) log.debug "name: $devName, isChange: $isChange"
                // if (device.currentState(name)?.value != tempValue) {
               
                String measure = "°F";
                if (getTemperatureScale() == 'C') measure = "°C";
               
                if (isChange) 
                {     
                  def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
                
                  sendEvent(name: "lastUpdate", value: now)
                  sendEvent(name: name, value: tempValue, unit: measure, isStateChange: isChange, displayed: isChange)
                }
                
                attributeUpdateNumber(lastTemp,"lastTemperature",measure,1)
                Boolean hasChanged = (attributeUpdateNumber(degrees, "temperature", measure, 1))
                
                if (debug) log.debug "In update temp val = $tempValue, measure = $measure lastTemp = $lastTemp, change = $change, hasChanged = $hasChanged"
                if (descText) log.info "Tag ($devName) Update: $tempValue, change: $change"
                if (hasChanged == true)
                  {
                   sendEvent(name: "temperatureChange", value: change)
                  }
                else 
                  {
                   sendEvent(name: "temperatureChange", value: 0.00)
                  }                
            }
                
                
        else {
                boolean isChange = isStateChange(device, name, value.toString())
                // log.debug "name: $name, isChange: $isChange"
                // log.debug "Previous: ${device.currentState(name)?.value}, New: $value"
                // if (device.currentState(name)?.value == value) {
                //     log.debug "It shouldn't be a state change..."
                // }
                // if (device.currentState(name)?.value != value) {
                if (isChange) {
                    // log.debug "It's a state change!"
                    if (valueUnit) {
                        sendEvent(name: name, value: value, unit: valueUnit, isStateChange: isChange, displayed: isChange)
                    } else {
                        sendEvent(name: name, value: value, isStateChange: isChange, displayed: isChange)
                    }
                }
            }          
                
             /*   
            	def tempValue = getTemperature(value)
            	def isChange = isTemperatureStateChange(device, name, tempValue.toString())
            	isDisplayed = isChange
                
				sendEvent(name: name, value: tempValue, unit: getTemperatureScale(), displayed: isDisplayed)                                     									 
            }
            else {
            	def isChange = isStateChange(device, name, value.toString())
                isDisplayed = isChange

             	sendEvent(name: name, value: value, isStateChange: isChange, displayed: isDisplayed)       
            }
*/
		}
	}
}

double getTemperature(double value) {
    double returnVal = value
    if (getTemperatureScale() == 'C') {
        returnVal = value
    } else {
        returnVal = (celsiusToFahrenheit(value) as double)
    }
    return returnVal.round(1)
}


def logsOff()
{
    log.debug "Turning off Logging!"
    device.updateSetting("debug",[value:"false",type:"bool"])
}

private Boolean attributeUpdateNumber(BigDecimal val, String attribute, String measure = null, Integer decimals = -1) {
  //
  // Only update "attribute" if different
  // Return true if "attribute" has actually been updated/created
  //
    
  // If rounding is required we use the Float one because the BigDecimal is not supported/not working on Hubitat
   if (debug) log.debug "in attr update number val [ $val attribute = $attribute"
    
  if (decimals >= 0) val = val.toFloat().round(decimals).toBigDecimal();

  BigDecimal integer = val.toBigInteger();

  // We don't strip zeros on an integer otherwise it gets converted to scientific exponential notation
  val = (val == integer)? integer: val.stripTrailingZeros();

  // Coerce Object -> BigDecimal
  if ((device.currentValue(attribute) as BigDecimal) != val) {
    if (measure) sendEvent(name: attribute, value: val, unit: measure);
    else sendEvent(name: attribute, value: val);
    return (true);
  }

  return (false);
}
