/**
 *  Wireless Tags (Connect)
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
 */
definition(
    name: 'Wireless Tags (Connect)',
    namespace: 'swanny',
    author: 'swanny',
    description: 'Wireless Tags connection',
    category: 'Convenience',
    iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png',
    iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png',
    oauth: true)

preferences {
    page(name: 'preferencesPage')
}

mappings {
    path('/swapToken') { action: [GET: 'swapToken'] }
}

def preferencesPage() {
    if (!atomicState.accessToken) {
        // log.debug 'about to create access token'
        createAccessToken()
        atomicState.accessToken = state.accessToken
    }

    // oauth docs = http://www.mytaglist.com/eth/oauth2_apps.html
    if (atomicState.authToken) {
        log.debug "have a valid wirelesstags authToken: ${atomicState.authToken}"
        return wirelessDeviceList()
    } else {
        String redirectUrl = oauthInitUrl()
        // log.debug "RedirectUrl = ${redirectUrl}"

        return dynamicPage(title: 'Connect', uninstall: true) {
            section {
                paragraph 'Tap below to log in to the Wireless Tags service and authorize Hubitat access.'
                href url:redirectUrl, style:'external', required:true, title:'Authorize wirelesstag.net', description:'Click to authorize'
            }
        }
    }
}
def wirelessDeviceList() {
    log.debug 'wirelessDeviceList()'
    Map availDevices = getWirelessTags()

    def p = dynamicPage(title: 'Select Your Devices', install: true, uninstall: true) {
        section {
            paragraph 'Tap below to see the list of Wireless Tag devices available in your Wireless Tags account and select the ones you want to connect to Hubitat.'
            paragraph 'When you hit Done, the setup can take as much as 10 seconds per device selected.'
            input(name: 'devices', title:'Tags to Connect', type: 'enum', required:true, multiple:true, options:availDevices)
            paragraph 'Configure the poll timer if you want to periodically poll the Wireless Tags server. Set to 0 to skip polling.'
            input 'pollTimer', 'number', title:'Minutes between poll updates of the sensors', required:true, defaultValue:5
            paragraph 'Select up to 5 devices in each instance. Use a unique name here to create multiple apps.'
            label title: 'Assign a name for this app instance (optional)', required: false
        }
    }

    return p
}

Map getWirelessTags() {
    log.debug 'getWirelessTags()'
    ArrayList result = getTagStatusFromServer()

    Map availDevices = [:]
    result?.each { device ->
        String dni = device?.uuid
        availDevices[dni] = device?.name
    }

    log.debug "devices: $availDevices"

    return availDevices
}

void installed() {
    initialize()
}

void updated() {
    unsubscribe()
    initialize()
}

String getChildNamespace() {
    return 'swanny'
}

String getChildName(Map tagInfo) {
    String deviceType = 'Wireless Tag Generic'
    return deviceType
}

void initialize() {
    unschedule()

    def curDevices = devices.collect { dni ->
        def d = getChildDevice(dni)

        def tag = atomicState.tags.find { it.uuid == dni }

        if (d) {
            log.debug "found ${d.displayName} $dni already exists"
            d.updated()
        }
        else
        {
            d = addChildDevice(getChildNamespace(), getChildName(tag), dni, null, [label:"${tag?.name} Wireless Tag"])
            d.initialSetup()
            log.debug "created ${d.displayName} $dni"
        }

        return dni
    }

    def delete
    // Delete any that are no longer in settings
    if (curDevices) {
        delete = getChildDevices().findAll { !curDevices.contains(it.deviceNetworkId) }
    }
    else {
        delete = getAllChildDevices()
    }

    delete.each { deleteChildDevice(it.deviceNetworkId) }

    if (atomicState.tags == null) { atomicState.tags = [:] }

    pollHandler()

    // set up internal poll timer
    if (pollTimer == null) {
        pollTimer = 5
    }
    log.trace "setting poll to ${pollTimer}"
    schedule("0 0/${pollTimer.toInteger()} * * * ?", pollHandler)
}

String oauthInitUrl() {
    log.debug 'oauthInitUrl()'
    atomicState.oauthInitState = UUID.randomUUID().toString()

    Map oauthParams = [
        client_id: getHubitatClientId(),
        state: atomicState.oauthInitState,
        redirect_uri: generateRedirectUrl(),
    ]

    return 'https://www.mytaglist.com/oauth2/authorize.aspx?' + toQueryString(oauthParams)
}

