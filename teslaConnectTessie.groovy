
/**
 *  Tesla Connect app for Tessie www.tessia.com api.tessie.com
 *
 *  Copyright 2024 Larry Kahn
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.

 * initial release 1/13/24 v 1.0 Beta
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
 * v 1.2 1/13/24 For some reason only open frunk trunk worked but was getting a timeout error.. not sure why but had to add new functions to pass in a 30 sec timeout.
 *
 * v 1.3 1/14/24 add current address attribute.
 *
 * v 1.4 1/16/24 attempt at fixing incorrect vin for multiple vehicles.
 * openning application and reselecting all the vehicles hopefully will fix the incorrect vin on the child device.
 *
 * v 1.5 1/16/24 skip over vehicle in account ie on order, without a valid vehicle id or vin
 * 
 * 1/17 Add attribute usableBattery.. may want to change rules to this as that is what tesla app appears to report for battery
 * also round the range as similiarly tesla app does this and when we get range for instance 250.62 tesla app reports 251,
 * we previously showed 250 as it was truncating.
 *
 * 1/17 add current verion fx and print in logs in app and device in updated and initialize
 * 1/18 clean up a couple of debug messages.
 *
 * v 1.7 add some vehicle config attributes: car_type, has_third_row_seats, has_seat_cooling, has_sunroof
 * and check these when trying commands and print warning when using these commands if car does not have that feature.
 * unfortunately hubitat groovy does not support dynamic capabilities/commands.
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
 * v 1.82 set status to awake after a wake call so we get data immediately.
 *
 * v 1.85 retry requests up to 3 times with exponential raise in timeouts.
 *
 * v 1.86 refine retry code to only raise timeout and retry on java.net.SocketTimeoutException: Read timed out error. 
 * Also change wording on wake on initial try and change default to false. Also handle org.apache.http.conn.ConnectTimeoutException in case
 * net is down in a similar way.
 *
 * v 1.9 typo in state.reducedRefreshDisabled kept it from working.
 * v 1.91 handle speed of 0 interpeted as null.
 * v 1.92 add savedLocation attribute and populate from tessie
 * v 1.93 missed an debug line in the new address/saved location code that was not checking if debug was on.
 * v 1.95 as Sebastian noticed i screwed up on the set charging amps call so new version to fix it.
 * v 1.96 missing the import to handle java.net.sockettimeoutexception
 * v 1.97 handle offline state better.
 * v 1.98 add battery health query
 * v 1.99 fix issue finding cars with a null name and add code to skip cars without a vin as it is required.
 * v 2.0 add following attributes active_route_destination, active_route_minutes_to_arrival thanks Alan_F
 * v 2.01 round minutes to arrivate to 2 decimal digits
 * v 2.02 it was truncating to whole integer instead of 2 digits for minutes to arrival... fix
 *    also added active_route_miles_to_arrival and active_route_energy_at_arrival
 * v 2.03 round miles to arrival to 1 digit past decimal.
 * v 2.04 fixed bug.. type in settemperature when in celsius mode
 *
 * v 2.1 first integration of webstocket fleet api
 * v 2.11 fix typo in disable fx
 * v 2.12 fromtime was not disabling the websocket correctly, the status fx was reopening it.
 * v 2.13 fix for timetofullcharge attr
 * v 2.16 fix set temps
 * v 2.17 change of debug to say when we set presence to false, also dont reset presence to either true or false 
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
 * v 2.20 ignore speed/motion if websocket is enabled and we get a valid speed from the websocket api. (reset on saving preference)
 * 
 *
 */

import groovy.transform.Field
import java.net.SocketTimeoutException

definition(
    name: "Tesla Connect Tessie",
    namespace: "lgkahn",
    author: "Larry Kahn",
    description: "Integrate your Tesla car with SmartThings.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%402x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%403x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%403x.png",
    singleInstance: true
)

preferences {
	page(name: "loginToTesla", title: "Tesla")
	page(name: "selectVehicles", title: "Tesla")
}

