//
// Copyright (c) 2020, Denny Page
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// Version 1.0.0    Initial release
// Version 1.0.1    Fix incorrect event name when no range test has been run
// Version 1.1.0    Don't indicate range test as in progress until the device
//                  responds. Provide feedback on the count of frames received
//                  as an attribute.
// Version 1.2.0    Power level set is a temporary setting for testing and
//                  requires a timeout value.
// Version 1.2.1    Clarify that node for range test needs to be decimal
// Version 1.3.0    Use zwaveSecureEncap method introduced in Hubitat 2.2.3.
// Version 1.3.1    Mark seconds as a required input for power test
// Version 1.4.0    Normalize logging
//

metadata
{
    definition (
        name: "Aeotec Range Extender 7", namespace: "cococafe", author: "Denny Page"
    )
    {
        capability "Configuration"
        capability "Refresh"

        attribute "indicator", "string"
        attribute "powerLevel", "string"
        attribute "rangeTest", "string"
        attribute "rangeTestReceived", "string"
        attribute "lastUpdate", "string"

        command "powerTest", [[name: "seconds*", type: "NUMBER", defaultValue: "0",
                               description: "Seconds before returning to normal power"],
                              [name: "power", type: "ENUM", constraints: ["normal",
                                                          "-1dBm", "-2dBm", "-3dBm",
                                                          "-4dBm", "-5dBm", "-6dBm",
                                                          "-7dBm", "-8dBm", "-9dBm"]]]

        command "rangeTest", [[name: "node*", type: "NUMBER", description: "Node to test against (decimal)"],
                              [name: "power", type: "ENUM", constraints: ["normal",
                                                          "-1dBm", "-2dBm", "-3dBm",
                                                          "-4dBm", "-5dBm", "-6dBm",
                                                          "-7dBm", "-8dBm", "-9dBm"]]]

        fingerprint mfr:"0371", prod:"0104", deviceId:"00BD",
            inClusters: "0x5E,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A"


        // 0x55 COMMAND_CLASS_TRANSPORT_SERVICE_V2
        // 0x59 COMMAND_CLASS_ASSOCIATION_GRP_INFO
        // 0x5A COMMAND_CLASS_DEVICE_RESET_LOCALLY
        // 0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
        // 0x6C COMMAND_CLASS_SUPERVISION_V1
        // 0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
        // 0x73 COMMAND_CLASS_POWERLEVEL
        // 0x7A COMMAND_CLASS_FIRMWARE_UPDATE_MD_V2
        // 0x85 COMMAND_CLASS_ASSOCIATION_V2
        // 0x86 COMMAND_CLASS_VERSION_V3
        // 0x87 COMMAND_CLASS_INDICATOR_V3
        // 0x8E COMMAND_CLASS_MULTICHANNEL_ASSOCIATION_V3
        // 0x9F COMMAND_CLASS_SECURITY_2
    }
}

preferences
{
    input name: "indicator", title: "Indicator light",
        type: "bool", defaultValue: "0"

    input name: "testFrames", title: "Frame count for range testing",
        type: "number", defaultValue: "10", range: "1..255"

    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: true
    input name: "txtEnable", title: "Enable descriptionText logging", type: "bool", defaultValue: true
    input name: "enableScheduledRefresh", title: "Enable daily scheduled refresh", type: "bool", defaultValue: false
}

void logsOff()
{
    device.updateSetting("logEnable", [value:"false", type:"bool"])
    log.warn "debug logging disabled"
}

void installed()
{
    runIn(1800, logsOff)
}

def refresh()
{
    if (logEnable) log.debug "Refresh"
    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.indicatorV3.indicatorGet()))
    cmds.add(zwaveSecureEncap(zwave.versionV3.versionGet()))
    //cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelGet()))
    //cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelTestNodeGet()))
    delayBetween(cmds, 200)
}

def scheduledRefresh()
{
    if (logEnable) log.debug "Scheduled Refresh"
   
    if (enableScheduledRefresh)
    {   if (logEnable) log.debug "Scheduled Refresh is true, re-scheduling for one day."
        unschedule()
        runIn(86400,"scheduledRefresh")
    } 
   runIn(5,"refresh")  
}

def configure()
{
    if (logEnable) log.debug "Configure"

    Integer indicatorValue = indicator ? 0xFF : 0

    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.indicatorV3.indicatorSet(value: indicatorValue)))
    cmds.add(zwaveSecureEncap(zwave.indicatorV3.indicatorGet()))
    delayBetween(cmds, 200)
}

