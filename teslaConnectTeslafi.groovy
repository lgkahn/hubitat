
/**
 *  TeslaFi Connect
 *
 *  Copyright 2023 Larry Kahn kahn-hubitat
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
 * lgk V 1.0 initial beta release
 *
 * 
 *

 */

import groovy.transform.Field

definition(
    name: "TeslaFi Connect 1.0",
    namespace: "larrykahn",
    author: "Larry Kahn",
    description: "Integrate your Tesla car with TeslaFi.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%402x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%403x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%403x.png",
    singleInstance: true
)

preferences {

		section("Tesla Setup") {
	        input "debug", "bool", title: "Enable detailed debugging?", required: true, defaultValue: false
            input "descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true
            input "teslaFiToken", "string", title: "What is your TeslaFi Token ?", required: true
            input "teslaVehicleName", "string", title: "What do you call your Tesla (ie name) ?", required: true, defaultValue: "Tesla 1"
            input "pauseTime", "enum", title: "Time (in seconds) to automatically Wait/Pause after a wake before issuing the requested command.", required: true, defaultValue: "10", options:["2","3","4","5","6","7","8","9","10","15","20","30"]
            input "wakeOnInitialTry", "bool", title: "Should I issue a wake and pause on the inital try?", required: true, defaultValue: true                                                                                                                                      
        }    
        
		section("To use Tesla, Hubitat encrypts and securely stores a token.") {}
	}


def logsOff()
{
    log.info "Turning off Logging!"
    device.updateSetting("debug",[value:"false",type:"bool"])
}
   
def getChildNamespace() { "lgkahn" }
def getChildName() { "Tesla" }
def getTeslaFiServerUrl() { "https://www.teslafi.com" }
def getUserAgent() { "lgkahn" }


private convertEpochSecondsToDate(epoch) {
	return new Date((long)epoch * 1000);
}



private authorizedTeslaFiHttpRequestNoJson(Map options = [:], String path, String method, Closure closure) {
    
   if (debug) log.debug "in authorize teslaFi http req2"
    def attempt = options.attempt ?: 0
  
    if (descLog) log.info "authorizedTeslaFiHttpRequest ${method} ${path} attempt ${attempt}"
    
    try {
        
    def params = [
        uri: "https://www.teslafi.com/feed.php" + "${path}",
        headers: [
            'Accept': 'application/json, text/javascript, */*; q=0.01', // */ comment
            'DNT': '1',
            'Accept-Encoding': 'gzip,deflate,sdch',
            'Cache-Control': 'max-age=0',
            'Accept-Language': 'en-US,en,q=0.8',
            'Connection': 'keep-alive',
            'Host': 'www.teslafi.com',
            'X-Requested-With': 'XMLHttpRequest',
            'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
             Authorization: "Bearer ${teslaFiToken}"
            ]
    ]

     
    if (debug) log.debug "command = $params"
    def result = httpPost(params) { resp -> closure(resp) } 
    
          } catch (groovyx.net.http.HttpResponseException e) {
        	log.error "Request failed for path: ${path}.  ${e.response?.data}"  
        }
}       
 
