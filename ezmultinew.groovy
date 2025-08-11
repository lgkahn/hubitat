// Express Controls EZMultiPli Multi-sensor
// Motion Sensor - Temperature - Light level - 8 Color Indicator LED - Z-Wave Range Extender - Wall Powered
// driver for SmartThings
// The EZMultiPli is also known as the HSM200 from HomeSeer.com
//
// 2016-10-06 - erocm1231 - Added updated method to run when configuration options are changed. Depending on model of unit, luminance is being
//              reported as a relative percentace or as a lux value. Added the option to configure this in the handler.
// 2016-01-28 - erocm1231 - Changed the configuration method to use scaledConfiguration so that it properly formatted negative numbers.
//              Also, added configurationGet and a configurationReport method so that config values can be verified.
// 2015-12-04 - erocm1231 - added range value to preferences as suggested by @Dela-Rick.
// 2015-11-26 - erocm1231 - Fixed null condition error when adding as a new device.
// 2015-11-24 - erocm1231 - Added refresh command. Made a few changes to how the handler maps colors to the LEDs. Fixed
//              the device not having its on/off status updated when colors are changed.
// 2015-11-23 - erocm1231 - Changed the look to match SmartThings v2 devices.
// 2015-11-21 - erocm1231 - Made code much more efficient. Also made it compatible when setColor is passed a hex value.
//              Mapping of special colors: Soft White - Default - Yellow, White - Concentrate - White,
//              Daylight - Energize - Teal, Warm White - Relax - Yellow
// 2015-11-19 - erocm1231 - Fixed a couple incorrect colors, changed setColor to be more compatible with other apps
// 2015-11-18 - erocm1231 - Added to setColor for compatibility with Smart Lighting
// v0.1.0 - DrZWave - chose better icons, Got color LED to work - first fully functional version
// v0.0.9 - jrs - got the temp and luminance to work. Motion works. Debugging the color wheel.
// v0.0.8 - DrZWave 2/25/2015 - change the color control to be tiles since there are only 8 colors.
// v0.0.7 - jrs - 02/23/2015 - Jim Sulin
// lgk v1.0 add debug and info logging options and auto turn off for debugging
// lgk v 1.1 add state variabled to cache saturation and hue and implement the sethue and setsaturation function,
// also add the initialize fx
// lgk v 2.2 8/11/25 change setcolor using command classes instead of raw should work in zwave js

metadata {
        definition (name: "EZmultiPli new", namespace: "erocm123", author: "Eric Maycock", oauth: true) {
        capability "Motion Sensor"
        capability "Temperature Measurement"
        capability "Illuminance Measurement"
        capability "Switch"
        capability "Color Control"
        capability "Configuration"
        capability "Refresh"
        capability "Health Check"
            
        attribute "hue", "Number"
        attribute "saturation", "Number"
        attribute "level", "number"

        fingerprint mfr: "001E", prod: "0004", model: "0001", deviceJoinName: "EZmultiPli"

        fingerprint deviceId: "0x0701", inClusters: "0x5E, 0x71, 0x31, 0x33, 0x72, 0x86, 0x59, 0x85, 0x70, 0x77, 0x5A, 0x7A, 0x73, 0xEF, 0x20"
        fingerprint deviceId: "0x0701", inClusters: "0x5E, 0x71, 0x31, 0x33, 0x26, 0x72, 0x86, 0x59, 0x85, 0x70, 0x77, 0x5A, 0x7A, 0x73, 0x9F, 0x55, 0x6C"

        } // end definition

 
       preferences {
       input "OnTime",  "number", title: "No Motion Interval", description: "N minutes lights stay on after no motion detected [0, 1-127]", range: "0..127", defaultValue: 10, displayDuringSetup: true, required: false
       input "OnLevel", "number", title: "Dimmer Onlevel", description: "Dimmer OnLevel for associated node 2 lights [-1, 0, 1-99]", range: "-1..99", defaultValue: -1, displayDuringSetup: true, required: false
       input "LiteMin", "number", title: "Luminance Report Frequency", description: "Luminance report sent every N minutes [0-127]", range: "0..127", defaultValue: 10, displayDuringSetup: true, required: false
       input "TempMin", "number", title: "Temperature Report Frequency", description: "Temperature report sent every N minutes [0-127]", range: "0..127", defaultValue: 10, displayDuringSetup: true, required: false
       input "TempAdj", "number", title: "Temperature Calibration", description: "Adjust temperature up/down N tenths of a degree F [(-127)-(+128)]", range: "-127..128", defaultValue: 0, displayDuringSetup: true, required: false
       input("lum", "enum", title:"Illuminance Measurement", description: "Percent or Lux", defaultValue: 1 ,required: false, displayDuringSetup: true, options:
          [1:"Percent",
           2:"Lux"])
       input name: "debugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
       input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true)
  }

} // end metadata


