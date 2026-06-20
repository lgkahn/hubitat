/*
* Copyright 2020 Taylor Vierrether
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

/*
*
* RELEASE HISTORY:
*     - 0.1.0: Initial Release
*     - 0.1.1: Fix typo introduced in cleanup of code
*     - 0.1.2: Add back in ability to enable debug messaging
*     - 0.1.3: Cleanup of cruft and a bit of debug logging added
*     - 0.1.4: Tiny bit of cleanup
*
*/

import groovy.transform.Field

//
// Driver Definition
//
metadata {
    definition(
        name: 'Aeotec Home Energy Monitor (Gen 5)',
        namespace: 'com.viertaxa.hubitat',
        author: 'Taylor Vierrether'
    ) {
        capability 'Configuration'
        capability 'Refresh'
        capability 'Initialize'

        capability 'EnergyMeter'
        capability 'PowerMeter'
        capability 'VoltageMeasurement'
        capability 'Sensor'

        command 'getConfig'
        command 'resetMeterAccumulation'

        attribute "energy", "string"
        attribute "power", "string"
        attribute "voltage", "string"
        attribute "current", "string"
        attribute "kVar", "string"
        attribute "kVarh", "string"
        attribute "energy-Clamp-1", "string"
        attribute "power-Clamp-1", "string"
        attribute "voltage-Clamp-1", "string"
        attribute "current-Clamp-1", "string"
        attribute "kVar-Clamp-1", "string"
        attribute "kVarh-Clamp-1", "string"
        attribute "energy-Clamp-2", "string"
        attribute "power-Clamp-2", "string"
        attribute "voltage-Clamp-2", "string"
        attribute "current-Clamp-2", "string"
        attribute "kVar-Clamp-2", "string"
        attribute "kVarh-Clamp-2", "string"
        attribute "lastUpdate" , "string"

        fingerprint deviceId: "95", inClusters: "0x5E,0x86,0x72,0x98,0x56", outClusters: "0x5A", mfr: "134", prod: "258", deviceJoinName: "Aeotec HEM Gen 5"
    }

    preferences {
        input name: 'enableCrc16Encap', type: 'bool', title: 'Enable CRC16 Encapsulation',
            description: 'Enable CRC16 Encapsulation. NOTE: C-7 currently is dropping CRC16 messages from this device. <em>Strongly</em> recommend leaving disabled.',
            defaultValue: false, required: true
        input name: 'enableSelectiveReport', type: 'bool', title: 'Enable Selective Reporting',
            description: 'Enable reporting only on threshold cross as defined below.',
            defaultValue: false, required: true
        input name: 'reportingMode', type: 'enum', title: 'Reporting Mode',
            description: 'Choose the type of energy reporting', defaultValue: 1,
            options: reportingModes, multiple: false, required: true

        input name: 'wattTriggerCombined', type: 'number', range: 1..60000, title: 'Wattage Report Trigger (Both Clamps Combined)',
            description: 'Enter the wattage delta that will result in a message to be sent',
            defaultValue: 50, required: true
        input name: 'wattTriggerClamp1', type: 'number', range: 1..60000, title: 'Wattage Report Trigger (Clamp 1)',
            description: 'Enter the wattage delta that will result in a message to be sent',
            defaultValue: 50, required: true
        input name: 'wattTriggerClamp2', type: 'number', range: 1..60000, title: 'Wattage Report Trigger (Clamp 2)',
            description: 'Enter the wattage delta that will result in a message to be sent',
            defaultValue: 50, required: true

        input name: 'wattPctTriggerCombined', type: 'number', range: 1..100, title: 'Wattage Percent Report Trigger (Both Clamps Combined)',
            description: 'Enter the wattage delta percentage that will result in a message to be sent',
            defaultValue: 10, required: true
        input name: 'wattPctTriggerClamp1', type: 'number', range: 1..100, title: 'Wattage Percent Report Trigger (Clamp 1)',
            description: 'Enter the wattage delta percentage that will result in a message to be sent',
            defaultValue: 10, required: true
        input name: 'wattPctTriggerClamp2', type: 'number', range: 1..100, title: 'Wattage Percent Report Trigger (Clamp 2)',
            description: 'Enter the wattage delta percentage that will result in a message to be sent',
            defaultValue: 10, required: true

        input name: 'group1ReportValues', type: 'enum', title: 'Group 1 Reports',
            description: 'Choose which values will be reported for group 1',
            defaultValue: 2, options: reportOptions, multiple: true, required: true
        input name: 'group2ReportValues', type: 'enum', title: 'Group 2 Reports',
            description: 'Choose which values will be reported for group 2',
            defaultValue: 1, options: reportOptions, multiple: true, required: true
        input name: 'group3ReportValues', type: 'enum', title: 'Group 3 Reports',
            description: 'Choose which values will be reported for group 3',
            defaultValue: 0, options: reportOptions, multiple: true, required: true

        input name: 'group1ReportInterval', type: 'number', range: 1..4294967295, title: 'Group 1 Report Interval',
            description: 'Choose how frequently values will be reported for group 1 in seconds',
            defaultValue: 5, required: true
        input name: 'group2ReportInterval', type: 'number', range: 1..4294967295, title: 'Group 2 Report Interval',
            description: 'Choose how frequently values will be reported for group 2 in seconds',
            defaultValue: 120, required: true
        input name: 'group3ReportInterval', type: 'number', range: 1..4294967295, title: 'Group 3 Report Interval',
            description: 'Choose how frequently values will be reported for group 3 in seconds',
            defaultValue: 120, required: true
        input name: 'logDebugMessages', type: 'bool', title: 'Enable Debug Logging',
            defaultValue: false, required: true
    }
}
//
// STATIC DATA
//
@Field def reportingModes = [
    0: 'Power & Energy Absolute Value',
    1: '+/- Power & Summed Energy',
    2: '+/- Power, Only Positive Energy (Consumption)',
    3: '+/- Power, Only Negative Energy (Generation)'
]

