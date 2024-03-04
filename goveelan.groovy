// Hubitat driver for Govee Manual LAN API device setup
// Version 1.0.20
//
// 9-12-22  Initial release
// 9-30-22  Resolve issue with polling, Enhance Debug Logging
// 10-5-22  Adding Lan Control options to driver. Additional Logging.
// 10-12-22 Updated text verbiage on preferences
// 11-3-22  Send Rate Limits to Parent app and adjust to work with limited devices.
// 11-4-22  Added methods for Lan control to have proper fade control. 
// 11-5-22  Updated to reflect on state when options other 'On' switch are used
// 11-22-22 Updated to validate successful api call before changing driver status.
// 12-19-22 Modifieid polling to properly allow a value of 0 for no polling
// 1-21-23  Changed position of setlLevl action in setColor command.
// 1-30-23  Added check to see if device is in Retry state and abort new commands until cleared.
// 4-4-23   Added ability for parent app to update API Key associated with device
// 4-7-23   Added reset of Cloud API State to getDeviceStatus and initialize routine
// lgk 4/24 add lastupdated , add desclog to briefly see what was set, also add logic to get basic colors from hue.

import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def commandPort() { "4003" }

metadata {
	definition(name: "Govee Manual LAN API Device", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "Actuator"
		capability "ColorControl"
		capability "ColorTemperature"
		capability "Light"
		capability "SwitchLevel"
		capability "ColorMode"
		capability "Refresh"
        capability "Initialize"
        capability "LightEffects"
		
		attribute "colorName", "string"
        attribute "cloudAPI", "string"
        attribute "effectNum", "integer" 
        attribute "lastUpdate", "string"
        attribute "colorName", "string"
        
        command "activateDIY", [
            [name: "diyName", type: "STRING", description: "DIY Number to activate"]
           ]
    }

	preferences {		
		section("Device Info") {
			input(name: "aRngBright", type: "bool", title: "Alternate Brightness Range", description: "For devices that expect a brightness range of 0-254", defaultValue: false)
            input("fadeInc", "decimal", title: "% Change each Increment of fade", defaultValue: 1)
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true)
		}
		
	}
}

def on() {
        sendCommandLan(GoveeCommandBuilder("turn",1, "turn"))
        sendEvent(name: "switch", value: "on")
        def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
        sendEvent(name: "lastUpdate", value: now)
        if (descLog) log.info "${device.label} was turned on."
    
}

def off() {
        sendCommandLan(GoveeCommandBuilder("turn",0, "turn"))
        sendEvent(name: "switch", value: "off")
        def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
        sendEvent(name: "lastUpdate", value: now)
        if (descLog) log.info "${device.label} was turned off."
     
}

def setColorTemperature(value,level = null,transitionTime = null)
{
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) { log.debug "setColorTemperature(): ${value}"}
    
    if (value < device.getDataValue("ctMin").toInteger()) { value = device.getDataValue("ctMin")}
    if (value > device.getDataValue("ctMax").toInteger()) { value = device.getDataValue("ctMax")}
    
    if (descLog) log.info "${device.label} Color Temp was set to. $value"
    if (debugLog) { log.debug "setColorTemperature(): ColorTemp = " + value }
	int intvalue = value.toInteger()
        sendCommandLan(GoveeCommandBuilder("colorwc",value, "ct"))
        if (level != null) setLevel(level,transitionTime);
        sendEvent(name: "colorTemperature", value: intvalue)
        sendEvent(name: "switch", value: "on")
        sendEvent(name: "colorMode", value: "CT")
        if (effectNum != 0){
            sendEvent(name: "effectNum", value: 0)
            sendEvent(name: "effectName", value: "None") 
        }
	    setCTColorName(intvalue)
}   