// Parse incoming device messages from device to generate events
def parse(String description){
        def result = []
        def cmd = zwave.parse(description, [0x31: 5]) // 0x31=SensorMultilevel which we force to be version 5
        if (cmd) {
                result << createEvent(zwaveEvent(cmd))
        }

    def statusTextmsg = ""
    if (device.currentState('temperature') != null && device.currentState('illuminance') != null) {
                statusTextmsg = "${device.currentState('temperature').value} °F - ${device.currentState('illuminance').value} ${(lum == "" || lum == null || lum == 1) ? "%" : "LUX"}"
        sendEvent("name":"statusText", "value":statusTextmsg, displayed:false)
        }
    if (result != [null]) if (debugEnable) log.debug "Parse returned ${result}"


        return result
}

def initialize()
{
    log.info "initialize"
    
    state.color = "#00ff00"
    state.hue = 70
    state.Saturation = 100
}

// Event Generation
def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd){
        def map = [:]
        switch (cmd.sensorType) {
                case 0x01:                              // SENSOR_TYPE_TEMPERATURE_VERSION_1
                        def cmdScale = cmd.scale == 1 ? "F" : "C"
                        map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
                        map.unit = getTemperatureScale()
                        map.name = "temperature"
                        if ((debugEnable) || (descLog)) log.info "Temperature report: temp is $map.value"
                        break;
                case 0x03 :                             // SENSOR_TYPE_LUMINANCE_VERSION_1
                        map.value = cmd.scaledSensorValue.toInteger().toString()
            if(lum == "" || lum == null || lum == 1) map.unit = "%"
            else map.unit = "lux"
                        map.name = "illuminance"
                       if ((debugEnable) || (descLog)) log.info "Luminance report: lux is $map.value"
                        break;
        } 
        return map
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
    if (debugEnable) log.debug "${device.displayName} parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd.configurationValue}'"
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
        def map = [:]
        if (cmd.notificationType==0x07) {       // NOTIFICATION_TYPE_BURGLAR
                if (cmd.event==0x07 || cmd.event==0x08) {
                        map.name = "motion"
                map.value = "active"
                        map.descriptionText = "$device.displayName motion detected"
                if ((debugEnable) || (descLog)) log.info "motion recognized"
                } else if (cmd.event==0) {
                        map.name = "motion"
                map.value = "inactive"
                        map.descriptionText = "$device.displayName no motion detected"
                if ((debugEnable) || (descLog)) log.info "No motion recognized"
                }
        }
        if (map.name != "motion") {
                if (debugEnable) log.debug "unmatched parameters for cmd: ${cmd.toString()}}"
        }
        return map
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    [name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def updated()
{
    initialize()
    if (debugEnable) log.debug "updated() is being called"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    
    if (debugEnable)
    {
       log.info "Debugging is on , will automatically turn off in 30 minutes."
       runIn(1800, logDebugOff);   
    }
    else log.info "Debugging is off."
    
    
    def cmds = configure()

    if (cmds != []) commands(cmds)
}

def on() {
        if ((debugEnable) || (descLog)) log.info "Turning Light 'on'"
        commands([
                zwave.basicV1.basicSet(value: 0xFF),
                zwave.basicV1.basicGet()
        ], 500)
}

def off() {
        if ((debugEnable) || (descLog)) log.info "Turning Light 'off'"
        commands([
                zwave.basicV1.basicSet(value: 0x00),
                zwave.basicV1.basicGet()
        ], 500)
}


def setSaturation(value)
{
    if ((debugEnable) || (descLog)) log.info "setSaturation() : ${value}"
    
    if ((value < 0) || (value > 100))
     log.error "Saturation Value must be between 0 and 100!"
    
    else
    {
        
     //lgk implement that but getting last hue and passing to set color
     def newvalue = [:]
                    
     newvalue.saturation = value
     newvalue.hue = state.hue
     newvalue.level = state.level
     setColor(newvalue)
    }
}
         
def setHue(value)
{
    if ((debugEnable) || (descLog)) log.info "setHue() : ${value}"
    
    if ((value < 0) || (value > 100))
     log.error "Hue Value must be between 0 and 100!"
    
    else
    {
        
     //lgk implement that but getting last saturation and passing to set color
     def newvalue = [:]
                    
     newvalue.hue = value
     newvalue.saturation = state.saturation
     newvalue.level = state.level
        
     log.debug "new value = ${newvalue}"
     setColor(newvalue)
    }
               
}

def setColor(value) {
    if ((debugEnable) || (descLog)) log.info "setColor() : ${value}"
    def myred
    def mygreen
    def myblue
    def hexValue
    def cmds2 = []

    if ( value.level == 1 && value.saturation > 20) {
		def rgb = huesatToRGB(value.hue as Integer, 100)
        myred = rgb[0] >=128 ? 255 : 0
        mygreen = rgb[1] >=128 ? 255 : 0
        myblue = rgb[2] >=128 ? 255 : 0
    } 
    else if ( value.level > 1 ) {
		def rgb = huesatToRGB(value.hue as Integer, value.saturation as Integer)
        myred = rgb[0] >=128 ? 255 : 0
        mygreen = rgb[1] >=128 ? 255 : 0
        myblue = rgb[2] >=128 ? 255 : 0
    } 
    else if (value.hex) {
		def rgb = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
        myred = rgb[0] >=128 ? 255 : 0
        mygreen = rgb[1] >=128 ? 255 : 0
        myblue = rgb[2] >=128 ? 255 : 0
    }
    else {
        myred=value.red >=128 ? 255 : 0
        mygreen=value.green >=128 ? 255 : 0
        myblue=value.blue>=128 ? 255 : 0
    }
    if ((debugEnable) || (descLog)) log.info "red = $myred, green = $mygreen, blue= $myblue"
    
    cmds2 << zwave.basicV1.basicSet(value: 0x00)
    cmds2 << zwave.basicV1.basicGet()
      
    sendEvent(name:"hue", value: value.hue)
  
    sendEvent(name:"saturation", value: value.saturation)
    sendEvent(name:"level", value: value.level)
    state.hue = value.hue
    state.saturation = value.saturation
    state.level = value.level           

    hexValue = rgbToHex([r:myred, g:mygreen, b:myblue])
    if(hexValue) sendEvent(name: "color", value: hexValue, displayed: true) 
    state.color = hexValue
    
    cmds2 << zwave.switchColorV3.switchColorSet(red: myred, green: mygreen, blue: myblue).format() 
    commands(cmds2)
}

def zwaveEvent(hubitat.zwave.Command cmd) {
        // Handles all Z-Wave commands we aren't interested in
    if (debugEnable) log.debug cmd
        [:]
}

// ensure we are passing acceptable param values for LiteMin & TempMin configs
def checkLiteTempInput(value) {
        if (value == null) {
        value=60
    }
    def liteTempVal = value.toInteger()
    switch (liteTempVal) {
      case { it < 0 }:
        return 60                       // bad value, set to default
        break
      case { it > 127 }:
        return 127                      // bad value, greater then MAX, set to MAX
        break
      default:
        return liteTempVal      // acceptable value
    }
}

// ensure we are passing acceptable param value for OnTime config
def checkOnTimeInput(value) {
        if (value == null) {
        value=10
    }
    def onTimeVal = value.toInteger()
    switch (onTimeVal) {
      case { it < 0 }:
        return 10                       // bad value set to default
        break
      case { it > 127 }:
        return 127                      // bad value, greater then MAX, set to MAX
        break
      default:
        return onTimeVal        // acceptable value
    }
}

// ensure we are passing acceptable param value for OnLevel config
def checkOnLevelInput(value) {
        if (value == null) {
        value=99
    }
    def onLevelVal = value.toInteger()
    switch (onLevelVal) {
      case { it < -1 }:
        return -1                       // bad value set to default
        break
      case { it > 99 }:
        return 99                       // bad value, greater then MAX, set to MAX
        break
      default:
        return onLevelVal       // acceptable value
    }
}


// ensure we are passing an acceptable param value for TempAdj configs
def checkTempAdjInput(value) {
    log.debug "in temp adjus value = $value"
    
        if (value == null) {
        value=0
    }
        def tempAdjVal = value.toInteger()
    switch (tempAdjVal) {
      case { it < -127 }:
        return 0                        // bad value, set to default
        break
      case { it > 128 }:
        return 128                      // bad value, greater then MAX, set to MAX
        break
      default:
        return tempAdjVal       // acceptable value
    }
}

def refresh() {
    def cmds = []
    cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType:1, scale:1)
    cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType:3, scale:1)
    cmds << zwave.basicV1.basicGet()
    return commands(cmds, 1000)
}

