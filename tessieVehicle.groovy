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
 * update for version 1.5 1/17/24
 * v 1.6 clean up debugging
 * 
 * v 1.7 add some vehicle config attributes: car_type, has_third_row_seats, has_seat_cooling, has_sunroof
 * and check these when trying commands and print warning when using these commands if car does not have that feature.
 *
 * v 1.8 check difference of current time vs last response time for car.. if greater than user inputed time in seconds. 
 * then do a status command instead of state.. and see if car is asleep .
 * if it is asleep change scheduled refresh to do a status until it is awake. then change back to the normal state.
 *
 * Relating to this is a new attribute currentVehicleState that can be checked..
 *
 *  v 1.81 change default for timeout to 300 from 90 as tessie seems to take longer to determine vehicle is asleep to save on api calls.
 *  also add code to check if temp is null before trying conversion from far. to celc. and vice versa.
 *
 * v 1.82 force vehicle status to be awake after wakeup call so we get a full refresh.. otherwise it would wake up on first refersh and require a 2nd refresh before
 * getting data.
 *
 * v 1.9 typo in state.reducedRefreshDisabled kept it from working.
 * v 1.91 handle speed of 0 interpeted as null.
 * v 1.92 new attribute savedLocation
 * v 1.97 handle state offline better. just report it dont try to process any data.
 * v 1.98 4/30/24 add command to get battery health (which shows battery degradation) and fill in related attributes, add option to get this data on very query either never, on every refresh or only on reenable if using that.
 *        since this is not really needed all the time, recommend if using set to only-on-reenable (assuming you use the sleep at night option to save on queries).
 *        this adds the following attributes: batteryCapacity, batteryOriginalCapacity, batteryDegradation, batteryHealth.
 * v 2.0 add following attributes active_route_destination, active_route_minutes_to_arrival thanks Alan_F
 * v 2.01 round minutes to arrivate to 2 decimal digits
 * v 2.02 it was truncating to whole integer instead of 2 digits for minutes to arrival... fix
 *    also added active_route_miles_to_arrival and active_route_energy_at_arrival
 * v 2.03 round miles to arrival to 1 digit past decimal.
 *
 * v 2.1 many changes. first integration using the new tesla/tessia fleet streaming API. Thanks to  Bloodtick_Jones and ALAN_F for initial code stub.
 *
 * Caveats, this is a little different that the non official version:
 *
 * 1. There is a new input preference flag that needs to be enabled to use the web fleet websocket API. Without this it works just like it used to albeit alternate presence sensing will
 * now NOT work without the websocket API. So all the code and preference flags for reduced refresh are gone and the minimum refresh time is 1 minute. The Reason being is that
 * this methods works much better for frequent data updates to figure out when you are close to home to set whether you are present or not. This is also enabled with a setting.
 * As before alternate present uses your home longitude and latitude and the distance you decide it should fire.. Without all these set it will flag an error in the logs and disable it.
 *
 * There is also a new debug toggle just for the new websocket api, if either this or the older debug_level=full are on websocket debugging will come out. This is so that if issues
 * arrise with the new code you don't have to commit to full debugging of the polling api as well.
 *
 * the following functions and attributes and related input preference have been removed:
 *        input "outerBoundryCircleDistance", 
 *        input "outerRefreshTime", 
 *        input "refreshOverrideTime",
 *
 * FromTime and ToTime settings are still relavent to disable polling and also now will disable the websocket api interface during this time as well. This normally is used
 * when you know the car will not be used ie. late at night to help it sleep and also reduce load on the hub. If the websocket interface is enabled it will also now re-enable
 * when the daily toTime is met.
 *
 * A timeToFullCharge attribute that displays the remaining time to charge (NOTE the name is misleading) it is not really the time to full charge but the time remaining to charge up- to the
 * level you have set. But anywhoo this is what telsa has called this attribute. The attribute is a string similiar to what is found in the tesla app.
 *
 * Other notes: Beyond our control is that the websocket interface seems to reset every 5 minutes, and in order to fill in missing data on intial startup or when this occurs 
   (as long as the car is NOT Asleep) a normal polling refresh is fired off.  The result is that you will see normal refreshes every 3 minutes if the car is awake.
 * For this reason, normal polling probably should not be set lower than 10 minutes, 15-30 minutes is now the recommended level for the normal API polling.
 *
 * v 2.11 fix typo in disable fx
 * v 2.12 fromtime was not disabling the websocket correctly, the status fx was reopening it.
 * v 2.13 timetofullcharge fixes
 * v 2.14 change to present not to set if already same state.
 * v 2.15 extra debugging in charging timestring removed, and also debugging that was supposed be info and was
 *     coming out related to new alt presence code, changed to only come out for full debugging. 
 *     Also removed old code for alt presence that was already commented out.
 *  v 2.16 fix set_temp
 *  v 2.17 change of debug to say when we set presence to false, also dont reset presence to either true or false 
 *    from the websocket api when useAltPresence is true.
 *
 * v 2.18 bug in temperatures from websocket.. zero coming back and was not getting converted correctly to 32 degress
 *   as apperently wherever the function celsiusToFahrenheit 
 *   has a bug and just returns 0 (probably a check for null = 0 issue, i had to hard code to 32 in this case.
 *   the prior result was when temp was coming back in websock as 0 it was getting set to fahrenheit 0 instead of 32.
 *   
 *   Also a second issue/special case.. vehicle speed comes back as invalid:true if car is not moving and sometimes the speed gets left set to the previous value
 *   in this case if if is invalid assume it is also 0 and set it to that as well as set motion to false.
 *   I have reported this to tessie and if they fix this in the legacy code I can then remove this hack.
 *
 *   Also add back altpresence setting of present in legacy code processing without reducded refresh as some legacy cards cannot
 *   use websocket telemetry. To use this there is a new input preference that needs toi be enabled:useAltPresenceWithLegacyAPI. 
 *
 *   v 2.19 apparently tessie cannot or will not fix the issue with the legacy api at times incorrectly sending a speed > 0
 *   the websocket api correctly either says 0 or invalid, but unfortunately everytime the legacy refresh it will reset the speed and motion to true, due to a speed coming
 *   in. As a second workaround i will ignore the speed and reset it to 0 and motion to inactive whenever the charge state is not disconnected or the presesence is true.
 *   For this reason i had to move the presence code above the speed/motion.
 *
 *  v 2.20 back out 2.19 code changes and instead set an state variable (reset at startup./initalize) if we get a speed report from the websocket.
 *  if so it means the telemetry is working via websocket and therefore ignore any speed/motion reports from the legacy api/refresh (assuming the preference to use
 *  the websocket api is still enabled).
 *
 */

metadata {
	definition (name: "tessieVehicle", namespace: "lgkahn", author: "Larry Kahn") {
		capability "Actuator"
		capability "Battery"
		capability "Lock"
		capability "Motion Sensor"
		capability "Presence Sensor"
		capability "Refresh"
		capability "Temperature Measurement"
		capability "Thermostat Mode"
        capability "Thermostat Setpoint"
        capability "Initialize"
     
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
        attribute "currentAddress", "string"
        attribute "usableBattery", "number"
        attribute "has_Third_Row_Seats", "string"
        attribute "has_Sunroof", "string"
        attribute "car_Type", "string"
        attribute "has_Seat_Cooling", "string"
        attribute "currentVehicleState", "string"
        attribute "savedLocation", "string"
        attribute "batteryCapacity", "number"
        attribute "batteryOriginalCapacity", "number"
        attribute "batteryDegradation", "number"
        attribute "batteryHealth", "number"
        attribute "active_route_destination", "string"
        attribute "active_route_minutes_to_arrival", "number"
        attribute "active_route_miles_to_arrival", "number"
        attribute "active_route_energy_at_arrival", "number"
        attribute "timeToFullCharge", "string"

        //attribute "weatherCloudiness", "number"
        attribute "weatherCondition", "string"
        attribute "weatherFeelsLike", "number"
        //attribute "weatherHumidity", "number"
        attribute "weatherLocation", "string"
        //attribute "weatherPressure", "number"
        //attribute "weatherSunrise", "number"
        //attribute "weatherSunset", "number"
        attribute "weatherTemperature", "number"
        //attribute "weatherVisibility", "number"
        //attribute "weatherWindDirection", "number"
        //attribute "weatherWindSpeed", "number"        
      
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
        command "getBatteryHealth"
      
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
       input "debugWebSocketAPI", "bool", title: "Debug the Websocket realtime api (this is independent of the overall debug level)? This automatically disable after an hour.", required: true, defaultValue: false
       input "useRealTimeAPI", "bool", title: "Use fleet Real Time API (note: if using this the refresh time should be set higher to fill in attributes missing in the Real Time API.) This needs to be enabled to use Alternate Presence sensing based on long. and lat.", required: true, defaultValue: false    
       input "useAltPresence", "bool", title: "Use alternate presence method based on distance from home based on longitude and latitude?", required: true, defaultValue: false    
       input "useAltPresenceWithLegacyAPI", "bool", title: "Use alternate presence with LEGACY api only as older vehicle cannot use Websocket telemetry?", required: true, defaultValue: false    
       input "homeLongitude", "Double", title: "Home longitude value?", required: false
       input "homeLatitude", "Double", title: "Home latitude value?", required: false
       input "enableAddress", "bool", title: "Enable an extra query on every refresh to fill in current address?", required:false, defaultValue:false
       input "enableWeather", "bool", title: "Enable an extra query on every refresh to fetch the current weather conditions?", required:false, defaultValue:false
       input "boundryCircleDistance", "Double", title: "Distance in KM from home to be considered as Present?", required: false, defaultValue: 1.0
      // input "outerBoundryCircleDistance", "Double", title: "Outer distance in KM from home where refresh time is reduced?", required: false, defaultValue: 5.0     
      // input "outerRefreshTime", "Number", title: "Reduced refresh time when location hit outer boundry (in seconds)?",  required: false, defaultValue: 30
       //input "refreshOverrideTime", "enum", title: "How long to allow reduced refresh before giving up and go back to default (Also resets when you arrive)?",options: ["30-Minutes", "15-Minutes", "10-Minutes", "5-Minutes"],  required: false, defaultValue: "5-Minutes"     
       input "numberOfSecsToConsiderCarAsleep", "Number", title: "After how many seconds have elapsed since last Tesla update should we check to see if the car is Asleep (default 300)?",resuired:true, defaultValue:300
       input "enableBatteryHealth", "enum", title: "Enable an extra query on every refresh to get battery health?", options: ["disabled", "on-every-refresh", "only-on-reenable"], required: false, defaultValue: "disabled" 
    }
}

def logsOff()
{
    log.info "Turning off Logging!"
    device.updateSetting("debugLevel",[value:"Info",type:"enum"])
    device.updateSetting("debugWebSocketAPI",[value:"false",type:"bool"])
}

def initialize() 
{
     log.info "'initialize - Current Version: ${parent.currentVersion()}'"
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "zzziFrame", value: "")
    
    sendEvent(name: "supportedThermostatModes", value: ["auto", "off"])
    log.info "Refresh time currently set to: $refreshTime"
    unschedule()
  
    sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
    sendEvent(name: "refreshTime", value: refreshTime)
    
    if (numberOfSecsToConsiderCarAsleep == null)
      device.updateSetting("numberOfSecsToConsiderCarAsleep",[value:300])
      
    log.info "Time after which to check if Vehicle is Asleep: ${numberOfSecsToConsiderCarAsleep}"
    state.currentVehicleState = "awake"
    state.disabled = false 
    sendEvent(name: "currentVehicleState", value: "awake")
    
      if (useAltPresence == true) 
       {
           if (useRealTimeAPI == false)
            {
               if (useAltPresenceWithLegacyAPI == false)
                {
                 log.error "Alternate Presence detection based on long. and lat. is enabled, but useRealTimeAPI is not so it will not function correctly - Disabling it."
                 log.error "If you want to use it with the older Legacy API Enable that option."
                 useAltPresence = false
                 device.updateSetting("useAltPresence",[value:"false",type:"bool"]) 
                }
            }
            else if (homeLongitude == null || homeLatitude == null)
             {
                log.error "Home longitude or latitude is null and Alternate Presence method selected, Boundry Checking disabled!"
                useAltPresence = false
                device.updateSetting("useAltPresence",[value:"false",type:"bool"]) 
             }
            else
            {
              if (debugLevel != "None") log.info "Using Alternate presence detection, requested distance: $boundryCircleDistance"
            }
       }
                   
    // remove no longer used reduce refresh states
   // state.remove('reducedRefresh')
    //state.remove('reducedRefreshDisabled')    
    //state.remove('tempReducedRefresh')
    //state.remove('tempReducedRefreshTime')
    
    // new websock api speed state
    state.haveWebSocketSpeedReport = "false"
    
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
          log.warn "Unknown refresh time specified.. defaulting to 15 Minutes"
          runEvery15Minutes(refresh)
      }
    // now handle scheduling to turn on and off to allow sleep
    if ((AllowSleep == true) && (fromTime != null) && (toTime != null))
    {
       log.info "Scheduling disable and re-enable times to allow sleep!" 
       schedule(fromTime, disable)
       schedule(toTime, reenable)       
    }
   
    if ((debugLevel == "Full") || (debugWebSocketAPI))
    {
        log.info "Turning off logging in 1 hour!"
        runIn(3600,logsOff)
    } 

    if (useRealTimeAPI)
    {
      log.info "Enabling Real Time Fleet API!"
      webSocketInit() 
    }
    else 
    {
      log.info "Disabling Real Time Fleet API!"
      webSocketClose()
    }
    
}

