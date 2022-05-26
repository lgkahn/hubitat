
/**
 *  Tesla Connect
 *
 *  Copyright 2018 Trent Foley
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * 10/18/20 lgkahn added open/unlock charge port
 * lgk added aption to put in new access token directly to get around login issues. Change4 to reset it to blank after use.
 * fix the updatesetting to clearsetting fx. 1/18/21
 *
 * lgk 3/2/21 new changes.. username password no longer used. Left in place if we ever get oauth2 working again.
 * added additional field, to get token from a web server in form of:
 *
 * {"access_token":"qts-689e5thisisatoken8","token_type":"bearer","expires_in":3888000,"refresh_token":"thisisarefreshtokenc0","created_at":1614466853}
 *
 * i use tesla.py to generate token monthly and push to my own webserver.
 * also added notification to tell you when token is refreshed via entered token or refresh url. Also notify of failures.
 *
 * 3/17/21 add set charge limit command and charge limit coming back from api in variable.
 *
 * lgk 10/19/21 change set temp setpoint to double precision to get around integer values being used in conversions.
 * 
 * bsr 4/15/22 - Add valet mode and tire pressures
 *
 * 5/16/22 thanks to darwin for help .. this is a new version of the app that using the new token logic.
 *
 * you still need to generate the initial token and refresh token.. recommend you create a separate login on tesla for it, as if
 * you use your teslafi or other app token that uses differing login (ie regenerates the normal token) they will step on each other
 * and it wont work.
 * also added new debug and desc log settings.
 * use tesla_auth.exe or something else to generate initial token pairs.
 * add notify of successfull refresh and schedule
 *
 * lgk may 22
 * integrate the local access point code to set token and add additional code to handle switching over from accesstoken (used by oauth)
 * to the new teslaAccessToken use the transittionAccessToken function immeidately after saving first the driver then the app to switch over.
 * Failure to do so will break existing installs and require removeal and reinstall.
 *
 * lgk add lastTokenRefresh and nextTokenRefresh attributes and also functions to pass to child vehicles.. 
 * also change wait time to be configuratble instead of the now default 2 secs between wake and command issuing. Also 
 * change the default to 10 secs as 2 seems not to work any longer.
 * also add child to the xx function so i can retry commands on an error 408 vehicle unavailable up to 3 times with an exponential backup of the pause time between commands.
 */

import groovy.transform.Field

