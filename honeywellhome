/*
Hubitat Driver For Honeywell Thermistate

Copyright 2020 - Taylor Brown

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Major Releases:
11-25-2020 :  Initial 
11-27-2020 :  Alpha Release (0.1)
12-15-2020 :  Beta 1 Release (0.2.0)
4/23 lgk fix supportedThermostatModes and supportedThermostatFanModes so dashboards work again for setting these.
 3/24 lgk add code to handle and retry after communication failures


Considerable inspiration an example to: https://github.com/dkilgore90/google-sdm-api/
*/


import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field
import java.net.SocketTimeoutException

@Field static String global_apiURL = "https://api.honeywell.com"
@Field static String global_redirectURL = "https://cloud.hubitat.com/oauth/stateredirect"

definition(
        name: "Honeywell Home",
        namespace: "thecloudtaylor",
        author: "Taylor Brown",
        description: "App for Lyric (LCC) and T series (TCC) Honeywell Thermostats, requires corisponding driver.",
        importUrl:"https://raw.githubusercontent.com/thecloudtaylor/hubitat-honeywell/main/honeywellhomeapp.groovy",
        category: "Thermostate",
        iconUrl: "",
        iconX2Url: "")

preferences 
{
    page(name: "mainPage")
    page(name: "debugPage", title: "Debug Options", install: false)
    page(name: "loginPage", title: "Login Options", install: true)
    page(name: "connectToHoneywell")
}

mappings {
    path("/handleAuth") {
        action: [
            GET: "handleAuthRedirect"
        ]
    }
}


def mainPage() {
    dynamicPage(name: "mainPage", title: "Honeywell Home", install: true, uninstall: true) {
        installCheck()
        if(state.appInstalled == 'COMPLETE')
        {   
            section {
                paragraph "Establish connection to Honeywell Home and Discover devices"
            }
            getDiscoverButton()
            listDiscoveredDevices()

            section {
                href(
                    name       : 'loginHref',
                    title      : 'Login Options',
                    page       : 'loginPage',
                    description: 'Access Login Options'
                )
            }            
            section {
                input name: "debugOutput", type: "bool", title: "Enable Debug Logging?", defaultValue: false, submitOnChange: true
            }
            getDebugLink()
        }      
    }
}

def installCheck()
{
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
		section{paragraph "Please hit 'Done' to install '${app.label}' app "}
  	}
  	else{
    	LogInfo("Parent Installed OK")
  	}
}



def loginPage() 
{
    dynamicPage(name:"loginPage", title: "Honeywell Auth Configuration", install: false, uninstall: false) {
        section {
            paragraph "The default key's below are shared and typically hit rate limits."
            paragraph "Please create and input your own Consumer and API Key From https://developer.honeywellhome.com/"  
                    
            paragraph """Signup is free, once you have a username/password login and then:
                1) Navigate to "My Apps"
                2) Click "Create New App"
                3) Provide an "App Name" can be anything (I use Hubitat)
                4) Input "https://cloud.hubitat.com/oauth/stateredirect" (without quotes) as the callback.
                5) The App will be created, click on it to expand the box and then copy over the Consumer Key and Secret below. 
            """

        }
        section {
            input name: "consumerKey", type: "text", title: "Consumer Key", description: "From https://developer.honeywellhome.com/", required: true, defaultValue: "DEb39Y2eKMrv3fGpoKudWvLOZ9LDey6N", submitOnChange: true
            input name: "consumerSecret", type: "text", title: "Consumer Secret", description: "From https://developer.honeywellhome.com/", required: true, defaultValue: "hGyrQFX5TU4frGG5", submitOnChange: true
            href name: "connectToHoneywell", title: "Connect to Honeywell", page: "connectToHoneywell", description: "Connect to Honeywell"
        }
    }
}

def debugPage() {
    dynamicPage(name:"debugPage", title: "Debug", install: false, uninstall: false) {
        section {
            paragraph "Debug buttons"
        }
        section {
            input 'refreshToken', 'button', title: 'Force Token Refresh', submitOnChange: true
        }
        section {
            input 'deleteDevices', 'button', title: 'Delete all devices', submitOnChange: true
        }
        section {
            input 'refreshDevices', 'button', title: 'Refresh all devices', submitOnChange: true
        }
        section {
            input 'initialize', 'button', title: 'initialize', submitOnChange: true
        }
        section {
            input 'createNewAccessToken', 'button', title: 'Create New Access Token', submitOnChange: true
        }
        
    }
}