def ping() {
    if (debugEnable) log.debug "ping()"
        refresh()
}

def configure() {
        if (debugEnable) log.debug "OnTime=${settings.OnTime} OnLevel=${settings.OnLevel} TempAdj=${settings.TempAdj}"
        def cmds = ([
                zwave.configurationV1.configurationSet(parameterNumber: 1, size: 1, scaledConfigurationValue: checkOnTimeInput(settings.OnTime)),
                zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: checkOnLevelInput(settings.OnLevel)),
                zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: checkLiteTempInput(settings.LiteMin)),
                zwave.configurationV1.configurationSet(parameterNumber: 4, size: 1, scaledConfigurationValue: checkLiteTempInput(settings.TempMin)),
                zwave.configurationV1.configurationSet(parameterNumber: 5, size: 1, scaledConfigurationValue: checkTempAdjInput(settings.TempAdj)),
                zwave.configurationV1.configurationGet(parameterNumber: 1),
                zwave.configurationV1.configurationGet(parameterNumber: 2),
                zwave.configurationV1.configurationGet(parameterNumber: 3),
                zwave.configurationV1.configurationGet(parameterNumber: 4),
                zwave.configurationV1.configurationGet(parameterNumber: 5)
        ])
        //if (debugEnable) log.debug cmds
        return commands(cmds)
}