def setCTColorName(int value)
{
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "lastUpdate", value: now)
     
		if (value < 2600) {
			sendEvent(name: "colorName", value: "Warm White")
		}
		else if (value < 3500) {
			sendEvent(name: "colorName", value: "Incandescent")
		}
		else if (value < 4500) {
			sendEvent(name: "colorName", value: "White")
		}
		else if (value < 5500) {
			sendEvent(name: "colorName", value: "Daylight")
		}
		else if (value >=  5500) {
			sendEvent(name: "colorName", value: "Cool White")
		}  	
}
    
def setColor(value) {
  
    def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
    sendEvent(name: "lastUpdate", value: now)
     
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) { log.debug "setColor(): HSBColor = "+ value + "${device.currentValue("level")}"}
 
 
	if (value instanceof Map) {
        
        def s = value.containsKey("saturation") ? value.saturation : null
        def b = value.containsKey("level") ? value.level : null
		def h = value.containsKey("hue") ? value.hue : null
       
        // lgk get color
        def theColor = getColor(h,s)
        if (descLog)
        {
            if (theColor == "Unknown")
            {
                if (debugLog) log.debug "trying alt. color name method"
                theColor = convertHueToGenericColorName(h,s)
                if (debugLog) log.debug "alt. method got back $theColor"
            }
            if (theColor != "Unknown") log.info "${device.label} Color is $theColor"
            else log.info "${device.label} Color is $value"
            sendEvent(name: "colorName", value: theColor)
        }
        
		
        if (b == null) { b = device.currentValue("level") }
		setHsb(h, s, b)
	} else {
        if (debugLog) {log.debug "setColor(): Invalid argument for setColor: ${value}"}
    }
}

def setHsb(h,s,b)
{
   
    def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
    sendEvent(name: "lastUpdate", value: now)
    
	hsbcmd = [h,s,b]
    if (debugLog) { log.debug "setHsb(): Cmd = ${hsbcmd}"}
     
	rgb = hubitat.helper.ColorUtils.hsvToRGB(hsbcmd)
	def rgbmap = [:]
	rgbmap.r = rgb[0]
	rgbmap.g = rgb[1]
	rgbmap.b = rgb[2]   
     
        if (debugLog) { log.debug "setHsb(): ${rgbmap}"}
       
        sendCommandLan(GoveeCommandBuilder("colorwc",rgbmap,"rgb"))
      	sendEvent(name: "hue", value: "${h}")
        sendEvent(name: "saturation", value: "${s}")
        sendEvent(name: "switch", value: "on")
   		sendEvent(name: "colorMode", value: "RGB")
        if (effectNum != 0){
            sendEvent(name: "effectNum", value: 0)
            sendEvent(name: "effectName", value: "None") 
        }
    if(100 != device.currentValue("level")?.toInteger()) {
    setLevel(100)
    }
}

def setHue(h)
{
    setHsb(h,device.currentValue( "saturation" )?:100,device.currentValue("level")?:100)
    if (descLog) log.info "${device.label} Hue was set to $h"
}

def setSaturation(s)
{
	setHsb(device.currentValue("hue")?:0,s,device.currentValue("level")?:100)
    if (descLog) log.info "${device.label} Saturation was set to ${s}%"
}

def setLevel(float v,duration = 0){
    
    def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
    sendEvent(name: "lastUpdate", value: now) 
    
    int intv = v.toInteger()
    if (descLog) log.info "${device.label} Level was set to ${intv}%"
    
    if (duration>0){
        int intduration = duration.toInteger()
        sendEvent(name: "switch", value: "on")
        fade(intv,intduration)
    }
    else {
        if (device.currentValue("cloudAPI") == "Retry") {
            log.error "setLevel(): CloudAPI already in retry state. Aborting call." 
        } else {
        setLevel2(intv)
        }
    }
}

def setLevel2(int v){
    
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "lastUpdate", value: now)
    
    if (descLog) log.info "${device.label} Saturation was set to ${v}%"
    
        sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
        sendEvent(name: "level", value: v)
        sendEvent(name: "switch", value: "on")
}