def LogDebug(logMessage)
{
    if(settings?.debugOutput)
    {
        log.debug "${logMessage}";
    }
}

def LogInfo(logMessage)
{
    log.info "${logMessage}";
}

def LogWarn(logMessage)
{
    log.warn "${logMessage}";
}

def LogError(logMessage)
{
    log.error "${logMessage}";
}

def installed()
{
    LogInfo("Installing Honeywell Home.");
    createAccessToken();
    
}

def initialize() 
{
    LogInfo("Initializing Honeywell Home.");
    unschedule()
    refreshToken()
    RefreshAllDevices()
    
    if (refreshIntervals != "0" && refreshIntervals != null)
    {
        def cronString = ('0 */' + refreshIntervals + ' * ? * *')
        LogDebug("Scheduling Refresh cronstring: ${cronString}")
        schedule(cronString, RefreshAllDevices)
    }
    else
    {
        LogInfo("Auto Refresh Disabled.")
    }
}

def updated() 
{
    LogDebug("Updated with config: ${settings}");
    if (refreshIntervals == null)
    {
        refreshIntervals = 10;
    }
    initialize();
}

def uninstalled() 
{
    LogInfo("Uninstalling Honeywell Home.");
    unschedule()
    for (device in getChildDevices())
    {
        deleteChildDevice(device.deviceNetworkId)
    }
}