definition(
    name: "Tesla Connect 3.0",
    namespace: "trentfoley",
    author: "Trent Foley, Larry Kahn",
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

mappings {
    path("/setToken") {
        action: [GET: "setEndpointToken"]
    }
}

def loginToTesla() {
	def showUninstall = email != null && password != null
	return dynamicPage(name: "VehicleAuth", title: "Connect your Tesla", nextPage:"selectVehicles", uninstall:showUninstall) {
		section("Token refresh options:") {
			input "email", "text", title: "Email (no longer used - enter anything!)", required: true, autoCorrect:false
			input "password", "text", title: "Password (no longer used - enter anything!)", required: true, autoCorrect:false
            input "newAccessToken", "string", title: "Input new access token when expired?", required: false
            input "newRefreshToken", "string", title: "Input new refresh token?", required: false
            input "refreshAccessTokenURL", "string", title: "URL (on your server) that holds new access token as generated from python script?", required: false
            input "allowEndpoint", "bool", title:"Activate Endpoint for updating token", required:false, submitOnUpdate:true
            input "notificationDevice", "capability.notification", title: "Notification device to receive info on Tesla Token Updates?", multiple: false, required: false
	        input "debug", "bool", title: "Enable detailed debugging?", required: true, defaultValue: false
            input "descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true
            input "pauseTime", "enum", title: "Time (in seconds) to automatically Wait/Pause after a wake before issuing the requested command.", required: true, defaultValue: "10", options:["2","3","4","5","6","7","8","9","10","15","20","30"]
                                                                                                                                                  
        }
           if (allowEndpoint){
                section("App Endpoint Information", , hideable: false, hidden: false){
                    if(state.accessToken == null) createAccessToken()
                    paragraph "<b>Local Server API:</b> ${getFullLocalApiServerUrl()}/setToken?teslaToken=[Tesla Token Value]&access_token=${state.accessToken}"
                    paragraph "<b>Cloud Server Endpoint: </b>${getFullApiServerUrl()}/setToken?teslaToken=[Tesla Token Value]&access_token=${state.accessToken}"

                    input "resetToken", "button", title:"Reset Endpoint Token"
                    input "disallow","button", title:"Remove Endpoint"
                }
        }  
        
		section("To use Tesla, Hubitat encrypts and securely stores a token.") {}
	}
}

def logsOff()
{
    log.debug "Turning off Logging!"
    device.updateSetting("debug",[value:"false",type:"bool"])
}

def transitionAccessToken(child)
{
    log.debug "Initializing tesla access token for switch over."
    state.teslaAccessToken = state.accessToken
    return true;
}  
    
def selectVehicles() 
{
    if (debug) log.debug "In select vehicles"
 
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

def getChildNamespace() { "trentfoley" }
def getChildName() { "Tesla" }
def getServerUrl() { "https://owner-api.teslamotors.com" }
def getClientId () { "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384" }
def getClientSecret () { "c7257eb71a564034f9419ee651c7d0e5f7aa6bfbd18bafb5c5c033b093bb2fa3" }
def getUserAgent() { "trentacular" }

def getAccessToken() {
    
    if (descLog) log.info "in get access token"
    
    if (debug)
    {
        log.debug "newaccess token - $newAccessToken"
        log.debug "old token = $state.teslaAccessToken"
        log.debug "last input token = $state.lastInputAccessToken"   
    }
    
    if (newAccessToken)
    {
        if (descLog) log.debug "Resetting access token."
        state.teslaAccessToken = newAccessToken
        app.clearSetting("newAccessToken")
        notifyIfEnabled("Tesla App - Succesfully refreshed token from entry!")
        if (descLog) "Tesla App - Succesfully refreshed token from entry!"
          
    }
    else
    {
         if ((!state.teslaAccessToken) && (refreshAccessTokenURL))
        {
           if (descLog) log.debug "Attempting to get access token from url!"
            refreshAccessTokenfromURL()
        }  
        else if (!state.teslaAccessToken)
        {
            if (descLog) log.debug "Attempting to get new access token from refresh token!"
            refreshAccessToken()
        }
    }
   // refreshAccessToken() for testing
	state.teslaAccessToken
}

private convertEpochSecondsToDate(epoch) {
	return new Date((long)epoch * 1000);
}

def refreshAccessTokenfromURL() {
    
        def params = [
        uri: refreshAccessTokenURL,
        headers: [
            'Accept': '*/*', // */ comment
            'cache': 'false',
            'Accept-Encoding': 'plain',
            'Accept-Language': 'en-US,en,q=0.8',
            'Connection': 'keep-alive',
            'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36'
        ],
	  timeout: 2
    ]
    if (debug) log.debug "sending refresh request: $params"
    try {
          httpPostJson(params) { resp ->  
             if (resp.status == 200)
              {
                  if (debug) log.debug "got data = $resp.data status $resp.status"
                  notifyIfEnabled("Tesla App - Succesfully refreshed token from URL")
                  if (descLog) log.debug"Tesla App - Successfully refreshed token from URL"
                      
                  state.teslaAccessToken = resp.data.access_token
                  state.refreshToken = resp.data.refresh_token
              }
            else
            {
                 state.teslaAccessToken = null  
                 notifyIfEnabled("Tesla App - Refresh token from URL Failed bad response!")  
                 if (debug) log.debug "Tesla App - Refresh token from URL Failed bad response!"
            }
        }
 }
    
    catch (groovyx.net.http.HttpResponseException e) {
            	log.warn e
                if (debug) 
                {
                  notifyIfEnabled("Tesla App - Refresh token from URL Failed ($e)!")
                  log.debug "Tesla App - Refresh token from URL Failed ($e)!"
                }
        
                state.teslaAccessToken = null
                if (e.response?.data?.status?.code == 14) {
                    state.refreshToken = null
                }
    }
         
    }

private authorizedHttpRequestWithChild(child, Integer attempts, Map options = [:], String path, String method, Closure closure) {
    if (debug) log.debug "in authorize http req with child child=$child attempts=$attempts"
    
    if (attempts > 1) log.debug "Attempt: $attempts"
 
    if (descLog) log.info "authorizedHttpRequest2 ${child} ${method} ${path} attempt ${attempts}"
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
                if (descLog) log.info "authorizedHttpRequest2 body: ${options.body}"
                httpPostJson(requestParameters) { resp -> closure(resp) }
            } else {
        		httpPost(requestParameters) { resp -> closure(resp) }
            }
        } else {
        	log.error "Invalid method ${method}"
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        if (descLog) log.debug "in error handler case error = $e response data = e.response data = ${e.response.data}"
        def errorString = e.response?.data?.error
        boolean foundVehicleUnavailable = false
  
        if (errorString)
        {
           if (debug) log.debug "got errorString = $errorString"
           def pair = errorString.split(" ")
           def p0 = pair[0]
           def p1 = pair[1]
          
           if ((p0 == "vehicle") && (p1 == "unavailable:"))
            {
                 if (debug) log.debug "set unavaiable to true "
                 foundVehicleUnavailable = true
            }
        }
          
        if ((e.response?.data?.status?.code == 14) || (e.response?.data?.status?.code == 401))
           {
            log.debug "code - 14 or 401"
        	if (attempts < 4) {
                refreshAccessToken()
                pause((pauseTime.toInteger() * 1000))
                ++attempts
                authorizedHttpRequestWithChild(child, attempts, options, path, method, closure)
            } else {
            	log.error "Failed after 3 attempts to perform request: ${path}"
            }
           }
        
          else if (foundVehicleUnavailable)
          {
              if (descLog) log.debug "Vehicle Unavailable"
              // 408 is vehicle unavailable ie offline or asleep. 
              if (attempts < 4)
              {
               long localPauseTime = (pauseTime.toInteger() * (attempts + 1)) 
                                      
               log.info "Got Back vehicle unavailable (408) ... Either disconnected or most likely asleep."
               log.info "Will Issue a Wake command, wait and retry."
               
               if (descLog) log.info "Pausing for $localPauseTime seconds between wake and command retry."
                  
               pause(localPauseTime * 1000)
               wake(child)
               pause(localPauseTime * 1000)
               ++attempts;
               authorizedHttpRequestWithChild(child, attempts, options, path, method, closure)
                  
              }
              else {
                 log.error "Failed after 3 attempts (vehicle was still unavailable) to perform request: ${path}" 
              }
          }
          else {
        	log.error "Request failed for path: ${path}.  ${e.response?.data}"
         
           if (refreshAccessTokenURL)
            {
             refreshAccessTokenfromURL()
            }
            else refreshAccessToken() 

        }
    }
}

