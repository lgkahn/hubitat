/**
 *  Tesla for tessie www.tessie.com using api api.tessie.com
 *
 *  Copyright 2024 larry kahn
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIO`NS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * initial release 1.0 beta 1/13/24
 *
 * v 1.1 1/14/24 Added:
 * all to seat heater option, and change name of close Trunk to open or close Trunk to be clearer as it is actually a toggle,
 * also added following commands:
 * set Seat Cooling
 * start Defrost
 * stop Defrost
 * remote Start
 * Close Sunroof
 * and list Drivers
 *
 * Obviously not all commands work with all vehicles, for instance many have no 3rd row seats, or steering wheel heater, or cooling seats or auto close trunk etc.
 *
 */

metadata {
	definition (name: "tessieVehicle", namespace: "lgkahn", author: "Larry Kahn") {
		capability "Actuator"
		capability "Battery"
// not supported	capability "Geolocation"
		capability "Lock"
		capability "Motion Sensor"
		capability "Presence Sensor"
		capability "Refresh"
		capability "Temperature Measurement"
		capability "Thermostat Mode"
        capability "Thermostat Setpoint"
     
		attribute "state", "string"
        attribute "vin", "string"
        attribute "odometer", "number"
        attribute "batteryRange", "number"
        attribute "chargingState", "string"
        attribute "refreshTime", "string"
        attribute "lastUpdate", "string"
        attribute "minutes_to_full_charge", "number"
        attribute "seat_heater_left", "number"
        attribute "seat_heater_right", "number"        
        attribute "seat_heater_rear_left", "number"
        attribute "seat_heater_rear_right", "number"
        attribute "seat_heater_rear_center", "number"    
        attribute "sentry_mode", "string"
        attribute "front_drivers_window" , "string"
        attribute "front_pass_window" , "string"
        attribute "rear_drivers_window" , "string"
        attribute "rear_pass_window" , "string"
        attribute "current_charge_limit", "number"
        attribute "current_charge_amps", "number"
        attribute "longitude", "number"
        attribute "latitude", "number"
        attribute "speed", "number"
        attribute "heading", "number"
        attribute "valet_mode", "string"
        attribute "tire_pressure_front_left", "string"
        attribute "tire_pressure_front_right", "string"
        attribute "tire_pressure_rear_left", "string"
        attribute "tire_pressure_rear_right", "string"
        attribute "last_known_tire_pressure_front_left", "number"
        attribute "last_known_tire_pressure_front_right", "number"
        attribute "last_known_tire_pressure_rear_left", "number"
        attribute "last_known_tire_pressure_rear_right", "number"
        attribute "outside_temperature", "number"
        attribute "passengerSetpoint", "number"
        attribute "lastTokenUpdate", "string"
        attribute "lastTokenUpdateInt", "number"
        attribute "nextTokenUpdate", "string"
        attribute "altPresent", "string"
        attribute "front_drivers_door", "string"
        attribute "front_pass_door", "string"
        attribute "rear_drivers_door", "string"
        attribute "rear_pass_door", "string"
        attribute "frunk" , "string"
        attribute "trunk", "string"
        attribute "user_present", "string"
        attribute "lastUpdateTime", "string"
        attribute "method", "string"
        
        attribute "zzziFrame", "text"
       
		command "wake"
        command "setThermostatSetpoint", ["Number"]
        command "startCharge"
        command "stopCharge"
        command "openFrontTrunk"
        command "openOrCloseRearTrunk"
        command "unlockAndOpenChargePort"
        command ("setSeatHeaters", [
            [
                "name": "Seat",
                "description": "Set seat hating to level 0-3 (0=off)",
                "type": "ENUM",
                "constraints": ["front_left","front_right","rear_left","rear_center","rear_right","third_row_left", "third_row_right","all"]
                ],
                [
                 "name": "level",
                 "description": "Heat Level (0-3, 0=Off)",
                 "type": "ENUM",
                 "constraints": ["0","1","2","3"]
                ]
                ]  )
        
         command ("setSeatCooling", [
            [
                "name": "Seat",
                "description": "Set seat cooling to level 0-3 (0=off)",
                "type": "ENUM",
                "constraints": ["front_left","front_right","rear_left","rear_center","rear_right","third_row_left", "third_row_right","all"]
                ],
                [
                 "name": "level",
                 "description": "Cooling Level (0-3, 0=Off)",
                 "type": "ENUM",
                 "constraints": ["0","1","2","3"]
                ]
                ]  )
        
        command "steeringWheelHeatOn"
        command "steeringWheelHeatOff"
        command "sentryModeOn"
        command "sentryModeOff"
        command "valetModeOn"
        command "valetModeOff"        
        command "ventWindows"
        command "closeWindows"
        command "setChargeLimit", ["number"] /* integer percent */
        command "setChargeAmps", ["number"] /* integer amperage */
        command "setRefreshTime", ["string"]
        command "startDefrost"
        command "stopDefrost"
        command "remoteStart"
        command "ventSunroof"
        command "closeSunroof"
        command "listDrivers"
      
	}


	simulator {
		// TODO: define status and reply messages here
	}


    preferences
    {
       input "refreshTime", "enum", title: "How often to refresh?",options: ["Disabled","1-Hour", "30-Minutes", "15-Minutes", "10-Minutes", "5-Minutes","1-Minute"],  required: true, defaultValue: "15-Minutes"
       input "AllowSleep", "bool", title: "Schedule a time to disable/reenable to allow the car to sleep?", required: true, defaultValue: false
       input "fromTime", "time", title: "From", required:false, width: 6, submitOnChange:true
       input "toTime", "time", title: "To", required:false, width: 6 
       input "tempScale", "enum", title: "Display temperature in F or C ?", options: ["F", "C"], required: true, defaultValue: "F" 
       input "mileageScale", "enum", title: "Display mileage/speed in Miles or Kilometers ?", options: ["M", "K"], required: true, defaultValue: "M"  
       input "debugLevel", "enum", title: "Set Debug/Logging Level (Full will automatically change to Info after an hour)?", options: ["Full","Info","None"], required:true,defaultValue: "Info"
       input "useAltPresence", "bool", title: "Use alternate presence method based on distance from home longitude and latitude?", required: true, defaultValue: false    
       input "homeLongitude", "Double", title: "Home longitude value?", required: false
       input "homeLatitude", "Double", title: "Home latitude value?", required: false
       input "boundryCircleDistance", "Double", title: "Distance in KM from home to be considered as Present?", required: false, defaultValue: 1.0
       input "outerBoundryCircleDistance", "Double", title: "Outer distance in KM from home where refresh time is reduced?", required: false, defaultValue: 5.0     
       input "outerRefreshTime", "Number", title: "Reduced refresh time when location hit outer boundry (in seconds)?",  required: false, defaultValue: 30
       input "refreshOverrideTime", "enum", title: "How long to allow reduced refresh before giving up and go back to default (Also resets when you arrive)?",options: ["30-Minutes", "15-Minutes", "10-Minutes", "5-Minutes"],  required: false, defaultValue: "5-Minutes"     
    }
}