def fade(int v,float duration){
      
    def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
    sendEvent(name: "lastUpdate", value: now)
    
    unschedule(fadeUp)
    unschedule(fadeDown)
    int curLevel = device.currentValue("level")
    if (v < curLevel){
    float fadeRep = (curLevel-v)/fadeInc
    float fadeInt = (duration*1000)/fadeRep
    fadeDown(curLevel, v, fadeRep, fadeInt)
        }
    else if (v > curLevel){
    float fadeRep = (v-curLevel)/fadeInc
    float fadeInt = (duration*1000)/fadeRep
    fadeUp(curLevel, v, fadeRep, fadeInt)
        }
    else {
        if (debugLog) {log.debug "fade(): Level is not changing"}
    }
}

def fadeDown( int curLevel, int level, fadeInt, fadeRep) {
    if (debugLog) {log.debug "fadeDown(): curLevel ${curLevel}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
    int v = (curLevel-fadeInc).toInteger()
    log.debug "fadeDown(): v ${v}"
    if ( v == 0 ) {
        log.debug "fadeDown(): Next fade is to 0 turning off device. Fade is complete"
        off()
    } else if (level==v) {
            if (debugLog) {log.debug "Final Loop"}
            sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
            sendEvent(name: "level", value: v)
    } else {
            sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
            sendEvent(name: "level", value: v)
            if (debugLog) {log.debug "fadeDown(): continueing  fading to ${v}"}
            def int delay = fadeRep
            if (debugLog) {log.debug "fadeDown(): delay ia ${delay}"}
            if (debugLog) {log.debug "fadeDown(): executing loop to fadedown() with values curLevel ${v}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
            runInMillis(delay, fadeDown, [data:[v ,level, fadeInt,fadeRep]])
    }
} 

def fadeUp( int curLevel, int level, fadeInt, fadeRep) {
    if (debugLog) {log.debug "fadeUp(): curLevel ${curLevel}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
    int v= (curLevel+fadeInc).toInteger()
    log.debug "fadeUp(): v ${v}"
    if (level==v)    {
        if (debugLog) {log.debug "Final Loop"}
        sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
        sendEvent(name: "level", value: v)
    }
    else {
        if (debugLog) {log.debug "fadeUp(): continueing  fading to ${v}"}
        def int delay= fadeRep
        if (debugLog) {log.debug "fadeUp(): delay ia ${delay}"}
        if (debugLog) {log.debug "fadeUp(): executing loop to fadeup() with values curLevel ${v}, level ${level}, fadeInt ${fadeInt}, fadeRep ${fadeRep}"}
        sendCommandLan(GoveeCommandBuilder("brightness",v, "level"))
        sendEvent(name: "level", value: v)
        runInMillis(delay, fadeUp, [data:[v ,level, fadeInt,fadeRep]])
    }
} 


//Turn Hubitat's 0-100 Brightness range to the 0-254 expected by some devices
def incBrightnessRange(v)
{
	v=v*(254/100)
	return Math.round(v)
}


//Go from 0-254 brightness range from some devices to Hubitat's 0-100 Brightness range. Maybe not needed?
def decBrightnessRange(v)
{
	v=v*(100/254)
	return Math.round(v)
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh - not implemented."}
    if (debugLog) runIn(1800, logsOff)
    log.warn "${device.label} Refresh not implemented"
    log.info "${device.label} IP is ${getIPString()}"
}

def updated() {
    if (debugLog) {log.warn "updated(): Driver Udated"}
    configure()
}

def configure() {
    if (debugLog) {log.warn "configure(): Configuration Changed"}
    if ( parent.atomicState."${"lightEffect_"+(device.getDataValue("DevType"))}" == null ) {
    if (debugLog) {log.warn "configure(): No Scenes present for device type. Initiate setup in parent app"}
        parent.lightEffectSetup()
        retrieveScenes()
        } else {
        retrieveScenes()
        }
    if (debugLog) runIn(1800, logsOff) 
    log.info "${device.label} IP is ${getIPString()}"
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}    
    if (debugLog) runIn(1800, logsOff)
}


def installed(){
    if (debugLog) {log.warn "installed(): Driver Installed"}
        sendEvent(name: "hue", value: 0)
        sendEvent(name: "saturation", value: 100)
        sendEvent(name: "level", value: 100) 
        getDevType()
        parent.lightEffectSetup()
        retrieveScenes()
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

def GoveeCommandBuilder(String command1, value1, String type) {   
    if (type=="ct") {
        if (debugLog) {log.debug "GoveeCommandBuilder(): Color temp action"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            color {
            r 0
            g 0
            b 0
        }
            colorTemInKelvin value1}
    }
    def  command = cmd1.toString()
        if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
   else if (type=="rgb") {
       if (debugLog) {log.debug "GoveeCommandBuilder(): rgb"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            color {
            r value1.r
            g value1.g
            b value1.b
                
        }
            colorTemInKelvin 0}
    }
    def  command = cmd1.toString()
       if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
       else if (type=="status") {
           if (debugLog) {log.debug "GoveeCommandBuilder():status"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            }
    }
    def  command = cmd1.toString()
           if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
    else { 
        if (debugLog) {log.debug "GoveeCommandBuilder():other action"}
    JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            value value1}
        }
    def  command = cmd1.toString()
        if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command
}
}

def sendCommandLan(String cmd) {
  def addr = getIPString();
    if (debugLog) {log.debug ("sendCommandLan(): ${cmd}")}

  pkt = new hubitat.device.HubAction(cmd,
                     hubitat.device.Protocol.LAN,
                     [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                     ignoreResponse    : false,
                     callback: parse,
                     parseWarning: true,
                     destinationAddress: addr])  
  try {    
      if (debugLog) {log.debug("sendCommandLan(): ${pkt} to ip ${addr}")}
    sendHubCommand(pkt) 
      
  }
  catch (Exception e) {      
      logDebug e
  }      
}

def getIPString() {
   return device.getDataValue("IP")+":"+commandPort()
}


def parse(message) {  
  log.error "Got something to parseUDP"
  log.error "UDP Response -> ${message}"    
}



def  setEffect(effectNo) {
    
     def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
     sendEvent(name: "lastUpdate", value: now)
    
    
    effectNumber = effectNo.toString()
    
    if (descLog) log.info "${device.label} SetEffect: $effectNumber"
    
    if ((parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}" != null) && (parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".containsKey(effectNumber))) {
        String sceneInfo =  parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".get(effectNumber).name
        String sceneCmd =  parent.state."${"lightEffect_"+(device.getDataValue("DevType"))}".get(effectNumber).cmd
        if (debugLog) {log.debug ("setEffect(): Activate effect number ${effectNo} called ${sceneInfo} with command ${sceneCmd}")}
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
//    if (debugLog) {log.debug ("setEffect(): setEffect to ${effectNo}")}
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("setEffect(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
   } else {
        sendEvent(name: "effectNum", value: effectNumber)
        sendEvent(name: "switch", value: "on")
    // Cozy Light Effect (static Scene to very warm light)
    if (effectNo == 6) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Cozy Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        sendEvent(name: "colorTemperature", value: 2000)
	    setCTColorName(2000)
        sendEvent(name: "effectName", value: "Cozy")
    }
    // Sunrise Effect
    if (effectNo == 9) {
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",1, "level"))
        sendEvent(name: "level", value: 1)
        fade(100,1800)        
        sendEvent(name: "effectName", value: "Sunrise")
    }
    // Sunset Effect
    if (effectNo == 10) {
        sendCommandLan(GoveeCommandBuilder("colorwc",6500, "ct"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        fade(0,1800)
        sendEvent(name: "effectName", value: "Sunset")
    }
    // Warm White Light Effect (static Scene to very warm light)
    if (effectNo == 11) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Warm White Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",3500, "ct"))
        sendEvent(name: "colorTemperature", value: 3500)
	    setCTColorName(3500)
        sendEvent(name: "effectName", value: "Warm White")
    } 
    // Daylight Light Effect    
    if (effectNo == 12) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Daylight Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",5600, "ct"))
        sendEvent(name: "colorTemperature", value: 5600)
	    setCTColorName(5600)
        sendEvent(name: "effectName", value: "Daylight")
    }
    // Cool White Light Effect    
    if (effectNo == 13) {
        if (debugLog) {log.debug ("setEffect(): Static Scene Cool White Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",6500, "ct"))
        sendEvent(name: "colorTemperature", value: 6500)
	    setCTColorName(6500)
        sendEvent(name: "effectName", value: "Cool White")
    }  
    // Night Light Effect   
    if (effectNo == 14) {
        if (debugLog) {log.debug ("setEffect(): Static Night Light Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",2000, "ct"))
        sendEvent(name: "colorTemperature", value: 2000)
	    setCTColorName(2000)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",5, "level"))
        sendEvent(name: "level", value: 5)
        sendEvent(name: "effectName", value: "Night Light")
    }
    // Focus Effect   
    if (effectNo == 15) {
        if (debugLog) {log.debug ("setEffect(): Focus Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",4500, "ct"))
        sendEvent(name: "colorTemperature", value: 4500)
	    setCTColorName(4500)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        sendEvent(name: "effectName", value: "Focus")
    } 
    // Relax Effect   
    if (effectNo == 16) {
        if (debugLog) {log.debug ("setEffect(): Static Relax Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:255, g:194, b:194], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "level", value: 100)
        sendEvent(name: "effectName", value: "Relax")
    }
    // True Color Effect   
    if (effectNo == 17) {
        if (debugLog) {log.debug ("setEffect(): True Color Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc",3350, "ct"))
        sendEvent(name: "colorTemperature", value: 3350)
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",100, "level"))
        sendEvent(name: "effectName", value: "True Color")
    }
    // TV Time Effect   
    if (effectNo == 18) {
        if (debugLog) {log.debug ("setEffect(): Static TV Time Effect Called. Calling CT Command directly")}        
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:179, g:134, b:254], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",45, "level"))
        sendEvent(name: "level", value: 45)
        sendEvent(name: "effectName", value: "TV Time")
    }
    // Plant Growth Effect   
    if (effectNo == 19) {
        if (debugLog) {log.debug ("setEffect(): Static Plant Growth Effect Called. Calling CT Command directly")}
        sendCommandLan(GoveeCommandBuilder("colorwc", [r:247, g:154, b:254], "rgb"))
        pauseExecution(750)
        sendCommandLan(GoveeCommandBuilder("brightness",45, "level"))
        sendEvent(name: "level", value: 45)
        sendEvent(name: "effectName", value: "Plant Growth")
    }
    }     
}

def setNextEffect() {
if (debugLog) {log.debug ("setNextEffect(): Current Color mode ${device.currentValue("colorMode")}")}
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) {log.debug ("setNextEffect(): current effectNum ${device.currentValue("effectNum")}")}
    if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == device.getDataValue("maxScene")) {
        setEffect(1)
    } 
    else if (device.currentValue("effectNum") == "21") {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        setEffect(101) 
    } else {
        if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
        int nextEffect = device.currentValue("effectNum").toInteger()+1
        setEffect(nextEffect)
        }  
}
      