private authorizedHttpRequest(Map options = [:], String path, String method, Closure closure) {
   if(debug) log.debug "in authorize http req"
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
                if (descLog) log.info "authorizedHttpRequest body: ${options.body}"
                httpPostJson(requestParameters) { resp -> closure(resp) }
            } else {
        		httpPost(requestParameters) { resp -> closure(resp) }
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

private refreshAccountVehicles() {
   if (debug) log.debug "in refreshAccountVehicles. current token = $state.teslaAccessToken"
   
	if (descLog) log.info "refreshAccountVehicles"

	state.accountVehicles = [:]

	authorizedHttpRequest("/api/1/vehicles", "GET", { resp ->
    	if (descLog) log.info "Found ${resp.data.response.size()} vehicles"
        resp.data.response.each { vehicle ->
        	if (descLog) log.info "${vehicle.id}: ${vehicle.display_name}"
        	state.accountVehicles[vehicle.id] = vehicle.display_name
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
                device = addChildDevice("trentfoley", "Tesla", dni, null, [name:"Tesla ${dni}", label:vehicleName])
                log.debug "created device ${device.label} with id ${dni}"
                device.initialize()
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

private transformVehicleResponse(resp) {
	return [
        state: resp.data.response.state,
        motion: "inactive",
        speed: 0,
        vin: resp.data.response.vin,
        thermostatMode: "off"
    ]
}

def refresh(child) {
   if (descLog) log.info "in refresh child"
    def data = [:]
	def id = child.device.deviceNetworkId
    authorizedHttpRequestWithChild(child,1,"/api/1/vehicles/${id}", "GET", { resp ->
        data = transformVehicleResponse(resp)
    })
    
    if (data.state == "online") {
    	authorizedHttpRequestWithChild(child,1,"/api/1/vehicles/${id}/vehicle_data", "GET", { resp ->
            def driveState = resp.data.response.drive_state
            def chargeState = resp.data.response.charge_state
            def vehicleState = resp.data.response.vehicle_state
            def climateState = resp.data.response.climate_state
            
            data.speed = driveState.speed ? driveState.speed : 0
            data.motion = data.speed > 0 ? "active" : "inactive"            
            data.thermostatMode = climateState.is_climate_on ? "auto" : "off"
            
           
            data["chargeState"] = [
                battery: chargeState.battery_level,
                batteryRange: chargeState.battery_range,
                chargingState: chargeState.charging_state,
                chargeLimit: chargeState.charge_limit_soc,
                minutes_to_full_charge: chargeState.minutes_to_full_charge    
            ]
            
            data["driveState"] = [
            	latitude: driveState.latitude,
                longitude: driveState.longitude,
                method: driveState.native_type,
                heading: driveState.heading,
                lastUpdateTime: convertEpochSecondsToDate(driveState.gps_as_of)
            ]
            
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
                tire_pressure_rear_right: vehicleState.tpms_pressure_rr
                ]
             
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
        })
    }
    
    return data
}

private celciusToFarenhiet(dC) {
	return dC * 9/5 + 32
}

private farenhietToCelcius(dF) {
	return (dF - 32) * 5/9
}

def wake(child) {
       
	def id = child.device.deviceNetworkId
    def data = [:]
    authorizedHttpRequest("/api/1/vehicles/${id}/wake_up", "POST", { resp ->
        data = transformVehicleResponse(resp)
    })
    return data
}

private executeApiCommand(Map options = [:], child, String command) {
    def result = false
    authorizedHttpRequestWithChild(child,1,options, "/api/1/vehicles/${child.device.deviceNetworkId}/command/${command}", "POST", { resp ->
       result = resp.data.result
    })
    return result
}

def lock(child) {
    //wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "door_lock")
}

def unlock(child) {
    wake(child)
  //  if (debug) log.debug "pausetime = $pauseTime modified = ${pauseTime.toInteger() * 1000}"
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "door_unlock")
}

def unlockandOpenChargePort(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
return executeApiCommand(child, "charge_port_door_open")
}

def climateAuto(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "auto_conditioning_start")
}

def climateOff(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "auto_conditioning_stop")
}

def setThermostatSetpointC(child, Number setpoint) {
	def Double setpointCelcius = setpoint.toDouble()
    wake(child)
    pause((pauseTime.toInteger() * 1000))
    return executeApiCommand(child, "set_temps", body: [driver_temp: setpointCelcius, passenger_temp: setpointCelcius])
}


def setThermostatSetpointF(child, Number setpoint) {
	def Double setpointCelcius = farenhietToCelcius(setpoint).toDouble()
    wake(child)
    pause((pauseTime.toInteger() * 1000))
    log.debug "setting tesla temp to $setpointCelcius input = $setpoint"
    return executeApiCommand(child, "set_temps", body: [driver_temp: setpointCelcius, passenger_temp: setpointCelcius])
}

def startCharge(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "charge_start")
}