@Field def reportOptions = [
    0:         'None',
    1:         'kWh Total',
    2:         'W Total',
    4:         'V Total',
    8:         'A Total',
    16:        'kVarh Total',
    32:        'kVar Total',
    //64:      "", //Reserved
    //128:     "", //Reserved
    256:       'W Clamp 1', 
    512:       'W Clamp 2',
    //1024:    "", //Reserved
    2048:      'kWh Clamp 1',
    4096:      'kWh Clamp 2',
    //8192:    "", //Reserved
    //16384:   "", //Reserved
    //32768:   "", //Reserved
    65536:     'V Clamp 1',
    131072:    'V Clamp 2',
    //262144:  "", //Reserved
    524288:    'A Clamp 1',
    1048576:   'A Clamp 2',
    //2097152: "", //Reserved
    //4194304: '', //Reserved
    //8388608: '', //Reserved
    16777216:  'kVarh Clamp 1',
    33554432:  'kVarh Clamp 2',
    //67108864:  '', //Reserved
    134217728: 'kVar Clamp 1',
    268435456: 'kVar Clamp 2'

]

@Field def commandClasses = [
    0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO V2COMMAND_CLASS_VERSION V2
    0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC V2
    0x32: 4, // COMMAND_CLASS_METER V4
    0x56: 1, // COMMAND_CLASS_CRC_16_ENCAP V1
    0x60: 4, // COMMAND_CLASS_MULTI_CHANNEL V4
    0x8E: 3, // COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
    0x70: 1, // COMMAND_CLASS_CONFIGURATION V1
    0x59: 1, // COMMAND_CLASS_ASSOCIATION_GRP_INFO V1
    0x85: 2, // COMMAND_CLASS_ASSOCIATION V2
    0x7A: 2, // COMMAND_CLASS_FIRMWARE_UPDATE_MD  V2
    0x73: 1, // COMMAND_CLASS_POWERLEVEL V1
    0x98: 1, // COMMAND_CLASS_SECURITY V1
    //0xEF: 1, // COMMAND_CLASS_MARK V1
    0x5A: 1 // COMMAND_CLASS_DEVICE_RESET_LOCALLY V1
]

    
def logsOff()
{
    logDebugMessages = false
}

