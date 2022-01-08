/**
 *  WZB Natural Gas Sensor
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

 * lgk fix issue in porting where configure caused error, also add checkin clear and last status 12/21
 * also add gas as well as smoke attribute, and attributes for zonetype enrollment model etc.
 * also add multiple fingerprints.
 *
 * lgk v 3 
 * add user defined checkin time. in seconds 120-86400 (1 day) cannot go less than 2 minutes/120 or it doesnt work.
 * also add debuging 
 * 
 * ;gk b4 remove gas and change to naturalGas add capability gas detector.
 *
 */
 
metadata {
	definition (name: "Homi/Heiman Gas Detector", namespace: "lgkahn", author: "jrhbcn/lgkahn") {
		
        capability "Configuration"
        capability "Smoke Detector"
        capability "Sensor"
        capability "Refresh"
        capability "Gas Detector"
        
        //command "enrollResponse"
        attribute "zoneType", "string"
        attribute "enrollment", "string"
        attribute "manufacture", "string"
        attribute "model", "string"
        attribute "lastUpdate", "string"
        //attribute "gas", "string"  
      
		fingerprint profileID: "0104", deviceID: "0402", inClusters: "0000,0003,0500,0009", outClusters: "0019"
    	fingerprint profileID: "0104", deviceID: "0402", inClusters: "0000,0003,0500,0B05", outClusters: "0019"
        fingerprint profileID: "0104", deviceID: "12",   inClusters: "0000,0003,0500,0009", outClusters: "0003,0019"  
        fingerprint profileID: "0104", deviceID: "176",  inClusters: "0000,0003,0500,0B05", outClusters: "0019"
    }  
 
	
   
  preferences {
    input(name: "checkInTime", type: "Integer", title: "Check in Time?", description: "Check in Every how many Seconds (range 120-86400 (1 day)) ?", range: "60-86400", defaultValue: 1800, required: true);
    input(name: "debug", type: "bool", title: "Debugging ?", defaultValue: false, required: true)
   
  }
}
 
def parse(String description) {
	if (settings.debug) log.debug "description: $description"
    
    def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
      sendEvent(name: "lastUpdate", value: now)  
    
	Map map = [:]
    
	if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
	}
	else if (description?.startsWith('read attr -')) {
		map = parseReportAttributeMessage(description)
	}
    else if (description?.startsWith('zone status')) {
    	map = parseIasMessage(description)
    }
 
	//log.debug "Parse returned $map"
	def result = map ? createEvent(map) : null
    //log.debug "result = $result"
    
    if (description?.startsWith('enroll request')) {
    	List cmds = enrollResponse()
        log.debug "enroll response: ${cmds}"
        result = cmds?.collect { new hubitat.device.HubAction(it) }
    }
    return result
}
 
private Map parseCatchAllMessage(String description) {
    Map resultMap = [:]
    def cluster = zigbee.parse(description)
    if (shouldProcessMessage(cluster)) {
        log.debug "Parse $cluster"
    }

    return resultMap
}

private boolean shouldProcessMessage(cluster) {
    // 0x0B is default response indicating message got through
    // 0x07 is bind message
    boolean ignoredMessage = cluster.profileId != 0x0104 || 
        cluster.command == 0x0B ||
        cluster.command == 0x07 ||
        (cluster.data.size() > 0 && cluster.data.first() == 0x3e)
    return !ignoredMessage
}

 
private Map parseReportAttributeMessage(String description) {
 
    // lgk new
 
     descMap = zigbee.parseDescriptionAsMap(description)
    
      if ((descMap?.cluster == "0500" && descMap.attrInt == 0x0001) && (descMap.value == '002B'))
        {  //Zone Type
                log.debug "Zone Type is Gas Sensor"
                sendEvent(name: "zoneType", value: "Gas Sensor")
        }
       else if ((descMap?.cluster == "0500" && descMap.attrInt == 0x0000) && (descMap.value == '01'))
        {  //Zone State
                log.debug "Zone State is enrolled"
                sendEvent(name: "enrollment", value: "Enrolled")
        }
       else if ((descMap?.cluster == "0500" && descMap.attrInt == 0x0002) && ((descMap.value == '0030') || (descMap.value == '0020')))
        {  //Zone Status
                log.debug "${device.displayName} is clear"
                sendEvent(name: "smoke", value: "clear")
                sendEvent(name: "naturalGas", value: "clear")
        }
       else if (descMap?.cluster == "0000" && descMap.attrInt == 0x0004)
        {  //Manufacture
            log.debug "Manuf: ${descMap.value}"
            sendEvent(name: "manufacture", value: descMap.value)
        }
       else if (descMap?.cluster == "0000" && descMap.attrInt == 0x0005)
        {  //Model 
            log.debug "Model: ${descMap.value}"
            sendEvent(name: "model", value: descMap.value)
        } 
      else if ((descMap?.cluster == "0502" && descMap.attrInt == 0x0000))
        {  //Alarm Max Duration
                def int alarmMinutes = Integer.parseInt(descMap.value,16) 
                log.debug "Max Alarm Duration is ${alarmMinutes} seconds"   
        }
		
}