def disable()
{
    if (debugLevel != "None") log.info "Disabling to allow sleep!"
    unschedule()
    // schedule reenable time
    if (toTime != null)
      schedule(toTime, reenable)
    state.disabled = true
    
    if (useRealTimeAPI)
    {
      if (debugLevel != "None") log.info "Disabling Real Time API" 
      webSocketClose()
    }
}

def reenable()
{
    if (debugLevel != "None") log.info "Waking up app in re-enable!"
    // now schedule the sleep again
    // pause for 3 secs so when we reschedule it wont run again immediately
    pauseExecution(3000)
    state.disabled = false
    initialize() 
    wake()
    
    if (enableBatteryHealth == "only-on-reenable")
       {
          if (debugLevel != "None") log.info "Getting Battery Health Status"
            getBatteryHealth()
       } 
    // lgk no need to reenable websocket here as it is done above in initialize.
}

// parse events into attributes
def parse(String description) {
	if (debugLevel == "Full") log.debug "Parsing '${description}'"
    webSocketParse(description)
}

private processVehicleState(data)
{
    if (data) 
    {
       if (debugLevel != "None") log.info "processVehicleState: ${data}"
  
        if (data == "awake")
        {
           if (debugLevel != "None") log.info "Vehicle is again awake ... resuming normal refresh!"
        }
        state.currentVehicleState = data
        sendEvent(name: "currentVehicleState", value: data)
    }
}           
    