def getRefreshAllDataCommands(){
    [
    // Whole HEM
    zwave.meterV4.meterGet(scale: 0), //kWh
    zwave.meterV4.meterGet(scale: 1), //kVAh
    zwave.meterV4.meterGet(scale: 2), //W
    zwave.meterV4.meterGet(scale: 3), //Pulse count
    zwave.meterV4.meterGet(scale: 4), //V
    zwave.meterV4.meterGet(scale: 5), //A
    zwave.meterV4.meterGet(scale: 6), //Power Factor
    zwave.meterV4.meterGet(scale: 7, scale2: 0), //kVar
    zwave.meterV4.meterGet(scale: 7, scale2: 1), //kVarh

    // Clamp 1
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 0)), //kWh
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 1)), //kVAh
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 2)), //W
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 3)), //Pulse count
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 4)), //V
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 5)), //A
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 6)), //Power Factor
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 7, scale2: 0)), //kVar
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 1).encapsulate(zwave.meterV4.meterGet(scale: 7, scale2: 1)), //kVarh
    
    // Clamp 2
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 0)), //kWh
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 1)), //kVAh
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 2)), //W
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 3)), //Pulse count
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 4)), //V
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 5)), //A
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 6)), //Power Factor
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 7, scale2: 0)), //kVar
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint: 2).encapsulate(zwave.meterV4.meterGet(scale: 7, scale2: 1)) //kVarh
    ]
}

//
// Capability Methods
//
def updated() {
    logTrace "Entering Updated"
    logTrace "Calling initialze"
    initialize()
    logTrace "Calling configure"
    configure()
    logTrace "Exiting updated"
}

def initialize() {
    logTrace "Entering: initialize"
    state.clear()
    getConfig()
    logTrace "Exiting: initialize"
}

def refresh() {
    logTrace "Entering refresh"
    logTrace "Calling runCommandsWithInterstitialDelay"
    runCommandsWithInterstitialDelay refreshAllDataCommands
    logTrace "Exiting refresh"
}