private command(hubitat.zwave.Command cmd) {
  //  log.debug "in command command = $cmd"
    if (getDataValue("zwaveSecurePairingComplete") != "true") {
        return cmd.format()
    }
    Short S2 = getDataValue("S2")?.toInteger()
    //log.debug "s2 = $s2"
    String encap = ""
    String keyUsed = "S0"
    if (S2 == null) { //S0 existing device
        encap = "988100"
    } else if ((S2 & 0x04) == 0x04) { //S2_ACCESS_CONTROL
        keyUsed = "S2_ACCESS_CONTROL"
        encap = "9F0304"
    } else if ((S2 & 0x02) == 0x02) { //S2_AUTHENTICATED
        keyUsed = "S2_AUTHENTICATED"
        encap = "9F0302"
    } else if ((S2 & 0x01) == 0x01) { //S2_UNAUTHENTICATED
        keyUsed = "S2_UNAUTHENTICATED"
        encap = "9F0301"
    } else if ((S2 & 0x80) == 0x80) { //S0 on C7
        encap = "988100"
    }
    return "${encap}${cmd.format()}"
}

private command(String cmd) {
   // log.debug "in cmd 2 = $cmd"
    if (cmd)
    {
      //  log.debug "got cmd"
        def cmds = cmd.split(" ")
        if (cmds[0] == "delay")
        {
         // log.debug "skipping delay"
          return
        }
    }
   
    
    if (getDataValue("zwaveSecurePairingComplete") != "true") {
        return cmd
    }
    Short S2 = getDataValue("S2")?.toInteger()
   // log.debug " s2 = $s2"
    String encap = ""
    String keyUsed = "S0"
    if (S2 == null) { //S0 existing device
        encap = "988100"
    } else if ((S2 & 0x04) == 0x04) { //S2_ACCESS_CONTROL
        keyUsed = "S2_ACCESS_CONTROL"
        encap = "9F0304"
    } else if ((S2 & 0x02) == 0x02) { //S2_AUTHENTICATED
        keyUsed = "S2_AUTHENTICATED"
        encap = "9F0302"
    } else if ((S2 & 0x01) == 0x01) { //S2_UNAUTHENTICATED
        keyUsed = "S2_UNAUTHENTICATED"
        encap = "9F0301"
    } else if ((S2 & 0x80) == 0x80) { //S0 on C7
        encap = "988100"
    }
    return "${encap}${cmd}"
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd){
    hubitat.zwave.Command encapCmd = cmd.encapsulatedCommand(commandClassVersions)
    if (encapCmd) {
        zwaveEvent(encapCmd)
    }
    sendHubCommand(new hubitat.device.HubAction(command(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)), hubitat.device.Protocol.ZWAVE))
}

private commands(commands, delay=1000) {
   // log.debug "incommands"
   // delayBetween(commands.collect{ command(it) }, delay)
     delayBetween(commands.collect{ command(it) })
    
}

def huesatToRGB(float hue, float sat) {
        while(hue >= 100) hue -= 100
        int h = (int)(hue / 100 * 6)
        float f = hue / 100 * 6 - h
        int p = Math.round(255 * (1 - (sat / 100)))
        int q = Math.round(255 * (1 - (sat / 100) * f))
        int t = Math.round(255 * (1 - (sat / 100) * (1 - f)))
        switch (h) {
                case 0: return [255, t, p]
                case 1: return [q, 255, p]
                case 2: return [p, 255, t]
                case 3: return [p, q, 255]
                case 4: return [t, p, 255]
                case 5: return [255, p, q]
        }
}
def rgbToHex(rgb) {
    def r = hex(rgb.r)
    def g = hex(rgb.g)
    def b = hex(rgb.b)
    def hexColor = "#${r}${g}${b}"

    hexColor
}
private hex(value, width=2) {
        def s = new BigInteger(Math.round(value).toString()).toString(16)
        while (s.size() < width) {
                s = "0" + s
        }
        s
}


void logDebugOff() {
  //
  // runIn() callback to disable "Debug" logging after 30 minutes
  // Cannot be private
  //
  log.info "Turning off debugging."
    
  device.updateSetting("debugEnable", [type: "bool", value: false]);
}