String generateRedirectUrl() {
    log.debug 'generateRedirectUrl'
    // return apiServerUrl("/api/token/${atomicState.accessToken}/smartapps/installations/${app.id}/swapToken")
    // return apiServerUrl("token/${atomicState.accessToken}/smartapps/installations/${app.id}/swapToken")
    // log.debug 'state.accessToken: ' + state.accessToken
    // return getFullApiServerUrl() + "/oauth/initialize?access_token=${state.accessToken}"
    return fullLocalApiServerUrl("/swapToken?access_token=${atomicState?.accessToken}&")
}

void swapToken() {
    log.debug "swapping token: $params"

    // mytaglist.com doesn't properly add parameters when one exists. So, also look for this mangled version.
    String code = params.code ?: params['?code']
    // log.debug "mytaglist.com authorization_code: ${code}"

    if (code) {
        try {
            Map refreshParams = [
                uri: 'https://www.mytaglist.com/',
                path: '/oauth2/access_token.aspx',
                query: [ grant_type: 'authorization_code', client_id: getHubitatClientId(), client_secret: '042cf455-0fe9-483c-a4e4-9198a2ae7c9d', code: code, redirect_uri: generateRedirectUrl() ],
            ]

            httpPost(refreshParams) { resp ->
                if (resp.status == 200) {
                    def jsonMap = resp.data
                    if (resp.data) {
                        atomicState.authToken = jsonMap?.access_token
                    } else {
                        log.error 'error in resp = ' + resp
                    }
                } else {
                    log.error 'response = ' + resp
                }
            }
        } catch ( ex ) {
            atomicState.authToken = null
            log.error 'catch error = ' + ex
        }
    }

    String html
    if (atomicState.authToken) {
        html = """
<!DOCTYPE html>
<html>
<head>
<title>Wireless Tags Connection Successful</title>
<style type="text/css">
    p {
        font-size: 1.5em;
        font-family: 'Swiss 721 W01 Thin';
        text-align: center;
        color: #666666;
    }
</style>
</head>
<body>
    <p>Your Wireless Tags account is now connected to your hub!</p>
    <p>Close this window and finish setup in the app.</p>
</body>
</html>
"""
    } else {
        html = """
<!DOCTYPE html>
<html>
<head>
<title>Wireless Tags Connection Successful</title>
<style type="text/css">
    p {
        font-size: 1.5em;
        font-family: 'Swiss 721 W01 Thin';
        text-align: center;
        color: ##aa3333;
    }
</style>
</head>
<body>
    <p>Problem connecting your Wireless Tags account.</p>
    <p>Close this window and try again in the app.</p>
</body>
</html>
"""
    }

    render contentType: 'text/html', data: html
}

// Map getEventStates() {
//     Map tagEventStates = [ 0: 'Disarmed', 1: 'Armed', 2: 'Moved', 3: 'Opened', 4: 'Closed', 5: 'Detected', 6: 'Timed Out', 7: 'Stabilizing...', ]
//     return tagEventStates
// }

void pollHandler() {
    log.trace 'pollHandler()'
    getTagStatusFromServer()
    updateAllDevices()
}

void updateAllDevices() {
    log.trace 'updateAllDevices()'
    atomicState.tags.each { device ->
        String dni = device.uuid
        def d = getChildDevice(dni)

        if (d) {
            updateDeviceStatus(device, d)
        }
    }
}

void pollSingle(def child) {
    log.trace 'pollSingle()'
    getTagStatusFromServer()

    def device = atomicState.tags.find { it.uuid == child.device.deviceNetworkId }

    if (device) {
        updateDeviceStatus(device, child)
    }
}

void updateDeviceStatus(def device, def d) {
     log.debug "device info: ${device}"

    // parsing data here
    Map data = [
        tagType: convertTagTypeToString(device),
        temperature: device.temperature.toDouble().round(1),
        battery: (device.batteryRemaining * 100) as int,
        humidity: (device.cap).toDouble().round(),
        illuminance: (device.lux)?.toDouble().round(),
        signaldBm: (device.signaldBm) as int,
        // water : (device.shorted == true) ? 'wet' : 'dry',
    ]
    d.generateEvent(data)
}

int getPollRateMillis() { return 2 * 1000 }

