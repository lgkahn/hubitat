/*
Hubitat Driver For Honeywell Thermistat

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
 10/24 lgk add tons of new attrbiutes
ie 
maxCoolSetPoint : 90
minCoolSetPoint : 70
maxHeatSetPoint : 79
minHeatSetPoint : 60
userDefinedDeviceName : Condo 304
isAlive : true
macID : 48A2E6363B49
fanOperatingState : off
vacationHold : false
model : T5-T6

lgk 04/25 added attributes and code for percent runtime statistics that updates the percent of time it is running in the last hour based on
how often it refreshes.. reset every hour or x times based on refresh time
so for instance if refresh time is 15 minutes it will only consider the last 4 updates to calculate the statistics. 
Similiarly 6 times for 10 minutes etc.

second round now keeps track of average runtime for days and months of the year.

Considerable inspiration an example to: https://github.com/dkilgore90/google-sdm-api/
*/

import groovy.transform.Field
@Field Map globalDays = [:]
@Field Map globalMonths = [:]

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field
import java.net.SocketTimeoutException
import java.time.LocalDate

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
                input name: "debugStats", type: "bool", title: "Enable Statistics Calc. Logging?", defaultValue: false, submitOnChange: true
                input name: "enableStats", type: "bool", title: "Enable Calculation of hourly runtime percentage?", defaultValue: false, submitOnChange: false
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
       section {
            input 'testGlobals', 'button', title: 'test Globals', submitOnChange: true  
       }
        section {
            input 'testfx', 'button', title: 'test fx', submitOnChange: true  
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

def LogDebugStats(logMessage)
{
    if(settings?.debugStats)
    {
        log.info "${logMessage}";
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
    log.debug "in updated refresh inter = $refreshIntervals"
    LogDebug("Updated with config: ${settings}");
  
    if (enableStats)
    {
        if (refreshIntervals == null)
          {
           refreshIntervals = 10;
          }
    
    def ri = refreshIntervals.toInteger()
    // lgk set stats stuff
    if (ri == 55)
      state.maxStats = 1
    else if (ru == 30)
      state.maxStats = 2
    else if (ri == 15)
      state.maxStats = 4
    else if (ri == 10)
      state.maxStats = 6
    else if (ri == 5)
      state.maxStats = 12
    else 
      state.maxStats = 12 // 12 max 
    
    log.debug "after loop max stats = ${state.maxStats}"
    
      state.currentRefresh = 1
      state.refresh1 = 0
      state.refresh2 = 0
      state.refresh3 = 0
      state.refresh4 = 0
      state.refresh5 = 0
      state.refresh6 = 0
      state.refresh7 = 0
      state.refresh8 = 0
      state.refresh9 = 0
      state.refresh10 = 0
      state.refresh11 = 0
      state.refresh12 = 0
    
      def cv = 0.0
      state.currentPercent = cv.setScale(2)
      
      if (state.globalDays == null)
        initializeGlobals()  
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

                input name: "refreshIntervals", type: "enum", title: "Set the refresh interval.", options: [0:"off"																																			, 5:"5 minutes",10:"10 minutes",15:"15 minutes",30:"30 minutes",55:"55 minutes"], required: true, defaultValue: "10", submitOnChange: true
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
     case 'testGlobals':
        initializeGlobals()
        break;
      case 'testfx':
        testfx()
        break;     
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

    def childCount = 0
    def children = getChildDevices()
    children.each 
    {
        if (it != null) 
        {
            ++childCount
           
            // Thermostat or Sensor?
            if (it.hasAttribute("groupId") && it.hasAttribute("roomId")) {
                refreshRemoteSensor(it)
            }
            else {
                refreshThermosat(it)
            }
        }
    }
    
    // lgk if stats enabled and more then one therm disable clear stuff and print warning
    //log.info "Child Count = $childCount"
    if ((enableStats) && (childCount > 1))
    {
        log.warn "Statistics are only designed to work with a single therm. Disabling them. If you want statistics with more than one create a separate instance for each."
        disableStats()
    }
}

def refreshHelper(jsonString, cloudString, deviceString, com.hubitat.app.DeviceWrapper device, optionalUnits=null, optionalMakeLowerMap=false, optionalMakeLowerString=false, optionalIsStateChange=false)
{
    try
    {
        LogDebug("refreshHelper() cloudString:${cloudString} - deviceString:${deviceString} - device:${device} - optionalUnits:${optionalUnits} - optionalMakeLowerMap:${optionalMakeLower} -optionalMakeLowerString:${optionalMakeLower}")
        
        def value = jsonString.get(cloudString)
       
         LogDebug("updateThermostats-${cloudString}: ${value}")
        if (value == null)
        {
            LogDebug("Thermostat Does not Support: ${deviceString} (${cloudString})")
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
          
            //lgk debugging
            LogDebug "sending: $deviceString value: $newValue"
            sendEvent(device, [name: deviceString, value: newValue])
        }
    }
    catch (java.lang.NullPointerException e)
    {
        LogDebug("Thermostat Does not Support: ${deviceString} (${cloudString})")
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
    LogDebug("updateThermostats-tempUnits: ${tempUnits}")
    
    
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
    
    
    // lgk new attributes    
    refreshHelper(reJson, "maxCoolSetpoint", "maxCoolSetPoint", device, tempUnits, false, false, true)
    refreshHelper(reJson, "minCoolSetpoint", "minCoolSetPoint", device, tempUnits, false, false, true)
    refreshHelper(reJson, "maxHeatSetpoint", "maxHeatSetPoint", device, tempUnits, false, false, true)   
    refreshHelper(reJson, "minHeatSetpoint", "minHeatSetPoint", device, tempUnits, false, false, true) 
    
    refreshHelper(reJson, "isAlive", "isAlive", device,  null, false, false, true) 
    refreshHelper(reJson, "userDefinedDeviceName", "userDefinedDeviceName", device, tempUnits, false, false, false)
    refreshHelper(reJson, "macID", "macID", device, tempUnits, false, false, false)
    refreshHelper(reJson, "deviceModel", "model", device, null, false, false, false)
    refreshHelper(reJson.vacationHold, "enabled", "vacationHold", device, null, false, false, true)  

     // now actual fan state
   
    def fanOn = reJson.operationStatus.fanRequest
    def fanCirc = reJson.operationStatus.circulationFanRequest
   
    if (fanOn == true || fanCirc == true)
         sendEvent(device, [name: "fanOperatingState", value: "on"]) 
    else  sendEvent(device, [name: "fanOperatingState", value: "off"]) 
        
    def operationStatus = reJson.operationStatus.mode
    def formatedOperationStatus =''
    def isOn = 0
    if (operationStatus == "EquipmentOff")
    {
        formatedOperationStatus = "idle";
        isOn = 0
    }
    else if(operationStatus == "Heat")
    {
        formatedOperationStatus = "heating";
        isOn = 1
    }
    else if(operationStatus == "Cool")
    {
        formatedOperationStatus = "cooling";
        isOn = 1
    }
    else if(operationStatus == "EmergencyHeat")
    {
        formatedOperationStatus = "emergencyHeating";
        isOn = 1
    } 
    
    else
    {
        LogError("Unexpected Operation Status: ${operationStatus}")
        isOn = 0
    }

    LogDebug("updateThermostats-thermostatOperatingState: ${formatedOperationStatus}")
    sendEvent(device, [name: 'thermostatOperatingState', value: formatedOperationStatus])
    if (enableStats)
      {
        updateStats(isOn)
        sendEvent(device, [name: 'hourlyRuntimePercentage', value: state.currentPercent, isStateChange: true]) 
        // force state change so if attr stays at same value rule for runtime still triggers
      }
    
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


def updateStats(isOn)
{
    LogDebugStats("in update status isOn = $isOn, maxStats = ${state.maxStats}, current Stats counter = ${state.currentRefresh}")
    
    switch(state.currentRefresh) {
        case 1:
         state.refresh1 = isOn
         break;
        case 2:
         state.refresh2 = isOn
         break;
        case 3:
         state.refresh3 = isOn
         break;
        case 4:
         state.refresh4 = isOn
         break;
         case 5:
         state.refresh5 = isOn
         break;
        case 6:
         state.refresh6 = isOn
         break;
        case 7:
         state.refresh7 = isOn
         break;
        case 8:
         state.refresh8 = isOn
         break;
        case 9:
         state.refresh9 = isOn
         break;
        case 10:
         state.refresh10 = isOn
         break;
        case 11:
         state.refresh11 = isOn
         break;
        case 12:
         state.refresh12 = isOn
         break; 
    }
    
    // calculate stats and up or reset counter
    if (state.maxStats == 12)
      {
        def theTotal = state.refresh1 + state.refresh2 + state.refresh3 + state.refresh4 + state.refresh5 + state.refresh6 + 
                       state.refresh7 + state.refresh8 + state.refresh9 + state.refresh10 + state.refresh11 + state.refresh12
        
        def BigDecimal stat = theTotal.toFloat() / state.maxStats.toFloat()   
        state.currentPercent = stat.setScale(2, BigDecimal.ROUND_HALF_UP)
        LogDebugStats("total: $theTotal , stats: ${state.currentPercent}")
      }
    else if (state.maxStats == 6)
      {
        def theTotal = state.refresh1 + state.refresh2 + state.refresh3 + state.refresh4 + state.refresh5 + state.refresh6
        def BigDecimal stat = theTotal.toFloat() / state.maxStats.toFloat()
        state.currentPercent = stat.setScale(2, BigDecimal.ROUND_HALF_UP) 
        LogDebugStats("total: $theTotal , stats: ${state.currentPercent}")
      }
    
    else if (state.maxStats == 4)
      {
        def theTotal = state.refresh1 + state.refresh2 + state.refresh3 + state.refresh4 
        def BigDecimal stat = theTotal.toFloat() / state.maxStats.toFloat()
        state.currentPercent = stat.setScale(2, BigDecimal.ROUND_HALF_UP)
        LogDebugStats("total: $theTotal , stats: ${state.currentPercent}")          
      }
    else if (state.maxStats == 2)
      {
        def theTotal = state.refresh1 + state.refresh2 
        def BigDecimal stat = theTotal.toFloat() / state.maxStats.toFloat()
        state.currentPercent = stat.setScale(2, BigDecimal.ROUND_HALF_UP)
        LogDebugStats("total: $theTotal , stats: ${state.currentPercent}")  
      }
    else if (state.maxStats == 1)
      {
        def theTotal = state.refresh1
        def BigDecimal stat = theTotal.toFloat() / state.maxStats.toFloat()
        state.currentPercent = stat.setScale(2, BigDecimal.ROUND_HALF_UP)
        LogDebugStats("total: $theTotal , stats: ${state.currentPercent}")         
      }  
     
    // total day status
    state.dayCounter = state.dayCounter + 1
    state.dayTotals = state.dayTotals + isOn.toInteger()
     LogDebugStats("accumulating day stats: current = $isOn total = ${state.dayTotals}, counter = ${state.dayCounter}")
    
    // reset counter
    state.currentRefresh = state.currentRefresh + 1
    if (state.currentRefresh > state.maxStats)
      {
        LogDebugStats("resetting stats counter to 1")
        state.currentRefresh = 1
      }              
}

void addToDays(id, type)
{
    LogDebugStats("in add to day list: [$id, $type]")
    if (state.globalDays != null)
    { 
        globalDays = state.globalDays
    }
    
    globalDays.put(id.toString(), type) 
    state.globalDays = globalDays
}

void listDays()
{
    log.info "In list days"
    globalDays = state.globalDays
    log.info "days = $globalDays"    
}

void addToMonths(id, type)
{
    LogDebugStats("in add to device list: [$id, $type]")
    if (state.globalMonths != null)
    {
        globalMonths = state.globalMonths
    }
    globalMonths.put(id.toString(), type)   
    state.globalMonths = globalMonths
}

void listMonths()
{
    log.info "In list Months"
    globalMonths = state.globalMonths
    log.info "months = $globalMonths"    
}

def resetDayCounters()
{
    LogDebugStats("In reset day counters")
    state.dayCounter = 0
    state.dayTotals = 0
}

def initializeGlobals()
{
    LogDebugStats("in init globals.")
    state.globalDays = [:]
    state.globalMonths = [:]
 
    for (i in 1..31)
    {
     addToDays(i,0.0.toFloat())
    }
    
   for (j in 1..12)
    {
     addToMonths(j,0.0.toFloat())
    }  
   
 resetDayCounters()
      
   // listDays()
   //listMonths()
   //addToDays(26,0.5) 
   // listDays()
    
    // schedule daily and monthly jobs
    unschedule("storeDailyStats")
    schedule("0 55 23 ? * * *", "storeDailyStats")
    
    unschedule("storeMonthlyStats")
    schedule("0 2 0 1 1-12 ? *","storeMonthlyStats")
  
    // test gettiong day
   // def now = new Date().format('dd', location.timeZone) 
   // def intday = now.toInteger()
   // log.debug "now = $intday"
    
    //storeMonthlyStats()   
}

def storeDailyStats()
{
    LogDebugStats("In store daily statistics")
    if ((state.dayCounter != null) && (state.dayCounter != 0))
      {
        def theTotal = state.dayTotals
        def theCount = state.dayCounter
        def BigDecimal daystat = theTotal.toFloat() / theCount.toFloat()
        log.warn "day stat = $daystat"
        def dayPercent = daystat.setScale(2, BigDecimal.ROUND_HALF_UP)
             
        // now store in table for current day
        def now = new Date().format('dd', location.timeZone) 
        def intday = now.toInteger() 
          
          // override to test
          //intday = 30
          log.warn "intday = $intday"
          
       addToDays(intday,dayPercent) 
       LogDebugStats("day total: $theTotal , percent: $dayPercent, global day stats: ${state.globalDays}")     
    
       resetDayCounters()  
          
      // get device
      def com.hubitat.app.DeviceWrapper dev
      def children = getChildDevices()
      children.each 
      {
        if (it != null) 
        {
           dev = it
            }
            else {
               dev = it
            }
      }
  
     sendEvent(dev, [name: 'lastDayPercentage', value: dayPercent, isStateChange: true]) 
     sendEvent(dev, [name: 'monthStats', value: state.globalDays, isStateChange: true])         
                   
      }
}
          
def storeMonthlyStats()
{ 
   
    //listDays()   
    
    LogDebugStats("In store Monthly Stats")
    // called on the first of month so need to go back a month
    
     def themonth = new Date().format('MM').toInteger()
    LogDebugStats("Current month: $themonth")
    
     def lastmonth = 0
    
    if (themonth == 1)
       lastmonth = 12
     else lastmonth = themonth - 1
      
     LogDebugStats("Last Month: $lastmonth")
    
     def theday = new Date().format('dd')
     def theyear = new Date().format('YYYY')
     
     def LocalDate ldate = LocalDate.of(theyear.toInteger(),lastmonth.toInteger(),1)
     def int mdays = ldate.lengthOfMonth();
     LogDebugStats("days in last month = $mdays")
    
     // now loop through those days and get totals
    def float runningTotal = 0.0
    
    def int loopctr = 1
    globalDays = state.globalDays
 
    while (loopctr <= mdays)
    {
     def dayvalue = globalDays.get(loopctr.toString())
    // log.info "day: $loopctr, value: $dayvalue"
     runningTotal = runningTotal + dayvalue
     loopctr++
    }
    
    LogDebugStats("end of loop total = $runningTotal")
    
   def BigDecimal monthstat = runningTotal.toFloat() / mdays.toFloat()
   def monthPercent = monthstat.setScale(2, BigDecimal.ROUND_HALF_UP)
   state.monthPercent = monthPercent
    
   addToMonths(lastmonth,monthPercent)   
   LogDebugStats("month total: $runningTotal, days: $mdays, percentage: $monthPercent, global month stats: ${globalMonths}")   
    
    // get device
    def com.hubitat.app.DeviceWrapper dev
    def children = getChildDevices()
    children.each 
    {
        if (it != null) 
        {
           dev = it
            }
            else {
               dev = it
            }
    }
    
   sendEvent(dev, [name: 'lastMonthPercentage', value: monthPercent, isStateChange: true]) 
   sendEvent(dev, [name: 'yearStats', value: state.globalMonths, isStateChange: true])  
    
   // reset day array on month turnover
   for (i in 1..31)
    {
     addToDays(i,0.0.toFloat())
    }
    
}

def testfx()
{
   // storeMonthlyStats()
    //storeDailyStats()
}

def disableStats()
{
    log.info "Disabling statistics!"
    unschedule("storeDailyStats")
    unschedule("storeMonthlyStats")
    resetDayCounters()
    
    // clear state variables
    state.dayCounter = 0
    state.dayTotals = 0
    state.globalDays = [:]
    state.globalMonths = [:]
    state.maxStats = 0
    state.monthPercent = 0
    state.currentRefresh = 1
    state.refresh1 = 0
    state.refresh2 = 0
    state.refresh3 = 0
    state.refresh4 = 0
    state.refresh5 = 0
    state.refresh6 = 0
    state.refresh7 = 0
    state.refresh8 = 0
    state.refresh9 = 0
    state.refresh10 = 0
    state.refresh11 = 0
    state.refresh12 = 0
    app.updateSetting("debugStats",false)
    app.updateSetting("enableStats",false)
    
}
    