def loginToTesla() {
//	def showUninstall = email != null && password != null
	return dynamicPage(name: "VehicleAuth", title: "Connect your Tesla", nextPage:"selectVehicles", uninstall:false) {
		section("Token refresh options:") {
            input "tessieAccessToken", "string", title: "Input your Tessie Access Token?", required: true
	        input "debug", "bool", title: "Enable detailed debugging?", required: true, defaultValue: false
            input "descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true
            input "pauseTime", "enum", title: "Time (in seconds) to automatically Wait/Pause after a wake before issuing the requested command.", required: true, defaultValue: "15", options:["2","3","4","5","6","7","8","9","10","15","20","30"]
            input "wakeOnInitialTry", "bool", title: "Should I issue a wake and pause (the above time) on every command issued - (Not necessary with Tessie unless you want the car awake on every refresh)?", required: true, defaultValue: false                                                                                                                                      
        }   
        }
              
		section("To use Tesla, Hubitat encrypts and securely stores a token.") {}
	}

def logsOff()
{
    log.debug "Turning off Logging!"
    device.updateSetting("debug",[value:"false",type:"bool"])
}

def transitionAccessToken(child)
{
    log.debug "Initializing tesla access token for switch over."
    state.tessieAccessToken = tessieAccessToken
    return true;
}  
    
def selectVehicles() 
{
    if (debug) log.debug "In select vehicles"
    state.tessieAccessToken = tessieAccessToken
 
	try {
        
        refreshAccountVehicles()

		return dynamicPage(name: "selectVehicles", title: "Tesla", install:true, uninstall:true) {
			section("Select which Tesla to connect"){
			input(name: "selectedVehicles", type: "enum", required:false, multiple:true, options:state.accountVehicles)
			}
		}
	} catch (Exception e) {
    	log.error e
        return dynamicPage(name: "selectVehicles", title: "Tesla", install:false, uninstall:true, nextPage:"") {
			section("") {
				paragraph "Please check your Token!"
			}
		}
	}
}

def getChildNamespace() { "lgkahn" }
def getChildName() { "tessieVehicle" }
def getServerUrl() { "https://api.tessie.com" }

def getUserAgent() { "lgkahn" }


private convertEpochSecondsToDate(epoch) {
	return new Date((long)epoch * 1000);
}
    
private authorizedHttpVehicleRequest(String path, String method, Closure closure) {
   if (debug) log.debug "in authorize http req"
   
    if (descLog) log.info "authorizedHttpVehicleRequest ${method} ${path}"
    if (debug)
    {
    log.debug "token = ${state.tessieAccessToken}"
    log.debug "server url = $serverUrl"
    log.debug "path = $path"
    log.debug "method = $method"
    }
   
    def timeout = 20
    
    try {
              
    	def requestParameters = [
            uri: serverUrl + path,
            timeout: timeout,
            headers: [
                'User-Agent': userAgent,
                Authorization: "Bearer ${state.tessieAccessToken}"
            ]
        ]
    
         if (debug)log.debug "request parms = ${requestParameters}"
        
     
    	if (method == "GET") {
            httpGet(requestParameters) { resp -> closure(resp) }
        } else if (method == "POST") {
        
           if (debug)
            {
                log.debug "in put case"
                log.debug "parms = $requestParameters"
            }   
            httpPostJson(requestParameters) { resp -> closure(resp) }
            
        } else {
        	log.error "Invalid method ${method}"
        }
    } catch (groovyx.net.http.HttpResponseException | java.net.SocketTimeoutException | org.apache.http.conn.ConnectTimeoutException e) {
        log.debug "in error handler case resp = $resp $e"
        if ((e.response?.data?.status?.code == 14) || (e.response?.data?.status?.code == 401))
           {
            log.debug "code - 14 or 401"
        } else {
        	log.error "Request failed for path: ${path}. $e"         
        }

    }
}

