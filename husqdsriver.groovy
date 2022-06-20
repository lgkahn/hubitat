/**
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *  Modified June 16, 2022
 * lgk add lastupdatE, Readable next run time and statixtics. june 2022
* lgk add error description for current and last error as well and latitude and longitude so we can map position.
 */
//file:noinspection unused

static String getVersionNum()		{ return "00.00.02" }
static String getVersionLabel() 	{ return "Husqvarna AutoMower, version ${getVersionNum()}" }

import groovy.transform.Field
import java.text.SimpleDateFormat

@Field static final String sNULL	= (String)null
@Field static final String sBLANK	= ''
@Field static final String sSPACE	= ' '
@Field static final String sCLRRED	= 'red'
@Field static final String sCLRGRY	= 'gray'
@Field static final String sCLRORG	= 'orange'
@Field static final String sLINEBR	= '<br>'

metadata {
	definition (
		name:			"Husqvarna AutoMower",
		namespace:		"imnotbob",
		author:			"imnot_bob",
		importUrl:		"https://raw.githubusercontent.com/imnotbob/AutoMower/master/mower-device.groovy"
	)
	{
		capability "Actuator"
		capability "Sensor"
		capability "Refresh"
		capability "Motion Sensor"
		capability "Battery"
		capability "Power Source"

		attribute 'mowerStatus',		'string'
			//MAIN_AREA, SECONDARY_AREA, HOME, DEMO, UNKNOWN
		attribute 'mower]',	'string'
			//UNKNOWN, NOT_APPLICABLE, MOWING, GOING_HOME, CHARGING, LEAVING, PARKED_IN_CS, STOPPED_IN_GARDEN
		attribute 'mowerState',	'string'
			//UNKNOWN, NOT_APPLICABLE, PAUSED, IN_OPERATION, WAIT_UPDATING, WAIT_POWER_UP, RESTRICTED,
			// OFF, STOPPED, ERROR, FATAL_ERROR, ERROR_AT_POWER_UP
		attribute 'mowerConnected',	'string' // TRUE or FALSE
		attribute 'mowerTimeStamp',	'string' // LAST TIME connected (EPOCH LONG)
		//attribute 'battery'		'NUMBER' // Battery %
		attribute 'errorCode',		'string' // current error code
		attribute 'errorTimeStamp',	'NUMBER' // (EPOCH LONG)
		attribute 'plannerNextStart',	'NUMBER' // (EPOCH LONG)
		attribute 'plannerOverride',	'string' // Override Action
		attribute 'name', 'string'
		attribute 'model', 'string'
		attribute 'serialNumber', 'string'
        attribute 'mowerActivity', 'string'

		attribute 'cuttingHeight',	'NUMBER' // (level)
		attribute 'headlight',	'string' // ALWAYS_ON, ALWAYS_OFF, EVENING_ONLY, EVENING_AND_NIGHT

		attribute 'apiConnected',		'string'
		attribute 'debugEventFromParent',	'STRING'		// Read only
		attribute 'debugLevel', 		'NUMBER'		// Read only - changed in preferences
		attribute 'lastPoll', 			'string'
        
        // deductions
		//attribute 'motion'		'ENUM' // active, inactive
		//attribute 'powerSource'	'ENUM' // "battery", "dc", "mains", "unknown"
		attribute 'stuck',	'STRING' // TRUE or FALSE
		attribute 'parked',	'STRING' // TRUE or FALSE
		attribute 'hold',	'STRING' // TRUE or FALSE
		attribute 'holdUntilNext',	'string' // TRUE or FALSE
		attribute 'holdIndefinite',	'string' // TRUE or FALSE
        
        // lgk additions
        attribute "nextRun", "string"
        attribute 'lastUpdate', 'string'
        attribute 'cuttineBladeUsageTime', 'number'
        attribute 'numberOfChargingCycles', 'number'
        attribute 'numberOfCollisions', 'number'
        attribute 'totalChargingTime', 'number'
        attribute 'totalCuttingTime', 'number'
        attribute 'totalRunningTime', 'number'
        attribute 'totalSearchingTime', 'number'
       // attribute 'cuttingBladeUsageTime', 'number'm // not using this attr lgk.. even though specified in husq api it is always 0
        attribute 'lastErrorDesc' , 'string'
        attribute 'lastErrorTime', 'string'
        attribute 'currentErrorDesc', 'string'
        attribute 'currentErrorTime', 'string'
       
        attribute 'latitude' , 'number'
        attribute 'longitude', 'number'
           
		command "start",		 		[[name: 'Duration*', type: 'NUMBER', description: 'Minutes']] // duration
		command "pause", 				[]
		command "parkuntilnext",		[] // until next schedule
		command "parkindefinite", 		[] // park until further notice
		command "park",					[[name: 'Duration*', type: 'NUMBER', description: 'Minutes']] // duration in minutes
		command "resumeSchedule", 		[]
		command "setCuttingHeight",		[[name: 'Height*', type: 'NUMBER', description: 'Level']]
		command "setHeadlightMode",		[[name: 'Mode*', type: 'ENUM', description: 'Mode', constraints: ["ALWAYS_ON", "ALWAYS_OFF", "EVENING_ONLY", "EVENING_AND_NIGHT"]]] // mode
		command "setSchedule",			[[name: 'taskList*', type: 'STRING', description: 'Task List']]
        command "clearError"
	}

	preferences {
		input(name: "dummy", type: "text", title: "${getVersionLabel()}", description: " ", required: false)
	}
}