private authorizedTeslaFiHttpRequest(Map options = [:], String path, String method, Closure closure) {
    
   if (debug) log.debug "in authorize teslaFi http req"
    def attempt = options.attempt ?: 0
  
    if (descLog) log.info "authorizedTeslaFiHttpRequest ${method} ${path} attempt ${attempt}"
    
    try {
        
    def params = [
        uri: "https://www.teslafi.com/feed.php" + "${path}",
        headers: [
            'Accept': 'application/json, text/javascript, */*; q=0.01', // */ comment
            'DNT': '1',
            'Accept-Encoding': 'gzip,deflate,sdch',
            'Cache-Control': 'max-age=0',
            'Accept-Language': 'en-US,en,q=0.8',
            'Connection': 'keep-alive',
            'Host': 'www.teslafi.com',
            'X-Requested-With': 'XMLHttpRequest',
            'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
             Authorization: "Bearer ${teslaFiToken}"
            ]
    ]

     
    if (debug) log.debug "command = $params"
    def result = httpPostJson(params) { resp -> closure(resp) } 
    
     } catch (groovyx.net.http.HttpResponseException e) {
        	log.error "Request failed for path: ${path}.  ${e.response?.data}"  
        }
}    
        
    /*
private authorizedHttpRequest(Map options = [:], String path, String method, Closure closure) {
   if (debug) log.debug "in authorize http req"
    def attempt = options.attempt ?: 0
   
    if (descLog) log.info "authorizedHttpRequest ${method} ${path} attempt ${attempt}"
    try {
              
    	def requestParameters = [
            uri: serverUrl,
            path: path,
            headers: [
                'User-Agent': userAgent,
                Authorization: "Bearer ${state.teslaAccessToken}"
            ]
        ]
    
     
    	if (method == "GET") {
            httpGet(requestParameters) { resp -> closure(resp) }
        } else if (method == "POST") {
        	if (options.body) {
            	requestParameters["body"] = options.body
                if (descLog) log.info "authorizedHttpRequest body: ${options}"
                httpPostJson(requestParameters) { resp -> closure(resp) }
            } else {
                 httpPostJson(requestParameters) { resp -> closure(resp) }
        		
            }
        } else {
        	log.error "Invalid method ${method}"
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.debug "in error handler case resp = $resp e= $e response data = e.response"
        if ((e.response?.data?.status?.code == 14) || (e.response?.data?.status?.code == 401))
           {
            log.debug "code - 14 or 401"
        	if (attempt < 3) {
                refreshAccessToken()
                options.attempt = ++attempt
                authorizedHttpRequestWithChild(child, options, path, method, closure )
            } else {
            	log.error "Failed after 3 attempts to perform request: ${path}"
            }
        } else {
        	log.error "Request failed for path: ${path}.  ${e.response?.data}"
            
           if (refreshAccessTokenURL)
            {
             refreshAccessTokenfromURL()
            }
            else refreshAccessToken()    

        }

    }
}
*/