def refresh() {
    
	if (settings.debug) log.debug "Refreshing..."
	def refreshCmds = []
    
    refreshCmds +=
	zigbee.readAttribute(0x0500, 1) +	// IAS ZoneType
    zigbee.readAttribute(0x0500, 0) +	// IAS ZoneState
    zigbee.readAttribute(0x0500, 2) +	// IAS ZoneStatus
   // zigbee.readAttribute(0x0502, 0x0000) +	   // Alarm Max Duration   // this doesnt work for this driver  
   // zigbee.readAttribute(0x0000, 7) +	// power source power is irrelevant always ac
   // zigbee.readAttribute(0x0009, 7) +	// not sure what this cluster do yet
    zigbee.readAttribute(0x0000, 4) +	// manufacture
    zigbee.readAttribute(0x0000, 5) //	// model indentification
    
	return refreshCmds
}

private Map parseIasMessage(String description) {
    List parsedMsg = description.split(' ')
    String msgCode = parsedMsg[2]
    
    Map resultMap = [:]
    switch(msgCode) {
        case '0x0020': // Clear
            log.debug "Alert: Gas Cleared!"
        	resultMap = getSmokeResult('clear')
            break
   
        case '0x0030': // Clear // 30 is checkin
            log.info "Checkin"
        	resultMap = getCheckInResult('clear')
            break

        case '0x0021': // ??
            break
        
        case '0x0022': // Smoke
            log.debug "Alert: Gas Detected!"
        	resultMap = getSmokeResult('detected')
            break

        case '0x0023': // ??
            break

        case '0x0024': // ??
            break

        case '0x0025': // ??
            break

        case '0x0026': // Trouble/Failure
            break

        case '0x0028': // Test Mode
            break
    }
    return resultMap
}

private Map getCheckInResult(value) {
    
	if (settings.debug) log.debug 'Checkin/Status'
    
	def linkText = getLinkText(device)
	def descriptionText = "${linkText} has checked In: $value"
    def gasmap =  [
			name			: 'naturalGas',
			value			: value,
			descriptionText : descriptionText
	]
    
    sendEvent gasmap
    
    return [
		name: 'smoke',
		value: value,
		descriptionText: descriptionText
	]
}

private Map getSmokeResult(value) {
	
    if (settings.debug) log.debug 'Gas Status'
    
	def linkText = getLinkText(device)
	def descriptionText = "${linkText} is $value"
    
 def gasmap =  [
			name			: 'naturalGas',
			value			: value,
			descriptionText : descriptionText
	]
    
    sendEvent gasmap
    
    
	return [
		name: 'smoke',
		value: value,
		descriptionText: descriptionText
	]
}

def updated()
{
    configure()
}

def configure() {
    
      unschedule()
      def String ctime = settings.checkInTime  
      def Integer ctimeint = ctime.toInteger()     

    if (ctimeint < 120)
    {
        log.debug "Reset checkIn/Reporting Time to 120"      
        device.updateSetting("checkInTime", [type: "integer", value: 120]);
        ctimeint = 120
        ctime = ctimeint.toString()
    }
     
    log.info "Check in time: $ctime secs."
     
    def debug = settings.debug 
    if (debug)
    log.debug "In Configure"
    
    if (debug)
    {
       log.info "Debugging is on , will automatically turn off in 30 minutes."
       runIn(1800, logDebugOff);   
    }
    else log.info "Debugging is off."
    

	String zigbeeId = swapEndianHex(device.zigbeeId)
	if (debug) log.debug "Confuguring Reporting, IAS CIE, and Bindings."
    
	def configCmds = [
		"zcl global write 0x500 0x10 0xf0 {${zigbeeId}}", "delay 200",
		//"send 0x${device.deviceNetworkId} 1 ${endpointId}", "delay 1500", endpoint is only 1 so this is null and already set below
        
        "raw 0x500 {01 23 00 00 00}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 1", "delay 1500",    
	]

    configCmds +=  zigbee.configureReporting(0x0500, 0x0002, 0x19, 0, ctimeint, 0x00) // try 2 min
  
   if (debug) log.debug "configure: Write IAS CIE -- $configCmds"
    
     // schedule refresh
    runIn(2,"refresh")
        
    return configCmds // send refresh cmds as part of config
}

def enrollResponse() {
	if (settings.debug) log.debug "Sending enroll response"
    [	
    	
	"raw 0x500 {01 23 00 00 00}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 ${endpointId}"
        
    ]
}
private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}

void logDebugOff() {
  //
  // runIn() callback to disable "Debug" logging after 30 minutes
  // Cannot be private
  //
  log.info "Turning off debugging."
    
  device.updateSetting("debug", [type: "bool", value: false]);
}

        