def setPreviousEffect() {
if (debugLog) {log.debug ("setNextEffect(): Current Color mode ${device.currentValue("colorMode")}")}
    unschedule(fadeUp)
    unschedule(fadeDown)
      if (device.currentValue("effectNum") == "0" || device.currentValue("effectNum") == "1") {
        setEffect(device.getDataValue("maxScene"))
        } else if (device.currentValue("effectNum") == 101) {
         setEffect(21) 
        } else {
            if (debugLog) {log.debug ("setNextEffect(): Increment to next scene}")}
            int prevEffect = device.currentValue("effectNum").toInteger()-1
            setEffect(prevEffect)
        }  
}


def activateDIY(diyActivate) {
        String diyEffectNumber = diyActivate.toString()
        String sceneInfo = parent.state.diyEffects.(device.getDataValue("deviceModel")).get(diyEffectNumber).name
        String sceneCmd = parent.state.diyEffects.(device.getDataValue("deviceModel")).get(diyEffectNumber).cmd 
        if (debugLog) {log.debug ("activateDIY(): Activate effect number ${diyActivate} called ${sceneInfo} with command ${sceneCmd}")}
        sendEvent(name: "effectName", value: sceneInfo)
        sendEvent(name: "effectNum", value: diyEffectNumber)
        sendEvent(name: "switch", value: "on")
        String cmd2 = '{"msg":{"cmd":"ptReal","data":{"command":'+sceneCmd+'}}}'
        if (debugLog) {log.debug ("activateDIY(): command to be sent to ${cmd2}")}
        sendCommandLan(cmd2)
}