def clearError()
{
    sendEvent(name: "errorCode", value: "N/A", descriptionText: "")
    sendEvent(name: "errorTimeStamp", value: "0")  
    sendEvent(name: "lastErrorDesc", value: "No Error") 
    sendEvent(name: "lastErrorTime", value: "N/A") 
    sendEvent(name: "currenterrorDesc", value: "No Error") 
    sendEvent(name: "currentErrorTime", value: "N/A")
}
    
// parse events into attributes
def parse(String description) {
	LOG("parse() --> Parsing ${description}", 4, sTRACE)
}

def refresh(Boolean force=false) {
	// No longer require forcePoll on every refresh - just get whatever has changed
	LOG("refresh() - calling pollChildren ${force?(forced):sBLANK}, deviceId = ${getDeviceId()}",2,sINFO)
	parent.pollFromChild(getDeviceId(), force) // tell parent to just poll me silently -- can't pass child/this for some reason
}

void doRefresh() {
	// Pressing refresh within 6 seconds of the prior refresh completing will force a complete poll - otherwise changes only
	refresh(state.lastDoRefresh?((now()-state.lastDoRefresh)<6000):false)
	state.lastDoRefresh = now()	// reset the timer after the UI has been updated
}

def forceRefresh() {
	refresh(true)
}

def installed() {
	LOG("${device.label} being installed",2,sINFO)
	if (device.label?.contains('TestingForInstall')) return	// we're just going to be deleted in a second...
	updated()
}

def uninstalled() {
	LOG("${device.label} being uninstalled",2,sINFO)
}

def updated() {
	LOG("${getVersionLabel()} updated",1,sTRACE)

	if (device.displayName.contains('TestingForInstall')) { return }

	state.version = getVersionLabel()
	updateDataValue("myVersion", getVersionLabel())
	runIn(2, 'forceRefresh', [overwrite: true])
}

def poll() {
	LOG("Executing 'poll' using parent App", 2, sINFO)
	parent.pollFromChild(getDeviceId(), false) // tell parent to just poll me silently -- can't pass child/this for some reason
}

def generateEvent(Map updates) {
	//log.error "generateEvent(Map): ${updates}"
	generateEvent([updates])
}