def stopCharge(child) {
   wake(child)
   pause((pauseTime.toInteger() * 1000))
   return executeApiCommand(child, "charge_stop")
}

def openTrunk(child, whichTrunk) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "actuate_trunk", body: [which_trunk: whichTrunk])
}

def setSeatHeaters(child, seat,level) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "remote_seat_heater_request", body: [heater: seat, level: level])
}

def sentryModeOn(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "set_sentry_mode", body: [on: "true"])
}

def sentryModeOff(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "set_sentry_mode", body: [on: "false"])
}

def ventWindows(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "window_control", body: [command: "vent"])
}

def closeWindows(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "window_control", body: [command: "close"])
}

def setChargeLimit(child,limit) {
    def limitPercent = limit.toInteger()
    wake(child)
    pause((pauseTime.toInteger() * 1000))
    return executeApiCommand(child,"set_charge_limit", body: [percent: limitPercent])
}

def notifyIfEnabled(message) {
    if (notificationDevice) {
        notificationDevice.deviceNotification(message)
    }
}

/* this is no longer working as well
def refreshAccessTokenold() {
	if (debug) log.debug "refreshAccessToken"
	try {
        if (state.refreshToken) {
        	log.debug "Found refresh token so attempting an oAuth refresh"
            try {
                httpPostJson([
                    uri: serverUrl,
                    path: "/oauth/token",
                    headers: [ 'User-Agent': userAgent ],
                    body: [
                        grant_type: "refresh_token",
                        client_id: clientId,
                        client_secret: clientSecret,
                        refresh_token: state.refreshToken
                    ]
                ]) { resp ->
                    state.teslaAccessToken = resp.data.access_token
                    state.refreshToken = resp.data.refresh_token
                }
            } catch (groovyx.net.http.HttpResponseException e) {
            	log.warn e
                state.teslaAccessToken = null
                if (e.response?.data?.status?.code == 14) {
                    state.refreshToken = null
                }
            }
        }
*/
         
         // login from username password deprecated lgk
        
       /* if (!state.teslaAccessToken) {
        	log.debug "Attemtping to get access token using password" 
            httpPostJson([
                uri: serverUrl,
                path: "/oauth/token",
                headers: [ 'User-Agent': userAgent ],
                body: [
                    grant_type: "password",
                    client_id: clientId,
                    client_secret: clientSecret,
                    email: email,
                    password: password
                ]
            ]) { resp ->
            	log.debug "Received access token that will expire on ${convertEpochSecondsToDate(resp.data.created_at + resp.data.expires_in)}"
                state.teslaAccessToken = resp.data.access_token
                state.refreshToken = resp.data.refresh_token
            }
        }
*/
/*
    } catch (Exception e) {
        log.error "Unhandled exception in refreshAccessToken: $e response = $resp"
    }

        // above is deprecated for now get using url
       if ((!state.teslaAccessToken) && (refreshAccessTokenURL))
        {
            log.debug "Attempting to get access token from url!"
            refreshAccessTokenfromURL()
        }    
        
        
}
*/