def logsOff()
{
    log.debug "Turning off Logging!"
    device.updateSetting("debugLevel",[value:"Info",type:"enum"])
}

def initialize() {
	log.debug "Executing 'initialize'"
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "zzziFrame", value: "")
 
    sendEvent(name: "supportedThermostatModes", value: ["auto", "off"])
    log.debug "Refresh time currently set to: $refreshTime"
    unschedule()
  
    sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
    sendEvent(name: "refreshTime", value: refreshTime)
    
    state.reducedRefresh = false
    state.reducedRefreshDisabled = false    
    state.tempReducedRefresh = false
    state.tempReducedRefreshTime = 0
    
    if (refreshTime == "1-Hour")
      runEvery1Hour(refresh)
      else if (refreshTime == "30-Minutes")
       runEvery30Minutes(refresh)
     else if (refreshTime == "15-Minutes")
       runEvery15Minutes(refresh)
     else if (refreshTime == "10-Minutes")
       runEvery10Minutes(refresh)
     else if (refreshTime == "5-Minutes")
       runEvery5Minutes(refresh)
     else if (refreshTime == "1-Minute")
       runEvery1Minute(refresh)
    else if (refreshTime == "Disabled")
    {
        log.debug "Disabling..."
    }
      else 
      { 
          log.error "Unknown refresh time specified.. defaulting to 15 Minutes"
          runEvery15Minutes(refresh)
      }
    // now handle scheduling to turn on and off to allow sleep
    if ((AllowSleep == true) && (fromTime != null) && (toTime != null))
    {
       log.debug "Scheduling disable and re-enable times to allow sleep!" 
       schedule(fromTime, disable)
       schedule(toTime, reenable)       
    }
   
     if (debugLevel == "Full")
    {
        log.debug "Turning off logging in 1 hour!"
        runIn(3600,logsOff)
    } 
   
}

def disable()
{
    if (debugLevel != "None") log.debug "Disabling to allow sleep!"
    unschedule()
    // schedule reenable time
    if (toTime != null)
      schedule(toTime, reenable)
}