def retrieveScenes() {
    state.scenes = [] as List
    state.diyEffects = [] as List
    if (debugLog) {log.debug ("retrieveScenes(): Retrieving Scenes from parent app")}
    if (debugLog) {log.debug ("retrieveScenes(): DIY Keyset ${parent.state.diyEffects.keySet()}")}
    if (parent.state.diyEffects.containsKey((device.getDataValue("deviceModel"))) == false) {
        if (debugLog) {log.debug ("retrieveScenes(): No DIY Scenes to retrieve for device")}    
    } else {
        parent.state.diyEffects.(device.getDataValue("deviceModel")).each {
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            String sceneValue = it.getKey() + "=" + it.value.name
            state.diyEffects.add(sceneValue)
            state.diyEffects = state.diyEffects.sort()
        }
    }
    if ( parent.atomicState."${"lightEffect_"+(device.getDataValue("DevType"))}" == null ) {
        if (debugLog) {log.debug ("retrieveScenes(): No Scenes to retrieve for device")}    
    } else { 
        parent.atomicState."${"lightEffect_"+(device.getDataValue("DevType"))}".each {
            if (it.getKey() == "999") {
                if (debugLog) {log.debug ("retrieveScenes(): Processing max scene value ${it.getKey()} of ${it.value.maxScene}")}
                device.updateDataValue("maxScene", it.value.maxScene.toString())
            } else {
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.getKey()}")}
            if (debugLog) {log.debug ("retrieveScenes(): Show all scenes in application ${it.value.name}")}
            String sceneValue = it.getKey() + "=" + it.value.name
            state.scenes.add(sceneValue)
            state.scenes = state.scenes.sort()
            }
        }
    }
}