void scheduleRefreshAccessToken() {
   if (descLog) log.info "in schedule refresh"
    if (state.tokenExpiration) {
        Long refreshDateEpoch = state.tokenExpiration - oneHourMs*2.5 // 2.5 hours before expiration
        //Min 1 hour, max 30 days
        if (refreshDateEpoch - now() < oneHourMs) {
            refreshDateEpoch = now() + oneHourMs
        } else if (refreshDateEpoch - now() > oneDayMs*30) {
            refreshDateEpoch = now() + oneDayMs*30
        }
        def refreshDate = new Date(refreshDateEpoch)
        if (descLog) log.info "Scheduling token refresh for ${refreshDate}."
         notifyIfEnabled("Tesla App - Succesfully scheduled next refresh for ${refreshDate}")
        setNextTokenUpdateTimeForVehicles(refreshDate)
        
        runOnce(refreshDate, refreshAccessToken) 
        state.scheduleRefreshToken = false
        state.refreshSchedTime = refreshDateEpoch
    } else {
        log.warn "Unable to schedule refresh token. No expiration date found!"        
    }
}
    
void acceptAccessToken (String token, Long expiresIn) {
    if (descLog) log.info "in accept access token"
    app.updateSetting("newAccessToken",[type:"text",value:token])
    settings.newAccessToken = token //ST workaround for immediate setting within dynamic page
    state.teslaAccessToken = token
    state.tokenExpiration = now() + expiresIn * 1000
    def refreshDate = new Date(state.tokenExpiration)
    if (descLog) log.info "Token expires on ${refreshDate}."
    state.scheduleRefreshToken = true  
    state.refreshTokenSuccess = true
    getTokenDateString() //Reset access token date status
    
   if (state.scheduleRefreshToken && state.tokenExpiration) {
            scheduleRefreshAccessToken()
        } 
}
  