ArrayList getTagStatusFromServer() {
    log.debug 'getTagStatusFromServer()'
    int timeSince = (atomicState.lastPoll != null) ? now() - atomicState.lastPoll : 1000 * 1000

    if ((atomicState.tags == null) || (timeSince > getPollRateMillis())) {
        Map result = postMessage('/ethClient.asmx/GetTagList', null)
        atomicState.tags = result?.d
        atomicState.lastPoll = now()
    } else {
        log.trace 'waiting to refresh from server'
    }
    return atomicState.tags
}

// Poll Child is invoked from the Child Device itself as part of the Poll Capability
void pollChild( def child ) {
    pollSingle(child)
}

void refreshChild( def child ) {
    String id = getTagID(child.device.deviceNetworkId)

    if (id != null) {
        // PingAllTags didn't reliable update the tag we wanted so just ping the one
        Map query = ['id':id]
        postMessage('/ethClient.asmx/PingTag', query)
        pollSingle( child )
    } else {
        log.trace 'Could not find tag'
    }
}

def postMessage(String path, Object query) {
    log.trace "postMessage - sending ${path}"

    Map message = [
                uri: 'https://www.mytaglist.com/',
                path: path,
                headers: ['Content-Type': 'application/json', 'Authorization': "Bearer ${atomicState.authToken}"],
            ]
    if (query != null) {
        if (query instanceof String) {
            message['body'] = query
        } else {
            message['body'] =  toJson(query)
        }
    }

    def jsonMap
    try {
        httpPost(message) { resp ->
            if (resp.status == 200) {
                if (resp.data) {
                    jsonMap = resp.data
                } else {
                    log.trace 'error = ' + resp
                }
            } else {
                if (resp.status == 500 && resp.data.status.code == 14) {
                    log.debug 'Need to refresh auth token?'
                    atomicState.authToken = null
                }
                else {
                    log.error 'Authentication error, invalid authentication method, lack of credentials, etc.'
                }
            }
        }
    } catch ( ex ) {
        //atomicState.authToken = null
        log.trace 'error = ' + ex
    }

    return jsonMap
}

String getQuoted(String orig) {
    return (orig != null) ? "\"${orig}\"" : orig
}

def getTagID(String uuid) {
    return atomicState.tags.find { it.uuid == uuid }?.slaveId
}

String getTagVersion(Map tag) {
   // log.debug "got tag = $tag"
    if (tag.version1 == 2) {
        return (tag.rev == 14) ? ' (v2.1)' : ' (v2.0)'
    }
    if (tag.tagType != 12) { return '' }
    switch (tag.rev) {
        case 0:
            return ' (v1.1)'
        case 1:
            return ' (v1.2)'
        case 11:
            return ' (v1.3)'
        case 12:
            return ' (v1.4)'
        case 13:
            return ' (v1.5)'
        default:
            return ''
    }
}

String convertTagTypeToString(Map tag) {
    String tagString = 'Unknown'

    switch (tag.tagType) {
        case 12:
            tagString = 'MotionSensor'
            break
        case 13:
            tagString = 'MotionHTU'
            break
        case 26:
            tagString = 'Lux'
            break
        case 72:
            tagString = 'PIR'
            break
        case 52:
            tagString = 'ReedHTU'
            break
        case 53:
            tagString = 'Reed'
            break
        case 62:
            tagString = 'Kumostat'
            break
        case 42:
            tagString = 'Outdoor Water Temp.'
            break
        case 32:
        case 33:
            tagString = 'Moisture'
            break
    }

    return tagString + getTagVersion(tag)
}

// int batteryVoltageToPercentage(double batteryVolt) {
//     if (batteryVolt >= 3) {
//         return 100
//     } else if (batteryVolt <= 2.6) {
//         return 1
//     }
//     return (((batteryVolt - 2.6) / 0.4) * 100).toDouble().round()
// }

String toJson(Map m) {
    return new groovy.json.JsonBuilder(m).toString()
}

String toQueryString(Map m) {
    return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join('&')
}

String getHubitatClientId() {
    return 'ec3ba18c-cb06-4d23-8b20-5d7ef6b86f1d'
}

void debugEvent(String message, boolean displayEvent) {
    Map results = [name: 'appdebug', descriptionText: message, displayed:displayEvent]
    log.debug "Generating AppDebug Event: ${results}"
    sendEvent (results)
}