def reenable()
{
    if (debugLevel != "None") log.debug "Waking up app in re-enable!"
    // now schedule the sleep again
    // pause for 3 secs so when we reschedule it wont run again immediately
    pauseExecution(3000)
    initialize() 
    wake()
}

// parse events into attributes
def parse(String description) {
	if (debugLevel == "Full") log.debug "Parsing '${description}'"
}
    
private processData(data) {
	if(data) {
    	if (debugLevel != "None") log.debug "processData: ${data}"
        
    	sendEvent(name: "state", value: data.state)
        sendEvent(name: "motion", value: data.motion)
        
        if (mileageScale == "M")
          {
           sendEvent(name: "speed", value: data.speed, unit: "mph")
          }   
         else
          {
           double kspd = (data.speed) *  1.609344   
           sendEvent(name: "speed", value: kspd.toInteger(), unit: "kph") 
          }
          
        sendEvent(name: "vin", value: data.vin)
        sendEvent(name: "thermostatMode", value: data.thermostatMode)
        
        if (data.chargeState) {
            if (debugLevel == "Full") log.debug "chargeState = $data.chargeState"
            
        	sendEvent(name: "battery", value: data.chargeState.battery)
            
            if (mileageScale == "M")
              {
                sendEvent(name: "batteryRange", value: data.chargeState.batteryRange.toInteger())
              }   
            else
              {
                double kmrange = (data.chargeState.batteryRange) *  1.609344   
                sendEvent(name: "batteryRange", value: kmrange.toInteger())   
              }
          
            sendEvent(name: "chargingState", value: data.chargeState.chargingState)
            sendEvent(name: "minutes_to_full_charge", value: data.chargeState.minutes_to_full_charge)
            sendEvent(name: "current_charge_limit", value: data.chargeState.chargeLimit)
            sendEvent(name: "current_charge_amps", value: data.chargeState.chargeAmps)

        }
        
        if (data.driveState) {
            if (debugLevel == "Full") log.debug "DriveState = $data.driveState"
           	
            sendEvent(name: "method", value: data.driveState.method)
            sendEvent(name: "heading", value: data.driveState.heading)
            sendEvent(name: "lastUpdateTime", value: data.driveState.lastUpdateTime)
            sendEvent(name: "longitude", value: data.driveState.longitude)
            sendEvent(name: "latitude", value: data.driveState.latitude)
             
            
            updateIFrame()
            
            if (useAltPresence == true)
              {
                  
               if (homeLongitude == null || homeLatitude == null)
                  {
                      log.error "Error: Home longitude or latitude is null and Alternate Presence method selected, Boundry Checking disabled!"
                  }
                else
                {

               if (debugLevel == "Full") log.debug "Using Alternate presence detection, requested distance: $boundryCircleDistance"
                  
               def Double vehlog = data.driveState.longitude.toDouble()
               def Double vehlat = data.driveState.latitude.toDouble()   
               def Double homelog =  homeLongitude.toDouble() //-71.5996 
               def Double homelat = homeLatitude.toDouble() // 42.908368 
                  
               if (debugLevel == "Full")    
                {
                 log.debug "current vehicle longitude,latitude = [ $vehlog, $vehlat ]"                 
                 log.debug "User set home longitude,latitude =   [ $homelog, $homelat ]"
                }
                  
                def Double dist = calculateDistanceBetweenTwoLatLongsInKm(vehlog, vehlat, homelog, homelat)
                if (debugLevel == "Full") log.debug "Calculated distance from home: $dist"
                  
                if (dist <= boundryCircleDistance.toDouble())
                { 
                    if (debugLevel == "Full") log.debug "Vehicle in range... setting presence to true"
                    sendEvent(name: "altPresent", value: "present")
                    sendEvent(name: "presence", value: "present")
                    state.reducedRefresh = false
                    state.reduceedRefreshDisabled = false
                    unschedule(reducedRefreshKill)
                    unschedule(reducedRefresh)
                }
                else 
                {
                    if (debugLevel == "Full") log.debug "Vehicle outside range... setting presence to false"
                    sendEvent(name: "altPresent", value: "not present")
                    sendEvent(name: "presence", value: "not present")
                    
                    // dont reenable or check outter boundry and change times if already did it and currently disabled.
                    
                    if (state.reducedRefreshDisabled != true)
                    {
                        
                    // not in range so try outter boundry
                    if (debugLevel == "Full") log.debug "Checking outer boundry range..."
                    if (debugLevel == "Full") log.debug "outer refresh time: $outerRefreshTime , outer boundry distance: $outerBoundryCircleDistance, refresh override time:  $refreshOverrideTime, boundry circle dist $boundryCircleDistance"
                    
                   if ((outerBoundryCircleDistance == null) || (outerRefreshTime == null) || (refreshOverrideTime == null) || (outerBoundryCircleDistance.toDouble() < boundryCircleDistance.toDouble()))
                     {
                      log.error "Error: OuterRefreshTime, OuterBoundryDistance, refreshOverrideTime are blank, or OuterBoundryDistance is less than inner. Outer bounder check disabled!"
                     }
                    else
                    {
                        if (debugLevel == "Full") log.debug "Using Alternate presence detection, requested outer distance: $outerBoundryCircleDistance"
                        if (dist <= outerBoundryCircleDistance.toDouble())
                        {
                           if (debugLevel == "Full") log.debug "We are inside outer boundry range..."
                           // now just schedule reduced refresh time,, leave other time in place so no unschedule necessary as one extra fresh will not hurt
                                                      
                            runIn(outerRefreshTime, reducedRefresh)
                            if (debugLevel == "Full") log.debug "Temporary reduced refresh time set to $outerRefreshTime seconds!"
                            
                            // only schedule reduced refresh kill first time
                            if (state.reducedRefresh == false)
                              {  
                                  if (debugLevel == "Full") log.debug "Scheduling reduced refresh override/kill to run in $refreshOverrideTime!"
                                  
                                  if (refreshOverrideTime == "30-Minutes")
                                    runIn(30*60,reducedRefreshKill)
                                  else if (refreshOverrideTime == "15-Minutes")
                                    runIn(15*60,reducedRefreshKill)
                                  else if (refreshOverrideTime == "10-Minutes")
                                   runIn(10*60,reducedRefreshKill)
                                  else if (refreshOverrideTime == "5-Minutes")  
                                   runIn(5*60,reducedRefreshKill)    
                              }
                            
                            state.reducedRefresh = true
                          
                        } // in outter boundry
                    } // outer boundry check                                 
                } // not in inner boundry
               
                } // inner boundry check
                } // not already done and disabled
              } // do alt presence check
            
            //sendEvent(name: "speed", value: data.driveState.speed)
            
        } // driver state processing
        
        if (data.vehicleState) {
           if (debugLevel == "Full") log.debug "vehicle state = $data.vehicleState"
            def toPSI  = 14.503773773
            
        	if (useAltPresence != true)
            {
                sendEvent(name: "presence", value: data.vehicleState.presence)
                sendEvent(name: "altPresent", value: data.vehicleState.presence)
                
                state.reducedRefresh = false
            }
            
            if (data.vehicleState.presence == present)
            {
                  unschedule(reducedRefreshKill)
                  unschedule(reducedRefresh)
            }
            
            sendEvent(name: "lock", value: data.vehicleState.lock)
           
            if (mileageScale == "M")
            {
              sendEvent(name: "odometer", value: data.vehicleState.odometer.toInteger())
            }   
            else
            {
              double odom = (data.vehicleState.odometer) *  1.609344   
              sendEvent(name: "odometer", value: odom.toInteger()) 
            }
            
            sendEvent(name: "sentry_mode", value: data.vehicleState.sentry_mode)
            sendEvent(name: "front_drivers_window" , value: data.vehicleState.front_drivers_window)
            sendEvent(name: "front_pass_window" , value: data.vehicleState.front_pass_window)
            sendEvent(name: "rear_drivers_window" , value: data.vehicleState.rear_drivers_window)
            sendEvent(name: "rear_pass_window" , value: data.vehicleState.rear_pass_window)
            sendEvent(name: "valet_mode", value: data.vehicleState.valet_mode)
            
            sendEvent(name: "front_drivers_door", value: data.vehicleState.front_drivers_door)
            sendEvent(name: "front_pass_door", value: data.vehicleState.front_pass_door)
            sendEvent(name: "rear_drivers_door", value: data.vehicleState.rear_drivers_door)
            sendEvent(name: "rear_pass_door", value: data.vehicleState.rear_pass_door)
            sendEvent(name: "frunk", value: data.vehicleState.frunk)
            sendEvent(name: "trunk", value: data.vehicleState.trunk)
            sendEvent(name: "user_present", value: data.vehicleState.user_present)
            
            if ((data.vehicleState.tire_pressure_front_left != null) && (data.vehicleState.tire_pressure_front_left != 0 ))
              { 
                def thePressure = ((float)data.vehicleState.tire_pressure_front_left * toPSI).round(1)
                sendEvent(name: "tire_pressure_front_left", value: thePressure)
                sendEvent(name: "last_known_tire_pressure_front_left", value: thePressure)
              }
            else  sendEvent(name: "tire_pressure_front_left", value: "n/a")
            
           if ((data.vehicleState.tire_pressure_front_right != null) && (data.vehicleState.tire_pressure_front_right != 0 ))
              {
                def thePressure = ((float)data.vehicleState.tire_pressure_front_right * toPSI).round(1) 
                sendEvent(name: "tire_pressure_front_right", value: thePressure)
                sendEvent(name: "last_known_tire_pressure_front_right", value: thePressure)
              }
            else  sendEvent(name: "tire_pressure_front_right", value: "n/a")
          
            if ((data.vehicleState.tire_pressure_rear_left != null) && (data.vehicleState.tire_pressure_rear_left != 0))
              { 
                def thePressure = ((float)data.vehicleState.tire_pressure_rear_left * toPSI).round(1)
                sendEvent(name: "tire_pressure_rear_left", value: thePressure)
                sendEvent(name: "last_known_tire_pressure_rear_left", value: thePressure)
              }
            else sendEvent(name: "tire_pressure_rear_left", value: "n/a")
 
             if ((data.vehicleState.tire_pressure_rear_right != null) && (data.vehicleState.tire_pressure_rear_right != 0))
              {
                def thePressure = ((float)data.vehicleState.tire_pressure_rear_right * toPSI).round(1)
                sendEvent(name: "tire_pressure_rear_right", value: thePressure)
                sendEvent(name: "last_known_tire_pressure_rear_right", value: thePressure)
              }
            else sendEvent(name: "tire_pressure_rear_right", value: "n/a")

        }
        
        if (data.climateState) {
            if (debugLevel == "Full") log.debug "climateState = $data.climateState"
            if (tempScale == "F")
            {
        	  sendEvent(name: "temperature", value: data.climateState.temperature.toInteger(), unit: "F")
              sendEvent(name: "outside_temperature", value: data.climateState.outside_temperature.toInteger(), unit: "F") 
              sendEvent(name: "thermostatSetpoint", value: data.climateState.thermostatSetpoint.toInteger(), unit: "F")
              sendEvent(name: "passengerSetpoint", value: data.climateState.passengerSetpoint.toInteger(), unit: "F")  
            }
            else
            {
              sendEvent(name: "temperature", value: farenhietToCelcius(data.climateState.temperature).toInteger(), unit: "C")
              sendEvent(name: "outside_temperature", value: farenhietToCelcius(data.climateState.outside_temperature).toInteger(), unit: "C")
              sendEvent(name: "thermostatSetpoint", value: farenhietToCelcius(data.climateState.thermostatSetpoint).toInteger(), unit: "C")
              sendEvent(name: "passengerSetpoint", value: farenhietToCelcius(data.climateState.passengerSetpoint).toInteger(), unit: "C") 
            }
            
            sendEvent(name: "seat_heater_left", value: data.climateState.seat_heater_left)
            sendEvent(name: "seat_heater_right", value: data.climateState.seat_heater_right)            
            sendEvent(name: "seat_heater_rear_left", value: data.climateState.seat_heater_rear_left) 
            sendEvent(name: "seat_heater_rear_right", value: data.climateState.seat_heater_rear_right)
            sendEvent(name: "seat_heater_rear_center", value: data.climateState.seat_heater_rear_center)

        }
	} else {
    	log.error "No data found for ${device.deviceNetworkId}"
    }
}