private authorizedHttpRequestWithTimeout(String child, String path, String method, Number timeout, Number tries, Closure closure) {
   if (debug) log.debug "AuthorizeHttpRequestWithTimeout"
   
    if (descLog) log.info "authorizedHttpRequestWithTimeout ${method} ${path} try: ${tries}"
   if (debug)
    {
    log.debug "token = ${state.tessieAccessToken}"
    log.debug "server url = $serverUrl"
    log.debug "path = $path"
    log.debug "method = $method"
    log.debug "timout = $timeout"
    }
   
    try {
              
    	def requestParameters = [
            //uri: 'http://192.168.2.5', test to force connection timeout failure
            uri: serverUrl + path, 
            timeout: timeout,
            headers: [
                'User-Agent': userAgent,               
                Authorization: "Bearer ${state.tessieAccessToken}"
            ]
        ]
    
        if (debug) log.debug "request parms = ${requestParameters}"
            
    	if (method == "GET") {   
            
            httpGet(requestParameters) { resp -> closure(resp) }
        } else if (method == "POST") {
           httpPostJson(requestParameters) { resp -> closure(resp) }
      
            if (debug) {
                log.debug "in put case"
                log.debug "parms = $requestParameters"
            }
                
                 httpPostJson(requestParameters) { resp -> closure(resp) }
        		          
        } else {
        	log.error "Invalid method ${method}"
        }
    } catch (groovyx.net.http.HttpResponseException e)
       {
        log.debug "In general error handler case resp = $resp $e"
        if ((e.response?.data?.status?.code == 14) || (e.response?.data?.status?.code == 401))
           {
            log.debug "code - 14 or 401"     
           }
        else log.error "Request failed for ${path}, ${e.response?.data} - $e!"
       }
    
     catch (java.net.SocketTimeoutException | org.apache.http.conn.ConnectTimeoutException e)
       {
        log.warn "In timeout error handler case $e for try: $tries"                     
        if (tries > 2)
          {    
            log.error "Request failed 3 times for path: ${path}, ${e} - Giving up!"
          } 
         else
         {
           def newtimeout = timeout * 2
           authorizedHttpRequestWithTimeout(child,path,method,newtimeout,++tries,closure)
         }
       } 
}

private refreshAccountVehicles() {
   if (debug) log.debug "in refreshAccountVehicles. current token = $state.tessieAccessToken"
   
	if (descLog) log.info "refreshAccountVehicles"

	state.accountVehicles = [:]
    state.accountVINs = [:]

	authorizedHttpVehicleRequest("/vehicles", "GET", { resp ->
      
        if (debug)
        {
            log.debug "result = $resp"
            log.debug "result = ${resp.data}"
            log.debug "one result = ${resp.data.results}"
        }
        
    	if (descLog) log.info "Found ${resp.data.results.size()} vehicles"
        resp.data.results.each { vehicle ->
           
         //lgk change vehicles can appear in acct without a valid vehicle id so skip thise
           if (vehicle?.last_state?.vehicle_id && vehicle.vin)  
            {
             if (debug)
               {
                log.debug "Found the vehicle = $vehicle"
                log.debug "last_state = ${vehicle.last_state}"
                log.debug "vehicle id= ${vehicle.last_state.vehicle_id}"
                log.debug "vehicle name = ${vehicle.last_state.vehicle_state.vehicle_name}"
                log.debug "vin = ${vehicle.vin}"
               }
            
            def id = vehicle.last_state.vehicle_id
            def vname = vehicle.last_state.vehicle_state.vehicle_name
            def vin = vehicle.vin
            
            if (descLog) log.info "Found Vehicle ${id}: ${vname}, vin: $vin"
        	if (vname != null)
            {
               if (vname != "")
                state.accountVehicles[id] = vname
              else state.accountVehicles[id] = "Tesla ${id}"
            }
           else state.accountVehicles[id] = "Tesla ${id}" // null vname case
           
           if (vin != null)
             {
              if (vin != "")
                 state.accountVINs[id] = vin
              else log.warn "Found an invalid vehicle without a VIN... skipping!"
             }
             else log.warn "Found an invalid vehicle without a VIN... skipping!"
            }
            else log.warn "Found an invalid vehicle without a vehicle id... skipping!"
        }
    })
}


def installed() {
	log.info "Installed"
	initialize()
}