private checkOrCreateAccountVehicle() {
    
   if (debug) 
    log.debug "in checkOrCreateAccountVehicles"
   
	if (descLog) 
    log.info "refreshAccountVehicle"

    // check for child car device
    
    def child = getChildDevice("TeslaVehicle1")
   if (descLog) log.info "child = $child"
    
    if (child == null)
    {
        log.info "No child/car exists ... Creating it!"
        def vehicleName = state.vehicleName
         device = addChildDevice("lgkahn", "TeslaVehicle", "TeslaVehicle1", null, [name:"Tesla 1", label:vehicleName])
         log.info "created device ${device.label}"
         device.initialize()
    }
         
    else {
        def device = getChildDevice("TeslaVehicle1")       
        log.info "Child Vehicle Device (${device.label}) Already Exists!"
    }
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
	log.info "deleting ${delete.size()} vehicles"
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def initialize() {
    if (descLog) log.info "in initialize"
    state.vehicleName = teslaVehicleName
    checkOrCreateAccountVehicle()
}


private transformVehicleResponseTeslaFiForCommand(resp) {
    
    def status = false
    
    if (debug)
    {
        log.debug "in transform teslafi for command"
        log.debug "data = ${resp.data}"
        log.debug "response = ${resp.data.response}"
        log.debug "error = ${resp.data.error}"
        log.debug "result = ${resp.data.response.result}"
        log.debug "reason = ${resp.data.response.reason}"
    }   
  
    // not weird results for commands it returns  status = false but resp.result = true when means it did work
    //[status:false, resp:[response:[reason:, result:true]], error:none, apiUsed:teslaFi]

    def errorString = "none"
    
  if (resp.data.response != null)
    {
    if (resp.data.response.result == "unauthorized")
    {
        log.error "Api failure: Error = ${resp.data.response.result}!"
        log.error "Please check your teslaFi Cookie!"
        errorString = resp.data.response.result
        status=false
    }
    
   else if (resp.data.response.result != null)
            {
                if (resp.data.response.result == true)
                {
                    status = true
                }
                else
                {
                    // status = false if we have it indicates error
                    status = false
                    errorString = resp.data.response.reason
                       return [
                       status : status,
                       resp: resp.data,
                       error : errorString,
                       apiUsed: "teslaFi"
                           ]
                }
            
            }
   
    }
            
   if (resp.data.response == null)
    {
      status = false
      errorString = resp.data.error
    }
    
    else status = true
    
    return [
        status : status,
        resp: resp.data,
        error : errorString,
        apiUsed: "teslaFi"
    ]

}

private transformVehicleResponseTeslaFiForWake(resp) {
    
    if (debug)
    {
    log.debug "in transform teslafi for wake" 
    log.debug "resp.data = ${resp.data}"
    log.debug "id = ${resp.data.response.id}"
    log.debug "data = ${resp.data}"
    log.debug "vin  = ${resp.data.response.vin}"
    log.debug "vid = ${resp.data.response.vehicle_id}"
    log.debug "state = ${resp.data.response.state}"
    log.debug "result = ${resp.data.response.result}"
    }
    
    def status = true
    def errorString = "none"
    
 if (resp.data.response != null)
    {
    if (resp.data.response.result == "unauthorized")
    { 
        log.error "Api failure: Error = ${resp.data.response.result}!"
        log.error "Please check your teslaFi Cookie!"
        errorString = resp.data.response.result
        status=false
    }
    }
    
   
    return [
        status: status,
        error: errorString,
        state: resp.data.response.state,
        vehicle_id: resp.data.response.vehicle_id,
        motion: "inactive",
        speed: 0,
        vin: resp.data.response.vin,
        thermostatMode: "off",
        apiUsed: "teslaFi"
    ]

}

private transformVehicleResponseTeslaFiForWakeInternal(resp) {
    
    if (debug)
    {
    log.debug "in transform teslafi for internal wake (start logging)" 
    log.debug "resp.data = ${resp.data}"
    }
    
    def status = true
    def errorString = "none"
    
 if (resp.data.response != null)
    {
   
    return [
        status: true,
        apiUsed: "teslaFi"
    ]

}
}

private transformVehicleResponseTeslaFi(resp) {
    if (debug)
    {    
    log.debug "in transform teslafi"
    log.debug "resp = ${resp}"
    log.debug "data = ${resp.data}"
    
    log.debug "vin  = ${resp.data.vin}"
    log.debug "data_id= ${resp.data.data_id}"
    log.debug "display name = ${resp.data.display_name}"
    log.debug "state = ${resp.data.state}"
    }
    
    def status = true
    def errorString = "none"
    
   if (resp.data.response != null)
    {
    if (resp.data.response.result == "unauthorized")
    {
        log.error "Api failure: Error = ${resp.data.response.result}!"
        log.error "Please check your teslaFi Cookie!"
        errorString = resp.data.response.result
        status=false
    }
    }
        
    return [
        status: status,
        error: errorString,
        result: resp.data,
        state: resp.data.state,
        vehicle_id: resp.data.vehicle_id,
        motion: "inactive",
        speed: 0,
        vin: resp.data.vin,
        thermostatMode: "off",
        apiUsed: "TeslaFi"
    ]
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

def refreshTeslaFi() {
   if (descLog) log.info "refresh TeslaFi"
    def data = [:]
    def datain = [:]
	//def id = child.device.deviceNetworkId
    authorizedTeslaFiHttpRequest( "?command=lastGood&wake=30", "GET", { resp ->
         datain = transformVehicleResponseTeslaFi(resp)
    })
    
  if (debug)
    {
    log.debug "vin = ${datain.result.vin}"
    log.debug "data_id= ${datain.result.data_id}"
    log.debug "display name = ${datain.result.display_name}"
    log.debug "data state = ${datain.result.state}"
    }
    
    // use display_id and display name to check for good data
  

    if (datain.result.state == "online") {
        
    // teslafi does not need another request all is in data
            
            data.speed = datain.result.speed ? datain.result.speed : 0
            data.motion = datain.result.speed > 0 ? "active" : "inactive"            
            data.thermostatMode = datain.result.is_climate_on.toInteger() ? "auto" : "off"
            data.apiUsed = datain.apiUsed
            data.softwareVersion = datain.result.car_version
            data.vin = datain.result.vin
            data.state = datain.result.carState
            
        
            data["chargeState"] = [
                battery: datain.result.battery_level,
                batteryRange: datain.result.battery_range.toFloat().toInteger(),
                chargingState: datain.result.charging_state,
                chargeLimit: datain.result.charge_limit_soc,
                chargeAmps: datain.result.charge_current_request,
                minutes_to_full_charge: datain.result.time_to_full_charge    
            ]
            
            data["driveState"] = [
            	latitude: datain.result.latitude,
                longitude: datain.result.longitude,
                //method: driveState.native_type,
                heading: datain.result.heading,
                lastUpdateTime: datain.result.Date
                //lastUpdateTime: convertEpochSecondsToDate(driveState.gps_as_of)
            ]
        
            data["vehicleState"] = [
            	presence: datain.result.homelink_nearby.toInteger() ? "present" : "not present",
                lock: datain.result.locked.toInteger() ? "locked" : "unlocked",
                odometer: datain.result.odometer.toFloat().toInteger(),
                sentry_mode: datain.result.sentry_mode.toInteger() ? "On" : "Off",
                front_drivers_window:  getWindowStatus(datain.result.fd_window),
                front_pass_window: getWindowStatus(datain.result.fp_window),
                rear_drivers_window: getWindowStatus(datain.result.rd_window),
                rear_pass_window: getWindowStatus(datain.result.rp_window),
                valet_mode: datain.result.valet_mode.toInteger() ? "On" : "Off",
                
               //no tire pressures in teslafi
                
                //tire_pressure_front_left: vehicleState.tpms_pressure_fl,
                //tire_pressure_front_right: vehicleState.tpms_pressure_fr,
                //tire_pressure_rear_left: vehicleState.tpms_pressure_rl,
                //tire_pressure_rear_right: vehicleState.tpms_pressure_rr,
                
                // no door info in teslafi
                
                //front_drivers_door: vehicleState.df ? "Open" : "Closed",
                //rear_drivers_door: vehicleState.dr ? "Open" : "Closed",
                //front_pass_door: vehicleState.pf ? "Open" : "Closed",
                //rear_pass_door: vehicleState.pr ? "Open" : "Closed",
                
                // no frunk trunk status in teslafi
                
                //frunk :  vehicleState.ft ? "Open" : "Closed",
                //trunk :  vehicleState.rt ? "Open" : "Closed",
                
                user_present: datain.result.is_user_present
                ]
             
            data["climateState"] = [
            	temperature: celciusToFarenhiet(datain.result.inside_temp.toFloat()),
                outside_temperature: celciusToFarenhiet(datain.result.outside_temp.toFloat()),
                thermostatSetpoint: celciusToFarenhiet(datain.result.driver_temp_setting.toFloat()),
                passengerSetpoint: celciusToFarenhiet(datain.result.passenger_temp_setting.toFloat()),
                seat_heater_left: datain.result.seat_heater_left,
                seat_heater_left: datain.result.seat_heater_left,
                seat_heater_right: datain.result.seat_heater_right, 
                seat_heater_rear_left: datain.result.seat_heater_rear_left,  
                seat_heater_rear_right: datain.result.seat_heater_rear_right,                
                seat_heater_rear_center: datain.result.seat_heater_rear_center    
            ]

        }
       
    return data
}

private celciusToFarenhiet(dC) {
    def fvalue  = dC * 9/5 +32
    return fvalue.toInteger()
	//return dC * 9/5 + 32
}

private farenhietToCelcius(dF) {
	return (dF - 32) * 5/9
}

def internalTeslaFiwake() {
      
    def data = [:] 
    
    if (descLog) log.info "TeslaFi internal wake aka start logging"
    
        authorizedTeslaFiHttpRequestNoJson("?command=wake","POST", { resp ->
            data = transformVehicleResponseTeslaFiForWakeInternal(resp)         
          
        })
    
    return data
}

def wakeTeslaFi() {
      
    def data = [:] 
    
    if (descLog) log.info "TeslaFi wake"
    
        authorizedTeslaFiHttpRequest("?command=wake_up","POST", { resp ->
            data = transformVehicleResponseTeslaFiForWake(resp)         
          
        })
    
    return data
}

private executeApiCommand(Map options = [:], String command) {
   
    def data = [:] 
    
    if (descLog) "In executeAPICommand command= $command"
    
          authorizedTeslaFiHttpRequest("?command=${command}","POST", { resp ->
          data = transformVehicleResponseTeslaFiForCommand(resp)       })  
        
    if (descLog) log.info "result = ${data}"
    
    if (data.status == false)
    {
        log.error "Api command failed: error = ${data.error}!"
    }
    
   return data.status
    
}

// starts teslafi logging/polling aka their wake command
// not to be confused with the actual car wake command.
// this is called automatically on morning wake if you have that feature enabled.
def startTeslaFiLogging()
{
  return internalTeslaFiwake()
}

def lock() {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("door_lock")
}

def unlock() {
  if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("door_unlock")
}

def unlockandOpenChargePort() {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
   return executeApiCommand("charge_port_door_open")
}

def climateAuto() {
  if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("auto_conditioning_start")
}

def climateOff() {
    if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
  
    return executeApiCommand("auto_conditioning_stop")
}

def setThermostatSetpointC(Number setpoint) {
	def Double setpointCelcius = setpoint.toDouble()
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
    return executeApiCommand("set_temps&temp=$setpoint") //, body: [driver_temp: setpointCelcius, passenger_temp: setpointCelcius])
}

def setThermostatSetpointF(Number setpoint) {
	def Double setpointCelcius = farenhietToCelcius(setpoint).toDouble()
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
    if (debug) log.debug "setting tesla temp to $setpointCelcius input = $setpoint"
    return executeApiCommand("set_temps&temp=$setpoint")//, body: [driver_temp: setpointCelcius, passenger_temp: setpointCelcius])
}

def startCharge() {
    if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("charge_start")
}

def stopCharge() {
  if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
   return executeApiCommand("charge_stop")
}

// not on teslafi
def openTrunk(whichTrunk) {
  if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("actuate_trunk", body: [which_trunk: whichTrunk])
}

def setSeatHeaters(seat,level) {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("seat_heater&heater=$seat&level=$level")
}

def sentryModeOn() {
    if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("set_sentry_mode&sentryMode=true")
}

def sentryModeOff() {
    if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("set_sentry_mode&sentryMode=false")
}

// not on teslafi
def ventWindows() {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("window_control", body: [command: "vent"])
}

// not on teslafi
def closeWindows() {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("window_control", body: [command: "close"]) 
}

def setChargeLimit(limit) {
    def limitPercent = limit.toInteger()
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
    return executeApiCommand("set_charge_limit&charge_limit_soc=$limitPercent")
}

def setChargeAmps(amps) {
    def ampsInt = amps.toInteger()
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }  
    return executeApiCommand("set_charging_amps&charging_amps=$ampsInt")
}

// not on teslafi
def valetModeOn() {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("set_valet_mode&on=true")
}

// not on teslafi
def valetModeOff() {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("set_valet_mode&on=false")
}

def steeringWheelHeaterOn() {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("steering_wheel_heater&statement=true")
}

def steeringWheelHeaterOff() {
   if (wakeOnInitialTry)
    { 
      wakeTeslaFi()
      pause((pauseTime.toInteger() * 1000))
    }
	return executeApiCommand("steering_wheel_heater&statement=false")
}

def notifyIfEnabled(message) {
    if (notificationDevice) {
        notificationDevice.deviceNotification(message)
    }
}


@Field static final Long oneHourMs = 1000*60*60
@Field static final Long oneDayMs = 1000*60*60*24