def refresh() {
	if (debugLevel != "None") log.debug "Executing 'refresh'"
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
   
    def data = parent.refresh(device.currentValue('vin'))
	processData(data)
 
}

def reducedRefresh() {
	 if (debugLevel != "None") log.debug "Executing 'reducedRefresh'"
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
   
    def data = parent.refresh(device.currentValue('vin'))
	processData(data)
 
}

def tempReducedRefresh()
{
     // this is called when we use the method to change the refresh temporarily to 30 20 or 10 seconds
     // it reschedules the temp refresh until cancelled.
    
     if (debugLevel != "None") log.debug "Executing 'tempReducedRefresh'"
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
   
    def data = parent.refresh(device.currentValue('vin'))
	processData(data)
    
    // reschedule if still true

    if (state.tempReducedRefresh)
    {
       if (state.tempReducedRefreshTime > 0) runIn(state.tempReducedRefreshTime,tempReducedRefresh)
    }    
}

def wake() {
    def vin1= device.currentValue('vin')
    
    log.debug "vin = $vin1"
    
	if (debugLevel != "None") log.debug "Executing 'wake'"
	def data = parent.wake(device.currentValue('vin'))
    processData(data)
    runIn(30, refresh)
}

def lock() {
	if (debugLevel != "None") log.debug "Executing 'lock'"
	def result = parent.lock(device.currentValue('vin'))
    if (result) { refresh() }
}