def updated() {
    log.info "Updated current version: ${currentVersion()}"
    
	unsubscribe()
	initialize() 
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	log.debug "deleting ${delete.size()} vehicles"
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def initialize() {
    if (descLog) log.info "in initialize current version: ${currentVersion()}"
    ensureDevicesForSelectedVehicles()
    removeNoLongerSelectedChildDevices()
}

private ensureDevicesForSelectedVehicles() {
	if (selectedVehicles) {
        selectedVehicles.each { dni ->
            def d = getChildDevice(dni)
            if(!d) {
                def vehicleName = state.accountVehicles[dni]
                def vin = state.accountVINs[dni]
                device = addChildDevice("lgkahn", "tessieVehicle", dni, null, [name:"Tesla ${dni}", label:vehicleName])
                log.debug "Created device ${device.label} with id ${dni} vin = ${vin}"
                device.initialize()
                device.sendEvent(name:"vin" , value:vin)
            } else {
                log.info "Device for ${d.label} with id ${dni} already exists"
                def cvin = d.currentValue('vin')
                if (cvin != state.accountVINs[dni])  
                {
                  log.info "Current vin = $cvin"
                  log.info "Resetting vin for vehicle setting vin to ${state.accountVINs[dni]}" 
                  d.sendEvent(name:"vin", value: state.accountVINs[dni])
                }
            }
        }
    }
}

private removeNoLongerSelectedChildDevices() {
	// Delete any that are no longer in settings
	def delete = getChildDevices().findAll { !selectedVehicles }
	removeChildDevices(delete)
}

private transformWakeResponse(resp)
{
 if (debug)
    {
    log.debug "in transform wake tessie"
    log.debug "resp = ${resp}"
    log.debug "result = ${resp.data}"
    }    

	return [
        result: resp.data.result,
        apiUsed: "tessie"
    ]
}

def refresh(child) {
   if (descLog) log.info "in refresh child"
    def data = [:]
	def id = child
    
    	authorizedHttpRequestWithTimeout(child,"/${id}/state", "GET", 20, 1, { resp ->
            
          if (debug) log.debug "In refresh data = ${resp.data}"
          if (resp.data.state != "online")
            {
                data.state = resp.data.state
            }
            else
            { 
           
            def driveState = resp.data.drive_state
            def chargeState = resp.data.charge_state
            def vehicleState = resp.data.vehicle_state
            def climateState = resp.data.climate_state
            def vehicleConfig = resp.data.vehicle_config
            
            data.state = resp.data.state
            data.vin = resp.data.vin
           // log.warn "drive state speed = ${driveState.speed}"
                
            data.speed = driveState.speed ? driveState.speed : 0
            data.motion = data.speed > 0 ? "active" : "inactive"   
            data.thermostatMode = climateState.is_climate_on ? "auto" : "off"      
            data.active_route_destination = driveState.active_route_destination ? driveState.active_route_destination : "none"
            data.active_route_minutes_to_arrival = driveState.active_route_minutes_to_arrival ? driveState.active_route_minutes_to_arrival : 0
            data.active_route_energy_at_arrival = driveState.active_route_energy_at_arrival ? driveState.active_route_energy_at_arrival : 0
            data.active_route_miles_to_arrival = driveState.active_route_miles_to_arrival ? driveState.active_route_miles_to_arrival : 0
                  
           if (debug) log.debug "Vehicle Config = $vehicleConfig"
            data["vehicleConfig"] = [
                has_third_row_seats: vehicleConfig.third_row_seats,
                has_seat_cooling: vehicleConfig.has_seat_cooling,
                car_type: vehicleConfig.car_type,
                sunroof_installed: vehicleConfig.sunroof_intalled 
                ]
            
           if (debug) log.debug "charging state = $chargeState"
            data["chargeState"] = [
                battery: chargeState.battery_level,
                usableBattery: chargeState.usable_battery_level,
                batteryRange: chargeState.battery_range,
                chargingState: chargeState.charging_state,
                chargeLimit: chargeState.charge_limit_soc,
                chargeAmps: chargeState.charge_current_request,
                minutes_to_full_charge: chargeState.minutes_to_full_charge    
            ]
            
            if (debug) log.debug "drive state = $driveState"
            data["driveState"] = [
            	latitude: driveState.latitude,
                longitude: driveState.longitude,
                method: driveState.native_type,
                heading: driveState.heading,
                lastUpdateTime: convertEpochSecondsToDate(driveState.gps_as_of)
            ]
            
              
           if (debug)  log.debug "vehicle state = $vehicleState"
            data["vehicleState"] = [
            	presence: vehicleState.homelink_nearby ? "present" : "not present",
                lock: vehicleState.locked ? "locked" : "unlocked",
                odometer: vehicleState.odometer,
                sentry_mode: vehicleState.sentry_mode ? "On" : "Off",
                front_drivers_window: vehicleState.fd_window ? "Open" : "Closed",
                front_pass_window: vehicleState.fp_window ? "Open" : "Closed",
                rear_drivers_window: vehicleState.rd_window ? "Open" : "Closed",
                rear_pass_window: vehicleState.rp_window ? "Open" : "Closed",
                valet_mode: vehicleState.valet_mode ? "On" : "Off",
                tire_pressure_front_left: vehicleState.tpms_pressure_fl,
                tire_pressure_front_right: vehicleState.tpms_pressure_fr,
                tire_pressure_rear_left: vehicleState.tpms_pressure_rl,
                tire_pressure_rear_right: vehicleState.tpms_pressure_rr,
                front_drivers_door: vehicleState.df ? "Open" : "Closed",
                rear_drivers_door: vehicleState.dr ? "Open" : "Closed",
                front_pass_door: vehicleState.pf ? "Open" : "Closed",
                rear_pass_door: vehicleState.pr ? "Open" : "Closed",
                frunk :  vehicleState.ft ? "Open" : "Closed",
                trunk :  vehicleState.rt ? "Open" : "Closed",
                user_present: vehicleState.is_user_present            
                ]
             
            if (debug) log.debug "climate state = $climateState"
            data["climateState"] = [
            	temperature: celciusToFarenhiet(climateState.inside_temp),
                outside_temperature: celciusToFarenhiet(climateState.outside_temp),
                thermostatSetpoint: celciusToFarenhiet(climateState.driver_temp_setting),
                passengerSetpoint: celciusToFarenhiet(climateState.passenger_temp_setting),
                seat_heater_left: climateState.seat_heater_left,
                seat_heater_left: climateState.seat_heater_left,
                seat_heater_right: climateState.seat_heater_right, 
                seat_heater_rear_left: climateState.seat_heater_rear_left,  
                seat_heater_rear_right: climateState.seat_heater_rear_right,                
                seat_heater_rear_center: climateState.seat_heater_rear_center    
            ]
        }
        })
      
    return data
}

String getWindowStatus(position)
{
    
     switch(position) {
          case "2":
              return "Vented"
              break
          case "1":
              return "Open"
           case "0":
              return "Closed"
              break;
          default: 
              return "Unknown"
              break
      }
}

private celciusToFarenhiet(dC) {
    if (dC)
    {
     def fvalue  = dC * 9/5 +32
     return fvalue.toInteger()
    }
    else return 0
	//return dC * 9/5 + 32
}

private farenhietToCelcius(dF) {
    // 0 = null = -17 probably never come up so dont worry about it lgk
    if (dF)
	return (dF - 32) * 5/9
    else return 0
}

def wake(child) {
     
    def id = child
    def data = [:]
 
 
    authorizedHttpRequestWithTimeout( child,"/${id}/wake","GET",20,1, { resp ->
        data = transformWakeResponse(resp)
    })
     
    return data
}

private executeApiCommandMulti(Map options = [:], child, String command) {
    def result = false
   if (descLog) log.info "ExecuteApiCommandMulti"
    authorizedHttpRequestWithTimeout(child,"/${child}/${command}", "GET",20,1, { resp ->
        if (debug) log.debug "resp data = ${resp.data}"
       result = resp.data.results       
    })
    return result
}


private executeApiCommandForAddress(Map options = [:], child, String command) {
    def result = [:]
    
   if (descLog) log.info "executeApiCommandForAddress"
    
    authorizedHttpRequestWithTimeout(child,"/${child}/${command}", "GET",20,1, { resp ->
        if (debug) log.debug "resp data = ${resp.data}"
     
        result.address = "Unknown"
        result.status = false
        result.savedLocation = "N/A"
    
        if (resp.data.address)
        {
          result.address = resp.data.address
          result.status = true
           if (resp.data.saved_location)
             result.savedLocation = resp.data.saved_location  
        }
    })
    return result
}

 private executeApiCommandForWeather(Map options = [:], child, String command) {
    def result = [:]
    
   if (descLog) log.info "executeApiCommandForWeather"
    
    authorizedHttpRequestWithTimeout(child,"/${child}/${command}", "GET",20,1, { resp ->
        if (debug) log.debug "resp data = ${resp.data}"       
    
        if (resp.data.condition)
        {
          result.location = resp.data.location
          result.condition = resp.data.condition
          result.feels_like = celciusToFarenhiet(resp.data.feels_like)
          result.temperature = celciusToFarenhiet(resp.data.temperature)
          result.cloudiness = resp.data.cloudiness
          result.humidity = resp.data.humidity
          result.pressure = resp.data.pressure
          result.sunrise = resp.data.sunrise
          result.sunset = resp.data.sunset
          result.visibility = resp.data.visibility
          result.wind_direction = resp.data.wind_direction
          result.wind_speed = resp.data.wind_speed
          result.status = true
       }
       else
       {
          result.status = false
       }
    })
    return result
}

private executeApiCommand(Map options = [:], child, String command) {
    def result = false
    
   if (descLog) log.info "executeApiCommand"
    
    authorizedHttpRequestWithTimeout(child,"/${child}/${command}", "GET",20,1, { resp ->
        if (debug) log.debug "resp data = ${resp.data}"
      
        // special case status check
        if (resp.data.status)
          result = resp.data.status
       //else if (resp.data.address)
        //  result = resp.data.address
        else result = resp.data.result       
    })
    return result
}

private executeApiCommand(Map options = [:], String command) {
    def result = false
    
   if (descLog) log.info "executeApiCommand"
    
    authorizedHttpRequestWithTimeout(child,"/${command}", "GET",20,1, { resp ->
        if (debug) log.debug "resp data = ${resp.data}"
        result = resp.data.results       
    })
    return result
}

private executeApiCommandWithTimeout(Map options = [:], child, String command, Number timeout)
 {
  
    def result = false
    def tries = 1
     
   if (descLog) log.info "ExecuteApiCommandtWithTimeout $timeout"

        authorizedHttpRequestWithTimeout(child,"/${child}/${command}", "GET", timeout, tries, { resp ->
        if (debug) log.debug "resp data = ${resp.data}"
            
          // special case status check
        if (resp.data.status)
          result = resp.data.status
        else if (resp.data.address)
          result = resp.data.address
        else result = resp.data.result                      
        })        
       return result
}

def lock(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/lock")
}

def unlock(child) {
  if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/unlock")
}

def unlockandOpenChargePort(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
   return executeApiCommand(child, "command/open_charge_port")
}

def climateAuto(child) {
  if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/start_climate")
}

def climateOff(child) {
    if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/stop_climate")
}

def setThermostatSetpointC(child, Number setpoint) {
	def Double setpointCelcius = setpoint.toDouble()
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
    return executeApiCommand(child, "command/set_temperatures?temperature=${setpointCelcius}")
}


def setThermostatSetpointF(child, Number setpoint) {
	def Double setpointCelcius = farenhietToCelcius(setpoint).toDouble()
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
    log.debug "setting tesla temp to $setpointCelcius input = $setpoint"
    return executeApiCommand(child, "command/set_temperatures?temperature=${setpointCelcius}")
}

def startCharge(child) {
    if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/start_charging")
}

def stopCharge(child) {
  if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
   return executeApiCommand(child, "command/stop_charging")
}

def openOrCloseTrunk(child) {
  if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommandWithTimeout(child, "command/activate_rear_trunk",30)
}

def openFrunk(child) {
  if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommandWithTimeout(child, "command/activate_front_trunk", 30) 
}

def setSeatHeaters(child, seat,level) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
    return executeApiCommand(child, "command/set_seat_heat?seat=${seat}&level=${level}")
}