void refreshAccessToken(){
    if (descLog) log.debug "in refresh access token"
    if (settings.newRefreshToken && settings.newRefreshToken != ""){
        String currentRefreshToken = settings.newRefreshToken
        String ssoAccessToken = ""
        Long expiresIn
        state.refreshTokenSuccess = false
        Map payload = ["grant_type":teslaBearerTokenGrantType,"refresh_token":currentRefreshToken, "client_id":teslaBearerTokenClientId, "scope":teslaBearerTokenScope]
        try{
            log.info "Getting updated refresh token and bearer token for access token"
            log.info "Calling ${teslaBearerTokenEndpoint} with ${payload}"
            httpPostJson([uri: teslaBearerTokenEndpoint, body: payload]){ resp ->
                Integer statusCode = resp.getStatus()
                if (statusCode == 200) {
                   // log.info "Bearer access request data: ${resp.data}"
                    app.updateSetting("newRefreshToken",[type:"text",value:resp.data["refresh_token"]])
                    ssoAccessToken = resp.data["access_token"]
                    expiresIn = resp.data.expires_in.toLong()
                    if (descLog) log.info "Successfully updated refresh/bearer token for access token!"
                       notifyIfEnabled("Tesla App - Succesfully refreshed refresh/bearer token for access token!")
                    setLastTokenUpdateTimeForVehicles()
 
                } 
                else {
                    log.warn "Unable to update refresh token and bearer token for access token. Status code: ${statusCode}"
                    if (now() < state.tokenExpiration) {
                        state.scheduleRefreshToken = true //Still time - try again later
                    }  
                }
            }
        }
        catch (Exception e){
            log.warn "Error getting Tesla server bearer token from refresh token: ${e}"
            if (now() < state.tokenExpiration) {
                state.scheduleRefreshToken = true //Still time - try again later
            }
        }
    
        // this no longer works
        
       if (descLog) log.info "Getting updated access token and expiry"
        Map ownerPayload = ["grant_type":teslaAccessTokenAuthGrantType, "client_id":teslaAccessTokenAuthClientId]
        Map ownerApiHeaders = ["Authorization": "Bearer " + ssoAccessToken]
        try{
            httpPostJson([uri: teslaAccessTokenEndpoint, headers: ownerApiHeaders, body: ownerPayload]){
                resp ->
                Integer statusCode = resp.getStatus()
                if (statusCode == 200){
                    if (descLog) log.info "Access Token access request data: ${resp.data}"
                    acceptAccessToken (resp.data["access_token"], resp.data.expires_in.toLong())
                }
                else {
                    log.error "Unable to update access token. Status code: ${statusCode}"
                }
            }
        }
        catch (Exception e){
            if (debug) log.info "Issue getting Tesla server access token from bearer refresh token: ${e} (this is normal for now)"
            //Use the sso token as is:
            if (ssoAccessToken && expiresIn) {
                acceptAccessToken (ssoAccessToken, expiresIn)
            }
        }

        
    }
}