def unlock() {
	if (debugLevel != "None") log.debug "Executing 'unlock'"
	def result = parent.unlock(device.currentValue('vin'))
    if (result) { refresh() }
}

def auto() {
	if (debugLevel != "None") log.debug "Executing 'auto'"
	def result = parent.climateAuto(device.currentValue('vin'))
    if (result) { refresh() }
}

def off() {
	if (debugLevel != "None") log.debug "Executing 'off'"
	def result = parent.climateOff(device.currentValue('vin'))
    if (result) { refresh() }
}

def heat() {
	if (debugLevel != "None") log.debug "Executing 'heat'"
	// Not supported
}

def emergencyHeat() {
	if (debugLevel != "None") log.debug "Executing 'emergencyHeat'"
	// Not supported
}

def cool() {
	if (debugLevel != "None") log.debug "Executing 'cool'"
	// Not supported
}

def setThermostatMode(mode) {
	if (debugLevel != "None") log.debug "Executing 'setThermostatMode'"
	switch (mode) {
    	case "auto":
        	auto()
            break
        case "off":
        	off()
            break
        default:
        	log.error "setThermostatMode: Only thermostat modes Auto and Off are supported"
    }
}

def setThermostatSetpoint(Number setpoint) {
	if (debugLevel != "None") log.debug "Executing 'setThermostatSetpoint with temp scale $tempScale'"
    if (tempScale == "F")
      {
	    def result = parent.setThermostatSetpointF(device.currentValue('vin'), setpoint)
        if (result) { refresh() }
      }
    else
    {
        def result = parent.setThermostatSetpointC(device.currentValue('vin'), setpoint)
        if (result) { refresh() }
    }
}

