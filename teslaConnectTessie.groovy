
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
 */

import groovy.transform.Field

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
            input "pauseTime", "enum", title: "Time (in seconds) to automatically Wait/Pause after a wake before issuing the requested command.", required: true, defaultValue: "10", options:["2","3","4","5","6","7","8","9","10","15","20","30"]
            input "wakeOnInitialTry", "bool", title: "Should I issue a wake and pause on the inital try?", required: true, defaultValue: true                                                                                                                                      
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
 //   def attempt = options.attempt ?: 0
   
    if (descLog) log.info "authorizedHttpVehicleRequest ${method} ${path}"
    if (debug)
    {
    log.debug "token = ${state.tessieAccessToken}"
    log.debug "server url = $serverUrl"
    log.debug "path = $path"
    log.debug "method = $method"
    }
   
    try {
              
    	def requestParameters = [
            uri: serverUrl + path,
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
    } catch (groovyx.net.http.HttpResponseException e) {
        log.debug "in error handler case resp = $resp e= $e response data = e.response"
        if ((e.response?.data?.status?.code == 14) || (e.response?.data?.status?.code == 401))
           {
            log.debug "code - 14 or 401"
        } else {
        	log.error "Request failed for path: ${path}.  ${e.response?.data}"
           
        }

    }
}

private authorizedHttpRequest(String child, String path, String method, Closure closure) {
   if (debug) log.debug "in authorize http req"
   
    if (descLog) log.info "authorizedHttpRequest ${method} ${path}"
    if (debug)
    {
    log.debug "token = ${state.tessieAccessToken}"
    log.debug "server url = $serverUrl"
    log.debug "path = $path"
    log.debug "method = $method"
    }
   
    try {
              
    	def requestParameters = [
            uri: serverUrl + path,
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
    } catch (groovyx.net.http.HttpResponseException e) {
        log.debug "in error handler case resp = $resp e= $e response data = e.response"
        if ((e.response?.data?.status?.code == 14) || (e.response?.data?.status?.code == 401))
           {
            log.debug "code - 14 or 401"
      
        } else {
        	log.error "Request failed for path: ${path}.  ${e.response?.data}"
            
        }

    }
}

private refreshAccountVehicles() {
   if (debug) log.debug "in refreshAccountVehicles. current token = $state.tessieAccessToken"
   
	if (descLog) log.info "refreshAccountVehicles"

	state.accountVehicles = [:]

	authorizedHttpVehicleRequest("/vehicles", "GET", { resp ->
      
        if (debug)
        {
            log.debug "result = $resp"
            log.debug "result = ${resp.data}"
            log.debug "one result = ${resp.data.results}"
        }
        
    	if (descLog) log.info "Found ${resp.data.results.size()} vehicles"
        resp.data.results.each { vehicle ->
            
        if (debug)
            {
                log.debug " found the vehicle = $vehicle"
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
            if (vin != null)
             if (vin != "")
               state.currentVIN = vin
            
           else state.accountVehicles[id] = "Tesla ${id}"
        }
    })
}


def installed() {
	log.info "Installed"
	initialize()
}

def updated() {
	log.info "Updated"
    

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
    if (descLog) log.info "in initialize"
    ensureDevicesForSelectedVehicles()
    removeNoLongerSelectedChildDevices()
}

private ensureDevicesForSelectedVehicles() {
	if (selectedVehicles) {
        selectedVehicles.each { dni ->
            def d = getChildDevice(dni)
            if(!d) {
                def vehicleName = state.accountVehicles[dni]
                def vin = state.currentVIN
                device = addChildDevice("lgkahn", "tessieVehicle", dni, null, [name:"Tesla ${dni}", label:vehicleName])
                log.debug "created device ${device.label} with id ${dni} vin = ${vin}"
                device.initialize()
                device.sendEvent(name:"vin" , value:vin)
            } else {
                log.info "device for ${d.label} with id ${dni} already exists"
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

    	authorizedHttpRequest(child,"/${id}/state", "GET", { resp ->
            
         if (debug) log.debug "in tessie refresh data = ${resp.data}"
            
             if (resp.data.state == "online") { 
           
            def driveState = resp.data.drive_state
            def chargeState = resp.data.charge_state
            def vehicleState = resp.data.vehicle_state
            def climateState = resp.data.climate_state
            
            data.state = resp.data.state
            data.vin = resp.data.vin
            data.speed = driveState.speed ? driveState.speed : 0
            data.motion = data.speed > 0 ? "active" : "inactive"            
            data.thermostatMode = climateState.is_climate_on ? "auto" : "off"
            
           if (debug) log.debug "charging state = $chargeState"
            data["chargeState"] = [
                battery: chargeState.battery_level,
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
            
            if (debug) log.debug "vehicle state = $vehicleState"
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
                user_present: vehicleState.is_user_present,
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
    def fvalue  = dC * 9/5 +32
    return fvalue.toInteger()
	//return dC * 9/5 + 32
}

private farenhietToCelcius(dF) {
	return (dF - 32) * 5/9
}

def wake(child) {
    
    log.debug "in wake child = $child"
    
    def id = child
    def data = [:] 
    
     def children = getChildDevices()
 
    authorizedHttpRequest( child,"/${id}/wake", "GET", { resp ->
        data = transformWakeResponse(resp)
    })
     
    return data
}

private executeApiCommandMulti(Map options = [:], child, String command) {
    def result = false
   if (descLog) log.debug "in execute api command Multi"
    authorizedHttpRequest(child,"/${child}/${command}", "GET", { resp ->
        if (debug) log.debug "resp data = ${resp.data}"
       result = resp.data.results       
    })
    return result
}


private executeApiCommand(Map options = [:], child, String command) {
    def result = false
   if (descLog) log.debug "in execute api command"
    authorizedHttpRequest(child,"/${child}/${command}", "GET", { resp ->
        if (debug) log.debug "resp data = ${resp.data}"
       result = resp.data.result       
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
    return executeApiCommand(child, "command/set_temperature?temperature=${setpointCelcius}")
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

def openTrunk(child, whichTrunk) {
  if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/actuate_rear_trunk")
}

def openFrunk(child, whichTrunk) {
  if (wakeOnInitialTry)
    { 
      wake(child)
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand(child, "command/actuate_front_trunk")
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
    return executeApiCommand(child,"command/set_charging_amps?charging_amps=${ampsInt}")
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

def notifyIfEnabled(message) {
    if (notificationDevice) {
        notificationDevice.deviceNotification(message)
    }
}

@Field static final Long oneHourMs = 1000*60*60
@Field static final Long oneDayMs = 1000*60*60*24