def configure() {
    logTrace "Entering configure"
    logInfo "Sending configuration for ${device.label}"

    logDebug "Generating configuration command list"
    logDebug "Setting param 3, size 1, value ${enableSelectiveReport ? 1 : 0}"
    logDebug "Setting param 13, size 1, value ${enableCrc16Encap ? 1 : 0}"
    logDebug "Setting param 2, size 1, value ${reportingMode.toInteger()}"
    logDebug "Setting param 4, size 2, value ${wattTriggerCombined.toInteger()}"
    logDebug "Setting param 5, size 2, value ${wattTriggerClamp1.toInteger()}"
    logDebug "Setting param 6, size 2, value ${wattTriggerClamp2.toInteger()}"
    logDebug "Setting param 8, size 1, value ${wattPctTriggerCombined.toInteger()}"
    logDebug "Setting param 9, size 1, value ${wattPctTriggerClamp1.toInteger()}"
    logDebug "Setting param 10, size 1, value ${wattPctTriggerClamp2.toInteger()}"
    logDebug "Setting param 101, size 4, value ${group1ReportValues.collect{ it.toInteger() }.sum()}"
    logDebug "Setting param 102, size 4, value ${group2ReportValues.collect{ it.toInteger() }.sum()}"
    logDebug "Setting param 103, size 4, value ${group3ReportValues.collect{ it.toInteger() }.sum()}"
    logDebug "Setting param 111, size 4, value ${group1ReportInterval.toInteger()}"
    logDebug "Setting param 112, size 4, value ${group2ReportInterval.toInteger()}"
    logDebug "Setting param 113, size 4, value ${group3ReportInterval.toInteger()}"

    def commands = [
        
        //General Config

        // Selective Reports
        zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: enableSelectiveReport ? 1 : 0),
        //CRC16 Encapsulation
        zwave.configurationV1.configurationSet(parameterNumber: 13, size: 1, scaledConfigurationValue: enableCrc16Encap ? 1 : 0),
        //Reporting Mode
        zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: reportingMode.toInteger()),

        //Threshold Configuration

        //Trigger HEM watts with change by this value
        zwave.configurationV1.configurationSet(parameterNumber: 4, size: 2, scaledConfigurationValue: wattTriggerCombined.toInteger()),
        //Trigger clamp 1 watts with change by this value
        zwave.configurationV1.configurationSet(parameterNumber: 5, size: 2, scaledConfigurationValue: wattTriggerClamp1.toInteger()),
        //Trigger clamp 2 watts with change by this value
        zwave.configurationV1.configurationSet(parameterNumber: 6, size: 2, scaledConfigurationValue: wattTriggerClamp2.toInteger()),
        //Trigger HEM watts with change by this percent
        zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, scaledConfigurationValue: wattPctTriggerCombined.toInteger()),
        //Trigger clamp 1 watts with change by this percent
        zwave.configurationV1.configurationSet(parameterNumber: 9, size: 1, scaledConfigurationValue: wattPctTriggerClamp1.toInteger()),
        //Trigger clamp 2 watts with change by this percent
        zwave.configurationV1.configurationSet(parameterNumber: 10, size: 1, scaledConfigurationValue: wattPctTriggerClamp2.toInteger()),

        // Reporting Group Configuration

        // Which reports need to send in Report group 1.
        zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: group1ReportValues.collect{ it.toInteger() }.sum()),
        // Which reports need to send in Report group 2.
        zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: group2ReportValues.collect{ it.toInteger() }.sum()),
        // Which reports need to send in Report group 3.
        zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: group3ReportValues.collect{ it.toInteger() }.sum()),
        // Interval to send Report group 1.
        zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: group1ReportInterval.toInteger()),
        // Interval to send Report group 2.
        zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: group2ReportInterval.toInteger()),
        // Interval to send Report group 3.
        zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: group3ReportInterval.toInteger())
    ]

    runCommandsWithInterstitialDelay(commands, 300)

    runIn(10, 'getConfig')

    runIn(20, 'setAssociations')
}

def parse(String description) {
   // log.debug "in parse"
    logTrace "Entering parse()"
    logTrace description
    logTrace "Calling zwave.parse()"
    def cmd = zwave.parse(description, commandClasses)
    if (cmd) {
        logTrace "cmd parsed"
        return zwaveEvent(cmd)
    }
    else {
        logErr("Command not able to be parsed: $description")
    }
}

//
// Custom Command Methods
//
def getConfig() {
    logTrace "Entering getConfig"
    def commands = [
        zwave.configurationV1.configurationGet(parameterNumber: 2),   //Energy detection mode configuration for parameters 101~103
        zwave.configurationV1.configurationGet(parameterNumber: 3),   //enable/disable parameter selective reporting parameters 4~10
        zwave.configurationV1.configurationGet(parameterNumber: 4),   //Induce an automatic report of HEM by watts
        zwave.configurationV1.configurationGet(parameterNumber: 5),   //Induce an automatic report of Channel 1 by watts
        zwave.configurationV1.configurationGet(parameterNumber: 6),   //Induce an automatic report of Channel 2 by watts
        zwave.configurationV1.configurationGet(parameterNumber: 8),   //Induce an automatic report of HEM by percent
        zwave.configurationV1.configurationGet(parameterNumber: 9),   //Induce an automatic report of Channel 1 by percent
        zwave.configurationV1.configurationGet(parameterNumber: 10),  //Induce an automatic report of Channel 2 by percent
        zwave.configurationV1.configurationGet(parameterNumber: 13),  //Enable/disable CRC-16 Encapsulation
        zwave.configurationV1.configurationGet(parameterNumber: 101), //Report group 1 reports
        zwave.configurationV1.configurationGet(parameterNumber: 102), //Report group 2 reports
        zwave.configurationV1.configurationGet(parameterNumber: 103), //Report group 3 reports
        zwave.configurationV1.configurationGet(parameterNumber: 111), //Report group 1 frequency
        zwave.configurationV1.configurationGet(parameterNumber: 112), //Report group 2 frequency
        zwave.configurationV1.configurationGet(parameterNumber: 113), //Report group 3 frequency
        zwave.configurationV1.configurationGet(parameterNumber: 200), //Is Aeon or Third-party
        zwave.configurationV1.configurationGet(parameterNumber: 252), //Configuration locked?

        zwave.versionV2.versionGet(),
        zwave.manufacturerSpecificV2.manufacturerSpecificGet()
    ]

    logTrace "Calling runCommandsWithInterstitialDelay"
    runCommandsWithInterstitialDelay commands

    logTrace "Scheduling refreshAssociations"
    runIn 10, "refreshAssociations"
    logTrace "Exiting getConfig"
}