def startCharge() {
	if (debugLevel != "None") log.debug "Executing 'startCharge'"
    def result = parent.startCharge(device.currentValue('vin'))
    if (result) { refresh() }
}

def stopCharge() {
	if (debugLevel != "None") log.debug "Executing 'stopCharge'"
    def result = parent.stopCharge(device.currentValue('vin'))
    if (result) { refresh() }
}

def openFrontTrunk() {
	if (debugLevel != "None") log.debug "Executing 'openFrontTrunk'"
    def result = parent.openFrunk(device.currentValue('vin'))
    // if (result) { refresh() }
}

def openOrCloseRearTrunk() {
	if (debugLevel != "None") log.debug "Executing 'openRearTrunk'"
    def result = parent.openOrCloseTrunk(device.currentValue('vin'))
    // if (result) { refresh() }
}

def unlockAndOpenChargePort() {
	if (debugLevel != "None") log.debug "Executing 'unock and open charge port'"
    def result = parent.unlockandOpenChargePort(device.currentValue('vin'))
    // if (result) { refresh() }   
}  

def setChargeLimit(Number Limit)
{
    if (debugLevel != "None") log.debug "Executing 'setChargeLimit with limit of $Limit %"
	def result = parent.setChargeLimit(device.currentValue('vin'), Limit)
        if (result) { refresh() }
}  
 
def setChargeAmps(Number Amps)
{
    if (debugLevel != "None") log.debug "Executing 'setChargeAmpe with Amps = $Amps "
	def result = parent.setChargeAmps(device.currentValue('vin'), Amps)
        if (result) { refresh() }
}    

def updated()
{  
   initialize()   
}

def setSeatHeaters(seat,level) {
	if (debugLevel != "None") log.debug "Executing 'setSeatHeater'"
	def result = parent.setSeatHeaters(device.currentValue('vin'), seat,level)
    if (result) { refresh() }
}


def setSeatCooling(seat,level) {
	if (debugLevel != "None") log.debug "Executing 'setSeatCooling'"
	def result = parent.setSeatCooling(device.currentValue('vin'), seat,level)
    if (result) { refresh() }
}

def sentryModeOn() {
	if (debugLevel != "None") log.debug "Executing 'Turn Sentry Mode On'"
	def result = parent.sentryModeOn(device.currentValue('vin'))
    if (result) { refresh() }
}