def getDevType() {
//    String state.DevType = null= 
    switch(device.getDataValue("deviceModel")) {
        case "H6117":
        case "H6163":
        case "H6168":
        case "H6172":
        case "H6173":
        case "H6175":
        case "H6176":
        case "H618A":
        case "H618B":
        case "H618C":
        case "H618E":
        case "H618F":
        case "H619A":
        case "H619B": 
        case "H619C": 
        case "H619D":
        case "H619E":
        case "H619Z":
        case "H61A0":
        case "H61A1":
        case "H61A2":
        case "H61A3":
        case "H61A5":
        case "H61A8":
        case "H61A9":
        case "H61B2":
        case "H61C2": 
        case "H61C3":
        case "H61C5":
        case "H61E1":
        case "H61E0":
            if (debugLog) {log.debug ("getDevType(): Found   ${device.getDataValue("deviceModel")} setting DevType to RGBIC_STRIP")}; 
            device.updateDataValue("DevType", "RGBIC_Strip");
            break; 
        case "H6066": 
        case "H606A":
        case "H6061":
            if (debugLog) {log.debug ("getDevType(): Found   ${device.getDataValue("deviceModel")} setting DevType to Hexa_Light")};
            device.updateDataValue("DevType", "Hexa_Light");            
            break;
        case "H6067": //Not added yet
            device.updateDataValue("DevType", "Tri_Light"); 
            break;
        case "H6065":
            device.updateDataValue("DevType", "Y_Light");
            break;        
        case "H6072": 
        case "H6079":
            device.updateDataValue("DevType", "Lyra_Lamp");
            break;
        case "H6076":
            device.updateDataValue("DevType", "Basic_Lamp");
            break;
        case "H6078":
            device.updateDataValue("DevType", "Cylinder_Lamp");
            break;        
        case "H6052": 
        case "H6051":
            device.updateDataValue("DevType", "Table_Lamp");
            break;
        case "H70C1":
        case "H70C2":
        case "H70CB":
            device.updateDataValue("DevType", "XMAS_Light");
            break;
        case "H610A": 
        case "H610B": 
        case "H6062":
            device.updateDataValue("DevType", "Wall_Light_Bar");
            break;
        case "H6046":
        case "H6056":
        case "H6047":
        case "H70CB":
            device.updateDataValue("DevType", "TV_Light_Bar"); 
            break;        
        case "H6088":
        case "H6087":
        case "H608A": 
        case "H608B":
        case "H608C":
            device.updateDataValue("DevType", "Indoor_Pod_Lights");
            break;        
        case "H705A":
        case "H705B":
        case "H705C":
        case "H706A":
        case "H706B":
        case "H706C":
            device.updateDataValue("DevType", "Outdoor_Perm_Light");
            break;
        case "H7050":
        case "H7051":
        case "H7055":
            device.updateDataValue("DevType", "Outdoor_Pod_Light");
            break;
        case "H7060":
        case "H7061":
        case "H7062":
        case "H7065":
            device.updateDataValue("DevType", "Outdoor_Flood_Light");
            break;
        case "H70B1":
            device.updateDataValue("DevType", "Curtain_Light");
            break;
        case "H7020":
        case "H7021":
        case "H7028":
        case "H7041":
        case "H7042":
            device.updateDataValue("DevType", "Outdoor_String_Light");
            break;
        default: 
            if (debugLog) {log.debug ("getDevType(): Unknown device Type. Model: (${device.getDataValue("deviceModel")}) Defaulting to RGBIC Strip.")}; 
            device.updateDataValue("DevType", "RGBIC_Strip");    
        break; 
        
    }       
}