def updated()
{
   if (logEnable) log.debug "Updated preferences"

    log.warn "debug logging is ${logEnable}"
    log.warn "description logging is ${txtEnable}"
    if (enableScheduledRefresh)
    {
        if (logEnable) log.debug "Scheduled Refresh is true, scheduling for one day."
        unschedule()
        runIn(86400,"scheduledRefresh")
    }   
    runIn(1, configure)
    if (logEnable) runIn(1800, logsOff)
}

static String powerLevelToString(Number power)
{
    return power ? "-${power}dBm" : "normal"
}

static Integer stringToPowerLevel(String string)
{
    def match = (string =~ /-([0-9]+)dBm/)
    if (match.find()) return match.group(1).toInteger()
    return 0
}

def powerTest(Number seconds, String powerString)
{
    if (seconds < 0 || seconds > 255)
    {
        log.error "Invalid powerTest seconds ${seconds}"
        return null
    }

    def power = stringToPowerLevel(powerString)

    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelSet(powerLevel: power, timeout: seconds)))
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelGet()))
    delayBetween(cmds, 200)
}

def rangeTest(Number node, String powerString)
{
    if (node < 1)
    {
        log.error "Invalid test node ${node}"
        return null
    }

    def power = stringToPowerLevel(powerString)
    def frames = testFrames ? testFrames.toInteger() : 10

    def map = [:]
    map.name = "rangeTest"
    map.value = "pending"
    map.descriptionText = "${device.displayName}: range test pending - sending ${frames} frames to node ${node} at power level ${powerString}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"

    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelTestNodeSet(powerLevel: power,
                                                                testFrameCount: frames,
                                                                testNodeid: node)))
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelTestNodeGet()))
    delayBetween(cmds, 100)
}

def requestPowerLevel()
{
    zwaveSecureEncap(zwave.powerlevelV1.powerlevelGet())
}

def requestTestNode()
{
    zwaveSecureEncap(zwave.powerlevelV1.powerlevelTestNodeGet())
}

def parse(String description)
{
    hubitat.zwave.Command cmd = zwave.parse(description,commandClassVersions)
    if (cmd)
    {
        
       def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
       sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
        
        return zwaveEvent(cmd)
    }

    if (logEnable) log.debug "Non Z-Wave parse event: ${description}"
    return null
}

void zwaveEvent(hubitat.zwave.commands.indicatorv3.IndicatorReport cmd)
{
    if (logEnable) log.debug "IndicatorReport: ${cmd.toString()}"

    String status = cmd.value ? "on" : "off"

    def map = [:]
    map.name = "indicator"
    map.value = "${status}"
    map.descriptionText = "${device.displayName}: indicator light is ${status}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"
}

def zwaveEvent(hubitat.zwave.commands.powerlevelv1.PowerlevelReport cmd)
{
    unschedule(requestPowerLevel)

    if (logEnable) log.debug "PowerLevelReport: ${cmd.toString()}"

    power = powerLevelToString(cmd.powerLevel)
    def map = [:]
    map.name = "powerLevel"
    map.value = "${power}"
    map.descriptionText = "${device.displayName}: transmit power level is ${power}, timeout ${cmd.timeout} seconds"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"

    if (cmd.timeout)
    {
        runIn(cmd.timeout, requestPowerLevel)
    }
}

def zwaveEvent(hubitat.zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd)
{
    unschedule(requestTestNode)

    if (logEnable) log.debug "PowerLevelTestNodeReport: ${cmd.toString()}"

    // Check test validity
    if (cmd.testNodeid == 0)
    {
        sendEvent(name: "rangeTest", value: "none")
        return
    }

    def Boolean inProgress = false
    switch (cmd.statusOfOperation)
    {
        case 0:    // ZW_TEST_FAILED
            status = "failed"
            break
        case 1:    // ZW_TEST_SUCCES
            status = "succeeded"
            break
        case 2:    // ZW_TEST_INPROGRESS
            inProgress = true
            status = "in progress"
            break
    }

    def map = [:]
    map.name = "rangeTest"
    map.value = "${status}"
    map.descriptionText = "${device.displayName}: range test ${status}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"

    map.name = "rangeTestReceived"
    map.value = "${cmd.testFrameCount}"
    map.descriptionText = "${device.displayName}: received ${cmd.testFrameCount} frames from node ${cmd.testNodeid}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"

    if (inProgress)
    {
        runIn(2, requestTestNode)
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd)
{
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd)
{
    
    encapCmd = cmd.encapsulatedCommand()
    if (encapCmd)
    {
        return zwaveEvent(encapCmd)
    }

    log.warn "Unable to extract encapsulated cmd: ${cmd.toString()}"
    return null
}

def zwaveEvent(hubitat.zwave.Command cmd)
{
    if (logEnable) log.debug "Unhandled cmd: ${cmd.toString()}"
    return null
}