def connectToHoneywell() 
{
    LogDebug("connectToHoneywell()");
    LogDebug("Key: ${settings.consumerKey}")

    //if this isn't defined early then the redirect fails for some reason...
    def redirectLocation = "http://www.bing.com";
    if (state.accessToken == null)
    {
        createAccessToken();
    }
    def auth_state = java.net.URLEncoder.encode("${getHubUID()}/apps/${app.id}/handleAuth?access_token=${state.accessToken}", "UTF-8")
    def escapedRedirectURL = java.net.URLEncoder.encode(global_redirectURL, "UTF-8")
    def authQueryString = "response_type=code&redirect_uri=${escapedRedirectURL}&client_id=${settings.consumerKey}&state=${auth_state}";

    def params = [
        uri: global_apiURL,
        path: "/oauth2/authorize",
        queryString: authQueryString.toString()
    ]
    LogDebug("honeywell_auth request params: ${params}");
    try {
        httpPost(params) { response -> 
            if (response.status == 302) 
            {
                LogDebug("Response 302, getting redirect")
                redirectLocation = response.headers.'Location'
                LogDebug("Redirect: ${redirectLocation}");
            }
            else
            {
                LogError("Auth request Returned Invalid HTTP Response: ${response.status}")
                return false;
            } 
        }
    }
    catch (groovyx.net.http.HttpResponseException e)
    {
        LogError("API Auth failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

    dynamicPage(name: "mainPage", title: "Honeywell Home", install: true, uninstall: true) {
        section
        {
            paragraph "Click below to be redirected to Honeywall to authorize Hubitat access."
            href(
                name       : 'authHref',
                title      : 'Establish OAuth Link with Honeywell',
                url        : redirectLocation,
                description: ''
            )
        }
    }
}

def getDiscoverButton() 
{
    if (state.access_token == null) 
    {
        section 
        {
            paragraph "Device discovery and configuration is hidden until authorization is completed."            
        }
    } 
    else 
    {
        section 
        {
            input 'discoverDevices', 'button', title: 'Discover', submitOnChange: true
        }
    }
}

def getDebugLink() {
    section{
        href(
            name       : 'debugHref',
            title      : 'Debug buttons',
            page       : 'debugPage',
            description: 'Access debug buttons (force Token refresh, delete child devices , refresh devices)'
        )
    }
}

def listDiscoveredDevices() {
    def children = getChildDevices()
    def builder = new StringBuilder()
    builder << "<ul>"
    children.each {
        if (it != null) {
            builder << "<li><a href='/device/edit/${it.getId()}'>${it.getLabel()}</a></li>"
        }
    }
    builder << "</ul>"
    def links = builder.toString()
    if (!children.isEmpty())
    {
        section {
            paragraph "Discovered devices are listed below:"
            paragraph links
        }
            section {
                paragraph "Refresh interval (how often devices are automaticaly refreshed/polled):"

                input name: "refreshIntervals", type: "enum", title: "Set the refresh interval.", options: [0:"off", 1:"1 minute", 2:"2 minutes", 5:"5 minutes",10:"10 minutes",15:"15 minutes",30:"30 minutes",55:"55 minutes"], required: true, defaultValue: "10", submitOnChange: true
        }
    }
}



def appButtonHandler(btn) {
    switch (btn) {
    case 'discoverDevices':
        discoverDevices()
        break
    case 'refreshToken':
        refreshToken()
        break
    case 'deleteDevices':
        deleteDevices()
        break
    case 'refreshDevices':
        RefreshAllDevices()
        break
    case 'initialize':
        initialize()
        break
    case 'createNewAccessToken':
        state.access_token = null
        createAccessToken()
        break
    case 'connectToHoneywell':
        connectToHoneywell()
        break
    default:
        LogError("Invalid Button In Handler")
    }
}

def deleteDevices()
{
    def children = getChildDevices()
    LogInfo("Deleting all child devices: ${children}")
    children.each {
        if (it != null) {
            deleteChildDevice it.getDeviceNetworkId()
        }
    } 
}

def discoverDevices()
{
    LogDebug("discoverDevices()");

    def uri = global_apiURL + '/v2/locations' + "?apikey=" + settings.consumerKey
    def headers = [ Authorization: 'Bearer ' + state.access_token ]
    def contentType = 'application/json'
    def params = [ uri: uri, headers: headers, contentType: contentType ]
    LogDebug("Location Discovery-params ${params}")

    //add error checking
    def reJson =''
    try 
    {
        httpGet(params) { response ->
            def reCode = response.getStatus();
            reJson = response.getData();
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("Location Discover failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return;
    }

    reJson.each {locations ->
        def locationID = locations.locationID.toString()
        LogDebug("LocationID: ${locationID}");
        locations.devices.each {dev ->
            LogDebug("DeviceID: ${dev.deviceID.toString()}")
            LogDebug("DeviceModel: ${dev.deviceModel.toString()}")
            def thermoNetId = "${locationID} - ${dev.deviceID.toString()}"
            if (dev.deviceClass == "Thermostat") {
                try {
                    def newDevice = addChildDevice(
                            'thecloudtaylor',
                            'Honeywell Home Thermostat',
                            thermoNetId,
                            [
                                    name : "Honeywell - ${dev.deviceModel.toString()} - ${dev.deviceID.toString()}",
                                    label: dev.userDefinedDeviceName.toString()
                            ])
                 }
                catch (com.hubitat.app.exception.UnknownDeviceTypeException e) {
                    LogInfo("${e.message} - you need to install the appropriate driver.")
                    return
                }
                catch (IllegalArgumentException ignored) {
                    //Intentionally ignored.  Expected if device id already exists in HE.
                }
                //Check this thermostat for remote sensors
                if (dev.groups != null) {
                    LogDebug("Checking Thermostat ${dev.deviceID.toString()} for remote sensors")
                    dev.groups.each { group ->
                        LogDebug("Group ID: ${group.id.toString()}")
                        LogDebug("Group Name: ${group.name.toString()}")
                        group.rooms.each { room ->
                            LogDebug("Room No.: ${room.toString()}")
                            if (room == 0) {
                                return  // ignore thermostat entry
                            }
                            def roomName = getRemoteSensorUserDefName(dev.deviceID.toString(), locationID,
                                                    group.id.toString(), room)
                            try
                            {
                                def newRemoteSensor = addChildDevice(
                                    'thecloudtaylor',
                                    'Honeywell Home Remote Sensor',
                                    "${locationID}-${dev.deviceID.toString()}-${group.id.toString()}-${room.toString()}",
                                    [
                                            name : "Honeywell Home Remote Sensor",
                                            label: "${dev.userDefinedDeviceName} Thermostat Sensor: ${roomName}"
                                    ])

                                sendEvent(newRemoteSensor, [name: "groupId", value: group.id])
                                sendEvent(newRemoteSensor, [name: "roomId", value: room])
                                sendEvent(newRemoteSensor, [name: "parentDeviceId", value: dev.deviceID.toString()])
                                sendEvent(newRemoteSensor, [name: "parentDeviceNetId", value: thermoNetId])
                                sendEvent(newRemoteSensor, [name: "locationId", value: locationID])
                            }
                            catch (com.hubitat.app.exception.UnknownDeviceTypeException e)
                            {
                            LogInfo("${e.message} - you need to install the appropriate driver.")
                            }
                            catch (IllegalArgumentException ignored)
                            {
                            //Intentionally ignored.  Expected if device id already exists in HE.
                            }
                        }
                    }
                }
            }
        }
    }
}

def discoverDevicesCallback(resp, data)
{
    LogDebug("discoverDevicesCallback()");

    def respCode = resp.getStatus()
    if (resp.hasError()) 
    {
        def respError = ''
        try 
        {
            respError = resp.getErrorJson()
        } 
        catch (Exception ignored) 
        {
            // no response body
        }
        if (respCode == 401 && !data.isRetry) 
        {
            LogWarn('Authorization token expired, will refresh and retry.')
            refreshToken()
            data.isRetry = true
            asynchttpGet(handleDeviceList, data.params, data)
        } 
        else 
        {
            LogWarn("Device-list response code: ${respCode}, body: ${respError}")
        }
    } 
    else 
    {
        def respJson = resp.getJson()
        LogDebug(respJson);
    }
}

def handleAuthRedirect() 
{
    LogDebug("handleAuthRedirect()");

    def authCode = params.code

    LogDebug("AuthCode: ${authCode}")
    def authorization = ("${settings.consumerKey}:${settings.consumerSecret}").bytes.encodeBase64().toString()

    def headers = [
                    Authorization: authorization,
                    Accept: "application/json"
                ]
    def body = [
                    grant_type:"authorization_code",
                    code:authCode,
                    redirect_uri:global_redirectURL
    ]
    def params = [uri: global_apiURL, path: "/oauth2/token", headers: headers, body: body]
    
    try 
    {
        httpPost(params) { response -> loginResponse(response) }
    } 
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("Login failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
    }

    def stringBuilder = new StringBuilder()
    stringBuilder << "<!DOCTYPE html><html><head><title>Honeywell Connected to Hubitat</title></head>"
    stringBuilder << "<body><p>Hubitate and Honeywell are now connected.</p>"
    stringBuilder << "<p><a href=http://${location.hub.localIP}/installedapp/configure/${app.id}/mainPage>Click here</a> to return to the App main page.</p></body></html>"
    
    def html = stringBuilder.toString()

    render contentType: "text/html", data: html, status: 200
}

def refreshToken()
{
    LogDebug("refreshToken()");

    if (state.refresh_token != null)
    {
        def authorization = ("${settings.consumerKey}:${settings.consumerSecret}").bytes.encodeBase64().toString()

        def headers = [
                        Authorization: authorization,
                        Accept: "application/json"
                    ]
        def body = [
                        grant_type:"refresh_token",
                        refresh_token:state.refresh_token

        ]
        def params = [uri: global_apiURL, path: "/oauth2/token", headers: headers, body: body]
        
        try 
        {
            httpPost(params) { response -> loginResponse(response) }
        } 
        catch (groovyx.net.http.HttpResponseException e) 
        {
            LogError("Login failed -- ${e.getLocalizedMessage()}: ${e.response.data}")  
        }
    }
    else
    {
        LogError("Failed to refresh token, refresh token null.")
    }
}

def loginResponse(response) 
{
    LogDebug("loginResponse()");

    def reCode = response.getStatus();
    def reJson = response.getData();
    LogDebug("reCode: {$reCode}")
    LogDebug("reJson: {$reJson}")

    if (reCode == 200)
    {
        state.access_token = reJson.access_token;
        state.refresh_token = reJson.refresh_token;
        
        def expireTime = (Integer.parseInt(reJson.expires_in) - 100)
        LogInfo("Honeywell API Token Refreshed Succesfully, Next Scheduled in: ${expireTime} sec")
        runIn(expireTime, refreshToken)
    }
    else
    {
        LogError("LoginResponse Failed HTTP Request Status: ${reCode}");
    }
}

def RefreshAllDevices()
{
    LogDebug("RefreshAllDevices()");

    def children = getChildDevices()
    children.each 
    {
        if (it != null) 
        {
            // Thermostat or Sensor?
            if (it.hasAttribute("groupId") && it.hasAttribute("roomId")) {
                refreshRemoteSensor(it)
            }
            else {
                refreshThermosat(it)
            }
        }
    }
}

def refreshHelper(jsonString, cloudString, deviceString, com.hubitat.app.DeviceWrapper device, optionalUnits=null, optionalMakeLowerMap=false, optionalMakeLowerString=false, optionalIsStateChange=false)
{
    try
    {
        LogDebug("refreshHelper() cloudString:${cloudString} - deviceString:${deviceString} - device:${device} - optionalUnits:${optionalUnits} - optionalMakeLowerMap:${optionalMakeLower} -optionalMakeLowerString:${optionalMakeLower}")
        
        def value = jsonString.get(cloudString)
        
         LogInfo("updateThermostats-${cloudString}: ${value}")
         LogDebug("updateThermostats-${cloudString}: ${value}")
        if (value == null)
        {
            LogDebug("Thermostate Does not Support: ${deviceString} (${cloudString})")
            return false;
        }
        if (optionalMakeLowerMap)
        {
            def lowerCaseValues = []
            value.each {m -> lowerCaseValues.add(m.toLowerCase())}
            value = lowerCaseValues
        }
        if (optionalMakeLowerString)
        {
            value = value.toLowerCase()
        }
        if (optionalUnits != null)
        {
            sendEvent(device, [name: deviceString, value: value, unit: optionalUnits, isStateChange: optionalIsStateChange])
        }
        else
        {
            def newValue = value     
            if ((deviceString == "supportedThermostatModes") || (deviceString == "supportedThermostatFanModes"))
            {
                newValue = JsonOutput.toJson(value)
                if (settings?.debugOutput) log.debug("Caught supportedModes... converted value = ${newValue}")
            }
          
            sendEvent(device, [name: deviceString, value: newValue])
        }
    }
    catch (java.lang.NullPointerException e)
    {
        LogDebug("Thermostate Does not Support: ${deviceString} (${cloudString})")
        return false;
    }

    return true;
}

def refreshThermosat(com.hubitat.app.DeviceWrapper device, retry=false)
{
    LogDebug("refreshThermosat()")

    def deviceID = device.getDeviceNetworkId();
    def locDelminator = deviceID.indexOf('-');
    def honeywellLocation = deviceID.substring(0, (locDelminator-1))
    def honewellDeviceID = deviceID.substring((locDelminator+2))

    LogInfo("Attempting to Update DeviceID: ${honewellDeviceID}, With LocationID: ${honeywellLocation}");

    def uri = global_apiURL + '/v2/devices/thermostats/'+ honewellDeviceID + '?apikey=' + settings.consumerKey + '&locationId=' + honeywellLocation
    def headers = [ Authorization: 'Bearer ' + state.access_token ]
    def contentType = 'application/json'
    def params = [ uri: uri, headers: headers, contentType: contentType ]
    LogDebug("Location Discovery-params ${params}")

    //add error checking
    def reJson =''
    try 
    {
        httpGet(params) 
        { 
            response ->
            def reCode = response.getStatus();
            reJson = response.getData();
            LogDebug("reCode: {$reCode}")
            LogDebug("reJson: {$reJson}")
        }
    }
    catch (java.net.SocketTimeoutException e)
    {
        if (!retry)
        {
        LogInfo("Thermosat API failed retrying... -- ${e.getLocalizedMessage()}")    
        refreshToken()
        refreshThermosat(device, true)
        }   
     else
     {
        LogError("Thermosat API failed 2nd time (giving up) -- ${e.getLocalizedMessage()}")     
     }
       
      return;
    }
    
    catch (groovyx.net.http.HttpResponseException | org.apache.http.conn.ConnectTimeoutException e ) 
    {
        if (e.getStatusCode() == 401 && !retry)
        {
            LogWarn('Authorization token expired, will refresh and retry.')
            refreshToken()
            refreshThermosat(device, true)
        }
         // lgk add code to only print error on retry failure
         if (retry) LogError("Thermosat API failed 2nd time -- ${e.getLocalizedMessage()}: ${e.response.data}")
          else LogInfo("Thermosat API failed retrying... -- ${e.getLocalizedMessage()}: ${e.response.data}")
       
        return;
    }

    def tempUnits = "°F"
    if (reJson.units != "Fahrenheit")
    {
        tempUnits = "°C"
    }
    sendEvent(device, [name: "units", value: tempUnits])
    LogInfo("updateThermostats-tempUnits: ${tempUnits}")
    
    
    def now = new Date().format('MM/dd/yyyy h:mm a', location.timeZone)
    sendEvent(device, [name: "lastUpdate", value: now, descriptionText: "Last Update: $now", isStateChange: true])

    refreshHelper(reJson, "indoorTemperature", "temperature", device, tempUnits, false, false, true)
    refreshHelper(reJson, "allowedModes", "supportedThermostatModes", device, null, true, false)
    refreshHelper(reJson, "indoorHumidity", "humidity", device, null, false, false)
    refreshHelper(reJson, "allowedModes", "allowedModes", device, null, false, false)
    refreshHelper(reJson.changeableValues, "heatSetpoint", "heatingSetpoint", device, tempUnits, false, false, false)
    refreshHelper(reJson.changeableValues, "coolSetpoint", "coolingSetpoint", device, tempUnits, false, false, false)
    refreshHelper(reJson.changeableValues, "mode", "thermostatMode", device, null, false, true)

    if ((reJson != null) && (settings?.debugOutput))
        {
            log.debug "reJson = ${reJson}"
        }
    
    if (reJson.changeableValues.containsKey("autoChangeoverActive"))
    {
        refreshHelper(reJson.changeableValues, "autoChangeoverActive", "autoChangeoverActive", device, null, false, false)
    }
    else
    {
        LogDebug("Thermostat does not support auto change over")
        sendEvent(device, [name: "autoChangeoverActive", value: "unsupported"])
    }

    if (reJson.changeableValues.containsKey("emergencyHeatActive"))
    {
        refreshHelper(reJson.changeableValues, "emergencyHeatActive", "emergencyHeatActive", device, null, false, false)
    }
    else
    {
        LogDebug("Thermostat does not support emergency heat")
        sendEvent(device, [name: "emergencyHeatActive", value: null])
    }

    if (reJson.containsKey("settings") && reJson.settings.containsKey("fan"))
    {
        refreshHelper(reJson.settings.fan, "allowedModes", "supportedThermostatFanModes", device, null, true, false)
        if (reJson.settings.fan.containsKey("changeableValues"))
        {
            refreshHelper(reJson.settings.fan.changeableValues, "mode", "thermostatFanMode", device, null, false, true)
        }
        refreshHelper(reJson.settings.fan, "fanRunning", "thermostatFanState", device, null, false, false)
    }

    def operationStatus = reJson.operationStatus.mode
    def formatedOperationStatus =''
    if (operationStatus == "EquipmentOff")
    {
        formatedOperationStatus = "idle";
    }
    else if(operationStatus == "Heat")
    {
        formatedOperationStatus = "heating";
    }
    else if(operationStatus == "Cool")
    {
        formatedOperationStatus = "cooling";
    }
    else if(operationStatus == "EmergencyHeat")
    {
        formatedOperationStatus = "emergencyHeating";
    } 
    
    else
    {
        LogError("Unexpected Operation Status: ${operationStatus}")
    }

    LogDebug("updateThermostats-thermostatOperatingState: ${formatedOperationStatus}")
    sendEvent(device, [name: 'thermostatOperatingState', value: formatedOperationStatus])
}

String getRemoteSensorUserDefName(String parentDeviceId, String locationId, String groupId, int roomId, retry=false)
{
    LogDebug("getRemoteSensorUserDefName()")
    def uri = global_apiURL + '/v2/devices/thermostats/'+ parentDeviceId + '/group/' +  groupId + '/rooms?apikey=' + settings.consumerKey + '&locationId=' + locationId
    def headers = [ Authorization: 'Bearer ' + state.access_token ]
    def contentType = 'application/json'
    def params = [ uri: uri, headers: headers, contentType: contentType ]
    LogDebug("getRemoteSensorUserDefName - params ${params}")

    def reJson =''
    try
    {
        httpGet(params)
                {
                    response ->
                        def reCode = response.getStatus();
                        reJson = response.getData();
                        LogDebug("reCode: {$reCode}")
                        LogDebug("reJson: {$reJson}")
                }
    }
    catch (groovyx.net.http.HttpResponseException e)
    {
        if (e.getStatusCode() == 401 && !retry)
        {
            LogWarn('Authorization token expired, will refresh and retry.')
            refreshToken()
            getRemoteSensorUserDefName(parentDeviceId, locationId, groupId, roomID, true)
        }

        LogError("Remote Sensor API failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return ""
    }

    def roomJson
    reJson.rooms.each{ room ->
        if (room.id == roomId) {
            roomJson = room
            return
        }
    }
    if (roomJson == null) {
        LogError("getRemoteSensorUserDefName() roomJson = null")
        return ""
    }
    LogDebug( "roomJson: ${roomJson}")

    def value = roomJson.get("name")
    LogDebug("getRemoteSensorUserDefName value: ${value}")

    return value
}

def refreshOccupiedAttr(jsonString, com.hubitat.app.DeviceWrapper device)
{
    try
    {
        LogDebug("refreshOccupiedAttr() - device:${device}")

        def value = jsonString.get("overallMotion")
        LogDebug("overallMotion: ${value}")

        if (value == null)
        {
            LogDebug("Remote Sensor Error -  Missing overallMotion entry")
            return false
        }

        def occupancy = value ? "Occupied" : "Vacant"
        sendEvent(device, [name: "occupied", value: occupancy])
    }
    catch (Exception e)
    {
        LogDebug("refreshOccupiedAttr() error: ${e.getLocalizedMessage()}")
        return false
    }
    return true
}

def refreshRemoteSensor(com.hubitat.app.DeviceWrapper device, retry=false)
{
    LogDebug("refreshRemoteSensor()")
    def honeywellDeviceID = device.currentValue("parentDeviceId")
    def honeywellLocation = device.currentValue("locationId")
    def roomID = device.currentValue("roomId")
    def uri = global_apiURL + '/v2/devices/thermostats/'+ honeywellDeviceID + '/priority?apikey=' + settings.consumerKey + '&locationId=' + honeywellLocation
    def headers = [ Authorization: 'Bearer ' + state.access_token ]
    def contentType = 'application/json'
    def params = [ uri: uri, headers: headers, contentType: contentType ]
    LogDebug("refreshRemoteSensor - params ${params}")

    //add error checking
    def reJson =''
    try
    {
        httpGet(params)
                {
                    response ->
                        def reCode = response.getStatus();
                        reJson = response.getData();
                        LogDebug("reCode: {$reCode}")
                        LogDebug("reJson: {$reJson}")
                }
    }
    catch (groovyx.net.http.HttpResponseException e)
    {
        if (e.getStatusCode() == 401 && !retry)
        {
            LogWarn('Authorization token expired, will refresh and retry.')
            refreshToken()
            refreshRemoteSensor(device, true)
        }

        LogError("Remote Sensor API failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return;
    }

    def parentDeviceNetId = device.currentValue("parentDeviceNetId")
    def tempUnits
    try
    {
        def parent = getChildDevice(parentDeviceNetId)
        tempUnits = parent.currentValue("units")
    }
    catch (Exception e)
    {
        LogError("Thermostat driver failed to get thermo units - ${e.getLocalizedMessage()} - Is driver up to date?")
        tempUnits = "F"
    }

    def roomJson
    reJson.currentPriority.rooms.each{ room ->
        if (room.id == roomID) {
            roomJson = room
            return
        }
    }
    if (roomJson == null) {
        LogError("RefreshRemoteSensor() roomJson = null")
        return
    }
    LogDebug( "roomJson: ${roomJson}")
    //TO DO: Fix accessory indexing workaround (if possible)
    refreshHelper(roomJson, "roomAvgTemp", "temperature", device, tempUnits, false, false)
    refreshHelper(roomJson, "roomAvgHumidity", "humidity", device, null, false, false)
    refreshHelper(roomJson.accessories[0], "status", "batterystatus", device, null, false, false)
    refreshHelper(roomJson, "roomName", "roomName", device, null, false, false)
    refreshOccupiedAttr(roomJson, device)
}

def setThermosatSetPoint(com.hubitat.app.DeviceWrapper device, mode=null, autoChangeoverActive=false, emergencyHeatActive=null, heatPoint=null, coolPoint=null, retry=false)
{
    LogDebug("setThermosatSetPoint()")
    def deviceID = device.getDeviceNetworkId();
    def locDelminator = deviceID.indexOf('-');
    def honeywellLocation = deviceID.substring(0, (locDelminator-1))
    def honewellDeviceID = deviceID.substring((locDelminator+2))


    if (mode == null)
    {
        mode=device.currentValue('thermostatMode');
    }

    //The Honeywell API expects uppercase.
    if (mode.toLowerCase() == "heat")
    {
        mode = "Heat"
    }
    else if (mode.toLowerCase() == "cool")
    {
        mode = "Cool"
    }
    else if (mode.toLowerCase() == "off")
    {
        mode = "Off"
    }
    else if (mode.toLowerCase() == "auto")
    {
        mode = "Auto"
    }
    else
    {
        LogError("Invalid Mode Specified: ${mode}")
        return false;
    }

    if (heatPoint == null)
    {
        heatPoint=device.currentValue('heatingSetpoint');
    }

    if (coolPoint == null)
    {
        coolPoint=device.currentValue('coolingSetpoint');
    }

    LogDebug("Attempting to Set DeviceID: ${honewellDeviceID}, With LocationID: ${honeywellLocation}");
    def uri = global_apiURL + '/v2/devices/thermostats/'+ honewellDeviceID + '?apikey=' + settings.consumerKey + '&locationId=' + honeywellLocation

    def headers = [
                    Authorization: 'Bearer ' + state.access_token,
                    "Content-Type": "application/json"
                    ]
    def body = []


    // For LCC devices thermostatSetpointStatus = "NoHold" will return to schedule. "TemporaryHold" will hold the set temperature until "nextPeriodTime". "PermanentHold" will hold the setpoint until user requests another change.
    // BugBug: Need to include nextPeriodTime if TemporaryHoldIs true
    if (honewellDeviceID.startsWith("LCC"))
    {
        body = [
                mode:mode,
                thermostatSetpointStatus:"PermanentHold", 
                heatSetpoint:heatPoint, 
                coolSetpoint:coolPoint]
    }
    else //TCC model
    {
        body = [
                mode:mode,
                heatSetpoint:heatPoint, 
                coolSetpoint:coolPoint]
    }

    if (autoChangeoverActive != "unsupported")
    {
        body.put("autoChangeoverActive",autoChangeoverActive)
    }
    
    if (emergencyHeatActive != null)
    {
        body.put("emergencyHeatActive", emergencyHeatActive)
    }

    def params = [ uri: uri, headers: headers, body: body]
    LogDebug("setThermosat-params ${params}")

    try
    {
        httpPostJson(params) { response -> LogInfo("SetThermostate() Mode: ${mode}; Heatsetpoint: ${heatPoint}; CoolPoint: ${coolPoint} API Response: ${response.getStatus()}")}
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        if (e.getStatusCode() == 401 && !retry)
        {
            LogWarn('Authorization token expired, will refresh and retry.')
            refreshToken()
            setThermosatSetPoint(device, mode, autoChangeoverActive, emergencyHeatActive,  heatPoint, coolPoint, true)
        }
        LogError("Set Api Call failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

    refreshThermosat(device)
    return true;
}

def setThermosatFan(com.hubitat.app.DeviceWrapper device, fan=null, retry=false)
{
    LogDebug("setThermosatFan()"  )
    def deviceID = device.getDeviceNetworkId();
    def locDelminator = deviceID.indexOf('-');
    def honeywellLocation = deviceID.substring(0, (locDelminator-1))
    def honewellDeviceID = deviceID.substring((locDelminator+2))


    if (fan == null)
    {
        fan=device.('thermostatFanMode');
    }

    if (fan.toLowerCase() == "auto")
    {
        fan = "Auto"
    }
    else if (fan.toLowerCase() == "on")
    {
        fan = "On"
    }
    else if (fan.toLowerCase() == "circulate")
    {
        fan = "Circulate"
    }
    else
    {
        LogError("Invalid Fan Mode Specified: ${fan}")
        return false;
    }


    LogDebug("Attempting to Set Fan For DeviceID: ${honewellDeviceID}, With LocationID: ${honeywellLocation}");
    def uri = global_apiURL + '/v2/devices/thermostats/'+ honewellDeviceID + '/fan' + '?apikey=' + settings.consumerKey + '&locationId=' + honeywellLocation

    def headers = [
                    Authorization: 'Bearer ' + state.access_token,
                    "Content-Type": "application/json"
                    ]
    def body = [
            mode:fan]

    def params = [ uri: uri, headers: headers, body: body]
    LogDebug("setThermosat-params ${params}")

    try
    {
        httpPostJson(params) { response -> LogDebug("SetThermostateFan Response: ${response.getStatus()}")}
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        if (e.getStatusCode() == 401 && !retry)
        {
            LogWarn('Authorization token expired, will refresh and retry.')
            refreshToken()
            setThermosatFan(device, fan, true)
        }
        LogError("Set Fan Call failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

    refreshThermosat(device)
    return true;
}