def resetMeterAccumulation() {
    logTrace "Entering resetMeterAccumulation"
    def commands = [
        zwave.meterV4.meterReset()
    ]
    
    logTrace "Appending refreshAllData commands"
    commands.addAll refreshAllDataCommands 

    logTrace "Calling runCommandsWithInterstitialDelay"
    runCommandsWithInterstitialDelay commands
    logTrace "Exiting resetMeterAccumulation"
}

def refreshAssociations() {
    logTrace "Entering refreshAssociations()"

    commands = [
        zwave.associationV2.associationGet(groupingIdentifier: 1),
        zwave.multiChannelAssociationV3.multiChannelAssociationGet(groupingIdentifier: 1)
    ]

    logTrace "Calling runCommandsWithInterstitialDelay"
    runCommandsWithInterstitialDelay commands
    
    logTrace "Exiting refreshAssociations"
}

def setAssociations() {
    logTrace "Entering setAssociations"

    logTrace "Iterating on associatedNodes"
    state.associatedNodes[1].each { node ->
        logTrace "Removing node: $node"
        runCommand(zwave.associationV2.associationRemove(groupingIdentifier: 1, nodeId: node.toInteger()))
        pauseExecution(1)
    }
    logTrace "Refreshing Associations"
    refreshAssociations()
    pauseExecution(5)
    logTrace "Iterating on multiChannelAssociations"
    // lgk cherck if null
    if (state.multiChannelAssociations != null)
    {
   // def size = state.multiChannelAssociations.size
    //log.debug "size = $size"
    state.multiChannelAssociations[1].each { node ->
        logTrace "Removing node: $node"
        runCommand(zwave.multiChannelAssociationV3.multiChannelAssociationRemove(groupingIdentifier: 1, nodeId: node))
        pauseExecution(1)
    }
        
    logTrace "Refreshing Associations"
    refreshAssociations()
    pauseExecution(5)
    logTrace "Iterating on multiChannelAssociationsMultiChannelNodeIDs"
    state.multiChannelAssociationsMultiChannelNodeIDs[1].each{ nodeAssociation ->
        logTrace "Removing node: $nodeAssociation"
        runCommand(zwave.multiChannelAssociationV3.multiChannelAssociationRemove(groupingIdentifier: 1, multiChannelNodeIds: nodeAssociation))
        pauseExecution(1)
    }
    
    }
    else
        {
            log.warn "Multi Channel Associations is null - ignoring!"
        }
        

    logTrace "Refreshing Associations"
    refreshAssociations()
    logTrace "Adding Hub back in"
    runCommand(zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: 1))
    pauseExecution(1)
    logTrace "Refreshing Associations"
    refreshAssociations()
}

//
// zwaveEvent Methods
//

def zwaveEvent(hubitat.zwave.Command cmd) {
  logErr "Unhandled: $cmd"
  cmd.properties.each { logTrace "$it.key => $it.value"}
}