def sentryModeOff() {
	if (debugLevel != "None") log.debug "Executing 'Turn Sentry Mode Off'"
	def result = parent.sentryModeOff(device.currentValue('vin'))
    if (result) { refresh() }
}

def valetModeOn() {
	if (debugLevel != "None") log.debug "Executing 'Turn Valet Mode On'"
	def result = parent.valetModeOn(device.currentValue('vin'))
    if (result) { refresh() }
}

def valetModeOff() {
	if (debugLevel != "None") log.debug "Executing 'Turn Valet Mode Off'"
	def result = parent.valetModeOff(device.currentValue('vin'))
    if (result) { refresh() }
}

def ventWindows() {
	if (debugLevel != "None") log.debug "Executing 'Venting Windows'"
	def result = parent.ventWindows(device.currentValue('vin'))
    if (result) { refresh() }
}

def closeWindows() {
	if (debugLevel != "None") log.debug "Executing 'Close Windows'"
	def result = parent.closeWindows(device.currentValue('vin'))
    if (result) { refresh() }
}

def steeringWheelHeatOn() {
	if (debugLevel != "None") log.debug "Executing 'Steering Wheel Heat On'"
	def result = parent.steeringWheelHeatOn(device.currentValue('vin'))
    if (result) { refresh() }
}

def steeringWheelHeatOff() {
	if (debugLevel != "None") log.debug "Executing 'Steering Wheel Heat Off'"
	def result = parent.steeringWheelHeatOff(device.currentValue('vin'))
    if (result) { refresh() }
}

def startDefrost() {
	if (debugLevel != "None") log.debug "Executing 'Start Max Defrost'"
	def result = parent.startDefrost(device.currentValue('vin'))
    if (result) { refresh() }
}

def stopDefrost() {
	if (debugLevel != "None") log.debug "Executing 'Stop Max Defrost'"
	def result = parent.stopDefrost(device.currentValue('vin'))
    if (result) { refresh() }
}


def remoteStart() {
	if (debugLevel != "None") log.debug "Executing 'Remote Start'"
	def result = parent.remoteStart(device.currentValue('vin'))
    if (result) { refresh() }
}

def ventSunroof() {
	if (debugLevel != "None") log.debug "Executing 'Vent Sunroof'"
	def result = parent.ventSunroof(device.currentValue('vin'))
    if (result) { refresh() }
}

def closeSunroof() {
	if (debugLevel != "None") log.debug "Executing 'Close Sunroof'"
	def result = parent.closeSunroof(device.currentValue('vin'))
    if (result) { refresh() }
}


def listDrivers() {
	if (debugLevel != "None") log.debug "Executing 'List Drivers'"
	def result = parent.listDrivers(device.currentValue('vin'))
    log.debug "data = ${result.data}"
    if (result) { 
        log.info ""
        log.info "Additional Drivers: $result"
        log.info ""
        refresh() }
}


private farenhietToCelcius(dF) {
	return (dF - 32) * 5/9
}

def setLastokenUpdateTime()
{
    def now1 = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
    Long nowint = now() / 1000
    sendEvent(name: "lastTokenUpdate", value: now1, descriptionText: "Last Token Update: $now1")
    sendEvent(name: "lastTokenUpdateInt", value: nowint)
}
    
/**
 * Calculate distance between two points in latitude and longitude taking
 * into account height difference. If you are not interested in height
 * difference pass 0.0. Uses Haversine method as its base.
 * 
 * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
 * el2 End altitude in meters
 * @returns Distance in Meters
 
def double calculateDistanceBetweenTwoLatLongsInKm(double lat1, double lon1, double lat2,
        double lon2) {

    final int R = 6371; // Radius of the earth

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
   // double distance =
        return R * c
    // 1000; // convert to meters

   // double height = el1 - el2;
}
*/

 def Double calculateDistanceBetweenTwoLatLongsInKm(
    Double lat1, Double lon1, Double lat2, Double lon2) {
    
     if (debugLevel == "Full") log.debug "in calc distance lat1 = $lat1, $lon1, lat2 = $lat2, $lon2"
    
  def Double p = 0.017453292519943295;
  def Double a = 0.5 -
      Math.cos((lat2 - lat1) * p) / 2 +
      Math.cos(lat1 * p) * Math.cos(lat2 * p) * (1 - Math.cos((lon2 - lon1) * p)) / 2;
  return 12742 * Math.asin(Math.sqrt(a));
}

def tempReducedRefreshKill()
{
    log.debug "Disabled Temp reduced refresh!"
    state.tempReducedRefresh = false
    state.tempReducedRefreshTime = 0
    unschedule(tempReducedRefresh)
}