private processData(data) {
	if (data) {
    	if (debugLevel != "None") log.info "processData: ${data}"
        
        if (data.state != "online")
        {
            // just mark it offline and ignore all data as it will be old or bogus
            sendEvent(name: "state", value: data.state)
        }
        else
        {
            
        // lgk new code to ignore speed and motion if charge state != disconnected or presence is true
        // code moved to the end so charge and presence already set.
            
    	sendEvent(name: "state", value: data.state)
               
        sendEvent(name: "active_route_destination", value: data.active_route_destination)
      
        def Float minToArrivalFloat = data.active_route_minutes_to_arrival
        def minToArrival = minToArrivalFloat.round(2)
        sendEvent(name: "active_route_minutes_to_arrival", value: minToArrival) 
        
        def Float milesToArrivalFloat = data.active_route_miles_to_arrival
        def milesToArrival = milesToArrivalFloat.round(1)
        sendEvent(name: "active_route_miles_to_arrival", value: milesToArrival)
        sendEvent(name: "active_route_energy_at_arrival", value: data.active_route_energy_at_arrival)
        
        if ((useRealTimeAPI == false) || (state.haveWebSocketSpeedReport != "true"))    
          {
           sendEvent(name: "motion", value: data.motion)
                    
           if (mileageScale == "M")
             {
              sendEvent(name: "speed", value: data.speed, unit: "mph")
             }  
           else
            {
              // handle speed of 0 which is interpreted as null
              if (data.speed)
                {
                 double kspd = (data.speed) *  1.609344   
                 sendEvent(name: "speed", value: kspd.toInteger(), unit: "kph") 
                }
               else sendEvent(name: "speed", value: 0, unit: "kph")
            }
          } // ignore speed and motion as getting from websocket
            
        sendEvent(name: "vin", value: data.vin)
        sendEvent(name: "thermostatMode", value: data.thermostatMode)
        
        if (data.chargeState) {
            if (debugLevel == "Full") log.debug "chargeState = ${data.chargeState}"
            
        	sendEvent(name: "battery", value: data.chargeState.battery)
            sendEvent(name: "usableBattery", value: data.chargeState.usableBattery)
            
            if (mileageScale == "M")
              {
                def theRange = Math.round(data.chargeState.batteryRange)
                if (debugLevel == "full") log.debug "rounded range = $theRange"
                sendEvent(name: "batteryRange", value: theRange)
              }   
            else
              {
                double kmrange = (data.chargeState.batteryRange) *  1.609344
                def theRange = Math.round(kmrange)
                sendEvent(name: "batteryRange", value: theRange)   
              }
          
            sendEvent(name: "chargingState", value: data.chargeState.chargingState)
            sendEvent(name: "minutes_to_full_charge", value: data.chargeState.minutes_to_full_charge)
            sendEvent(name: "current_charge_limit", value: data.chargeState.chargeLimit)
            sendEvent(name: "current_charge_amps", value: data.chargeState.chargeAmps)

            if (data.chargeState.chargingState != "Charging") sendEvent(name: "timeToFullCharge", value: "Not Charging")
            else processTimeToFullCharge(data.chargeState.minutes_to_full_charge.toInteger())
        }
        
        if (data.driveState) {
            if (debugLevel == "Full") log.debug "DriveState = ${data.driveState}"
           	
            sendEvent(name: "method", value: data.driveState.method)
            sendEvent(name: "heading", value: data.driveState.heading)
            sendEvent(name: "lastUpdateTime", value: data.driveState.lastUpdateTime)
            sendEvent(name: "longitude", value: data.driveState.longitude)
            sendEvent(name: "latitude", value: data.driveState.latitude)
             
            
            // lgk calculate diff in date of last updat time and now
    
           if (data?.driveState?.lastUpdateTime)
            {
              def dstring = data.driveState.lastUpdateTime
              def ct = new Date()
                
            
            if (debugLevel == "Full") log.debug "calculate difference in seconds from last update ($dstring) and now ($ct)"
             
            use (groovy.time.TimeCategory)
              {
                diff = ct-dstring
              }
  
           def hrdiff = diff.getHours()
           def mindiff = diff.getMinutes()
           def secdiff = diff.getSeconds() 
           def finaldiff = (hrdiff * 3600) + (mindiff * 60) + secdiff
            
           if (debugLevel == "Full") log.debug "Difference in Seconds between now and last update = $finaldiff !"
                
            if (finaldiff > numberOfSecsToConsiderCarAsleep)
              {
               if (debugLevel != "None")  
                  log.info "$finaldiff is greater than $numberOfSecsToConsiderCarAsleep secs... Checking if car is asleep!"
                  
               def result = parent.sleepStatus(device.currentValue('vin'))
                  if (result)
                  {  
                      if (debugLevel == "Full") log.debug "got result $result" 
                      if (result != "awake")
                      {
                            if (debugLevel != "None")
                              log.info "Car is $result"
                              
                             state.currentVehicleState = result
                             sendEvent(name: "currentVehicleState", value: result)
                      }     
                       
                  }
              }
                    
            }
        
            updateIFrame()
      
            // legacy alt presence code without reduced refresh added back as some legacy vehicles s/x do not have telemetry data via websocket
            
            if ((useAltPresence == true) && (useAltPresenceWithLegacyAPI == true))
              {
               if ((homeLongitude == null) || (homeLatitude == null))
                  {
                   log.error "Error: Home longitude or latitude is null and Alternate Presence method selected, Boundry Checking disabled!"
                  }
                else
                {   
                  if (debugLevel == "Full") log.debug "Using Legacy Alternate presence detection, requested distance: $boundryCircleDistance"
                 
                  def double homelog =  homeLongitude.toDouble() //-71.5996 
    	          def double homelat = homeLatitude.toDouble() // 42.908368 
                  def double vehlat  = data.driveState.latitude.toDouble()
                  def double vehlog = data.driveState.longitude.toDouble()
                    
                  if (debugLevel == "Full")    
                    {
                     log.debug "current vehicle longitude,latitude = [ $vehlog, $vehlat ]"                 
                     log.debug "User set home longitude,latitude =   [ $homelog, $homelat ]"
                    }
                  
                  def Double dist = calculateDistanceBetweenTwoLatLongsInKm(vehlog, vehlat, homelog, homelat)
                  if (debugLevel != "None") log.debug "Calculated distance from home: $dist"
                  
                  if (dist <= boundryCircleDistance.toDouble())
                   { 
                     if (device.currentValue('altPresent') == 'not present')
                       {
                        if (debugLevel != "None") log.debug "Vehicle in range... setting presence to true"
                        sendEvent(name: "altPresent", value: "present")
                        sendEvent(name: "presence", value: "present")
                       }
                   }
                  else 
                   {
                     if (device.currentValue('altPresent') == 'present')
                       {
                        if (debugLevel != "None")  log.debug "Vehicle outside range... setting presence to false"
                        sendEvent(name: "altPresent", value: "not present")
                        sendEvent(name: "presence", value: "not present")
                       }
                   } // inner boundry check
                } // not already done and disabled
              } // do alt presence check       
            
        } // driver state processing
        
        if (data.vehicleState) 
          {
            if (debugLevel == "Full") log.debug "vehicle state = ${data.vehicleState}"
          
        	if (useAltPresence != true)
            { 
                if (device.currentValue('presence') != data.vehicleState.presence)
                  sendEvent(name: "presence", value: data.vehicleState.presence)
                if (device.currentValue('altPresent') != data.vehicleState.presence)
                  sendEvent(name: "altPresent", value: data.vehicleState.presence)  
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
                def thePressure = ((float)data.vehicleState.tire_pressure_front_left * toPSI()).round(1)
                sendEvent(name: "tire_pressure_front_left", value: thePressure, unit: "psi")
                sendEvent(name: "last_known_tire_pressure_front_left", value: thePressure,  unit: "psi")
              }
            else  sendEvent(name: "tire_pressure_front_left", value: "n/a")
            
           if ((data.vehicleState.tire_pressure_front_right != null) && (data.vehicleState.tire_pressure_front_right != 0 ))
              {
                def thePressure = ((float)data.vehicleState.tire_pressure_front_right * toPSI()).round(1) 
                sendEvent(name: "tire_pressure_front_right", value: thePressure, unit: "psi")
                sendEvent(name: "last_known_tire_pressure_front_right", value: thePressure, unit: "psi")
              }
            else  sendEvent(name: "tire_pressure_front_right", value: "n/a")
          
            if ((data.vehicleState.tire_pressure_rear_left != null) && (data.vehicleState.tire_pressure_rear_left != 0))
              { 
                def thePressure = ((float)data.vehicleState.tire_pressure_rear_left * toPSI()).round(1)
                sendEvent(name: "tire_pressure_rear_left", value: thePressure, unit: "psi")
                sendEvent(name: "last_known_tire_pressure_rear_left", value: thePressure,  unit: "psi")
              }
            else sendEvent(name: "tire_pressure_rear_left", value: "n/a")
 
             if ((data.vehicleState.tire_pressure_rear_right != null) && (data.vehicleState.tire_pressure_rear_right != 0))
              {
                def thePressure = ((float)data.vehicleState.tire_pressure_rear_right * toPSI()).round(1)
                sendEvent(name: "tire_pressure_rear_right", value: thePressure, unit: "psi")
                sendEvent(name: "last_known_tire_pressure_rear_right", value: thePressure, unit: "psi")
              }
            else sendEvent(name: "tire_pressure_rear_right", value: "n/a")

        } // end vehicle state
        
        if (data.climateState) {
            if (debugLevel == "Full") log.debug "climateState = ${data.climateState}"
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
        
       if (data.vehicleConfig)
        {
            if (debugLevel == "Full") log.debug "VehicleConfig = ${data.vehicleConfig}"
            
            if (data.vehicleConfig.sunroof_installed)
              sendEvent(name: "has_Sunroof", value: data.vehicleConfig.sunroof_installed)  
            else  sendEvent(name: "has_Sunroof", value: "false")  
            
            if (data.vehicleConfig.has_third_row_seats)
                sendEvent(name: "has_Third_Row_Seats", value: data.vehicleConfig.has_third_row_seats)   
            else sendEvent(name: "has_Third_Row_Seats", value: "false") 
            
            sendEvent(name: "has_Seat_Cooling", value: data.vehicleConfig.has_seat_cooling)
            sendEvent(name: "car_Type", value: data.vehicleConfig.car_type)
        } 
        } // online
    } // have data     
      else {
    	    log.error "No data found for ${device.deviceNetworkId}"
    }
}

def refresh()
{
	if (debugLevel != "None") log.info "Executing 'refresh'"
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
   
    // lgk if vehicle asleep just do state instead of normal refresh
    if (state.currentVehicleState == "awake")
    {      
       def data = parent.refresh(device.currentValue('vin'))
	   processData(data)
    
      if (enableAddress)
      {
        if (debugLevel != "None") log.info "Getting current Address"
        def adata = parent.currentAddress(device.currentValue('vin'))
        if (debugLevel == "Full") log.debug "address data = $adata"
        if (adata?.status)
          {
             sendEvent(name: "currentAddress", value: adata.address, descriptionText: "Current Address: ${adata.address}")
             sendEvent(name: "savedLocation", value: adata.savedLocation, descriptionText: "Location: ${adata.savedLocation}")
          }
          else 
          {
            sendEvent(name: "currentAddress", value: "Unknown", descriptionText: "Current Address: Unknown")
            sendEvent(name: "savedLocation", value: "N/A", descriptionText: "Location: N/A")   
          }
      }
        
      if (enableWeather)
      {
        if (debugLevel != "None") log.info "Getting current Weather Conditions"
        def adata = parent.getWeather(device.currentValue('vin'))
        if (debugLevel == "Full") log.debug "weather data = $adata"
        if (adata?.status)
          {
             //sendEvent(name: "weatherCloudiness", value: adata.cloudiness, descriptionText: "Weather Cloudiness: ${adata.cloudiness}", unit: "%")
             sendEvent(name: "weatherCondition", value: adata.condition, descriptionText: "Weather Condition: ${adata.condition}")
             //sendEvent(name: "weatherHumidity", value: adata.humidity, descriptionText: "Weather Humidity: ${adata.humidity}", unit: "%RH")
             sendEvent(name: "weatherLocation", value: adata.location, descriptionText: "Weather Location City: ${adata.location}")
             //sendEvent(name: "weatherPressure", value: adata.pressure, descriptionText: "Weather Pressure: ${adata.pressure}", unit: "millibar")
             //sendEvent(name: "weatherSunrise", value: adata.sunrise, descriptionText: "Weather Sunrise: ${adata.sunrise}")
             //sendEvent(name: "weatherSunset", value: adata.sunset, descriptionText: "Weather Sunset: ${adata.sunset}")
             //sendEvent(name: "weatherVisibility", value: adata.visibility, descriptionText: "Weather Visibility: ${adata.visibility}", unit: "feet")
             //sendEvent(name: "weatherWindDirection", value: adata.wind_direction, descriptionText: "Weather Wind Direction Heading: ${adata.wind_direction}", unit: "degrees")
             //sendEvent(name: "weatherWindSpeed", value: adata.wind_speed, descriptionText: "Weather Wind Speed: ${adata.wind_speed}", unit: "knots?")
            if (tempScale == "F")
            {
              sendEvent(name: "weatherTemperature", value: adata.temperature.toInteger(), unit: "F")
              sendEvent(name: "weatherFeelsLike", value: adata.feels_like.toInteger(), unit: "F")          
            }
            else  
            {
              sendEvent(name: "weatherTemperature", value: farenhietToCelcius(adata.temperature).toInteger(), unit: "C")
              sendEvent(name: "weatherFeelsLike", value: farenhietToCelcius(adata.feels_like).toInteger(), unit: "C") 
            }
          }
          else 
          {
             //sendEvent(name: "weatherCloudiness", value: -0, descriptionText: "Weather Cloudiness: Unknown", unit: "%")
             sendEvent(name: "weatherCondition", value: "Unknown", descriptionText: "Weather Condition: Unknown")
             //sendEvent(name: "weatherHumidity", value: -0, descriptionText: "Weather Humidity: Unknown", unit: "%RH")
             sendEvent(name: "weatherLocation", value: "Unknown", descriptionText: "Weather Location City: Unknown")
             //sendEvent(name: "weatherPressure", value: -0, descriptionText: "Weather Pressure: Unknown", unit: "millibar")
             //sendEvent(name: "weatherSunrise", value: 0, descriptionText: "Weather Sunrise: Unknown")
             //sendEvent(name: "weatherSunset", value: 0, descriptionText: "Weather Sunset: Unknown")
             //sendEvent(name: "weatherVisibility", value: -0, descriptionText: "Weather Visibility: Unknown", unit: "feet")
             //sendEvent(name: "weatherWindDirection", value: -0, descriptionText: "Weather Wind Direction Heading: Unknown", unit: "degrees")
             //sendEvent(name: "weatherWindSpeed", value: -0, descriptionText: "Weather Wind Speed: Unknown", unit: "knots?")
             sendEvent(name: "weatherTemperature", value: -100)
             sendEvent(name: "weatherFeelsLike", value: -100) 
           }                     
      }

      if (enableBatteryHealth == "on-every-refresh")
        {
          if (debugLevel != "None") log.info "Getting Battery Health Status"
            getBatteryHealth()
        }
                                  
    }
    else
    {
      def data = parent.sleepStatus(device.currentValue('vin'))
      processVehicleState(data)
    }
}

def wake() {
    def vin1= device.currentValue('vin')
    
	if (debugLevel != "None") log.info "Executing 'wake'"
	def data = parent.wake(device.currentValue('vin'))
    processData(data)
    
    // lgk add code here to set vehicle to awake so we get a full refresh
    state.currentVehicleState = "awake"
    sendEvent(name: "currentVehicleState", value: "awake")
  
    runIn(30, refresh)
}

def lock() {
	if (debugLevel != "None") log.info "Executing 'lock'"
	def result = parent.lock(device.currentValue('vin'))
    if (result) { refresh() }
}

def unlock() {
	if (debugLevel != "None") log.info "Executing 'unlock'"
	def result = parent.unlock(device.currentValue('vin'))
    if (result) { refresh() }
}

def auto() {
	if (debugLevel != "None") log.info "Executing 'auto'"
	def result = parent.climateAuto(device.currentValue('vin'))
    if (result) { refresh() }
}

def off() {
	if (debugLevel != "None") log.info "Executing 'off'"
	def result = parent.climateOff(device.currentValue('vin'))
    if (result) { refresh() }
}

def heat() {
	if (debugLevel != "None") log.warn "'heat but not supported.'"
	// Not supported
}

def emergencyHeat() {
	if (debugLevel != "None") log.warn "'emergencyHeat not supported!'"
	// Not supported
}

def cool() {
	if (debugLevel != "None") log.warn "'cool not supported'"
	// Not supported
}

def setThermostatMode(mode) {
	if (debugLevel != "None") log.info "Executing 'setThermostatMode'"
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
	if (debugLevel != "None") log.info "Executing 'setThermostatSetpoint with temp scale $tempScale'"
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
	if (debugLevel != "None") log.info "Executing 'startCharge'"
    def result = parent.startCharge(device.currentValue('vin'))
    if (result) { refresh() }
}

def stopCharge() {
	if (debugLevel != "None") log.info "Executing 'stopCharge'"
    def result = parent.stopCharge(device.currentValue('vin'))
    if (result) { refresh() }
}

def openFrontTrunk() {
	if (debugLevel != "None") log.info "Executing 'openFrontTrunk'"
    def result = parent.openFrunk(device.currentValue('vin'))
     if (result) { refresh() }
}

def openOrCloseRearTrunk() {
	if (debugLevel != "None") log.info "Executing 'openRearTrunk'"
    def result = parent.openOrCloseTrunk(device.currentValue('vin'))
     if (result) { refresh() }
}

def unlockAndOpenChargePort() {
	if (debugLevel != "None") log.info "Executing 'unock and open charge port'"
    def result = parent.unlockandOpenChargePort(device.currentValue('vin'))
     if (result) { refresh() }   
}  

def setChargeLimit(Number Limit)
{
    if (debugLevel != "None") log.info "Executing 'setChargeLimit with limit of $Limit %"
	def result = parent.setChargeLimit(device.currentValue('vin'), Limit)
        if (result) { refresh() }
}  
 
def setChargeAmps(Number Amps)
{
    if (debugLevel != "None") log.info "Executing 'setChargeAmpe with Amps = $Amps "
	def result = parent.setChargeAmps(device.currentValue('vin'), Amps)
        if (result) { refresh() }
}    

def updated()
{  
   initialize()   
}

def setSeatHeaters(seat,level) {
	if (debugLevel != "None") log.info "Executing 'setSeatHeater'"
    if ((device.currentValue('has_Third_Row_Seats') == "None") && ((seat == "third_row_left") || (seat == "third_row_right")))
    {
     log.warn "Vehicle Does not have Third Row Seats! Operation Ignored!"
    }
    else
    {
	 def result = parent.setSeatHeaters(device.currentValue('vin'), seat,level)
     if (result) { refresh() }
    }
}

def setSeatCooling(seat,level) {
	if (debugLevel != "None") log.info "Executing 'setSeatCooling'"
    if (device.currentValue('has_Seat_Cooling') != "false")
    {
       if ((device.currentValue('has_Third_Row_Seats') == "None") && ((seat == "third_row_left") || (seat == "third_row_right")))
         log.warn "Vehicle Does not have Third Row Seats! Operation Ignored!"
        else
        {
   	      def result = parent.setSeatCooling(device.currentValue('vin'), seat,level)
          if (result) { refresh() }
        }
    }
  else log.warn "Vehicle Does not have Seat Cooling! Opearation Ignored!"
}

def sentryModeOn() {
	if (debugLevel != "None") log.info "Executing 'Turn Sentry Mode On'"
	def result = parent.sentryModeOn(device.currentValue('vin'))
    if (result) { refresh() }
}

def sentryModeOff() {
	if (debugLevel != "None") log.info "Executing 'Turn Sentry Mode Off'"
	def result = parent.sentryModeOff(device.currentValue('vin'))
    if (result) { refresh() }
}

def valetModeOn() {
	if (debugLevel != "None") log.info "Executing 'Turn Valet Mode On'"
	def result = parent.valetModeOn(device.currentValue('vin'))
    if (result) { refresh() }
}

def valetModeOff() {
	if (debugLevel != "None") log.info "Executing 'Turn Valet Mode Off'"
	def result = parent.valetModeOff(device.currentValue('vin'))
    if (result) { refresh() }
}

def ventWindows() {
	if (debugLevel != "None") log.info "Executing 'Venting Windows'"
	def result = parent.ventWindows(device.currentValue('vin'))
    if (result) { refresh() }
}

def closeWindows() {
	if (debugLevel != "None") log.info "Executing 'Close Windows'"
	def result = parent.closeWindows(device.currentValue('vin'))
    if (result) { refresh() }
}

def steeringWheelHeatOn() {
	if (debugLevel != "None") log.info "Executing 'Steering Wheel Heat On'"
	def result = parent.steeringWheelHeatOn(device.currentValue('vin'))
    if (result) { refresh() }
}

def steeringWheelHeatOff() {
	if (debugLevel != "None") log.info "Executing 'Steering Wheel Heat Off'"
	def result = parent.steeringWheelHeatOff(device.currentValue('vin'))
    if (result) { refresh() }
}

def startDefrost() {
	if (debugLevel != "None") log.info "Executing 'Start Max Defrost'"
	def result = parent.startDefrost(device.currentValue('vin'))
    if (result) { refresh() }
}

def stopDefrost() {
	if (debugLevel != "None") log.info "Executing 'Stop Max Defrost'"
	def result = parent.stopDefrost(device.currentValue('vin'))
    if (result) { refresh() }
}


def remoteStart() {
	if (debugLevel != "None") log.info "Executing 'Remote Start'"
	def result = parent.remoteStart(device.currentValue('vin'))
    if (result) { refresh() }
}

def ventSunroof() {
	if (debugLevel != "None") log.info "Executing 'Vent Sunroof'"
    if (device.currentValue('has_Sunroof') != "false")
    {
	   def result = parent.ventSunroof(device.currentValue('vin'))
       if (result) { refresh() }
    }
    else log.warn "Sunroof not present in Vehicle! Opearation Ignored!"
}

def closeSunroof() {
	if (debugLevel != "None") log.info "Executing 'Close Sunroof'"
    if (device.currentValue('has_Sunroof') != "false")
    {
	def result = parent.closeSunroof(device.currentValue('vin'))
    if (result) { refresh() }
    }
    else log.warn "Sunroof not present in Vehicle! Opearation Ignored!"      
}

def listDrivers() {
	if (debugLevel != "None") log.info "Executing 'List Drivers'"
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
    

 def Double calculateDistanceBetweenTwoLatLongsInKm(
    Double lat1, Double lon1, Double lat2, Double lon2) {
    
     if (debugLevel == "Full") log.debug "in calc distance lat1 = $lat1, $lon1, lat2 = $lat2, $lon2"
    
  def Double p = 0.017453292519943295;
  def Double a = 0.5 -
      Math.cos((lat2 - lat1) * p) / 2 +
      Math.cos(lat1 * p) * Math.cos(lat2 * p) * (1 - Math.cos((lon2 - lon1) * p)) / 2;
  return 12742 * Math.asin(Math.sqrt(a));
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
    if (debugLevel != "None") log.info "Refresh time currently set to: $refreshTime, overriding manually with $newRefreshTime"
 
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
        if (debugLevel != "None") log.info "Disabling..."
        unschedule(refresh)
        device.updateSetting("refreshTime",[value:"Disabled",type:"enum"])
    }
    
    else 
      { 
          log.warn "Unknown refresh time specified.. defaulting to 15 Minutes"
          runEvery15Minutes(refresh)
          device.updateSetting("refreshTime",[value:"15-Minutes",type:"enum"])
      }
}

def getBatteryHealth()
{
   	if (debugLevel != "None") log.info "Executing 'getBatteryHealth'"
    
       def data = parent.getBatteryHealth(device.currentValue('vin'))
	   processBatteryHealth(data)  
}
   
private processBatteryHealth(data) {
    
    // this returns a result list so make sure we have correct vin
    
	if (data) {
    	if (debugLevel != "None") log.info "processbatteryHealthData: ${data}"
        
        if (data.results)
        {
         if (debugLevel == "Full") log.debug "got battery result data = ${data}"
         
         for (onecar in data)
            {
         
             def vin = onecar.vin
             def cvin = device.currentValue('vin')
           
              if (onecar?.vin)  
                {
                 if (debugLevel == "Full")  log.debug "vin = $vin, cvin = $cvin"  
                
                 if (vin == cvin)
                  {
                   def capacity = onecar.capacity
                   def originalCapacity = onecar.original_capacity
                   def degradation = onecar.degradation_percent
                   def health = onecar.health_percent
                     
                   if (debugLevel == "Full") log.debug "health percent = $health %, capacity = $capacity, orig. capacity = $originalCapacity, degradation = $degradation %"
            
                   sendEvent(name: "batteryCapacity", value: capacity)
                   sendEvent(name: "batteryOriginalCapacity", value: originalCapacity)
                   sendEvent(name: "batteryDegradation", value: degradation)
                   sendEvent(name: "batteryHealth", value: health)
                  }
                }
            }
            
        }
    }
}

// lgk new web socket fleet api code

def toPSI()
{
    def BigDecimal PSI = 14.503773773
    return PSI
}
 
def processTimeToFullCharge2(perc)
{
    def double minutestofc = perc * 60.0
    def intmin = minutestofc.toInteger()
    processTimeToFullCharge(intmin)
}

def processTimeToFullCharge(perc)
{       
    if (device.currentValue('chargingState') == "Charging")
     {  
       if (perc != null )  
       {  
        def intmin = perc
        def remain = 0
        def hrs = 0
        def hrsint = 0
           
        
       // send minutes here are this is no longer coming through the websocket api
       sendEvent(name: "minutes_to_full_charge", value: intmin)   
        
       if (intmin >= 60)
       {
        hrs = (intmin / 60)  
        hrsint = hrs.toInteger()
        remain = (intmin - (hrsint * 60) ).toInteger() 
          
       }
       else
       {
         hrsint = hrs.toInteger()
         remain = intmin
       }
           
        // now create string
        def timestring = ""
        if (hrsint == 1)
        {
           timestring = "1 hour"
           // now handle minutes
           if (remain == 1)
             {
               timestring = timestring + " and 1 minute"
             }
           else if (remain > 0)
           {
            timestring = timestring + " and " + remain.toString() + " minutes"
           }
        }
        else if (hrsint > 1)
        {
           timestring = hrsint.toString() + " hours"
           if (remain == 1)
             {
               timestring = timestring + " and 1 minute"
             }
           else if (remain > 0)
           {
            timestring = timestring + " and " + remain.toString() + " minutes"
           }
        }
        else if (hrsint == 0)
        { 
          if (remain == 1)
            {
               timestring = timestring + "1 minute"
            }
           else if (remain > 0)
           {
            timestring = timestring + remain.toString() + " minutes"
           }   
        }
        
        // finish it off
        timestring = timestring + " remaining to charge limit"
        
        sendEvent(name: "timeToFullCharge", value: timestring)
            
       } 
     } // we are charging
   else
   {
       // not charge reset charge to time as for some reason the websocket still sends times here when the car is NOT charging but power is on say for conditioning.
       sendEvent(name: "timeToFullCharge", value: "Not Charging")
   }
}  

def checkAltHomePresence(it) {
  if (useAltPresence == true)
    {
      if (homeLongitude == null || homeLatitude == null)
        {
         log.error "Error: Home longitude or latitude is null and Alternate Presence method selected, Boundry Checking disabled!"
         useAltPresence = false;
         device.updateSetting("useAltPresence",[value:"false",type:"bool"]) 
        }
       else
       {
    
        def Float vehlat = it.value?.locationValue?.latitude?.toFloat()
		def Float vehlong = it.value?.locationValue?.longitude?.toFloat()
		def Float homelong =  homeLongitude.toFloat() //-71.5996 
    	def Float homelat = homeLatitude.toFloat() // 42.908368 
		
        if (debugLevel == "Full")    
        	{
            log.debug "current vehicle longitude,latitude = [ $vehlong, $vehlat ]"                 
            log.debug "User set home longitude,latitude =   [ $homelong, $homelat ]"
            }
        
        def Double dist = calculateDistanceBetweenTwoLatLongsInKm(vehlong, vehlat, homelong, homelat)
        if (debugLevel != "None") log.info "Calculated distance from home: $dist"
         
        if (dist <= boundryCircleDistance.toDouble())
        	{ 
                if (device.currentValue('altPresent') == 'not present')
                {
                    if (debugLevel != "None") log.info "Vehicle in range... setting presence to true"
                    sendEvent(name: "altPresent", value: "present")
                    sendEvent(name: "presence", value: "present")
                }
            }
            else 
            {
            	if (device.currentValue('altPresent') == 'present')
                {
                	if (debugLevel != "None") log.debug "Vehicle outside range... setting presence to false"
                    sendEvent(name: "altPresent", value: "not present")
                    sendEvent(name: "presence", value: "not present")
        		}
			}
       }
    }
}

/* --------------------------- web socket fleet api code enhancements -------------------------------- */

def webSocketInit() {
    // this gets called on a close or problem and spawns a new open, dont want a new open if we are disabled to sleep.
    state.remove('webSocketOpenDelay')
    webSocketOpen()
}

def webSocketClose() {
    state.remove('webSocketTimestamp')
    interfaces.webSocket.close()
}

def webSocketOpen() {
    String tessieAccessToken = getParent()?.state?.tessieAccessToken ?: getParent()?.tessieAccessToken
    String cvin = device.currentValue('vin')
    if (tessieAccessToken && cvin && !state?.webSocketTimestamp)
    {
        try
        {
    		interfaces.webSocket.connect("wss://streaming.tessie.com/${cvin}?access_token=${tessieAccessToken}")  
             if ((debugLevel != "None") || (debugWebSocketAPI))  "Websock opened!"
        }
        catch (Exception e)
        {
        	log.error("${device.displayName} webSocketOpen() exception: $e")
    	}      
    }
    else if(state?.webSocketTimestamp)
    {
        webSocketClose()
        runIn(5,"webSocketOpen")
    } 
    else
    {
        log.error "${device.displayName} webSocketOpen() could not start websocket vin:$cvin token:$tessieAccessToken"
    }
}

def webSocketStatus(String message) {
    // lgk dont do anything such as reopen if it is not enabled
    
    if (useRealTimeAPI)
    {      
  
    if ((debugLevel == "FULL") || (debugWebSocketAPI)) log.debug("${device.displayName} webSocketStatus $message${state?.webSocketTimestamp ? " after ${(now() - state?.webSocketTimestamp)/1000 as Integer} seconds" : ""}")    
    if (message?.contains("open"))
    {
        if ((debugLevel != "None" || (debugWebSocketAPI)) && state?.webSocketOpenDelay!=1) log.info "<b>${device.displayName} websocket open</b>"
        state.webSocketTimestamp = now()
        
        if (state?.currentVehicleState!="asleep")
        {
            runIn(2,refresh)
            if ((debugLevel != "None") || (debugWebSocketAPI)) log.info "Scheduling refresh from Websocket API"
        }
    }
    else if (state?.webSocketTimestamp)
    {
        // we had a good connection and now closing. but if open for less than one minute we had a problem. lets slow down next attempt. max ten minutes delay     
        // if disabled by time dont re-open
        if (state.disabled != true)
        {
         state.webSocketOpenDelay = (now() - state.webSocketTimestamp > 60*1000) ? 1 : (state?.webSocketOpenDelay ? Math.min( state.webSocketOpenDelay * 2, 600 ) : 2)
         if (state?.webSocketOpenDelay>2) log.warn "${device.displayName} delaying websocket retry by $state.webSocketOpenDelay seconds with reason: $message (short connection)"
         webSocketClose()
         runIn(state.webSocketOpenDelay,"webSocketOpen")
        }
    }
    else
    {
        
        // we never had a good connection. lets slow down next attempt. max ten minutes delay 
        if (state.disabled != true)
        {
          state.webSocketOpenDelay = state?.webSocketOpenDelay ? Math.min( state.webSocketOpenDelay * 2, 600 ) : 2
          if (state?.webSocketOpenDelay>2) log.warn "${device.displayName} delaying websocket retry by $state.webSocketOpenDelay seconds with reason: $message"
          runIn(state.webSocketOpenDelay,"webSocketOpen")
        }
    }
    }
}

def webSocketParse(String message) {
    try {
        def data = parseJson(message)        
        if (data?.data)
        {
        	webSocketProcess(data.data)
        }
        else
        {
             if ((debugLevel == "FULL") || (debugWebSocketAPI)) log.debug("${device.displayName} webSocketParse() message: $data")
            // if (state?.currentVehicleState=="asleep") runIn(2,refresh) // dont want refresh on every iteration. 
        }
    }
    catch (Exception e)
    {
        log.warn("${device.displayName} webSocketParse() exception ignored: $e message: $message")
    }
}


// The api was changing, so not sure how long term these functions are needed. But for now, they work. 
// [value:[stringValue:6.700000695884228], key:InsideTemp] but sometimes [value:[doubleValue:6.700000695884228], key:InsideTemp]
float getValueFloat(Map value) { return (value?.doubleValue ?: value?.stringValue ?: value?.intValue ?: 0.0).toFloat() }
float getValueTemp(Map value) 
{     
   // lgk special case here celsius to fahrenheit not working when the value = 0 which is realy 32
    def fval = getValueFloat(value)
    //if (debugWebSocketAPI) log.debug " input = ${value.toString()}  fval = ${value.toString()}"
 
    if ((fval = 0.0) && (tempScale != "C")) return 32
    else
    { 
        def res = (tempScale == "C") ? getValueFloat(value) : celsiusToFahrenheit(getValueFloat(value))
        //if (debugWebSocketAPI) log.debug "result = ${res.toFloat().toString()}"
        return res.toFloat()
    }
}

def handleInvalidSpeed(it)
{ 
   // assume invalid means stopped to work around a bug in api missing the 0 case or interpretting it as null when we first stop
   // if speed is invalid and we have a speed > 0 reset it
    if (debugWebSocketAPI) log.warn "in vehicle speed case"
    if (it.value?.invalid?.toBoolean() == true)
        {
          sendEventX(name: "speed", value: 0, unit: (mileageScale == "M") ? "mph" : "kph" );  
          sendEventX(name: "motion", value: "inactive")
        }
     else // set new websocket speed report state
     {
         if ((debugLevel != "None") && (state.haveWebSocketSpeedReport != "true")) // check so only get message once after startup
           {
            log.info "Have Telemetry data via WebSocket. Disabling Speed and Motion reports via legacy API."
           }      
         state.haveWebSocketSpeedReport = "true"      
     }
}

float getValueMile(Map value) { return (mileageScale == "M") ? getValueFloat(value) : getValueFloat(value)*1.609344 }
Integer getValueInt(Map value) { return Math.round(getValueFloat(value)) as Integer}
Boolean getValueBool(Map value) { return (value?.stringValue ?: value?.booleanValue).toBoolean() }

void webSocketProcess(data) {
 
    final Map<String, Closure> handlers = [
        "BatteryLevel": { it -> sendEventX(name: "battery", value: getValueInt(it?.value), unit: "%") }, // [value:[stringValue:80.68002108592515], key:BatteryLevel]
        "SentryMode": { it -> sendEventX(name: "sentry_mode", value: it.value?.sentryModeStateValue=="SentryModeStateOff" ? "Off" : "On") }, // [value:[sentryModeStateValue:SentryModeStateOff], key:SentryMode]
        "Locked": { it -> sendEventX(name: "lock", value: getValueBool(it?.value) ? "locked" : "unlocked") }, // [value:[stringValue:true], key:Locked]
        "InsideTemp": { it -> sendEventX(name: "temperature", value: Math.round( getValueTemp(it?.value) ), unit: tempScale) }, // [value:[stringValue:6.700000695884228], key:InsideTemp] but sometimes [value:[doubleValue:6.700000695884228], key:InsideTemp]
        "OutsideTemp": { it -> sendEventX(name: "outside_temperature", value: Math.round( getValueTemp(it?.value) ), unit: tempScale) }, // [value:[stringValue:4.5], key:OutsideTemp]
        "TpmsHardWarnings": { it -> /* Handle TpmsHardWarnings */ },
        "TpmsLastSeenPressureTimeFl": { it -> /* Handle TpmsLastSeenPressureTimeFl */ }, // [value:[stringValue:Sat Jan 11 13:31:17 2025], key:TpmsLastSeenPressureTimeFl]
        "TpmsLastSeenPressureTimeFr": { it -> /* Handle TpmsLastSeenPressureTimeFr */ },
        "TpmsLastSeenPressureTimeRl": { it -> /* Handle TpmsLastSeenPressureTimeRl */ },
        "TpmsLastSeenPressureTimeRr": { it -> /* Handle TpmsLastSeenPressureTimeRr */ },
        "TpmsPressureFl": { it -> sendEventX(name: "tire_pressure_front_left", value: (getValueFloat(it?.value) * toPSI()).round(1), unit: "psi") }, //  [value:[stringValue:2.525000037625432], key:TpmsPressureFl]
        "TpmsPressureFr": { it -> sendEventX(name: "tire_pressure_front_right", value: (getValueFloat(it?.value) * toPSI()).round(1), unit: "psi") },
        "TpmsPressureRl": { it -> sendEventX(name: "tire_pressure_rear_left", value: (getValueFloat(it?.value) * toPSI()).round(1), unit: "psi") },
        "TpmsPressureRr": { it -> sendEventX(name: "tire_pressure_rear_right", value: (getValueFloat(it?.value) * toPSI()).round(1), unit: "psi") },        
        "FdWindow": { it -> sendEventX(name: "front_drivers_window", value: it.value?.stringValue) }, // [value:[stringValue:Closed], key:FdWindow]
        "FpWindow": { it -> sendEventX(name: "front_pass_window", value: it.value?.stringValue) },
        "RdWindow": { it -> sendEventX(name: "rear_drivers_window", value: it.value?.stringValue) },
        "RpWindow": { it -> sendEventX(name: "rear_pass_window", value: it.value?.stringValue) },
        "SoftwareUpdateScheduledStartTime": { it -> /* Handle SoftwareUpdateScheduledStartTime */ },
        "VehicleSpeed": { it -> Integer speed = Math.round(getValueMile(it?.value)); sendEventX(name: "speed", value: speed, unit: (mileageScale == "M") ? "mph" : "kph" ); sendEventX(name: "motion", value: speed>0 ? "active" : "inactive") }, // [value:[invalid:true], key:VehicleSpeed]
        "ExpectedEnergyPercentAtTripArrival": { it -> sendEventX(name: "active_route_energy_at_arrival", value: getValueInt(it?.value)) },
        "MilesToArrival": { it -> sendEventX(name: "active_route_miles_to_arrival", value: getValueMile(it?.value).round(1), unit: mileageScale ) }, // [value:[doubleValue:1.4336150427289294], key:MilesToArrival] 
        "MinutesToArrival": { it -> sendEventX(name: "active_route_minutes_to_arrival", value: getValueFloat(it?.value).round(2), unit: "min" ) }, // [value:[doubleValue:7], key:MinutesToArrival] 
        "Gear": { it -> /* Handle Gear */ }, //  [value:[shiftStateValue:ShiftStateP], key:Gear],  [value:[shiftStateValue:ShiftStateD], key:Gear], [value:[shiftStateValue:ShiftStateR], key:Gear]
        "ClimateKeeperMode": { it -> /* Handle ClimateKeeperMode */ },
        "ScheduledChargingStartTime": { it -> /* Handle ScheduledChargingStartTime */ },

        "SeatHeaterLeft": { it -> sendEventX(name: "seat_heater_left", value: getValueInt(it?.value)) }, //  [value:[stringValue:0], key:SeatHeaterLeft]
        "SeatHeaterRight": { it -> sendEventX(name: "seat_heater_right", value: getValueInt(it?.value)) },
        "SeatHeaterRearLeft": { it -> sendEventX(name: "seat_heater_rear_left", value: getValueInt(it?.value)) },
        "SeatHeaterRearCenter": { it -> sendEventX(name: "seat_heater_rear_center", value: getValueInt(it?.value)) },
        "SeatHeaterRearRight": { it -> sendEventX(name: "seat_heater_rear_right", value: getValueInt(it?.value)) },
        "HvacSteeringWheelHeatAuto": { it -> /* Handle HvacSteeringWheelHeatAuto */ },
        "HvacLeftTemperatureRequest": { it -> sendEventX(name: "thermostatSetpoint", value: Math.round( getValueTemp(it?.value) ), unit: tempScale) }, // [value:[doubleValue:20.5], key:HvacLeftTemperatureRequest] 
        "HvacRightTemperatureRequest": { it -> sendEventX(name: "passengerSetpoint", value: Math.round( getValueTemp(it?.value) ), unit: tempScale) },        
        "HvacPower": { it -> sendEventX(name: "thermostatMode", value: it.value?.hvacPowerValue=="HvacPowerStateOff" ? "off" : "auto") }, // [value:[hvacPowerValue:HvacPowerStateOn], key:HvacPower], [value:[hvacPowerValue:HvacPowerStateOff], key:HvacPower] 
        "AutoSeatClimateLeft": { it -> /* Handle AutoSeatClimateLeft */ },
        "AutoSeatClimateRight": { it -> /* Handle AutoSeatClimateRight */ },        

        "RouteTrafficMinutesDelay": { it -> /* Handle RouteTrafficMinutesDelay */ },
        "BatteryHeaterOn": { it -> /* Handle BatteryHeaterOn */ },
        "CurrentLimitMph": { it -> /* Handle CurrentLimitMph */ },
        "RatedRange": { it -> sendEventX(name: "batteryRange", value: Math.round( getValueMile(it?.value) ), unit: mileageScale ) }, // [value:[stringValue:225.9622591002932], key:RatedRange]
        "RemoteStartEnabled": { it -> /* Handle RemoteStartEnabled */ },
        "SoftwareUpdateVersion": { it -> /* Handle SoftwareUpdateVersion */ },
        "Odometer": { it -> sendEventX(name: "odometer", value: getValueMile(it?.value).toInteger(), unit: mileageScale ) }, // [value:[stringValue:3379.656716239699], key:Odometer]
        "DefrostMode": { it -> /* Handle DefrostMode */ },
        "ScheduledChargingPending": { it -> /* Handle ScheduledChargingPending */ },
        "VehicleName": { it -> /* Handle VehicleName */ },
        "CabinOverheatProtectionMode": { it -> /* Handle CabinOverheatProtectionMode */ },
        "ACChargingPower": { it -> /* Handle ACChargingPower */ },
        "EnergyRemaining": { it -> /* Handle EnergyRemaining */ },        
        "DestinationLocation": { it -> /* Handle DestinationLocation */ },
        "PackCurrent": { it -> /* Handle PackCurrent */ },
        "CabinOverheatProtectionTemperatureLimit": { it -> /* Handle CabinOverheatProtectionTemperatureLimit */ },        
        "ModuleTempMin": { it -> /* Handle ModuleTempMin */ },
        "SoftwareUpdateExpectedDurationMinutes": { it -> /* Handle SoftwareUpdateExpectedDurationMinutes */ },
        "SoftwareUpdateInstallationPercentComplete": { it -> /* Handle SoftwareUpdateInstallationPercentComplete */ },

        "PackVoltage": { it -> /* Handle PackVoltage */ },
		"GuestModeEnabled": { it -> /* Handle GuestModeEnabled */ },
        "ValetModeEnabled": { it -> sendEventX(name: "valet_mode", value: getValueBool(it?.value) ? "On" : "Off") }, // [value:[booleanValue:false], key:ValetModeEnabled]
        "GpsHeading": { it -> sendEventX(name: "heading", value: getValueInt(it?.value), unit: "°") }, // [value:[stringValue:63.3255034690718], key:GpsHeading]
        "LifetimeEnergyUsed": { it -> /* Handle LifetimeEnergyUsed */ },
        "DCChargingPower": { it -> /* Handle DCChargingPower */ },
        "Location": { it -> checkAltHomePresence(it); sendEventX(name: "latitude", value: it.value?.locationValue?.latitude?.toFloat(), unit: "°");  sendEventX(name: "longitude", value: it.value?.locationValue?.longitude?.toFloat(), unit: "°"); }, //  [value:[locationValue:[latitude:41.040694, longitude:-73.540426]], key:Location]
        
        "Soc": { it -> /* Handle Soc */ },
        "Version": { it -> /* Handle Version */ },
        "SpeedLimitMode": { it -> /* Handle SpeedLimitMode */ }, // [value:[stringValue:false], key:SpeedLimitMode]
        "ScheduledChargingMode": { it -> /* Handle ScheduledChargingMode */ }, // [value:[stringValue:Off], key:ScheduledChargingMode]
        "ModuleTempMax": { it -> /* Handle ModuleTempMax */ }, // [value:[stringValue:5], key:ModuleTempMax]
        "SoftwareUpdateDownloadPercentComplete": { it -> /* Handle SoftwareUpdateDownloadPercentComplete */ }, // [value:[intValue:0], key:SoftwareUpdateDownloadPercentComplete]
        "HomelinkNearby": { it -> if (useAltPresence != true) { sendEventX(name: "presence", value: getValueBool(it?.value) ? "present" : "not present")  }/* Handle HomelinkNearby */ }, // [value:[booleanValue:false], key:HomelinkNearby]
        "ChargeAmps": { it -> sendEventX(name: "current_charge_amps", value: getValueInt(it?.value)) },
        "ChargeLimitSoc": { it -> sendEventX(name: "current_charge_limit", value: getValueInt(it?.value)) },
        "ChargeCurrentRequestMax": { it -> /* Handle ChargeCurrentRequestMax */ },
        "ChargeCurrentRequest": { it -> /* Handle ChargeCurrentRequest */ },
        "ChargePortLatch": { it -> /* Handle ChargePortLatch */ },
        "ChargePortDoorOpen": { it -> /* Handle ChargePortDoorOpen */ },
        "FastChargerPresent": { it -> /* Handle FastChargerPresent */ }, //  [value:[stringValue:false], key:FastChargerPresent]
        "FastChargerType": { it -> /* Handle FastChargerType */ },
        "DetailedChargeState": { it -> /* Handle DetailedChargeState */ },
        "TimeToFullCharge": { it -> processTimeToFullCharge2(getValueFloat(it?.value)) /* Handle TimeToFullCharge */ }, // this is minutes in decimal percentage of an hour so multiple by 60.
        "PinToDriveEnabled": { it -> /* Handle PinToDriveEnabled */ }, // [value:[stringValue:false], key:PinToDriveEnabled]
        "DriverSeatOccupied": { it -> sendEventX(name: "user_present", value: getValueBool(it?.value)) }, //  [value:[booleanValue:false], key:DriverSeatOccupied] 
        "DestinationName": { it -> sendEventX(name: "active_route_destination", value: it?.value?.stringValue) }, //  [value:[stringValue:Home], key:DestinationName]
        "ChargerPhases": { it -> /* Handle ChargerPhases */ }, // [value:[intValue:1], key:ChargerPhases]
      
        "DoorState": { it -> // [value:[doorValue:[DriverRear:false, PassengerFront:false, TrunkFront:false, PassengerRear:false, TrunkRear:false, DriverFront:false]], key:DoorState] was [value:[stringValue:DriverFront|PassengerFront|DriverRear|PassengerRear|TrunkFront|TrunkRear], key:DoorState] or [value:[stringValue:], key:DoorState]
            sendEventX(name: "front_drivers_door", value: it.value?.doorValue?.DriverFront?.toBoolean() ? "Open" : "Closed")
            sendEventX(name: "front_pass_door", value: it.value?.doorValue?.PassengerFront?.toBoolean() ? "Open" : "Closed")
            sendEventX(name: "rear_drivers_door", value: it.value?.doorValue?.DriverRear?.toBoolean() ? "Open" : "Closed")
            sendEventX(name: "rear_pass_door", value: it.value?.doorValue?.PassengerRear?.toBoolean() ? "Open" : "Closed")
            sendEventX(name: "frunk", value: it.value?.doorValue?.TrunkFront?.toBoolean() ? "Open" : "Closed")
            sendEventX(name: "trunk", value: it.value?.doorValue?.TrunkRear?.toBoolean() ? "Open" : "Closed")          
        }, 
    ]

    if ((debugLevel == "FULL") || (debugWebSocketAPI)) log.debug "full Websocket fleet api data = $data"
    
	data?.each
    {
        if ((debugLevel == "FULL") || (debugWebSocketAPI)) log.debug "in handler item = $it"
        // special case spped invalid
        //log.warn "key = ${it.key}"
        
        if (it.key == "VehicleSpeed")
        {
            handleInvalidSpeed(it)
        }
        
        if (it?.value==null || it.value?.invalid?.toBoolean()!=true)
        { // toss any invalid data first
        	if ((handlers.get(it.key, { 
                if (debugLevel == "Full") log.warn("${device.displayName} webSocketProcess() did not handle item: $it")
                return true 
            })(it)) != true) {
            	if (debugLevel == "Full") log.debug("${device.displayName} webSocketProcess() did not process item: $it")
        	}
        }
    }
    
    if (data != null)
    {
        def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
        sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
        
        //if (debugLevel != "None") log.info "WebsocketAPI: processing data!"
    }
}

Boolean sendEventX(Map x)
{
   if ((debugLevel == "Full") || (debugWebSocketAPI)) log.debug "in sendeventx:[ ${x?.name}, ${x?.value} prior: ${device.currentValue(x?.name).toString().toLowerCase()} ]"
   if (x?.value!=null && device.currentValue(x?.name).toString().toLowerCase() != x?.value.toString().toLowerCase() && !x?.eventDisable)
    {
        if ((debugLevel == "Full") || (debugWebSocketAPI)) log.debug("new value will be set")
    	x.descriptionText = x.descriptionText ?: "${device.displayName} ${x.name.replace('_', ' ')} is $x.value${x?.unit?:""}"
        if (x?.logLevel=="warn") log.warn (x?.descriptionText); else if (debugLevel == "Full") log.info (x?.descriptionText)
        sendEvent(name: x?.name, value: x?.value, unit: x?.unit, descriptionText: x?.descriptionText, isStateChange: (x?.isStateChange ?: false))
        
    }     
    return true
}