//COMMAND_CLASS_ZWAVEPLUS_INFO V2
    // NOTE: Unlikely to receive one of these, as there's no Get method in Hubitat,
    //       but we will handle and log the information just in case.
def zwaveEvent(hubitat.zwave.commands.zwaveplusinfov2.ZwaveplusInfoReport cmd) {
    logTrace 'zwaveEvent(hubitat.zwave.commands.zwaveplusinfov2.ZwaveplusInfoReport cmd)'
    logTrace cmd
    cmd.properties.each { p -> logTrace "${p.key} => ${p.value}"}
}
//COMMAND_CLASS_VERSION V2
def zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
   // log.debug "in zwave event 1"
    logTrace "zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd)"
    logTrace cmd
    cmd.properties.each { p -> logTrace "${p.key} => ${p.value}"}

    def firmwareStr = "${cmd.firmware0Version}.${cmd.firmware0SubVersion}"
    def zwaveVerStr = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
    def hwVerStr = "$cmd.hardwareVersion"

    updateDataValue("firmware", firmwareStr)
    updateDataValue("zwave-version", zwaveVerStr)
    updateDataValue("hardware-version", hwVerStr)
    logDebug "${device.displayName} is running firmware version: $firmwareStr, Z-Wave version: $zwaveVerStr"
}
//COMMAND_CLASS_MANUFACTURER_SPECIFIC V2
def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    //log.debug "in zwave event 2"
    logTrace 'zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd)'
    logTrace cmd
    cmd.properties.each { p -> logTrace "${p.key} => ${p.value}"}
}
//COMMAND_CLASS_METER V4
def zwaveEvent(hubitat.zwave.commands.meterv4.MeterReport cmd, int endpoint = 0) {
    //log.debug "in zwave event3"
    def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
    sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
    
    
    logTrace 'zwaveEvent(hubitat.zwave.commands.meterv4.MeterReport cmd, int clampNo = 0)'
    logTrace cmd
    cmd.properties.each { p -> logTrace "${p.key} => ${p.value}"}

    def source = endpoint == 0 ? '' : "-Clamp-$endpoint"

    def label = Null
    switch (cmd.scale){
        case 0: //kWh
            unit = 'kWh'
            label = 'energy'
            break
        case 1: //kVAh
            unit = 'kVAh'
            label = 'kVAh'
            break
        case 2: //W
            unit = 'W'
            label = 'power'
        log.info "got power report"
            break
        case 3: //Pulse count
            unit = 'Hz'
            label = 'Cycles'
            break
        case 4: //V
            unit = 'V'
            label = 'voltage'
            break
        case 5: //A
            unit = 'A'
            label = 'current'
            break
        case 6: //Power Factor
            unit = ''
            label = 'PF'
            break
        case 7:
            switch (cmd.scale2){
                case 0: //kVar
                    unit = 'kVar'
                    label = 'kVar'
                    break
                case 1: //kVarh
                    unit = 'kVarh'
                    label = 'kVarh'
                    break
                default:
                    logErr "Scale not implemented. ${cmd.scale}, ${cmd.scale2}: ${cmd.scaledMeterValue}"
                    break
            }
            break
        default:
            logWarn "Scale not implemented. ${cmd.scale}, ${cmd.scale2}: ${cmd.scaledMeterValue}"
            break
    }
    logDebug "Got message for endpoint: $endpoint for scale: $cmd.scale, scale2: $cmd.scale2 value: $cmd.scaledMeterValue"
    sendEvent(name: "$label$source", value: cmd.scaledMeterValue.toFloat(), unit: unit)
}
//COMMAND_CLASS_CRC_16_ENCAP V1
// CRC16 handled by Hubitat C-7