def generateEvent(List<Map<String,Object>> updates) {
	//log.debug "updates: $updates}"

	String myVersion = getDataValue("myVersion")
	if (!myVersion || (myVersion != getVersionLabel())) updated()
	String msgH="generateEvent() | "
	Long startMS = now()
	Boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG(msgH+"parsing data ${updates}",4, sTRACE)
	//LOG("Debug level of parent: ${getParentSetting('debugLevel')}", 4, sDEBUG)
//	String linkText = device.displayName
	Boolean forceChange = false

	Integer objectsUpdated = 0

	if(updates) {
		updates.each { Map<String,Object> update ->
			update.each { String name, value ->
				String sendValue = value.toString()
				Boolean isChange = isStateChange(device, name, sendValue)
				if(isChange) {
					//def eventFront = [name: name, linkText: linkText, handlerName: name]
					Map eventFront = [name: name ]
					objectsUpdated++
					Map event
					if (debugLevelFour) LOG(msgH+"processing object #${objectsUpdated} name: ${name} value: "+sendValue, 5, sTRACE)
					event = eventFront + [value: sendValue]

					switch (name) {
						case 'forced':
							forceChange = (sendValue == 'true')
							break

						case 'id':
							state.id = sendValue
							event = null
							break

						case 'lastPoll':
							if (debugLevelFour) event = eventFront + [value: sendValue, descriptionText: "Poll: " + sendValue ]
							else event = null
							break

						case 'apiConnected':
							// only display in the devices' log if we are in debug level 4 or 5
							if (forceChange) event = eventFront + [value: sendValue, descriptionText: "API Connection is ${value}" ]
							break

						case 'debugEventFromParent':
						case 'appdebug':
							event = eventFront + [value: sendValue ] //, descriptionText: "-> ${sendValue}" ]
							Integer ix = sendValue.lastIndexOf(" ")
							String msg = sendValue.substring(0, ix)
							String type = sendValue.substring(ix + 1).replaceAll("[()]", "")
							switch (type) {
								case sERROR:
									LOG(msg,1,sERROR)
									break
								case sTRACE:
									LOG(msg,1,sTRACE)
									break
								case sINFO:
									LOG(msg,1,sINFO)
									break
								case sWARN:
									LOG(msg,1,sWARN)
									break
								default:
									LOG(msg,1,sDEBUG)
							}
							break

						case 'debugLevel':
							String sendText = (sendValue && (sendValue != 'null') && (sendValue != "")) ? sendValue : 'null'
							updateDataValue('debugLevel', sendText)
							event = eventFront + [value: sendText, descriptionText: "debugLevel is ${sendValue}" ]
							break

						default:
							String desc = name + " is " + sendValue
							if (name.endsWith("TimeStamp") || name.endsWith("NextStart")) {
								if(sendValue != sNULL && sendValue != 'null' && sendValue != "0"){
									Long t = sendValue.toLong()
									Long n = now()
									if (name.endsWith("NextStart")) {
										t -= location.timeZone.getOffset(n) + Math.round(
												(Integer)location.timeZone.getOffset(t)-(Integer)location.timeZone.getOffset(n)*1.0D )

									}
									Date aa = new Date(t)
									desc = name + " is " + formatDt(aa)
								}
							}
							event = eventFront + [value: sendValue, descriptionText: desc ]
							break
					}
					if (event) {
						if (debugLevelFour) LOG(msgH+"calling sendevent(${event})", 4, sTRACE)
						sendEvent(event)
					}
				} else LOG(msgH+"${name} did not change", 5, sTRACE)
			}
		}
	} else LOG(msgH+'NO UPDATES')
	Long elapsed = now() - startMS
	LOG(msgH+"Updated ${objectsUpdated} object${objectsUpdated!=1?'s':''} (${elapsed}ms)", 4, sINFO)
}

// ***************************************************************************
// commands
// API calls and UI handling
// ***************************************************************************
void start(mins) {
	if(mins) {
		LOG("start($mins)", 3, sTRACE)
		Map foo = [data:[type:'Start',attributes:[duration:mins]]]
		if(parent.sendCmdToHusqvarna((String)state.id, foo)) {
			LOG("start($mins) sent",4, sTRACE)
		}
	}
	else LOG("start($mins) no minutes specified",1, sERROR)
}