String getTokenDateString() {
    if (newAccessToken != state.lastInputAccessToken) {
        state.lastInputAccessToken = newAccessToken
        state.tokenChangeTime = now()
        state.tokenAgeWarnSent = false
    }
    String msg = ""
    if (state.tokenChangeTime) {
        //msg = "\nToken last updated ${((now()-state.tokenChangeTime)/1000/60/60/24).toInteger()} days ago."
        def dateStr = new Date(state.tokenChangeTime)
        msg = "\nToken updated: ${dateStr}."                
        if (state.tokenExpiration) {
            dateStr = new Date(state.tokenExpiration)
            msg = msg + "\nExpires: ${dateStr}."
            if (!state.scheduleRefreshToken && state.refreshSchedTime && now() < state.refreshSchedTime) {
                dateStr = new Date(state.refreshSchedTime)
                msg = msg + "\nRefresh scheduled: ${dateStr}."
            } else if (state.scheduleRefreshToken) {
                msg = msg + "\nToken Refresh pending."
            } else if (state.tokenExpiration && now() > state.tokenExpiration) {
                msg = msg + "\nExpired."
            }
        }
    }
    return msg
}

def setEndpointToken(){
    if(allowEndpoint) {
    	log.debug "Resetting access token from endpoint"
        //log.debug "${params.teslaToken}"
        newToken = params.teslaToken   

	    state.teslaAccessToken = newToken
        jsonText = "{\"status\": \"acknowledged\"}"
        render contentType:'application/json', data: "$jsonText", status:200
    }else 
        render contentType:'application/json', data: "{\"status\": \"denied\"}", status:404
}

def valetModeOn(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "set_valet_mode", body: [on: "true"])
}

def valetModeOff(child) {
    wake(child)
    pause((pauseTime.toInteger() * 1000))
	return executeApiCommand(child, "set_valet_mode", body: [on: "false"])
}

void appButtonHandler(btn) {
    switch(btn) {
          case "resetToken":
              createAccessToken()
              break
          case "disallow":
              app.updateSetting("allowEndpoint",[value:"false",type:"bool"])
              createAccessToken() //resetting this revokes all previous
              break
          default: 
              if(debugEnabled) log.error "Undefined button $btn pushed"
              break
      }
}

def scheduleTokenRefresh(child) {
  if (descLog) log.info "In force reschedule of refresh token!"
  refreshAccessToken()
  pause(10000)
  if (state.scheduleRefreshToken)
    return true
    else return false
}

def setLastTokenUpdateTimeForVehicles()
{   
  def children = getChildDevices()

	if (debug) log.debug "In send update time to children" 
    children.each {
         def oneChild = getChildDevice(it.deviceNetworkId)
         if (oneChild) oneChild.setLastokenUpdateTime()
    }
}

def setNextTokenUpdateTimeForVehicles(nextTime)
{   
  def children = getChildDevices()

	if (debug) log.debug "In send next update time to children" 
    children.each {
         def oneChild = getChildDevice(it.deviceNetworkId)
         if (oneChild) oneChild.setNextTokenUpdateTime(nextTime)
    }
}


@Field static final Long oneHourMs = 1000*60*60
@Field static final Long oneDayMs = 1000*60*60*24

@Field static final String teslaBearerTokenEndpoint = "https://auth.tesla.com/oauth2/v3/token"
@Field static final String teslaBearerTokenGrantType = "refresh_token"
@Field static final String teslaBearerTokenClientId = "ownerapi"
@Field static final String teslaBearerTokenScope = "openid email offline_access"
@Field static final String teslaAccessTokenEndpoint = "https://owner-api.teslamotors.com/oauth/token"
@Field static final String teslaAccessTokenAuthGrantType = "urn:ietf:params:oauth:grant-type:jwt-bearer"
@Field static final String teslaAccessTokenAuthClientId = "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384"