def getColor(hue, saturation)
{
   
    def color = "Unknown"
    
   	//Set the hue and saturation for the specified color.
	switch(hue) {
		case 0:
            if (saturation == 0)
              color = "White"
            else color = "Red"
		    break;
        case 53:
            color = "Daylight"
			break;
        case 23:
  		    color = "Soft White"
			break;
        case 20:
		    color = "Warm White"
			break;
        case 61:
            color = "Navy Blue"
            break;
        case 65:
		    color = "Blue"
			break;
		case 35:
			color = "Green"
			break;
        case 47:
        	color = "Turquoise"
            break;
        case 50:
            color = "Aqua"
            break;
        case 13:
            color = "Amber"
            break;
		case 17:
        case 25:
			color = "Yellow"
			break; 
        case 7:
            color ="Safety Orange" 
            break;
		case 10:
			color = "Orange"
			break;
        case 73:
            color = "Indigo"
            break;
		case 82:
			color = "Purple"
			break;
        case 90:
        case 91:
		case 90.78:
			color = "Pink"
			break;
        case 94:
            color = "Rasberry"
            break;
        case 4:
            color = "Brick Red"
            break;  
        case 69:
            color = "Slate Blue"
            break;
    }
  return color
}
    
def apiKeyUpdate() {
    if (device.getDataValue("apiKey") != parent?.APIKey) {
        if (debugLog) {log.warn "initialize(): Lan Only Device"}
//        device.updateDataValue("apiKey", parent?.APIKey)
    }
}