void pause(){
	LOG("pause",3, sTRACE)
	Map foo = [data:[type:'Pause']]
	if(parent.sendCmdToHusqvarna((String)state.id, foo)) {
		LOG("pause sent",4, sTRACE)
	}
}

void parkuntilnext() {
	LOG("parkuntilnext()", 3, sTRACE)
	Map foo = [data:[type:'ParkUntilNextSchedule']]
	if(parent.sendCmdToHusqvarna((String)state.id, foo)) {
		LOG("parkuntilnext() sent",4, sTRACE)
	}
}

void parkindefinite() {
	LOG("parkindefinite()",3,sTRACE)
	log.warn "state.id: ${state.id} getDeviceId(): ${getDeviceId()}"
	Map foo = [data:[type:'ParkUntilFurtherNotice']]
	if(parent.sendCmdToHusqvarna((String)state.id, foo)) {
		LOG("parkindefinite() sent",4, sTRACE)
	}
}

void park(mins) {
	if(mins) {
		LOG("park($mins)",3,sTRACE)
		Map foo = [data:[type:'Park',attributes:[duration:mins]]]
		if(parent.sendCmdToHusqvarna((String)state.id, foo)) {
			LOG("park($mins) sent",4, sTRACE)
		}
	} else LOG("start($mins) no minutes specified",1, sERROR)
}

void resumeSchedule() {
	LOG("resumeSchedule",3,sTRACE)
	Map foo = [data:[type:'ResumeSchedule']]
	if(parent.sendCmdToHusqvarna((String)state.id, foo)) {
		LOG("resumeSchedule() sent",4, sTRACE)
	}
}

void setCuttingHeight(level) {
	if(level) {
		LOG("setCuttingHeight($level)", 3, sTRACE)
		Map foo = [data:[type:'settings',attributes:[cuttingHeight:level]]]
		if(parent.sendSettingToHusqvarna((String)state.id, foo)) {
			LOG("setCuttingHeight($level) sent",4, sTRACE)
		}
	}
	else LOG("setCuttingHeight($level) no level specified",1, sERROR)
}

void setHeadlightMode(mode) {
	if(mode) {
		LOG("setHeadlight($mode)", 3, sTRACE)
		Map foo = [data:[type:'settings',attributes:[headlight:[mode:mode]]]]
		if(parent.sendSettingToHusqvarna((String)state.id, foo)) {
			LOG("setHeadlight($mode) sent",4, sTRACE)
		}
	}
	else LOG("setHeadlight($mode) no mode specified",1, sERROR)
}

void setSchedule(taskList) {
	if(taskList != null) {
		LOG("setSchedule($taskList)", 3, sTRACE)
		Map foo = [data:[type:'calendar', attributes:[tasks:taskList]]]
		if(parent.sendScheduleToHusqvarna((String)state.id, foo)) {
			LOG("setSchedule($taskList)",4, sTRACE)
		}
	}
	else LOG("setSchedule missing input parameter(s)",1, sERROR)
}

void off() {
	LOG('off()', 4, sTRACE)
	parkindefinite()
}

void on() {
	LOG('on()', 4, sTRACE)
	resumeSchedule()
}

String getDtNow() {
	Date now=new Date()
	return formatDt(now)
}

String formatDt(Date dt, Boolean tzChg=true) {
	SimpleDateFormat tf=new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
	if(tzChg) { if(location.timeZone) { tf.setTimeZone((TimeZone)location.timeZone) } }
	return (String)tf.format(dt)
}

String getDeviceId() {
	String deviceId = ((String)device.deviceNetworkId).split(/\./).last()
	LOG("getDeviceId() returning ${deviceId}", 4)
	return deviceId
}

static String span(String str, String clr=sNULL, String sz=sNULL, Boolean bld=false, Boolean br=false){ return str ? "<span ${(clr || sz || bld) ? "style='${clr ? "color: ${clr};" : sBLANK}${sz ? "font-size: ${sz};" : sBLANK}${bld ? "font-weight: bold;" : sBLANK}'" : sBLANK}>${str}</span>${br ? sLINEBR : sBLANK}" : sBLANK }