def setSeatCooling(child, seat,level) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
    return executeApiCommand(child, "command/set_seat_cool?seat=${seat}&level=${level}")
}

def sentryModeOn(child) {
    if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/enable_sentry")
}

def sentryModeOff(child) {
    if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/disable_sentry")
}

def ventWindows(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/vent_windows")
}

def closeWindows(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/close_windows") 
}

def setChargeLimit(child,limit) {
    def limitPercent = limit.toInteger()
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
    return executeApiCommand(child,"command/set_charge_limit?percent=${limitPercent}")
}

def setChargeAmps(child,amps) {
    def ampsInt = amps.toInteger()
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
    return executeApiCommand(child,"command/set_charging_amps?amps=${ampsInt}")
}

def valetModeOn(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/enable_valet")
}

def valetModeOff(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/disable_valet")
}
    
def steeringWheelHeatOn(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/start_steering_wheel_heater")
}

def steeringWheelHeatOff(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/stop_steering_wheel_heater")
}
   
def startDefrost(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/start_max_defrost")
}

def stopDefrost(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/stop_max_defrost")
}

def remoteStart(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/remote_start")
}

def ventSunroof(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/vent_sunroof")
}

def closeSunroof(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/close_sunroof")
}

def listDrivers(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommandMulti(child, "drivers")
}

def currentAddress(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommandForAddress(child, "location")
}

def getWeather(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }  
    return executeApiCommandForWeather(child, "weather")   
}

def getBatteryHealth(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("battery_health")
}


def notifyIfEnabled(message) {
    if (notificationDevice) {
        notificationDevice.deviceNotification(message)
    }
}

def sleepStatus(child) {
   if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
    
	return executeApiCommandWithTimeout(child, "status",20)
}


def currentVersion()
{
    return "2.20"
}

@Field static final Long oneHourMs = 1000*60*60
@Field static final Long oneDayMs = 1000*60*60*24