def reducedRefreshKill()
{
 if (state.reducedRefresh == true)
    {
        isPresent = device.currentValue("presence")
         if (debugLevel == "Full") log.debug "Presence = $isPresent"
        // only set override if not still present
        if (isPresent != "present")
        {
             state.reducedRefreshDisabled = true
        }
        if (debugLevel == "Full") log.debug "Disabling reduced refresh time."
        unschedule(reducedRefresh)
        state.reducedRefresh = false 
       
    }
}

 def updateIFrame() {
     
        def lon = device.currentValue('longitude')
        def lat = device.currentValue('latitude')
        
        if (lon == null) lon = 0.0
        if (lat == null) lat = 0.0
        
       sendEvent(name: "zzziFrame", value: "<div style='height: 100%; width: 100%'><iframe src='https://maps.google.com/maps?q=${lat},${lon}&hl=en&z=19&t=k&output=embed&' style='height: 100%; width:100%; frameborder:0 marginheight:0 marginwidth:0 border: none;'></iframe><div>")       
 }   


def setRefreshTime(String newRefreshTime)
{  
    log.debug "Refresh time currently set to: $refreshTime, overriding manually with $newRefreshTime"
 
    sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
    sendEvent(name: "refreshTime", value: newrefreshTime)
    
    if (newRefreshTime == "1-Hour")
    {
      unschedule(refresh)
      runEvery1Hour(refresh)
      device.updateSetting("refreshTime",[value:"1-Hour",type:"enum"])
    }
      else if (newRefreshTime == "30-Minutes")
      {
       unschedule(refresh)
       runEvery30Minutes(refresh)
       device.updateSetting("refreshTime",[value:"30-Minutes",type:"enum"]) 
      }
     else if (newRefreshTime == "15-Minutes")        
     {
       unschedule(refresh)
       runEvery15Minutes(refresh)
       device.updateSetting("refreshTime",[value:"15-Minutes",type:"enum"])
     }
     else if (newRefreshTime == "10-Minutes")
     {
       unschedule(refresh)
       runEvery10Minutes(refresh)
       device.updateSetting("refreshTime",[value:"10-Minutes",type:"enum"]) 
     }
     else if (newRefreshTime == "5-Minutes")
     {
       unschedule(refresh)         
       runEvery5Minutes(refresh)
       device.updateSetting("refreshTime",[value:"5-Minutes",type:"enum"])
     }
     else if (newRefreshTime == "1-Minute")
     {
       unschedule(refresh)
       runEvery1Minute(refresh)
       device.updateSetting("refreshTime",[value:"1-Minute",type:"enum"])
     }
    else if (newRefreshTime == "Disabled")
    {
        log.debug "Disabling..."
        unschedule(refresh)
        device.updateSetting("refreshTime",[value:"Disabled",type:"enum"])
    }
    
    // can set less than one minute but will revert in 3 minutes back to normal automatically
    // existing refresh is left in place.. just extra ones 30 20 or 10 secs
    // also add wake or this doesnt really do anything if car asleep
    
    else if (newRefreshTime == "30-Seconds")
    {
        log.debug "Setting temp refresh to 30 seconds for 3 minutes!"
        unschedule(tempReducedRefresh)
        unschedule(tempReducedRefreshKill)
        state.tempReducedRefresh = true
        state.tempReducedRefreshTime = 30
        wake()
        runIn(180,tempReducedRefreshKill)
        runIn(30,tempReducedRefresh)
    }
    else if (newRefreshTime == "20-Seconds")
    {
        log.debug "Setting temp refresh to 20 seconds for 3 minutes!"
        unschedule(tempReducedRefresh)
        unschedule(tempReducedRefreshKill)
        state.tempReducedRefresh = true
        state.tempReducedRefreshTime = 20
        wake()
        runIn(180,tempReducedRefreshKill)
        runIn(20,tempReducedRefresh)
    }
    else if (newRefreshTime == "10-Seconds")
    {
        log.debug "Setting temp refresh to 10 seconds for 3 minutes!"
        unschedule(tempReducedRefresh)
        unschedule(tempReducedRefreshKill)
        state.tempReducedRefresh = true
        state.tempReducedRefreshTime = 10
        wake()
        runIn(180,tempReducedRefreshKill)
        runIn(10,tempReducedRefresh)
    }
   
    else 
      { 
          log.error "Unknown refresh time specified.. defaulting to 15 Minutes"
          runEvery15Minutes(refresh)
          device.updateSetting("refreshTime",[value:"15-Minutes",type:"enum"])
      }
}