Integer getIDebugLevel(){
	return (getDataValue('debugLevel') ?: (device.currentValue('debugLevel') ?: (getParentSetting('debugLevel') ?: 3))) as Integer
}

Boolean debugLevel(Integer level=3){
	return (getIDebugLevel() >= level)
}

void LOG(message, Integer level=3, String ilogType=sDEBUG, Exception ex=null, Boolean event=false, Boolean displayEvent=false) {
	String logType,prefix

	logType=ilogType
	if(logType == sNULL) logType = sDEBUG
	prefix = sBLANK

	Integer dbgLevel = getIDebugLevel()
	if(logType == sERROR){
		String a = getDtNow() // getTimestamp()
		state.lastLOGerror = "${message} @ "+a
		state.LastLOGerrorDate = a
	} else {
		if (level > dbgLevel) return	// let's not waste CPU cycles if we don't have to...
	}

	if(!lLOGTYPES.contains(logType)){
		logerror("LOG() - Received logType (${logType}) which is not in the list of allowed types ${lLOGTYPES}, message: ${message}, level: ${level}")
		logType = sDEBUG
	}

	if( dbgLevel == 5 ){ prefix = 'LOG: ' }
	"log${logType}"("${prefix}${message}", ex)
	if (event) { debugEvent(message+" (${logType})", displayEvent) }
}

private void logdebug(String msg, Exception ex=null){ log.debug logPrefix(msg, "purple") }
private void loginfo(String msg, Exception ex=null){ log.info sSPACE + logPrefix(msg, "#0299b1") }
private void logtrace(String msg, Exception ex=null){ log.trace logPrefix(msg, sCLRGRY) }
private void logwarn(String msg, Exception ex=null){ logexception(msg,ex,sWARN, sCLRORG) }
void logerror(String msg, Exception ex=null){ logexception(msg,ex,sERROR, sCLRRED) }

void logexception(String msg, Exception ex=null, String typ, String clr) {
	String msg1 = ex ? " Exception: ${ex}" : sBLANK
	log."$typ" logPrefix(msg+msg1, clr)
	String a
	try {
		if(ex) a = getExceptionMessageWithLine(ex)
	} catch (ignored){ }
	if(a) log."$typ" logPrefix(a, clr)
}

static String logPrefix(String msg, String color = sNULL){
	return span("AutoMower Device (v" + getVersionNum() + ") | ", sCLRGRY) + span(msg, color)
}

void debugEvent(message, Boolean displayEvent = false) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent,
		isStateChange: true
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent(results)
}

def getParentSetting(String settingName) {
	return parent?."${settingName}"
}

// lgk handle last and current errors.
def handleErrors(String currentErrorDesc, String currentErrorTime)
{
     log.debug "in handle error $currentErrorDesc $currentErrorTime"
      def led = device.getDataValue('lastErrorDesc')  
      if (led == null)
       {
        led = "No Error"
        sendEvent(name: "lastErrorDesc", value: led) 
       }
    unset('cuttingBladeUsagetime')

      log.debug "got last error string = $led"
    
     // only set last for real error and do not rewrite for no error.. that only changes current once cleared.
     if ((currentErrorDesc != led) & (currentErrorDesc != "No Error"))
      {
        log.warn "New Error: $currentErrorDesc"
        sendEvent(name: "lastErrorDesc", value: currentErrorDesc)
        sendEvent(name: "lastErrorTime", value: currentErrorTime)
      }
    
    sendEvent(name: "currentErrorDesc", value: currentErrorDesc)
    sendEvent(name: "currentErrorTime", value: currentErrorTime)
    
}
             

@Field static final List<String> lLOGTYPES =			['error', 'debug', 'info', 'trace', 'warn']

@Field static final String sDEBUG		= 'debug'
@Field static final String sERROR		= 'error'
@Field static final String sINFO		= 'info'
@Field static final String sTRACE		= 'trace'
@Field static final String sWARN		= 'warn'