//COMMAND_CLASS_MULTI_CHANNEL V4
def zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd) {
   // log.debug "in zwave event4"
    logTrace "Entering zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd)"
    logTrace "Attempting to unpack the encapsulated command"
    zwaveEvent cmd.encapsulatedCommand(), cmd.sourceEndPoint.toInteger()
    
}
//COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
def zwaveEvent(hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport cmd) {
   // log.debug "in zwave event5"
    logTrace 'hubitat.zwave.commands.multichannelassociationv3.MultiChannelAssociationReport'

    cmd.properties.each{ logTrace "${it.key} => ${it.value}" }

    if (!state.multiChannelAssociations) state.multiChannelAssociations = []
    if (!state.multiChannelAssociationsMultiChannelNodeIDs) state.multiChannelAssociationsMultiChannelNodeIDs = []

    state.multiChannelAssociations[cmd.groupingIdentifier] = cmd.nodeId
    state.multiChannelAssociationsMultiChannelNodeIDs[cmd.groupingIdentifier] = cmd.multiChannelNodeIds
}
//COMMAND_CLASS_CONFIGURATION V1
def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
   //  log.debug "in zwave event 6"
    logTrace 'zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd)'
    logTrace cmd
    cmd.properties.each{ logTrace "${it.key} => ${it.value}" }

    logInfo "$device.displayName has value '$cmd.scaledConfigurationValue' for property with ID '$cmd.parameterNumber'"
}
//COMMAND_CLASS_ASSOCIATION_GRP_INFO V1
    //We don't need to support this one. 
//COMMAND_CLASS_ASSOCIATION V2
def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
   // log.debug "in zwave event 7"
    logTrace 'hubitat.zwave.commands.associationv2.AssociationReport'
    cmd.properties.each{ logTrace "${it.key} => ${it.value}" }
    if (!state.associatedNodes) {
        state.associatedNodes = []
    }
    state.associatedNodes[cmd.groupingIdentifier] = cmd.nodeId
}
//COMMAND_CLASS_FIRMWARE_UPDATE_MD  V2
    //No Need for us to do anything with this CC
//COMMAND_CLASS_POWERLEVEL V1
    //Not going to handle this here, info is available on Z-Wave Settings page
//COMMAND_CLASS_SECURITY V1
    // Security handled by Hubitat on C-7
//COMMAND_CLASS_DEVICE_RESET_LOCALLY V1
def zwaveEvent(hubitat.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
   // log.debug "in zwave event 8"
    logTrace 'zwaveEvent(hubitat.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd)'
    logTrace cmd
    cmd.properties.each{ logTrace "${it.key} => ${it.value}" }

    logErr "WARNING: $device.displayName sent a DeviceResetLocallyNotification!"
}


//
// Custom Methods
//
def logErr(message) {
    log.error message
}

def logWarn(message) {
    log.warn message
}

def logInfo(message) {
    log.info message
}

def logDebug(message) {
    if (logDebugMessages) log.debug message
}

def logTrace(message) {
    //VERY VERBOSE. Only enable during development!
    def logTraceMessages = false
    if (logTraceMessages) log.trace message
}
def runCommandsWithInterstitialDelay(commands, delay = 300) {
    logTrace "Entering runCommandsWithInterstitialDelay"

    logTrace "Calling delayBetween"
    commandsWithDelays = delayBetween(commands.collect { command -> zwaveSecureEncap command }, delay)

    commandsWithDelays.each { command -> logTrace command }

    logTrace "Generating hubMultiAction"
    multiAction = new hubitat.device.HubMultiAction(commandsWithDelays, hubitat.device.Protocol.ZWAVE)

    logTrace "Sending multiAction"
    sendHubCommand multiAction

    logTrace "Returning from runCommandsWithInterstitialDelay"
}

def runCommand(command) {
    logTrace "Entering runCommand"

    logTrace "Generating hubAction for command ${zwaveSecureEncap(command)}"
    action = new hubitat.device.HubAction(zwaveSecureEncap(command), hubitat.device.Protocol.ZWAVE)

    logTrace "Sending hubAction"
    sendHubCommand action
    logTrace "Returning from runCommand"
}
