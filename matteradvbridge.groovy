/* groovylint-disable CompileStatic, CouldBeSwitchStatement, DuplicateListLiteral, DuplicateMapLiteral, DuplicateNumberLiteral, DuplicateStringLiteral, ImplicitClosureParameter, ImplicitReturnStatement, InsecureRandom, LineLength, MethodCount, MethodParameterTypeRequired, MethodSize, NglParseError, NoDef, NoDouble, PublicMethodsBeforeNonPublicMethods, StaticMethodsBeforeInstanceMethods, UnnecessaryGetter, UnnecessaryObjectReferences, UnnecessarySetter */
/**
 *  Matter Advanced Bridge - Device Driver for Hubitat Elevation
 *
 *  https://community.hubitat.com/t/dynamic-capabilities-commands-and-attributes-for-drivers/98342
 *  https://community.hubitat.com/t/project-zemismart-m1-matter-bridge-for-tuya-zigbee-devices-matter/127009
 *
 *     Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License. You may obtain a copy of the License at:
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *     on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *     for the specific language governing permissions and limitations under the License.
 *
 * Thanks to Hubitat for publishing the sample Matter driver https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/thirdRealityMatterNightLight.groovy
 *
 * The full revisions history is available at https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/wiki/Matter-Advanced-Bridge-%E2%80%90-revisions-history
 * The full TODO list is available at https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/wiki/Matter-Advanced-Bridge-%E2%80%90-TODO-list
 *
 * ver. 1.0.0  2024-03-16 kkossev  - public release version.
 * ver. 1.0.1  2024-04-13 kkossev  - tests; resetStats bug fix;
 * ver. 1.1.0  2024-07-20 kkossev  - merged pull request from dds82 (added Matter_Generic_Component_Door_Lock); added Identify command; reduced battery attribute subscriptions;
 * ver. 1.1.1  2024-07-23 kkossev  - added Switch capability to the Matter Door Lock component driver.
 * ver. 1.1.2  2024-07-31 kkossev  - skipped General Diagnostics cluster 0x0033 discovery - Aqara M3 firmware 4.1.7_0013 returns error reading attribute 0x0000
 * ver. 1.1.3  2024-08-09 kkossev  - fixed sendSubsribeList() typo; 
 * ver. 1.2.0  2024-10-03 kkossev  - [2.3.9.186] platform: cleanSubscribe; decoded events for child devices w/o the attribute defined are sent anyway; added Matter Thermostats.
 * ver. 1.2.1  2024-10-05 kkossev  - thermostatSetpoint attribute is also updated; Matter Events basic decoding (buttons and Locks are still NOT working!); thermostat driver automatic assignment bug fix; 
 *                                   checking both 'maxHeatSetpointLimit' and 'absMaxHeatSetpointLimit' when setting the thermostatSetpoint; thermostatOperatingState is updated (digital); thermostat on() and of() commands bug fix;
 * ver. 1.2.2  2024-10-11 kkossev -  added 'Matter Generic Component SwitchBot Button' by @ymerj
 * ver. 1.3.0  2024-10-10 kkossev  - adding 'Matter Generic Component Air Purifier' (W.I.P.) : cluster 005B 'AirQuality'
 * ver. 1.3.1  2024-11-12 kkossev  - bugfix: nullpointer exception in discoverAllStateMachine()
 * ver. 1.4.0  2024-12-26 kkossev  - HE Platform 2.4.0.x compatibility update
 * ver. 1.4.1  2025-01-12 kkossev  - restored the commands descriptions;
 * ver. 1.5.0  2025-04-04 kkossev  - added 'Matter Custom Component Power Energy'
 * ver. 1.5.1  2025-04-07 kkossev  - RMSVoltage and RMSCurrent fix
 * ver. 1.5.2  2025-05-23 kkossev  - added 'Matter Custom Component Signal'
 * ver. 1.5.3  2025-06-28 kkossev + Claude Sonnet 4 : added custom decodeTLVToHex() and decodeTLV() as a workaround for the Hubitat bug with TLV decoding
 * ver. 1.5.4  2026-01-08 kkossev + GPT-5.2 : added discoveryTimeoutScale; added 'Matter Generic Component Button' driver
 * ver. 1.5.5  2026-01-10 kkossev + Claude Sonnet 4.5 : Matter Locks are now working!; componentPing command added;
 * ver. 1.5.6  2026-01-11 kkossev + Claude Sonnet 4.5 : Fixed button events subscription issue; fixed to RGBW child devices detection; fixed deviceTypeList parsing issue; added 'generatePushedOn' preference for buttons that don't send multiPressComplete events
 * ver. 1.6.0  2026-01-17 kkossev + Claude Sonnet 4.5 + GPT-5.2 : A major refactoring of the Door Lock driver; optimized subsciption management;
 *                                  water leak sensors (deviceType 0x0043) automatic detection
 * ver. 1.7.0  2026-01-25 kkossev   DEVICE_TYPE = 'MATTER_BRIDGE' bug fix in initialize(); added ALPSTUGA Air Quality Monitor support - CarbonDioxideConcentrationMeasurement; improved BasicInformation (0x0028) decoding; 
 *                                  added ping() delta calculcation; added cleanSubscribe Min/MaxInterval preferences; added new parse(Map) toggle (experimental, WIP); added matterCommonLib.groovy library for common functions;
 * ver. 1.7.1  2026-01-26 kkossev   reduced debug/warn logging; Best Name auto-label for all child devices @iEnam
 * ver. 1.7.2  2026-01-29 kkossev   bugfixes: contact/water/motion/lock state parsing issue; child device pings; Patch for Zemismart M1 battery percentage reporting issue; 
 * ver. 1.7.3  2026-01-30 kkossev   newParse=true by default; bugfixes: RGB&CT bulbs level parsing;
 * ver. 1.7.4  2026-02-06 kkossev   device ping() bugfix; filter all events (including Door Lock) after reboot/resubscribe; eventPaths are subscribed first; removed lockType and operatingMode and supportedOperatingModes attributes subscriptins; 
 *                                  added General Diagnostics (0x0033) to SupportedMatterClusters with subscriptions for RebootCount (0x0001) and UpTime (0x0002) using min 60 / max 3600 / delta 60
 * ver. 1.7.5  2026-02-11 kkossev   processing the new callbackType:WriteAttributes and callbackType:SubscribeResult;
 * ver. 1.7.6  2026-02-12 kkossev   bugfix: WindowCovering processing exceptions; 'Matter Generic Component Window Shade' getInfo() method;
 * ver. 1.7.7  2026-02-14 kkossev   bugfix: Power/Energy processing exceptions; 'Matter Custom Component Power Energy' getInfo() method; newParse is true by default;
 *
 *                                   TODO: use subscriptionResult - subscriptionId: XXXXXX   to determine when subscription attribute/event reports have completed.
 *                                   TODO: Scheduled jobs (ping) is not started automatically after driver installation ! (side effect of disabling the Initialize capability?)
 *                                   TODO: use events timestamp / priority as a filtering criteria for duplicated events and out-of-order events ? (may not ne needed anymore after callbackType:SubscribeResult processing is implemented)
 *                                   TODO: _discoverAll to call updated() or to start the periodic jobs
 *                                   TODO: Composite grouping of different attributes of a child device @iEnam
 *                                   TODO: thermostat component - supported modes JSON initialization duafter discovery
 *                                   TODO: add networkStatus attribute : http://192.168.0.151/hub/matterDetails/json 
 *                                   TODO: IKEA Thread devices - handle the Battery reproting (EP=00) + ALPSTUGA air quality monitor
 *                                   TODO: store the BestName to Device Data [0000] DeviceTypeList = [0015] ('Contact Sensor'), also store in the state deviceType	
 *                                   TODO: decode [0013] CapabilityMinima = 1524000324010318 [0012] UniqueID = 4A6A0276A1834629
 *                                   TODO: add ping as a first step in the state machines before reading attributes
 *                                   TODO: reset statistics on Hub reboot
 *                                   TODO: TLV decode [0004] TagList = [24, 2408, 34151802, 24, 2443, 34151800, 24, 2443, 34151803, 24, 2443, 08032C08]
 *                                   TODO: add cluster 0071 'HEPAFilterMonitoring'
 *                                   TODO: add cluster 0202 'Window Covering'
 *                                   TODO: check if duplicated:  updateChildFingerprintData() and copyEntireFingerprintToChild(); 
 *
 */

static String version() { '1.7.7' }
static String timeStamp() { '2026/02/14 10:42 AM' }


@Field static final Boolean _DEBUG = false                    // make it FALSE for production!
@Field static final String  DRIVER_NAME = 'Matter Advanced Bridge'
@Field static final String  COMM_LINK =   'https://community.hubitat.com/t/release-matter-advanced-bridge-limited-device-support/135252'
@Field static final String  GITHUB_LINK = 'https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/wiki'
@Field static final String  IMPORT_URL =  'https://raw.githubusercontent.com/kkossev/Hubitat---Matter-Advanced-Bridge/main/Matter_Advanced_Bridge.groovy'
@Field static final Boolean DEFAULT_LOG_ENABLE = false       // make it FALSE for production!
@Field static final Boolean DO_NOT_TRACE_FFFX = true         // make it  TRUE for production! (don't trace the FFFx global attributes)
@Field static final Boolean MINIMIZE_STATE_VARIABLES_DEFAULT = false     // make it TRUE for production!
@Field static final Integer DIGITAL_TIMER = 3000             // command was sent by this driver
@Field static final Integer REFRESH_TIMER = 6000             // refresh time in miliseconds
@Field static final Integer INFO_AUTO_CLEAR_PERIOD = 60      // automatically clear the Info attribute after 60 seconds
@Field static final Integer COMMAND_TIMEOUT = 15             // timeout time in seconds
@Field static final Integer MAX_PING_MILISECONDS = 15000     // rtt more than 15 seconds will be ignored
@Field static final Integer PRESENCE_COUNT_THRESHOLD = 2     // missing 3 checks will set the device healthStatus to offline
@Field static final String  UNKNOWN = 'UNKNOWN'
@Field static final Integer SHORT_TIMEOUT  = 7
@Field static final Integer LONG_TIMEOUT   = 15
@Field static final Integer CLEAN_SUBSCRIBE_MIN_INTERVAL_DEFAULT = 1
@Field static final Integer CLEAN_SUBSCRIBE_MAX_INTERVAL_DEFAULT = 600
@Field static final Integer CLEAN_SUBSCRIBE_MAX_ALLOWED_INTERVAL = 0xFFFF
@Field static final Integer defaultMinReportingTime = 10

// Internal events that should be routed through parse() without requiring attribute declaration
@Field static final List<String> INTERNAL_EVENTS = ['unprocessed', 'handleInChildDriver']

import com.hubitat.app.ChildDeviceWrapper
import com.hubitat.app.DeviceWrapper
import com.hubitat.app.exception.UnknownDeviceTypeException
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import groovy.transform.CompileStatic
import hubitat.helper.HexUtils
import java.util.concurrent.ConcurrentHashMap
import hubitat.matter.DataType

metadata {
    definition(name: DRIVER_NAME, namespace: 'kkossev', author: 'Krassimir Kossev', importUrl: IMPORT_URL, singleThreaded: true ) {
        capability 'Actuator'
        capability 'Sensor'
        // capability 'Initialize' // commented out to avoid automatic initialize() call on driver update and hub reboot, resulting in subscribing to all Matter attributes and events again!
        capability 'Refresh'
        capability 'Health Check'

        attribute 'healthStatus', 'enum', ['unknown', 'offline', 'online']
        attribute 'rtt', 'number'
        attribute 'Status', 'string'
        attribute 'productName', 'string'
        attribute 'nodeLabel', 'string'
        attribute 'softwareVersionString', 'string'
        attribute 'rebootCount', 'number'
        attribute 'upTime', 'number'
        attribute 'totalOperationalHours', 'number'
        attribute 'deviceCount', 'number'
        attribute 'endpointsCount', 'number'
        attribute 'initializeCtr', 'number'
        attribute 'reachable', 'string'
        attribute 'state', 'enum', [
            'not configured',
            'error',
            'authenticating',
            'authenticated',
            'connected',
            'disconnected',
            'ready'
        ]

        command '_DiscoverAll',  [[name:'Discover all bridged devices!' , type:ENUM, description: 'Type', constraints: ['All', 'BasicInfo', 'PartsList', 'ChildDevices', 'Subscribe']]]
        command 'reSubscribe', [[name: 're-subscribe to the Matter controller events']]
        command 'loadAllDefaults', [[name: 'panic button: Clear all States and scheduled jobs']]
        command 'identify'      // works with Nuki Lock!
        if (_DEBUG) {
            command 'getInfo', [
                    [name:'infoType', type: 'ENUM', description: 'Bridge Info Type', constraints: ['Basic', 'Extended']],   // if the parameter name is 'type' - shows a drop-down list of the available drivers!
                    [name:'endpoint', type: 'STRING', description: 'Endpoint', constraints: ['STRING']]
            ]
            command 'test', [[name: 'test', type: 'STRING', description: 'test', defaultValue : '']]
        }
        // do not expose the known Matter Bridges fingerprints for now ... Let the stock driver be assigned automatically.
        // fingerprint endpointId:"01", inClusters:"0003,001D", outClusters:"001E", model:"Aqara Hub E1", manufacturer:"Aqara", controllerType:"MAT"
    }
    preferences {
	    input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
        input name:'txtEnable', type: 'bool', title: '<b>Enable descriptionText logging</b>', defaultValue: true
        input name:'logEnable', type: 'bool', title: '<b>Enable debug logging</b>', defaultValue: DEFAULT_LOG_ENABLE
        input name: 'advancedOptions', type: 'bool', title: '<b>Advanced Options</b>', description: '<i>These advanced options should be already automatically set in an optimal way for your device...</i>', defaultValue: false
        input name: "minReportingTimeIllum", type: "number", title: "Minimum time between illumination/lux reports", description: "Minimum time between illumination/lux reporting, seconds", defaultValue: 10, range: "1..3600",  limit:['ALL']
        if (device && advancedOptions == true) {
            input name: 'healthCheckMethod', type: 'enum', title: '<b>Healthcheck Method</b>', options: HealthcheckMethodOpts.options, defaultValue: HealthcheckMethodOpts.defaultValue, required: true, description: '<i>Method to check device online/offline status.</i>'
            input name: 'healthCheckInterval', type: 'enum', title: '<b>Healthcheck Interval</b>', options: HealthcheckIntervalOpts.options, defaultValue: HealthcheckIntervalOpts.defaultValue, required: true, description: '<i>How often the hub will check the device health.<br>3 consecutive failures will result in status "offline"</i>'
            input name: 'discoveryTimeoutScale', type: 'enum', title: '<b>Discovery timeout scale</b>', options: ['1':'1x (default)', '2':'2x', '3':'3x (slow/battery bridges)'], defaultValue: '1', required: true, description: '<i>Scales discovery/state-machine retry timeouts and discovery scheduling delays.</i>'
            input name: 'traceEnable', type: 'bool', title: '<b>Enable trace logging</b>', defaultValue: false, description: '<i>Turns on detailed extra trace logging for 30 minutes.</i>'
            input name: 'minimizeStateVariables', type: 'bool', title: '<b>Minimize State Variables</b>', defaultValue: MINIMIZE_STATE_VARIABLES_DEFAULT, description: '<i>Minimize the state variables size.</i>'
            input name: 'newParse', type: 'bool', title: '<b>Use new parse(Map) handler</b>', defaultValue: true, description: '<i>Enable Hubitat"s new parse(Map) callback instead of the legacy description text.</i>'
            input name: 'cleanSubscribeMinInterval', type: 'number', title: '<b>Clean subscribe minimum reporting interval (seconds)</b>', defaultValue: CLEAN_SUBSCRIBE_MIN_INTERVAL_DEFAULT, required: true, description: '<i>Minimum reporting interval used when subscribing to Matter attributes/events.</i>'
            input name: 'cleanSubscribeMaxInterval', type: 'number', title: '<b>Clean subscribe maximum reporting interval (seconds)</b>', defaultValue: CLEAN_SUBSCRIBE_MAX_INTERVAL_DEFAULT, required: true, description: '<i>Maximum reporting interval used when subscribing to Matter attributes/events.</i>'
 
        }
    }
}

@Field static final Map HealthcheckMethodOpts = [            // used by healthCheckMethod
    defaultValue: 2,
    options     : [0: 'Disabled', 1: 'Activity check', 2: 'Periodic polling']
]
@Field static final Map HealthcheckIntervalOpts = [          // used by healthCheckInterval
    defaultValue: 15,
    options     : [1: 'Every minute (not recommended!)', 15: 'Every 15 Mins', 30: 'Every 30 Mins', 60: 'Every 1 Hour', 240: 'Every 4 Hours', 720: 'Every 12 Hours']
]
@Field static final Map StartUpOnOffEnumOpts = [0: 'Off', 1: 'On', 2: 'Toggle']

@Field static final Map<Integer, Map> SupportedMatterClusters = [
    // On/Off Cluster
    0x0006 : [attributes: 'OnOffClusterAttributes', commands: 'OnOffClusterCommands',  parser: 'parseOnOffCluster',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    // Level Control Cluster
    0x0008 : [attributes: 'LevelControlClusterAttributes', commands: 'LevelControlClusterCommands', parser: 'parseLevelControlCluster',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    0x002F : [parser: 'parsePowerSource', attributes: 'PowerSourceClusterAttributes',
              subscriptions : [
                            //   [0x0000: [min: 0, max: 0xFFFF, delta: 0]],   // Status - commented out 2026-01-29
                            //   [0x0001: [min: 0, max: 0xFFFF, delta: 0]],   // Order
                            //   [0x0002: [min: 0, max: 0xFFFF, delta: 0]],   // Description
                               [0x000B: [min: 0, max: 0xFFFF, delta: 0]],   // BatVoltage (11)
                               [0x000C: [min: 0, max: 0xFFFF, delta: 0]],   // BatPercentRemaining (12)
                            //   [0x000E: [min: 0, max: 0xFFFF, delta: 0]],   // BatChargeLevel (14)
                            //   [0x000F: [min: 0, max: 0xFFFF, delta: 0]]    // BatReplacementNeeded (15)
              ]
    ],

     // General Diagnostics Cluster (bridge/node)
     0x0033 : [attributes: 'GeneralDiagnosticsClusterAttributes', parser: 'parseGeneralDiagnostics',
                subscriptions : [
                    [0x0001: [min: 60, max: 3600, delta: 60]],  // RebootCount
                    [0x0002: [min: 60, max: 3600, delta: 60]]   // UpTime
                ]
     ],
    /*
    0x0039 : [attributes: 'BridgedDeviceBasicAttributes', commands: 'BridgedDeviceBasicCommands', parser: 'parseBridgedDeviceBasic',            // BridgedDeviceBasic
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    */
    
    0x003B : [parser: 'parseSwitch', attributes: 'SwitchClusterAttributes', events: 'SwitchClusterEvents',       // Switch
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]],   // NumberOfPositions
                               [0x0001: [min: 0, max: 0xFFFF, delta: 0]],   // CurrentPosition
                               [0x0002: [min: 0, max: 0xFFFF, delta: 0]]],  // MultiPressMax
              eventSubscriptions : [-1]  // -1 means subscribe to ALL events from this cluster
    ],
    
    // Descriptor Cluster - subscribing to it seems to create a lot of issues!! :( 
    /*
    0x001D : [attributes: 'DescriptorClusterAttributes', parser: 'parseDescriptorCluster',      // decimal(29) manually subscribe to the Bridge device ep=0 0x001D 0x0003
              subscriptions : [[0x0003: [min: 0, max: 0xFFFF, delta: 0]]]   // PartsList
    ],
    */
    // Contact Sensor Cluster
    0x0045 : [attributes: 'BooleanStateClusterAttributes', parser: 'parseBooleanState',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    // Air Quality Cluster
    0x005B : [attributes: 'AirQualityClusterAttributes', parser: 'parseAirQuality',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    // Electrical Power Measurement Cluster
    0x0090 : [attributes: 'ElectricalPowerMeasurementAttributes',  parser: 'parseElectricalPowerMeasurement',
              subscriptions : [//[0x0004: [min: 0, max: 0xFFFF, delta: 0]],   // 'Voltage',
                               //[0x0005: [min: 0, max: 0xFFFF, delta: 0]],   // 'ActiveCurrent',
                               [0x0008: [min: 0, max: 0xFFFF, delta: 100]],   // 'ActivePower', (report every 0.1W change)
                               [0x000B: [min: 0, max: 0xFFFF, delta: 1000]],  // 'RMSVoltage', (report every 1V change)
                               [0x000C: [min: 0, max: 0xFFFF, delta: 10]],    // 'RMSCurrent', (report every 0.01A change)
                               [0x000E: [min: 0, max: 0xFFFF, delta: 1000]],  // 'Frequency', (report every 1Hz change)
                               [0x0011: [min: 0, max: 0xFFFF, delta: 500]]    // 'PowerFactor' (report every 0.5 change)
              ]
    ],
    // Electrical Energy Measurement Cluster
    0x0091 : [attributes: 'ElectricalEnergyMeasurementAttributes', parser: 'parseElectricalEnergyMeasurement',
              subscriptions : [[0x0001: [min: 0, max: 0xFFFF, delta: 1000]],   // 'CumulativeEnergyImported', report every 1Wh change
                               [0x0002: [min: 0, max: 0xFFFF, delta: 1000]]    // 'CumulativeEnergyExported', report every 1Wh change
                            // [0x0003: [min: 0, max: 0xFFFF, delta: 0]],   // (PeriodicEnergyImported)
                            // [0x0004: [min: 0, max: 0xFFFF, delta: 0]]    // (PeriodicEnergyExported)
              ]
    ],
    // DoorLock Cluster
    0x0101 : [attributes: 'DoorLockClusterAttributes', commands: 'DoorLockClusterCommands', parser: 'parseDoorLock',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]],   // LockState (Mandatory)
                               //[0x0002: [min: 0, max: 0xFFFF, delta: 0]],   // ActuatorEnabled (Mandatory)
                               //[0x0003: [min: 0, max: 0xFFFF, delta: 0]],   // DoorState (Optional but recommended if supported)
                               //[0x0025: [min: 0, max: 0xFFFF, delta: 0]]],  // OperatingMode (Mandatory)
              ],
              eventSubscriptions : [-1]  // Subscribe to ALL Door Lock events (DoorLockAlarm, DoorStateChange, LockOperation, LockOperationError, LockUserChange)
    ],
    // WindowCovering
    0x0102 : [attributes: 'WindowCoveringClusterAttributes', commands: 'WindowCoveringClusterCommands', parser: 'parseWindowCovering',
    
              subscriptions : [[0x000A: [min: 0, max: 0xFFFF, delta: 0]],   // OperationalStatus
                               [0x000B: [min: 0, max: 0xFFFF, delta: 0]],   // TargetPositionLiftPercent100ths
                               [0x000E: [min: 0, max: 0xFFFF, delta: 0]]    // CurrentPositionLiftPercent100ths
              ]
    ],    
    // Thermostat
    0x0201 : [attributes: 'ThermostatClusterAttributes', commands: 'ThermostatClusterCommands', parser: 'parseThermostat',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]],   // LocalTemperature +Aqara
                               [0x0003: [min: 0, max: 0xFFFF, delta: 0]],   // AbsMinHeatSetpointLimit  +Aqaea
                               [0x0004: [min: 0, max: 0xFFFF, delta: 0]],   // AbsMaxHeatSetpointLimit  +Aqara
                               [0x0010: [min: 0, max: 0xFFFF, delta: 0]],   // LocalTemperatureCalibration
                               [0x0012: [min: 0, max: 0xFFFF, delta: 0]],   // OccupiedHeatingSetpoint  +Aqara
                               [0x0015: [min: 0, max: 0xFFFF, delta: 0]],   // MinHeatSetpointLimit +Aqara
                               [0x0016: [min: 0, max: 0xFFFF, delta: 0]],   // MaxHeatSetpointLimit +Aqara
                               [0x001A: [min: 0, max: 0xFFFF, delta: 0]],   // RemoteSensing
                               [0x001B: [min: 0, max: 0xFFFF, delta: 0]],   // ControlSequenceOfOperation   +Aqara
                               [0x001C: [min: 0, max: 0xFFFF, delta: 0]],   // SystemMode   +Aqara
                               [0x001D: [min: 0, max: 0xFFFF, delta: 0]],   // AlarmMask
                               [0x001E: [min: 0, max: 0xFFFF, delta: 0]],   // ThermostatRunningMode
                               [0x0029: [min: 0, max: 0xFFFF, delta: 0]]]   // ThermostatRunningState
    ],
    // ColorControl Cluster
    0x0300 : [attributes: 'ColorControlClusterAttributes', commands: 'ColorControlClusterCommands', parser: 'parseColorControl',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]],   // CurrentHue
                               [0x0001: [min: 0, max: 0xFFFF, delta: 0]],   // CurrentSaturation
                               [0x0007: [min: 0, max: 0xFFFF, delta: 0]],   // ColorTemperatureMireds
                               [0x0008: [min: 0, max: 0xFFFF, delta: 0]]]   // ColorMode
    ],
    // IlluminanceMeasurement Cluster
    0x0400 : [attributes: 'IlluminanceMeasurementClusterAttributes', parser: 'parseIlluminanceMeasurement',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    // TemperatureMeasurement Cluster
    0x0402 : [attributes: 'TemperatureMeasurementClusterAttributes', parser: 'parseTemperatureMeasurement',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    // HumidityMeasurement Cluster
    0x0405 : [attributes: 'RelativeHumidityMeasurementClusterAttributes', parser: 'parseHumidityMeasurement',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    // OccupancySensing (motion) Cluster
    0x0406 : [attributes: 'OccupancySensingClusterAttributes', parser: 'parseOccupancySensing',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    // CarbonDioxideConcentrationMeasurement Cluster
    0x040D : [attributes: 'ConcentrationMeasurementClustersAttributes', parser: 'parseCarbonDioxideConcentrationMeasurement',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],
    // PM25ConcentrationMeasurement Cluster
    0x042A : [attributes: 'ConcentrationMeasurementClustersAttributes', parser: 'parseConcentrationMeasurement',
              subscriptions : [[0x0000: [min: 0, max: 0xFFFF, delta: 0]]]
    ],    
]

@Field static final Map<Integer, String> ParsedMatterClusters = [
    0x0006 : 'parseOnOffCluster',
    0x0008 : 'parseLevelControlCluster',
    0x001D : 'parseDescriptorCluster',
    0x0028 : 'parseBasicInformationCluster',
    0x002F : 'parsePowerSource',
    0x0033 : 'parseGeneralDiagnostics',
    0x0039 : 'parseBridgedDeviceBasic',
    0x003B : 'parseSwitch',
    0x0045 : 'parseBooleanState',
    0x005B : 'parseAirQuality',
    0x0090 : 'parseElectricalPowerMeasurement',
    0x0091 : 'parseElectricalEnergyMeasurement',
    0x0101 : 'parseDoorLock',
    0x0102 : 'parseWindowCovering',
    0x0201 : 'parseThermostat',
    0x0300 : 'parseColorControl',
    0x0400 : 'parseIlluminanceMeasurement',
    0x0402 : 'parseTemperatureMeasurement',
    0x0405 : 'parseHumidityMeasurement',
    0x0406 : 'parseOccupancySensing',
    0x040D : 'parseCarbonDioxideConcentrationMeasurement',
    0x042A : 'parseConcentrationMeasurement'
]

// Json Parsing Cache
@Field static final Map<String, Map> jsonCache = new ConcurrentHashMap<>()

// Track for dimming operations
@Field static final Map<String, Integer> levelChanges = new ConcurrentHashMap<>()

// Json Parser
@Field static final JsonSlurper jsonParser = new JsonSlurper()

// Random number generator
@Field static final Random random = new Random()

// old parser
void parse(final String description) {
    prepareForParse()

    Map descMap
    try {
        descMap = myParseDescriptionAsMap(description)
    } catch (e) {
        logWarn "parse: exception ${e} <br> Failed to parse description: ${description}"
        return
    }
    if (descMap == null) {
        logWarn "parse: descMap is null description:${description}"
        return
    }

    processParsedDescription(descMap, description)
}


// New parse(Map) method to handle events (and attribute reports) when Device Data.newParse	is set to true
// example : [callbackType:Report, endpointInt:2, clusterInt:59, attrInt:1, data:[1:UINT:0], value:0] 
// example:  [callbackType:WriteAttributes, endpointInt:82, clusterInt:513, attrInt:28, sucess:true, cluster:0201, endpoint:52, attrId:001C]
// example : [endpoint:01, cluster:003B, evtId:0006, clusterInt:59, evtInt:6, values:[0:[type:04, isContextSpecific:true, value:01], 1:[type:04, isContextSpecific:true, value:01]]]

void parse(Map msg) {
    if (!isNewParseEnabled()) {
        logWarn 'parse(Map) received but newParse preference is disabled; enable the option to allow map parsing.'
        return
    }
    Map pacthedNewParseMap = [:]
    //log.trace "parse(Map) called with msg: ${msg}"
    pacthedNewParseMap = newParseCompatibilityPatch(msg)
    prepareForParse()
    processParsedDescription(pacthedNewParseMap, "new Parse/Map payload: ${pacthedNewParseMap}")
}

private void prepareForParse() {
    checkDriverVersion()
    checkSubscriptionStatus()
    unschedule('deviceCommandTimeout')
    setHealthStatusOnline()
}

private void processParsedDescription(final Map descMap, final String description) {
    if (descMap == null || descMap?.isEmpty()) {
        logDebug "processParsedDescription: descMap is null or empty  description: ${description}"
        return
    }

    updateStateStats(descMap)
    checkStateMachineConfirmation(descMap)

    if (isDeviceDisabled(descMap)) {
        if (traceEnable) { logWarn "parse: device is disabled: ${descMap}" }
        return
    }

    if (!(((descMap.attrId in ['FFF8', 'FFF9', 'FFFA', 'FFFC', 'FFFD', '00FE']) && DO_NOT_TRACE_FFFX) || state['states']['isDiscovery'] == true)) {
        logDebug "parse: descMap:${descMap}  description:${description}"
    }
    // Additional debug for Matter events (especially Switch/buttons)
    if (descMap?.evtId != null && descMap?.cluster == '003B' && settings?.logEnable) {
        logDebug "parse: received Switch EVENT endpoint:${descMap.endpoint} evtId:${descMap.evtId} value:${descMap.value}"
    }
    // 2026-02-11  [callbackType:SubscriptionResult, subscriptionId:3617819414] 
    if (descMap?.callbackType == 'SubscriptionResult') {
        logDebug "parse: received SubscriptionResult callback with subscriptionId:${descMap.subscriptionId}"
        return
    }
    // 2026-02-11  [callbackType:WriteAttributes, endpointInt:82, clusterInt:513, attrInt:28, sucess:true, cluster:0201, endpoint:52, attrId:001C]
    if (descMap?.callbackType == 'WriteAttributes') {
        logDebug "parse: received WriteAttributes callback for endpoint:${descMap.endpoint} cluster:${descMap.cluster} attrId:${descMap.attrId} success:${descMap.sucess}"
        return
    }
    
    // Check for child devices ping responses before normal parsing
    checkChildDevicePingResponse(descMap)
    
    parseGlobalElements(descMap)

    gatherAttributesValuesInfo(descMap)

    Integer clusterInt = (descMap.clusterInt != null) ? safeToInt(descMap.clusterInt, null) : safeHexToInt(descMap.cluster, null)
    String parserFunc = (clusterInt != null) ? ParsedMatterClusters[clusterInt] : null

    if (parserFunc) {
        if (_DEBUG) {
            this."${parserFunc}"(descMap)
        }
        else {
            try {
                this."${parserFunc}"(descMap)
            } catch (e) {
                logWarn "parserFunc: exception ${e} <br> Failed to parse description: ${description}"
            }
        }
    } else {
        logWarn "parserFunc: NOT PROCESSED: ${descMap} description:${description}"
    }
}

void deviceTypeUpdated() {
    log.warn "${device.displayName} driver change detected"

}

private boolean isNewParseEnabled() {
    return settings?.newParse == true
}

private void ensureNewParseFlag() {
    String desiredValue = isNewParseEnabled() ? 'true' : 'false'
    if (device.getDataValue('newParse') != desiredValue) {
        device.updateDataValue('newParse', desiredValue)
        logDebug "ensureNewParseFlag: newParse flag set to ${desiredValue}"
    }
}

private void forceNewParseFlag() {
    device.updateSettings(updateSettings('newParse', true))
    ensureNewParseFlag()
    sendInfoEvent "forceNewParseFlag: newParse flag forced to <b>true</b>"
}

/**
 * Check if the current descMap is a response to a pending ping request.
 * If a match is found, calculate RTT and send event to child device.
 * @param descMap The parsed description map from Matter event
 */
void checkChildDevicePingResponse(final Map descMap) {
    if (state.pendingPings == null || state.pendingPings.isEmpty()) {
        return
    }
    
    Integer endpointInt = (descMap.endpoint != null) ? safeHexToInt(descMap.endpoint, null) : null
    if (endpointInt == null || descMap.cluster == null || descMap.attrId == null) {
        return
    }
    
    // Find matching ping entry: choose the most recent (prevents stale matches)
    String matchedPingId = null
    Map matchedPingEntry = null
    state.pendingPings.each { pingId, pingEntry ->
        if (pingEntry.deviceNumber == endpointInt &&
            pingEntry.cluster == descMap.cluster &&
            pingEntry.attrId == descMap.attrId) {
            if (matchedPingEntry == null || (pingEntry.startTime as Long) > (matchedPingEntry.startTime as Long)) {
                matchedPingId = pingId
                matchedPingEntry = pingEntry
            }
        }
    }
    
    if (matchedPingId != null && matchedPingEntry != null) {
        Long rttMs = now() - (matchedPingEntry.startTime as Long)
        
        // Send ping response event to child device
        sendHubitatEvent([
            name: 'rtt',
            value: rttMs,
            descriptionText: "Ping response time: ${rttMs}ms",
            type: 'digital'
        ], [endpoint: descMap.endpoint], false)
        
        logDebug "Ping response from device ${matchedPingEntry.deviceNumber}, RTT: ${rttMs}ms"
        
        // Cleanup
        state.pendingPings.remove(matchedPingId)
        // Do NOT unschedule pingTimeout globally; old scheduled timeouts will harmlessly no-op
    }
}

Map myParseDescriptionAsMap(description) {
    Map descMap
    try {
        descMap = matter.parseDescriptionAsMap(description)
        //log.trace "myParseDescriptionAsMap: descMap:${descMap} description:${description}"
    } catch (e) {
        logDebug "myParseDescriptionAsMap: platform parser failed with ${e.class.simpleName}, attempting custom fallback parser"
        // For global attributes that commonly cause parsing issues, create a basic descMap manually
        if (true) {
            logTrace "myParseDescriptionAsMap: attempting basic parsing for global attribute"
            try {
                Map result = [:]
                if (description.contains('endpoint:')) {
                    result.endpoint = description.split('endpoint:')[1].split(',')[0].trim()
                }
                if (description.contains('cluster:')) {
                    String clusterStr = description.split('cluster:')[1].split(',')[0].trim()
                    result.cluster = clusterStr
                    result.clusterInt = HexUtils.hexStringToInt(clusterStr)
                }
                if (description.contains('attrId:')) {
                    String attrIdStr = description.split('attrId:')[1].split(',')[0].trim()
                    result.attrId = attrIdStr
                    result.attrInt = HexUtils.hexStringToInt(attrIdStr)
                }
                if (description.contains('value:')) {
                    result.value = description.split('value:')[1].trim()
                    // For AttributeList (FFFB) and other list attributes, try to decode TLV data
                    if (/*result.attrId in ['FFFB', 'FFF8', 'FFF9', 'FFFA'] && */result.value?.length() > 4) {
                        try {
                            List<String> decodedAttrs = decodeTLVToHex(result.value)
                            if (decodedAttrs.size() > 0) {
                                result.value = decodedAttrs
                                logTrace "myParseDescriptionAsMap: decoded TLV for ${result.attrId}: ${decodedAttrs}"
                            }
                        } catch (Exception ex) {
                            logWarn "myParseDescriptionAsMap: TLV decoding failed for ${result.attrId}: ${ex} - keeping raw value"
                            // Keep the raw value rather than crashing
                        }
                    }
                }
                logTrace "myParseDescriptionAsMap: basic parsing result: ${result}"
                return result
            } catch (Exception ex) {
                logWarn "myParseDescriptionAsMap: basic parsing also failed: ${ex}"
            }
        }
        return null
    }
    if (descMap == null) {
        logWarn "myParseDescriptionAsMap: descMap is null description:${description}"
        return null
    }
    // descMap:[endpoint:00, cluster:0028, attrId:0000, value:01, clusterInt:40, attrInt:0] description:read attr - endpoint: 00, cluster: 0028, attrId: 0000, value: 0401
    
    // Handle TLV decoding for list attributes even when normal parsing succeeds
    if (descMap.value != null && descMap.attrId != null) {
        // Check for AttributeList and other list attributes that use TLV encoding
        if (descMap.attrId in ['FFFB', 'FFF8', 'FFF9', 'FFFA'] && descMap.value instanceof String && descMap.value.length() > 4) {
            //logTrace "myParseDescriptionAsMap: (newParse=false) attempting TLV decoding for ${descMap.attrId}"
            try {
                List<String> decodedAttrs = decodeTLVToHex(descMap.value)
                if (decodedAttrs.size() > 0) {
                    descMap.value = decodedAttrs
                    logTrace "myParseDescriptionAsMap: decoded TLV for normal parsing ${descMap.attrId}: ${decodedAttrs}"
                }
            } catch (Exception ex) {
                logTrace "myParseDescriptionAsMap: TLV decoding skipped for ${descMap.attrId}: ${ex} - keeping raw value"
            }
        }
        // Handle legacy empty list detection
        else if (descMap.value in ['1518', '1618', '1818'] 
            && ( descMap.attrId in ['FFF8', 'FFF9','FFFA', 'FFFB', 'FFFC', 'FFFD', 'FFFE', 'FFFF']
            || descMap.cluster == '001D')
        ) {
            descMap.value = []
            if (settings?.traceEnable) { log.warn "myParseDescriptionAsMap: legacy empty list detected: ${descMap} description:${description}" }
        }
    }
    // handle the case when parse returns null value: descMap:[endpoint:00, cluster:001D, attrId:0003, encoding:16, value:null, clusterInt:29, attrInt:3] description:read attr - endpoint: 00, cluster: 001D, attrId: 0003, encoding: 16, value: 0401042E0429042A042B042C042D042F0439043A041104120413043004310432040304040405040A040B040C04330434043504360437043818
    // -> call the custome TLV decoder
    if (/*descMap.value == null && descMap.attrId == '0003' && */descMap.cluster == '001D') {
        // descMap.value = JvmDescMap.decodedValue.toString()
         String descriptionValue = description.split('value:')[1].trim()
        //log.warn "myParseDescriptionAsMap: descMap.value is null, trying to decode it: ${descMap} descriptionValue=${descriptionValue}"
        try {
            //log.trace "myParseDescriptionAsMap: descriptionValue=${descriptionValue} for Parts List (attr 0003)"
            List<String> decodedAttrs = decodeTLVToHex(descriptionValue)
            logTrace "myParseDescriptionAsMap: decoded TLV for Parts List (attr 0003): descriptionValue=${descriptionValue},   decodedAttrs=${decodedAttrs}"
            if (decodedAttrs.size() > 0) {
                descMap.value = decodedAttrs
                logTrace "myParseDescriptionAsMap: decoded TLV for Parts List (attr 0003): ${decodedAttrs}"
            }
        } catch (Exception ex) {
            logWarn "myParseDescriptionAsMap: TLV decoding failed for Parts List (attr 0003): ${ex} - keeping raw value"
        }
    }

    return descMap
}

Map newParseCompatibilityPatch(final Map descMap) {
    Map patchedMap = descMap.clone()
    //log.trace "newParseCompatibilityPatch: descMap before patch:${descMap} settings.newParse:${settings.newParse} patchedMap before patch:${patchedMap}"
    if (settings.newParse != true) {
        return patchedMap
    }

    // patches for both Reports and Events
    if (patchedMap.cluster == null && patchedMap.clusterInt != null) {
        patchedMap.cluster = HexUtils.integerToHexString(patchedMap.clusterInt as Integer, 2)
    }
    if (patchedMap.cluster?.length() > 4) {
        patchedMap.cluster = patchedMap.cluster[-4..-1]
    }

    if (patchedMap.endpoint == null && patchedMap.endpointInt != null) {
        patchedMap.endpoint = HexUtils.integerToHexString(patchedMap.endpointInt as Integer, 1)
    }
    if (patchedMap.endpoint?.length() > 2) {
        patchedMap.endpoint = patchedMap.endpoint[-2..-1]
    }

    // callbackType:Event 
    // descMap:[callbackType:Event, endpointInt:1, clusterInt:257, evtId:2, timestamp:29456912, priority:2, data:[2:STRUCT:[3:NULL:null, 0:UINT:1, 1:UINT:0, 2:NULL:null, 5:NULL:null, 4:NULL:null]], value:[3:null, 0:1, 1:0, 2:null, 5:null, 4:null], cluster:0101, endpoint:01]
    if (descMap.callbackType == 'Event') {
        logTrace "newParseCompatibilityPatch: <b>Event</b> descMap before patch:${descMap}"
        // TODO ....
        return patchedMap
    }

    // callbackType:Report
    // descMap:[callbackType:Report, endpointInt:1, clusterInt:257, attrInt:0, data:[0:UINT:2], value:2, attrId:0000, cluster:0101, endpoint:01]
    // descMap:[callbackType:Report, endpointInt:1, clusterInt:29, attrInt:0, data:[0:ARRAY-STRUCT:[[0:UINT:10, 1:UINT:1], [0:UINT:17, 1:UINT:1]]], value:[[[tag:0, value:10], [tag:1, value:1]], [[tag:0, value:17], [tag:1, value:1]]], attrId:0000, cluster:001D, endpoint:01

    if (patchedMap.attrId == null && patchedMap.attrInt != null) {
        patchedMap.attrId = HexUtils.integerToHexString(patchedMap.attrInt as Integer, 2)
    }
    if (patchedMap.attrId?.length() > 4) {
        patchedMap.attrId = patchedMap.attrId[-4..-1]
    }
   

    //log.trace "newParseCompatibilityPatch: newParse is enabled - checking for compatibility patches for descMap:${descMap}"
    if (patchedMap.value != null && patchedMap.attrId != null) {
        // Check for AttributeList and other list attributes that use TLV encoding
        //log.trace "newParseCompatibilityPatch: descMap.attrId = ${patchedMap.attrId} descMap.value instanceof List = ${patchedMap.value instanceof List}"
        if (/*patchedMap.attrId in ['FFFB', 'FFF8', 'FFF9', 'FFFA'] && */patchedMap.value instanceof List) {
            logTrace "newParseCompatibilityPatch: newParse is enabled - converting the value ${patchedMap.value} to HEX for ${patchedMap.attrId}"
            // descMap:[callbackType:Report, endpointInt:0, clusterInt:29, attrInt:65531, data:[65531:ARRAY-UINT:[0, 1, 2, 3, 65528, 65529, 65531, 65532, 65533]], value:[0, 1, 2, 3, 65528, 65529, 65531, 65532, 65533], cluster:001D, attrId:FFFB, endpoint:00]
            // readAttribute 0x00 0x001D 0xFFFB
            //log.trace "newParseCompatibilityPatch: descMap before HEX conversion:${patchedMap}"
            try {
                List<String> hexValues = []
                if (patchedMap.value instanceof List) {
                    patchedMap.value.each { val ->
                        if (val != null) {
                            String hexStr = HexUtils.integerToHexString((Integer) val, 2)
                            hexValues.add(hexStr)
                        }
                    }
                }
                patchedMap.value = hexValues
                logTrace "newParseCompatibilityPatch: converted to HEX for ${patchedMap.attrId}: ${hexValues}"
            } catch (Exception ex) {
                logTrace "newParseCompatibilityPatch: HEX conversion failed for ${patchedMap.attrId}: ${ex} - keeping raw value"
            }
        }
        // Handle legacy empty list detection
        else if (patchedMap.value in ['1518', '1618', '1818'] 
            && ( patchedMap.attrId in ['FFF8', 'FFF9','FFFA', 'FFFB', 'FFFC', 'FFFD', 'FFFE', 'FFFF']
            || patchedMap.cluster == '001D')
        ) {
            patchedMap.value = []
            if (settings?.traceEnable) { log.warn "myParseDescriptionAsMap: legacy empty list detected: ${patchedMap} description:${description}" }
        }
        //log.trace "newParseCompatibilityPatch: descMap after patch:${patchedMap}"
    }
    else {
        logDebug "newParseCompatibilityPatch: descMap.attrId is null or descMap.value is null: descMap:${patchedMap}"
    }
    //log.trace "newParseCompatibilityPatch: patchedMap after patch:${patchedMap}"
    return patchedMap

}


boolean isDeviceDisabled(final Map descMap) {
    if (descMap.endpoint == '00') {
        return false
    }
    // get device dni
    String dni = "${device.id}-${descMap.endpoint}"
    ChildDeviceWrapper dw = getChildDevice(dni)
    if (dw == null) {
        return false
    }
    if (dw?.disabled == true) {
        if (traceEnable) { logWarn "isDeviceDisabled: device:${dw} is disabled" }
        return true
    }
    return false
}

void checkStateMachineConfirmation(final Map descMap) {
    if (descMap.callbackType == 'Event') {
        return
    }
    if (state['stateMachines'] == null || state['stateMachines']['toBeConfirmed'] == null) {
        return
    }
    List toBeConfirmedList = state['stateMachines']['toBeConfirmed']
    //logTrace "checkStateMachineConfirmation: toBeConfirmedList:${toBeConfirmedList} (endpoint:${descMap.endpoint} clusterInt:${descMap.clusterInt} attrInt:${descMap.attrInt})"
    if (toBeConfirmedList == null || toBeConfirmedList.size() == 0) {
        return
    }
    // toBeConfirmedList first element is endpoint, second is clusterInt, third is attrInt
    if (safeHexToInt(descMap.endpoint, null) == toBeConfirmedList[0] && descMap.clusterInt == toBeConfirmedList[1] && descMap.attrInt == toBeConfirmedList[2]) {
        logDebug "checkStateMachineConfirmation: endpoint:${descMap.endpoint} cluster:${descMap.cluster} attrId:${descMap.attrId} - CONFIRMED!"
        state['stateMachines']['Confirmation'] = true
    }
}

String getClusterName(final String cluster) { return MatterClusters[HexUtils.hexStringToInt(cluster)] ?: UNKNOWN }
String getAttributeName(final Map descMap) { return (descMap.attrId != null) ? getAttributeName(descMap.cluster, descMap.attrId) : UNKNOWN }
String getAttributeName(final String cluster, String attrId) { return getAttributesMapByClusterId(cluster)?.get(HexUtils.hexStringToInt(attrId)) ?: GlobalElementsAttributes[HexUtils.hexStringToInt(attrId)] ?: UNKNOWN }
String getFingerprintName(final Map descMap) { return descMap.endpoint == '00' ? 'bridgeDescriptor' : "fingerprint${descMap.endpoint}" }
String getFingerprintName(final Integer endpoint) { return getFingerprintName([endpoint: HexUtils.integerToHexString(endpoint, 1)]) }

String getStateClusterName(final Map descMap) {
    String clusterMapName = ''
    if (descMap.cluster == '001D') {
        clusterMapName = getAttributeName(descMap)
    }
    else {
        clusterMapName = descMap.cluster + '_' + descMap.attrId
    }
    return clusterMapName
}

@CompileStatic
String getDeviceDisplayName(final Integer endpoint) { return getDeviceDisplayName(HexUtils.integerToHexString(endpoint, 1)) }
/**
 * Returns the device label based on the provided endpoint.
 * If a child device exists, the label is retrieved from the child device display name.
 * If no child device exists yet, the label is constructed by combining the endpoint with the vendor name, product name, and custom label.
 * If the vendor name or product name is available, they are included in parentheses.
 *
 * @param endpoint The endpoint of the device.
 * @return The device display label.
 */
String getDeviceDisplayName(final String endpoint) {
    // if a child device exists, use its endpoint to get the ${device.displayName}
    if (getChildDevice("${device.id}-${endpoint}") != null) {
        return getChildDevice("${device.id}-${endpoint}")?.displayName
    }
    String label = "Bridge#${device.id} Device#${endpoint} "
    String fingerprintName = getFingerprintName([endpoint: endpoint])
    String vendorName  = state[fingerprintName]?.VendorName ?: ''
    String productName = state[fingerprintName]?.ProductName ?: ''
    String customLabel = state[fingerprintName]?.Label ?: ''
    if (vendorName || productName) {
        label += "(${vendorName} ${productName}) "
    }
    label += customLabel
    return label
}

// credits: @jvm33
// Matter payloads need hex parameters of greater than 2 characters to be pair-reversed.
// This function takes a list of parameters and pair-reverses those longer than 2 characters.
// Alternatively, it can take a string and pair-revers that.
// Thus, e.g., ["0123", "456789", "10"] becomes "230189674510" and "123456" becomes "563412"
@CompileStatic
private String byteReverseParameters(String oneString) { byteReverseParameters([] << oneString) }
@CompileStatic
private String byteReverseParameters(List<String> parameters) {
    StringBuilder rStr = new StringBuilder(64)

    for (hexString in parameters) {
        if (hexString.length() % 2) throw new Exception("In method byteReverseParameters, trying to reverse a hex string that is not an even number of characters in length. Error in Hex String: ${hexString}, All method parameters were ${parameters}.")

        for(Integer i = hexString.length() -1 ; i > 0 ; i -= 2) {
            rStr << hexString[i-1..i]
        }
    }
    return rStr
}

// 7.13. Global Elements - used for self-description of the server
//@CompileStatic
void parseGlobalElements(final Map descMap) {
    //logTrace "parseGlobalElements: descMap:${descMap}"
    switch (descMap.attrId) {
        case '00FE' :   // FabricIndex          fabric-idx
        case 'FFF8' :   // GeneratedCommandList list[command-id]
        case 'FFF9' :   // AcceptedCommandList  list[command-id]
        case 'FFFA' :   // EventList            list[eventid]
        case 'FFFC' :   // FeatureMap           map32
        case 'FFFD' :   // ClusterRevision      uint16
        case 'FFFB' :   // AttributeList        list[attribid]
            String fingerprintName = getFingerprintName(descMap)
            String attributeName = getStateClusterName(descMap)
            String action = 'stored in'
            if (state[fingerprintName] == null) {
                state[fingerprintName] = [:]
            }
            if (state[fingerprintName][attributeName] == null) {
                state[fingerprintName][attributeName] = [:]
                action = 'created in'
            }
            state[fingerprintName][attributeName] = descMap.value
            logTrace "parseGlobalElements: cluster: <b>${getClusterName(descMap.cluster)}</b> (0x${descMap.cluster}) attr: <b>${attributeName}</b> (0x${descMap.attrId})  value:${descMap.value} <b>-> ${action}</b> [$fingerprintName][$attributeName]"
            //logTrace "parseGlobalElements: state[${fingerprintName}][${attributeName}] = ${state[fingerprintName][attributeName]}"
            
            // Update child device fingerprint data with cluster-specific data
            updateChildFingerprintData(fingerprintName, attributeName, descMap.value)
            
            // Mark attribute as received for state machine wait logic
            markClusterDataReceived(descMap.endpoint, descMap.cluster, descMap.attrId)
            break
        default :
            break   // not a global element
    }

}

void gatherAttributesValuesInfo(final Map descMap) {
    if (descMap == null || descMap?.attrId == null) {
        return
    }
    Integer attrInt = descMap.attrInt as Integer
    String  attrName = getAttributeName(descMap)
    Integer tempIntValue
    String  tmpStr
    if (state.states['isInfo'] == true) {
        logTrace "gatherAttributesValuesInfo: <b>isInfo:${state.states['isInfo']}</b> state.states['cluster'] = ${state.states['cluster']} "
        if (state.states['cluster'] == descMap.cluster) {
            if (descMap.value != null && descMap.value != '') {
                tmpStr = "[${descMap.attrId}] ${attrName}"
                if (state.tmp?.contains(tmpStr)) {
                    logDebug "gatherAttributesValuesInfo: tmpStr:${tmpStr} is already in the state.tmp"
                    return
                }
                // Normalize DeviceTypeList for display (show device types only, no revision numbers)
                // TODO - remove this patch when newParse is fully adopted !
                def displayValue = descMap.value
                String deviceTypeNamesStr = ''
                
                // Only process DeviceTypeList from Descriptor cluster (0x001D)
                if (descMap.cluster == '001D' && attrName == 'DeviceTypeList' && descMap.value instanceof List) {
                    List rawList = []
                    
                    if (settings.newParse == true) {
                        // Extract device type IDs from newParse format: [[[tag:0, value:22], [tag:1, value:1]]]
                        descMap.value.each { structEntry ->
                            if (structEntry instanceof List && structEntry.size() >= 1) {
                                def deviceTypeEntry = structEntry[0]
                                if (deviceTypeEntry instanceof Map && deviceTypeEntry.value != null) {
                                    rawList.add(HexUtils.integerToHexString((Integer) deviceTypeEntry.value, 2))
                                }
                            }
                        }
                    } else {
                        // Extract device type IDs from legacy format (even-indexed elements only)
                        List fullList = descMap.value as List
                        rawList = (0..<fullList.size()).findAll { (it % 2) == 0 }.collect { fullList[it] }
                    }
                    
                    displayValue = rawList
                    
                    // Add human-readable device type names
                    Map typeNames = deviceTypeNames(displayValue)
                    if (typeNames.names) {
                        String namesStr = typeNames.names.collect { "'${it}'" }.join(', ')
                        deviceTypeNamesStr = typeNames.names.size() > 1 && typeNames.best 
                            ? "  (${namesStr}, best = '${typeNames.best}')"
                            : "  (${namesStr})"
                    }
                }
                //
                try {
                    tempIntValue = HexUtils.hexStringToInt(displayValue)
                    if (tempIntValue >= 10) {
                        tmpStr += ' = 0x' + displayValue + ' (' + tempIntValue + ')'
                        if (descMap.attrId == '0015') {
                            tmpStr += " [Matter Spec: ${(tempIntValue>>24)&0xFF}.${(tempIntValue>>16)&0xFF}.${(tempIntValue>>8)&0xFF}.${tempIntValue&0xFF}]"
                        } else if (descMap.attrId == '000B') {
                            // ManufacturingDate: 0xYYYYMMDD (hex string)
                            String dateStr = String.format('%08X', tempIntValue)
                            int y = dateStr[0..3] as int
                            int m = dateStr[4..5] as int
                            int d = dateStr[6..7] as int
                            tmpStr += " [Manufacturing Date: ${y}-${String.format('%02d', m)}-${String.format('%02d', d)}]"
                        } else if (descMap.attrId == '0009') {
                            tmpStr += " [SW Ver: ${(tempIntValue>>24)&0xFF}.${(tempIntValue>>16)&0xFF}.${(tempIntValue>>8)&0xFF}.${tempIntValue&0xFF}]"
                        }
                    } else {
                        tmpStr += ' = ' + displayValue
                    }
                } catch (e) {
                    tmpStr += ' = ' + displayValue + deviceTypeNamesStr
                }
                state.tmp = (state.tmp ?: '') + "${tmpStr} " + '<br>'
            }
        }
    }
    else if ((state.states['isPing'] ?: false) == true && descMap.cluster == '0028' && descMap.attrId == '0000') {
        Long now = new Date().getTime()
        Integer timeRunning = now.toInteger() - (state.lastTx['pingTime'] ?: '0').toInteger()
        if (timeRunning > 0 && timeRunning < MAX_PING_MILISECONDS) {
            state.stats['pingsOK'] = (state.stats['pingsOK'] ?: 0) + 1
            if (timeRunning < safeToInt((state.stats['pingsMin'] ?: '999'))) { state.stats['pingsMin'] = timeRunning }
            if (timeRunning > safeToInt((state.stats['pingsMax'] ?: '0')))   { state.stats['pingsMax'] = timeRunning }
            state.stats['pingsAvg'] = approxRollingAverage(safeToDouble(state.stats['pingsAvg']), safeToDouble(timeRunning)) as int
            sendRttEvent()
        } else {
            logWarn "unexpected ping timeRunning=${timeRunning} "
        }
        state.states['isPing'] = false
    }
    else {
        logTrace "gatherAttributesValuesInfo: isInfo:${state.states['isInfo']} descMap:${descMap}"
    }
}

//@CompileStatic
void parseGeneralDiagnostics(final Map descMap) {
    //logTrace "parseGeneralDiagnostics: descMap:${descMap}"
    Integer value
    switch (descMap.attrId) {
        case '0001' :   // RebootCount -  a best-effort count of the number of times the Node has rebooted
            value = getDescMapValueAsInt(descMap)
            sendHubitatEvent([name: 'rebootCount', value: value,  descriptionText: "${getDeviceDisplayName(descMap.endpoint)} RebootCount is ${value}"])
            break
        case '0002' :   // UpTime -  a best-effort assessment of the length of time, in seconds,since the Node’s last reboot
            value = getDescMapValueAsInt(descMap)
            sendHubitatEvent([name: 'upTime', value:value,  descriptionText: "${getDeviceDisplayName(descMap.endpoint)} UpTime is ${value} seconds"])
            break
        case '0003' :   // TotalOperationalHours -  a best-effort attempt at tracking the length of time, in hours, that the Node has been operational
            value = getDescMapValueAsInt(descMap)
            sendHubitatEvent([name: 'totalOperationalHours', value: value,  descriptionText: "${getDeviceDisplayName(descMap.endpoint)} TotalOperationalHours is ${value} hours"])
            break
        default :
            if (descMap.attrId != '0000') { if (traceEnable) { logInfo "parseGeneralDiagnostics: ${attrName} = ${descMap.value}" } }
            break
    }
}

Integer getDescMapValueAsInt(final Map descMap) {
    if (descMap == null) {
        return null
    }
    // Handle value as a list of maps (e.g., [[tag:0, value:268], ...])
    def patchedValue = descMap.value
    if (patchedValue instanceof List && patchedValue && patchedValue[0] instanceof Map && patchedValue[0].containsKey('value')) {
        patchedValue = patchedValue[0].value
    } else if (patchedValue instanceof Map && patchedValue.containsKey('value')) {
        patchedValue = patchedValue.value
    }
    // If already an Integer, return directly (newParse = true case)
    if (patchedValue instanceof Integer) {
        return patchedValue
    }
    Map dataMap = (descMap.data instanceof Map) ? descMap.data : null
    //log.trace "getDescMapValueAsInt: dataMap:${dataMap} descMap.data instanceof Map:${descMap.data instanceof Map}"
    if (dataMap != null) {
        for (Object entry : dataMap.values()) {
            String typeHint = null
            if (entry instanceof Map) {
                logTrace "getDescMapValueAsInt: entry Map:${entry}"
                typeHint = (entry.type ?: entry.dataType)?.toString()?.toUpperCase()
            } else if (entry instanceof String) {
                logTrace "getDescMapValueAsInt: entry String:${entry}"
                typeHint = entry.toUpperCase()
            }
            if (typeHint?.contains('UINT')) {
                logTrace "getDescMapValueAsInt: typeHint contains UINT, returning safeNumberToInt for value:${patchedValue}"
                return safeNumberToInt(patchedValue, null)
            }
        }
    }
    try {
        //logTrace "getDescMapValueAsInt: trying 'toInteger' for value:${patchedValue}"
        return patchedValue.toInteger()
    } catch (e) {
        String rawValue = patchedValue?.toString()?.trim()
        if (rawValue) {
            String hexCandidate = rawValue
            if (hexCandidate.startsWith('0x') || hexCandidate.startsWith('0X')) {
                hexCandidate = hexCandidate.substring(2)
            }
            if (hexCandidate.matches('[0-9A-Fa-f]+')) {
                return HexUtils.hexStringToInt(hexCandidate)
            }
        }
        logWarn "getDescMapValueAsInt: exception ${e} converting value:${patchedValue} to Integer"
        return safeNumberToInt(patchedValue, null)
    }
}

void parsePowerSource(final Map descMap) {
    logTrace "parsePowerSource: descMap:${descMap}"
    String attrName = getAttributeName(descMap)
    //log.trace "after getAttributeName:${attrName}"
    Integer value
    String descriptionText = ''
    Map eventMap = [:]
    String eventName = attrName[0].toLowerCase() + attrName[1..-1]  // change the attribute name first letter to lower case
    switch (attrName) {
        case ['BatTimeRemaining', 'BatChargeLevel', 'BatReplacementNeeded', 'BatReplaceability', 'BatReplacementDescription', 'BatQuantity'] :
            descriptionText = "${getDeviceDisplayName(descMap?.endpoint)} Power source ${attrName} is ${descMap.value}"
            eventMap = [name: eventName, value: descMap.value, descriptionText: descriptionText]
            break
        case 'BatPercentRemaining' :   // BatteryPercentageRemaining 0x000C
            // newParse : : descMap:[callbackType:Report, endpointInt:2, clusterInt:47, attrInt:12, data:[12:UINT:114], value:114, attrId:000C, cluster:002F, endpoint:02]
            value = getDescMapValueAsInt(descMap)
            // Patch for Zemismart M1 - reports battery percentage remaining as 1 ???? TODO
            if (value == 1 && device.getDataValue('model') == 'Zemismart M1 Hub') {
                value = 200  // interpret as 100%
                logDebug "parsePowerSource: applying Zemismart M1 battery percentage patch, setting value to 200"
            }
            descriptionText = "${getDeviceDisplayName(descMap?.endpoint)} Battery percentage remaining is ${value / 2}% (raw:${descMap.value})"
            eventMap = [name: 'battery', value: value / 2, descriptionText: descriptionText]
            break
        case 'BatVoltage' :   // BatteryVoltage 0x000B
            //log.trace "parsePowerSource: before getDescMapValueAsInt for BatVoltage:${descMap.value}"
            value = getDescMapValueAsInt(descMap)
            //log.trace "parsePowerSource: after getDescMapValueAsInt for BatVoltage:${value}"
            descriptionText = "${getDeviceDisplayName(descMap?.endpoint)} Battery voltage is ${value / 1000}V (raw:${descMap.value})"
            eventMap = [name: 'batteryVoltage', value: value / 1000, descriptionText: descriptionText]
            break
        case 'Status' :  // PowerSourceStatus 0x0000
            String statusDesc = PowerSourceClusterStatus[getDescMapValueAsInt(descMap)] ?: UNKNOWN
            statusDesc = statusDesc[0].toLowerCase() + statusDesc[1..-1]  // change the powerSourceStatus attribute value first letter to lower case
            descriptionText = "${getDeviceDisplayName(descMap?.endpoint)} Power source status is ${statusDesc} (raw:${descMap.value})"
            eventMap = [name: 'powerSourceStatus', value: statusDesc, descriptionText: descriptionText]
            break
        case 'Order' :   // PowerSourceOrder 0x0001
            descriptionText = "${getDeviceDisplayName(descMap?.endpoint)} Power source order is ${descMap.value}"
            eventMap = [name: 'powerSourceOrder', value: descMap.value, descriptionText: descriptionText]
            break
        case 'Description' :   // PowerSourceDescription 0x0002
            descriptionText = "${getDeviceDisplayName(descMap?.endpoint)} Power source description is ${descMap.value}"
            eventMap = [name: 'powerSourceDescription', value: descMap.value, descriptionText: descriptionText]
            break
        default :
            logInfo "Power source ${attrName} is ${descMap.value} (unprocessed)"
            break
    }
    if (eventMap != [:]) {
        eventMap.type = 'physical'
        eventMap.isStateChange = true
        sendHubitatEvent(eventMap, descMap, ignoreDuplicates = true) // bridge events
    }
}

void parseBasicInformationCluster(final Map descMap) {  // 0x0028 BasicInformation (the Bridge)
    Map eventMap = [:]
    String attrName = getAttributeName(descMap)
    String fingerprintName = getFingerprintName(descMap)
    if (state[fingerprintName] == null) { state[fingerprintName] = [:] }
    String eventName = attrName[0].toLowerCase() + attrName[1..-1]  // change the attribute name first letter to lower case
    if (attrName in ['ProductName', 'NodeLabel', 'SoftwareVersionString', 'Reachable']) {
        if (descMap.value != null && descMap.value != '') {
            state[fingerprintName][attrName] = descMap.value
            eventMap = [name: eventName, value:descMap.value, descriptionText: "${getDeviceDisplayName(descMap?.endpoint)}  ${eventName} is: ${descMap.value}"]
            if (logEnable) { logInfo "parseBasicInformationCluster: ${attrName} = ${descMap.value}" }
        }
    }
    if (eventMap != [:]) {
        eventMap.type = 'physical'; eventMap.isStateChange = true
        sendHubitatEvent(eventMap, descMap) // bridge events
    }
}

void parseBridgedDeviceBasic(final Map descMap) {       // 0x0039 BridgedDeviceBasic (the child devices)
    Map eventMap = [:]
    String attrName = getAttributeName(descMap)
    String fingerprintName = getFingerprintName(descMap)
    if (state[fingerprintName] == null) { state[fingerprintName] = [:] }
    String eventName = attrName[0].toLowerCase() + attrName[1..-1]  // change the attribute name first letter to lower case
    if (attrName in ['VendorName', 'ProductName', 'NodeLabel', 'SoftwareVersionString', 'Reachable', 'ProductLabel']) {
        if (descMap.value != null && descMap.value != '') {
            state[fingerprintName][attrName] = descMap.value
            
            // Note: These values are stored in state[fingerprintName] and don't need to be duplicated in device data
            // Child drivers can access them via parent methods if needed
            
            eventMap = [name: eventName, value:descMap.value, descriptionText: "${getDeviceDisplayName(descMap?.endpoint)}  ${eventName} is: ${descMap.value}"]
            if (logEnable) { logInfo "parseBridgedDeviceBasic: ${attrName} = ${descMap.value}" }
        }
    }
    if (eventMap != [:]) {
        eventMap.type = 'physical'; eventMap.isStateChange = true
        sendHubitatEvent(eventMap, descMap) // child events
    }
}

void parseDescriptorCluster(final Map descMap) {    // 0x001D Descriptor
    logTrace "parseDescriptorCluster: descMap:${descMap}"
    String attrName = getAttributeName(descMap)    //= DescriptorClusterAttributes[descMap.attrInt as int] ?: GlobalElementsAttributes[descMap.attrInt as int] ?: UNKNOWN
    String endpointId = descMap.endpoint
    String fingerprintName =  getFingerprintName(descMap)  /*"fingerprint${endpointId}"
/*
[0000] DeviceTypeList = [0013, 0301] ('Bridged Node', 'Thermostat', best = 'Thermostat')
[0001] ServerList = [03, 1D, 1F, 28, 29, 2A, 2B, 2C, 2E, 30, 31, 32, 33, 34, 37, 39, 3C, 3E, 3F, 40]
[0002] ClientList = [03, 1F, 29, 39]
[0003] PartsList = [01, 03, 04, 05, 06, 07, 08, 09, 0A, 0B, 0C, 0D, 0E, 0F, 10, 11]
*/
    switch (descMap.attrId) {
        case ['0000', '0001', '0002', '0003'] :
            if (state[fingerprintName] == null) { state[fingerprintName] = [:] }
            // Normalize DeviceTypeList at storage time for both legacy and newParse formats
            if (attrName == 'DeviceTypeList' && descMap.value instanceof List) {
                List rawList = descMap.value as List
                List deviceTypesOnly = []
                if (settings?.newParse == true && rawList && rawList[0] instanceof List && rawList[0][0] instanceof Map && rawList[0][0].containsKey('value')) {
                    // newParse format: list of lists of maps
                    rawList.each { structEntry ->
                        if (structEntry instanceof List && structEntry.size() >= 1) {
                            def deviceTypeEntry = structEntry[0]
                            if (deviceTypeEntry instanceof Map && deviceTypeEntry.value != null) {
                                deviceTypesOnly.add(HexUtils.integerToHexString((Integer) deviceTypeEntry.value, 2))
                            }
                        }
                    }
                } else {
                    // Legacy format: even-indexed elements only
                    deviceTypesOnly = (0..<rawList.size()).findAll { (it % 2) == 0 }.collect { rawList[it] }
                }
                state[fingerprintName][attrName] = deviceTypesOnly
                logTrace "parse: Descriptor (${descMap.cluster}): ${attrName} = <b>-> normalized and stored</b> ${deviceTypesOnly} (from raw ${rawList})"
            } else {
                state[fingerprintName][attrName] = descMap.value
                logTrace "parse: Descriptor (${descMap.cluster}): ${attrName} = <b>-> updated state[$fingerprintName][$attrName]</b> to ${descMap.value}"
            }
            if (endpointId == '00' && descMap.cluster == '001D') {
                if (attrName == 'PartsList') {
                    logDebug "parseDescriptorCluster: Bridge partsList: ${descMap.value} descMap:${descMap}"
                    List partsList = descMap.value as List
                    if (partsList == null || partsList.isEmpty()) {
                        logWarn "parseDescriptorCluster: PartsList is null or empty for endpoint ${descMap.endpoint}"
                        return
                    }
                    int partsListCount = partsList.size()   // the number of the elements in the partsList
                    int oldCount = device.currentValue('endpointsCount') ?: 0 as int
                    String descriptionText = "${getDeviceDisplayName(descMap?.endpoint)} Bridge partsListCount is: ${partsListCount}"
                    sendHubitatEvent([name: 'endpointsCount', value: partsListCount, descriptionText: descriptionText], descMap, true) // bridge event - sent only when endpointsCount changes: changed 01/14/2026
                    if (partsListCount != oldCount) {
                        logInfo "THE NUMBER OF THE BRIDGED DEVICES CHANGED FROM ${oldCount} TO ${partsListCount} !!!"
                    }
                }
            }
            break
        default :
            logTrace "parseDescriptorCluster: Descriptor: ${attrName} = ${descMap.value}"
            break
    }
}

void parseOnOffCluster(final Map descMap) {
    logTrace "parseOnOffCluster: descMap:${descMap}"
    if (descMap.cluster != '0006') { logWarn "parseOnOffCluster: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return  }
    Integer value

    switch (descMap.attrId) {
        case '0000' : // Switch
            String switchState = ((descMap.value?.toString()?.trim()?.toLowerCase()) in ['1', '01', 'true', 'on']) ? 'on' : 'off'
            sendHubitatEvent([
                name: 'switch',
                value: switchState,
                descriptionText: "${getDeviceDisplayName(descMap.endpoint)} switch is ${switchState}"
            ], descMap, true)
            break
        case '4000' : // GlobalSceneControl
            if (logEnable) { logInfo "parse: Switch: GlobalSceneControl = ${descMap.value}" }
            if (state.onOff  == null) { state.onOff =  [:] } ; state.onOff['GlobalSceneControl'] = descMap.value
            break
        case '4001' : // OnTime
            if (logEnable) { logInfo  "parse: Switch: OnTime = ${descMap.value}" }
            if (state.onOff  == null) { state.onOff =  [:] } ; state.onOff['OnTime'] = descMap.value
            break
        case '4002' : // OffWaitTime
            if (logEnable) { logInfo  "parse: Switch: OffWaitTime = ${descMap.value}" }
            if (state.onOff  == null) { state.onOff =  [:] } ; state.onOff['OffWaitTime'] = descMap.value
            break
        case '4003' : // StartUpOnOff
            value = descMap.value as int
            String startUpOnOffText = "parse: Switch: StartUpOnOff = ${descMap.value} (${StartUpOnOffEnumOpts[value] ?: UNKNOWN})"
            if (logEnable) { logInfo  "${startUpOnOffText}" }
            if (state.onOff  == null) { state.onOff =  [:] } ; state.onOff['StartUpOnOff'] = descMap.value
            break
        case ['FFF8', 'FFF9', 'FFFA', 'FFFB', 'FFFC', 'FFFD', '00FE'] :
            logTrace "parse: Switch: ${attrName} = ${descMap.value}"
            break
        default :
            logWarn "parseOnOffCluster: unexpected attrId:${descMap.attrId} (raw:${descMap.value})"
    }
}

Integer hex254ToInt100(String value) {
    return Math.round(hexStrToUnsignedInt(value) / 2.54)
}

Integer int256ToInt100(Integer value) {
    return Math.round(value / 2.54)
}

String int100ToHex254(value) {
    return intToHexStr(Math.round(value * 2.54))
}

Integer getLuxValue(rawValue) {
    return Math.max((Math.pow(10, (rawValue / 10000)) - 1).toInteger(), 1)
}


void parseLevelControlCluster(final Map descMap) {
    Integer scaledValue = safeHexToInt(descMap.value)   // integer value if newParse=true, else hex string 
    logTrace "parseLevelControlCluster: _scaledValue:${scaledValue} (descMap:${descMap})"
    if (descMap.cluster != '0008') { logWarn "parseLevelControlCluster: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    Integer value
    switch (descMap.attrId) {
        case '0000' : // CurrentLevel
            if (descMap.callbackType == 'Report') {
                // newParse : [callbackType:Report, endpointInt:23, clusterInt:8, attrInt:0, data:[0:UINT:53], value:53, cluster:0008, endpoint:17, attrId:0000]
                value = int256ToInt100(scaledValue ?: 0)
                logTrace "parseLevelControlCluster: newParse:true : CurrentLevel Report scaledValue:${scaledValue} value:${value}"
            } else {
                value = int256ToInt100(scaledValue ?: 0)
                logTrace "parseLevelControlCluster: newParse:false : CurrentLevel Report scaledValue:${scaledValue} value:${value}"
            }
            sendHubitatEvent([
                name: 'level',
                value: value, //.toString(),
                descriptionText: "${getDeviceDisplayName(descMap.endpoint)} level is ${value}"
            ], descMap, true)
            break
        default :
            value = scaledValue
            Map eventMap = [:]
            String attrName = getAttributeName(descMap)
            String fingerprintName = getFingerprintName(descMap)
            if (state[fingerprintName] == null) { state[fingerprintName] = [:] }
            String eventName = attrName[0].toLowerCase() + attrName[1..-1]  // change the attribute name first letter to lower case
            if (attrName in ['CurrentLevel', 'RemainingTime', 'MinLevel', 'MaxLevel', 'OnOffTransitionTime', 'OnLevel', 'OnTransitionTime', 'OffTransitionTime', 'Options', 'StartUpCurrentLevel', 'Reachable']) {
                eventMap = [name: eventName, value:value, descriptionText: "${eventName} is: ${value}"]
                if (logEnable) { logInfo "parseLevelControlCluster: ${attrName} = ${value}" }
            }
            else {
                logDebug "parseLevelControlCluster: unsupported LevelControl: attribute ${descMap.attrId} ${attrName} = ${value}"
            }
            if (eventMap != [:]) {
                eventMap.type = 'physical'; eventMap.isStateChange = true
                sendHubitatEvent(eventMap, descMap, true) // child events
            }
    }
}

// Method for parsing occupancy sensing
void parseOccupancySensing(final Map descMap) {
    if (descMap.cluster != '0406') {
        logWarn "parseOccupancySensing: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"
        return
    }
    String motionAttr = ((descMap.value?.toString()?.trim()?.toLowerCase()) in ['1', '01', 'true', 'on']) ? 'active' : 'inactive'
    if (descMap.attrId == '0000') { // Occupancy
        sendHubitatEvent([
            name: 'motion',
            value: motionAttr,
            descriptionText: "${getDeviceDisplayName(descMap.endpoint)} motion is ${motionAttr}"
        ], descMap, true)
    } else {
        logTrace "parseOccupancySensing: ${(OccupancySensingClusterAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
    }
}

String getEventName(final Map descMap) {
    if (descMap?.evtId == null) {
        return 'NONE'
    }

    Integer evtInt = null
    Object evtIdVal = descMap.evtId

    // newParse=true: Hubitat provides evtId as a Number (typically Long)
    if (evtIdVal instanceof Number) {
        evtInt = ((Number)evtIdVal).intValue()
    }
    // newParse=false: evtId is typically a 4-char hex String (e.g. '0006')
    else {
        evtInt = safeHexToInt(evtIdVal?.toString(), null)
    }

    return (evtInt != null) ? (getEventsMapByClusterId(descMap.cluster)?.get(evtInt) ?: UNKNOWN) : UNKNOWN
}

String getEventName(final String cluster, String evtId) {
    Integer evtInt = safeHexToInt(evtId, null)
    return (evtInt != null) ? (getEventsMapByClusterId(cluster)?.get(evtInt) ?: UNKNOWN) : UNKNOWN
}

// Filter noisy Matter *events* (evtId present) that arrive shortly after (re)subscription.
// Some devices/controllers send a burst of events right after subscribe; these are often duplicates/stale.
private boolean shouldFilterNoisyPostSubscribeEvent(final Map descMap, final String source = null) {
    if (descMap?.evtId == null) {
        return false
    }

    def lastSubscribe = state.lastTx?.subscribeTime
    if (lastSubscribe == null) {
        return false
    }

    long ageMs = now() - (lastSubscribe as long)
    long uptimeSec = location.hub.uptime ?: 0L
    // Use 30s threshold if hub just booted (uptime < 5min) OR subscription is recent (age < 60s)
    long thresholdMs = (uptimeSec < 300 || ageMs < 60000) ? 30000 : 10000

    if (settings?.logEnable) {
        String src = (source != null) ? "${source}: " : ''
        logDebug "${src}EVENT age=${ageMs}ms uptime=${uptimeSec}s threshold=${thresholdMs}ms"
    }

    if (ageMs >= 0 && ageMs < thresholdMs) {
        String src = (source != null) ? "${source}: " : ''
        logDebug "${src}FILTERED event (ep=${descMap.endpoint} evt=${descMap.evtId}) ${ageMs}ms after subscribe (hub uptime=${uptimeSec}s)"
        return true
    }

    return false
}


// Method for parsing 003B Switch cluster attributes and events
void parseSwitch(final Map descMap) {
    if (descMap.cluster != '003B') { logWarn "parseSwitch: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }

    // Filter noisy post-(re)subscription Matter events (now handled centrally too)
    if (shouldFilterNoisyPostSubscribeEvent(descMap, 'parseSwitch')) {
        logDebug "parseSwitch: FILTERED noisy post-subscribe event: descMap:${descMap}"
        return
    }
    
    String attributeName = (descMap.attrId != null) ? getAttributeName(descMap) : null
    String eventName = (descMap.evtId != null) ? getEventName(descMap) : null
    logTrace "parseSwitch: <b>UNPROCESSED</b> ${attributeName ?: eventName ?: UNKNOWN} = ${descMap.value}"
    // send the unprocessed attributes and events to the child driver for further processing
    sendHubitatEvent([
        name: 'unprocessed',
        value: JsonOutput.toJson(descMap),
        descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} unprocessed cluster ${descMap.cluster} ${descMap.attrId ? 'attribute ' + descMap.attrId : 'event ' + descMap.evtId} <i>(to be re-processed in the child driver!)</i>"
    ], descMap, ignoreDuplicates = false)
}

// Method for parsing Boolean State Cluster 0x0045  (Contact Sensor and Water Sensor)
void parseBooleanState(final Map descMap) {
    if (descMap.cluster != '0045') { logWarn "parseBooleanState: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    
    if (descMap.attrId == '0000') { // StateValue attribute
        boolean isActive = ((descMap.value?.toString()?.trim()?.toLowerCase()) in ['1', '01', 'true', 'on'])
        String boolValue = isActive ? 'closed' : 'open'
        String waterValue = isActive ? 'wet' : 'dry'
        
        // Determine sensor type from DeviceTypeList in fingerprint
        String fingerprintName = getFingerprintName(descMap)
        List<String> deviceTypes = state[fingerprintName]?.DeviceTypeList ?: []
        
        // Check if it's a water leak sensor (0x0043) vs contact sensor (0x0015)
        boolean isWaterSensor = deviceTypes.any { it.toUpperCase() in ['43', '0043'] }
        
        if (isWaterSensor) {
            sendHubitatEvent([
                name: 'water',
                value: waterValue,
                descriptionText: "${getDeviceDisplayName(descMap.endpoint)} water is ${waterValue} (raw:${descMap.value})"
            ], descMap, true)
        } else {
            sendHubitatEvent([
                name: 'contact',
                value: boolValue,
                descriptionText: "${getDeviceDisplayName(descMap.endpoint)} contact is ${boolValue} (raw:${descMap.value})"
            ], descMap, true)
        }
    } else {
        logTrace "parseBooleanState: ${(BooleanStateClusterAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
    }
}

// Method for parsing illuminance measurement
void parseIlluminanceMeasurement(final Map descMap) {// 0400
  
    if (descMap.cluster != '0400') { logWarn "parseIlluminanceMeasurement: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    if (descMap.attrId == '0000') { // Illuminance
        Integer valueInt = (descMap.value instanceof Integer) ? descMap.value : HexUtils.hexStringToInt(descMap.value)
        Integer valueLux = Math.pow( 10, (valueInt -1) / 10000)  as Integer
        if (valueLux < 0 || valueLux > 100000) {
            logWarn "parseIlluminanceMeasurement: valueInt:${valueInt} is out of range"
            return
        }
        int lux = valueLux.toInteger()
        illumEvent(lux, descMap)
        /*
           sendHubitatEvent([
            name: 'illuminance',
            value: valueLux as int,
            unit: 'lx',
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)}  illuminance is ${valueLux} lux"
        ], descMap, true)
        
        */
    } else {
        logTrace "parseIlluminanceMeasurement: ${(IlluminanceMeasurementClusterAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
    }
}

// Method for parsing temperature measurement
void parseTemperatureMeasurement(final Map descMap) { // 0402
    if (descMap.cluster != '0402') { logWarn "parseTemperatureMeasurement: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    if (descMap.attrId == '0000') { // Temperature
        Double valueInt = safeHexToInt(descMap.value) / 100.0
        String unit
        //log.debug "parseTemperatureMeasurement: location.temperatureScale:${location.temperatureScale}"
        if (valueInt < -100 || valueInt > 300) {
            logWarn "parseTemperatureMeasurement: valueInt:${valueInt} is out of range"
            return
        }
        if (location.temperatureScale == 'F') {
            valueInt = (valueInt * 1.8) + 32
            unit = "\u00B0" + 'F'
        }
        else {
            unit = "\u00B0" + 'C'
        }
        sendHubitatEvent([
            name: 'temperature',
            value: valueInt.round(1) as double,
            descriptionText: "${getDeviceDisplayName(descMap.endpoint)} temperature is ${valueInt.round(2)} ${unit}",
            unit: unit
        ], descMap, true)
    } else {
        logTrace "parseTemperatureMeasurement: ${(TemperatureMeasurementClusterAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
        logTrace "parseTemperatureMeasurement: ${getAttributeName(descMap)} = ${descMap.value}"
    }
}

// Method for parsing humidity measurement
void parseHumidityMeasurement(final Map descMap) { // 0405
    if (descMap.cluster != '0405') {
        logWarn "parseHumidityMeasurement: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"
        return
    }
    if (descMap.attrId == '0000') { // Humidity
        Double valueInt = safeHexToInt(descMap.value) / 100.0
        if (valueInt <= 0 || valueInt > 100) {
            logWarn "parseHumidityMeasurement: valueInt:${valueInt} is out of range"
            return
        }
        sendHubitatEvent([
            name: 'humidity',
            value: valueInt.round(0) as int,
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)}  humidity is ${valueInt.round(1)} %"
        ], descMap, true)
    } else {
        logTrace "parseHumidityMeasurement: ${(RelativeHumidityMeasurementClusterAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
    }
}

void parseDoorLock(final Map descMap) { // 0101
    if (descMap.cluster != '0101') { logWarn "parseDoorLock: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    if (descMap.attrId == '0000') { // LockState
        boolean isLocked = ((descMap.value?.toString()?.trim()?.toLowerCase()) in ['1', '01', 'true', 'on'])
        String lockState = isLocked ? 'locked' : 'unlocked'
        sendHubitatEvent([
            name: 'lock',
            value: lockState,
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} is ${lockState}"
        ], descMap)
    } else {
        // we handle both Reports and Events in the child driver
        logDebug "parseDoorLock: handling cluster ${descMap.cluster} ${descMap.callbackType == 'Event' ? "Event ${descMap.evtId}" : "Report ${descMap.attrId}"} in the child driver"
        sendHubitatEvent([
            name: 'handleInChildDriver',
            //value: descMap.toString(),
            value: descMap,                 // since version 1.7.0 we are sending the full descMap to the child driver
            descriptionText: "<i>(to be re-processed in the child driver!)</i>"
        ], descMap, ignoreDuplicates = false)
    }
}


void parseAirQuality(final Map descMap) { // 005B
    if (descMap.cluster != '005B') { logWarn "parseAirQuality: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    logTrace "parseAirQuality: <b>UNPROCESSED</b> ${(AirQualityClusterAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
    // send the unprocessed attributes to the child driver for further processing
    sendHubitatEvent([
        name: 'unprocessed',
        value: descMap.toString(),
        descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} unprocessed cluster ${descMap.cluster} attribute ${descMap.attrId} <i>(to be re-processed in the child driver!)</i>"
    ], descMap, ignoreDuplicates = true)
}

void parseElectricalPowerMeasurement(final Map descMap) { // 0090
    if (descMap.cluster != '0090') { logWarn "parseElectricalPowerMeasurement: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    // send the unprocessed attributes to the child driver for further processing
    sendHubitatEvent([
        name: 'handleInChildDriver',
        value: descMap,
        descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} unprocessed cluster ${descMap.cluster} attribute ${descMap.attrId} <i>(to be re-processed in the child driver!)</i>"
    ], descMap, ignoreDuplicates = false)
}

void parseElectricalEnergyMeasurement(final Map descMap) { // 0091
    if (descMap.cluster != '0091') { logWarn "parseElectricalEnergyMeasurement: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    // send the unprocessed attributes to the child driver for further processing
    sendHubitatEvent([
        name: 'handleInChildDriver',
        value: descMap,
        descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} unprocessed cluster ${descMap.cluster} attribute ${descMap.attrId} <i>(to be re-processed in the child driver!)</i>"
    ], descMap, ignoreDuplicates = false)
}

// to be used in multiple clusters !
void parseConcentrationMeasurement(final Map descMap) { // 042A
    if (descMap.cluster != '042A') { logWarn "parseConcentrationMeasurement: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    logTrace "parseConcentrationMeasurement: <b>UNPROCESSED</b> ${(ConcentrationMeasurementClustersAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
    // send the unprocessed attributes to the child driver for further processing
    sendHubitatEvent([
        name: 'unprocessed',
        value: descMap.toString(),
        descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} unprocessed cluster ${descMap.cluster} attribute ${descMap.attrId} <i>(to be re-processed in the child driver!)</i>"
    ], descMap, ignoreDuplicates = true)
}

void parseCarbonDioxideConcentrationMeasurement(final Map descMap) { // 040D
    if (descMap.cluster != '040D') { logWarn "parseCarbonDioxideConcentrationMeasurement: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    logTrace "parseCarbonDioxideConcentrationMeasurement: <b>UNPROCESSED</b> ${(ConcentrationMeasurementClustersAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
    // send the unprocessed attributes to the child driver for further processing
    sendHubitatEvent([
        name: 'unprocessed',
        value: descMap.toString(),
        descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} unprocessed cluster ${descMap.cluster} attribute ${descMap.attrId} <i>(to be re-processed in the child driver!)</i>"
    ], descMap, ignoreDuplicates = true)
}


void parseWindowCovering(final Map descMap) { // 0102
    if (descMap.cluster != '0102') { logWarn "parseWindowCovering: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    if (descMap.attrId == '000B') { // TargetPositionLiftPercent100ths
        Integer valueInt = (safeHexToInt(descMap.value) / 100) as int
        sendHubitatEvent([
            name: 'targetPosition',
            value: valueInt,
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} <b>targetPosition</b> is reported as ${valueInt} <i>(to be re-processed in the child driver!)</i>"
        ], descMap, ignoreDuplicates = false)
    } else if (descMap.attrId == '000E') { // CurrentPositionLiftPercent100ths
        Integer valueInt = (safeHexToInt(descMap.value) / 100) as int
        sendHubitatEvent([
            name: 'position',
            value: valueInt,
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} <b>position</b> is is reported as ${valueInt} <i>(to be re-processed in the child driver!)</i>"
        ], descMap, ignoreDuplicates = false)
    } else if (descMap.attrId == '000A') { // OperationalStatus
        sendHubitatEvent([
            name: 'operationalStatus',
            value: descMap.value,
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} operationalStatus is ${descMap.value}"
        ], descMap, ignoreDuplicates = false)
    }
    else {
        logDebug "parseWindowCovering: ${(WindowCoveringClusterAttributes[descMap.attrInt] ?: GlobalElementsAttributes[descMap.attrInt] ?: UNKNOWN)} = ${descMap.value}"
        // send the unprocessed attributes to the child driver for further processing
        sendHubitatEvent([
            name: 'handleInChildDriver',
            value: descMap,                 // since version 1.7.0 we are sending the full descMap to the child driver
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} unprocessed cluster ${descMap.cluster} attribute ${descMap.attrId} <i>(to be re-processed in the child driver!)</i>"
        ], descMap, ignoreDuplicates = false)
    }
}

/**
 * Convert a color temperature in Kelvin to a mired value
 * @param kelvin color temperature in Kelvin
 * @return mired value
 */
 @CompileStatic
private static Integer ctToMired(final int kelvin) {
    return (1000000 / kelvin).toInteger()
}

/**
 * Mired to Kelvin conversion
 * @param mired mired value in hex
 * @return color temperature in Kelvin
 */
private int miredHexToCt(final String mired) {
    Integer miredInt = hexStrToUnsignedInt(mired)
    return miredInt > 0 ? (1000000 / miredInt) as int : 0
}

private int miredIntToCt(final Integer miredInt) {
    return miredInt > 0 ? (1000000 / miredInt) as int : 0
}

void parseColorControl(final Map descMap) { // 0300
    if (descMap.cluster != '0300') { logWarn "parseColorControl: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    ChildDeviceWrapper dw = getDw(descMap)
    Integer value = safeHexToInt(descMap.value)
    switch (descMap.attrId) {
        case '0000' : // CurrentHue
            Integer scaledValue = int256ToInt100(value ?: 0)
            logTrace "parseColorControl: hue = ${scaledValue} (raw=0x${descMap.value})"
            sendHubitatEvent([
                name: 'hue',
                value: scaledValue,
                descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} hue is ${scaledValue}"
            ], descMap, true)
            if (dw?.currentValue('colorMode') != 'CT') {
                sendColorNameEvent(descMap, hue=scaledValue, saturation=null)   // added 02/19/2024
            }
            break
        case '0001' : // CurrentSaturation
            Integer scaledValue = int256ToInt100(value ?: 0)
            logTrace "parseColorControl: CurrentSaturation = ${scaledValue} (raw=0x${descMap.value})"
            sendHubitatEvent([
                name: 'saturation',
                value: scaledValue,
                descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} saturation is ${scaledValue}"
            ], descMap, true)
            if (dw?.currentValue('colorMode') != 'CT') {
                sendColorNameEvent(descMap, hue=null, saturation=scaledValue)   // added 02/19/2024
            }
            break
        case '0007' : // ColorTemperatureMireds
            // parse: descMap:[callbackType:Report, endpointInt:11, clusterInt:768, attrInt:7, data:[7:UINT:263], value:263, attrId:0007, cluster:0300, endpoint:0B]
            Integer valueCt = miredIntToCt(value ?: 0)
            logTrace "parseColorControl: ColorTemperatureCT = ${valueCt} (raw=0x${descMap.value})"
            sendHubitatEvent([
                name: 'colorTemperature',
                value: valueCt,
                descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} colorTemperature is ${valueCt}",
                unit: '°K'
            ], descMap, true)
            String colorMode = dw?.currentValue('colorMode') ?: UNKNOWN
            if (colorMode == 'CT') {
                String colorName = convertTemperatureToGenericColorName(valueCt)
                sendHubitatEvent([
                    name: 'colorName',
                    value: colorName,
                    descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} color is ${colorName}"
                ], descMap, true)
            }
            break
        case '0008' : // ColorMode
            // Normalize value to integer for robust mapping
            int colorModeInt = safeHexToInt(descMap.value, -1)
            String colorMode = (colorModeInt == 0) ? 'RGB' : (colorModeInt == 1) ? 'XY' : (colorModeInt == 2) ? 'CT' : UNKNOWN
            logTrace "parseColorControl: ColorMode= ${colorMode} (raw=0x${descMap.value}) - sending <b>colorName</b>"
            if (dw != null) {
                Integer colorTemperature = dw.currentValue('colorTemperature') ?: -1
                Integer hue = dw.currentValue('hue') ?: -1
                Integer saturation = dw.currentValue('saturation') ?: -1
                if (colorMode == 'CT') {
                    logTrace "parseColorControl: CT colorTemperature = ${colorTemperature}"
                    if (colorTemperature != -1) {
                        String colorName = convertTemperatureToGenericColorName(colorTemperature)
                        sendHubitatEvent([
                            name: 'colorName',
                            value: colorName,
                            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} color is ${colorName}"
                        ], descMap, true)
                    }
                }
                else if (colorMode == 'RGB' || colorMode == 'XY') {
                    if (hue != -1 && saturation != -1) {
                        String colorName = convertHueToGenericColorName(hue, saturation)
                        logTrace "parseColorControl: RGB colorName = ${colorName}"
                        sendHubitatEvent([
                            name: 'colorName',
                            value: colorName,
                            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} color is ${colorName}"
                        ], descMap, true)
                    }
                }
            }
            //
            sendHubitatEvent([
                name: 'colorMode',
                value: colorMode,
                descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} colorMode is ${colorMode}"
            ], descMap, true)
            break
        case ['FFF8', 'FFF9', 'FFFA', 'FFFB', 'FFFC', 'FFFD', '00FE'] :
            // Check if FFFB (AttributeList) indicates this CT device should be RGBW
            // TODO - remove this patch !!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if (descMap.attrId == 'FFFB' && descMap.value instanceof List) {
                List colorAttrList = descMap.value
                boolean hasHue = colorAttrList?.contains('00')
                boolean hasSaturation = colorAttrList?.contains('01')
                
                // If AttributeList has hue + saturation, check if child device needs upgrading
                if (hasHue && hasSaturation) {
                    String dni = "${device.id}-${descMap.endpoint}"
                    def child = getChildDevice(dni)
                    if (child && child.typeName?.contains('CT') && !child.typeName?.contains('RGBW')) {
                        String oldName = child.displayName
                        String oldLabel = child.label
                        logInfo "parseColorControl: ${oldName} has hue/saturation - upgrading from CT to RGBW driver"
                        
                        try {
                            deleteChildDevice(dni)      // TODO : check if this changes the deviceId ...
                            def newChild = addChildDevice('hubitat', 'Generic Component RGBW', dni, [name: oldName])
                            if (oldLabel && oldLabel != oldName) { newChild.label = oldLabel }
                            newChild.updateDataValue('id', descMap.endpoint)
                            logInfo "parseColorControl: Successfully upgraded ${oldName} to RGBW driver"
                        } catch (Exception e) {
                            logWarn "parseColorControl: Failed to upgrade ${oldName}: ${e.message}"
                        }
                    }
                }
            }
            //logTrace "parseColorControl: ${getAttributeName(descMap)} = ${descMap.value}"
            break
        default :
            Map eventMap = [:]
            String attrName = getAttributeName(descMap)
            String fingerprintName = getFingerprintName(descMap)
            //logDebug "parseColorControl: fingerprintName:${fingerprintName} attrName:${attrName}"
            if (state[fingerprintName] == null) { state[fingerprintName] = [:] }
            String eventName = attrName[0].toLowerCase() + attrName[1..-1]  // change the attribute name first letter to lower case
            if (attrName in ColorControlClusterAttributes.values().toList()) {
                eventMap = [name: eventName, value:descMap.value, descriptionText: "${eventName} is: ${descMap.value}"]
                if (logEnable) { logInfo "parseLevelControlCluster: ${attrName} = ${descMap.value}" }
            }
            else {
                logDebug "parseLevelControlCluster: unsupported LevelControl: ${attrName} = ${descMap.value}"
            }
            if (eventMap != [:]) {
                eventMap.type = 'physical'; eventMap.isStateChange = true
                sendHubitatEvent(eventMap, descMap, true) // child events
            }
            break
    }
}

ChildDeviceWrapper getDw(descMap) {
    String id = descMap?.endpoint ?: '00'
    return getChildDevice("${device.id}-${id}")
}

void sendColorNameEvent(final Map descMap, final Integer huePar=null, final Integer saturationPar=null) {
    Integer hue = huePar == null ? safeToInt(getDw(descMap)?.currentValue('hue')) : huePar
    Integer saturation = saturationPar == null ? safeToInt(getDw(descMap)?.currentValue('saturation')) : saturationPar
    logTrace "sendColorNameEvent -> huePar:${huePar}  saturationPar=${saturationPar} hue:${hue} saturation:${saturation}"
    if (hue == null || saturation == null) { logWarn "sendColorNameEvent: hue:${hue} <b>or</b> saturation:${saturation} is null"; return }
    String colorName = convertHueToGenericColorName(hue, saturation)    //  (Since 2.3.2) - for RGB bulbs only
    sendHubitatEvent([
        name: 'colorName',
        value: colorName,
        descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} color is ${colorName}"
    ], descMap, true)
}

String getTemperatureUnit() {
    return location.temperatureScale == 'F' ? "\u00B0" + 'F' : "\u00B0" + 'C'
}

Double convertTemperature(final Map descMap) {
    Double valueInt = safeHexToInt(descMap.value) / 100.0
    String unit
    //log.debug "convertTemperature: location.temperatureScale:${location.temperatureScale}"
    if (location.temperatureScale == 'F') {
        valueInt = (valueInt * 1.8) + 32
    }
    Double valueIntCorrected = valueInt.round(1)
    return valueIntCorrected
}

void parseThermostat(final Map descMap) {
    if (descMap.cluster != '0201') { logWarn "parseThermostat: unexpected cluster:${descMap.cluster} (attrId:${descMap.attrId})"; return }
    ChildDeviceWrapper dw = getDw(descMap)
    Double valueIntCorrected = 0.0
    String unit = getTemperatureUnit()
    switch (descMap.attrId) {
        case '0000' : // LocalTemperature -> temperature
            valueIntCorrected = convertTemperature(descMap)
            sendHubitatEvent([
                name: 'temperature',
                value: valueIntCorrected,
                descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} temperature is ${valueIntCorrected} ${unit}",
                unit: unit
            ], descMap, false)
            break
        case '0012' : // OccupiedHeatingSetpoint -> heatingSetpoint
            valueIntCorrected = convertTemperature(descMap)
            sendHubitatEvent([
                name: 'heatingSetpoint',
                value: valueIntCorrected,
                descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} heatingSetpoint is ${valueIntCorrected} ${unit}",
                unit: unit
            ], descMap, false)
            sendHubitatEvent([name: 'thermostatSetpoint', value: valueIntCorrected, descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} heatingSetpoint is ${valueIntCorrected} ${unit}", unit: unit], descMap, false)
            if ( valueIntCorrected > safeToDouble(dw?.currentValue('temperature'))) {
                sendHubitatEvent([name: 'thermostatOperatingState', value: 'heating', type: 'digital', descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} thermostatOperatingState was set to heating"], descMap, true)
            }
            else {
                sendHubitatEvent([name: 'thermostatOperatingState', value: 'idle', type: 'digital', descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} thermostatOperatingState was set to idle"], descMap, true)
            }
            break
        case '001B' : // ControlSequenceOfOperation -> supportedThermostatModes
            String valueStr = (descMap.value instanceof Integer) ? descMap.value.toString() : descMap.value
            String controlSequenceMatter = ThermostatControlSequences[HexUtils.hexStringToInt(valueStr)] ?: UNKNOWN
            List<String> supportedThermostatModes = HubitatThermostatModes[HexUtils.hexStringToInt(valueStr)] ?: UNKNOWN
            sendHubitatEvent([
                name: 'supportedThermostatModes',
                value:  JsonOutput.toJson(supportedThermostatModes),
                descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} supportedThermostatModes is ${supportedThermostatModes} (${controlSequenceMatter})"
            ], descMap, false)
            break
        case '001C' : // SystemMode -> ThermostatSystemMode -> thermostatMode 
            String valueStr = String.valueOf(descMap.value)
            String systemMode = ThermostatSystemModes[HexUtils.hexStringToInt(valueStr)] ?: UNKNOWN
            // change the attribute name first letter to lower case
            systemMode = systemMode[0].toLowerCase() + systemMode[1..-1]
            sendHubitatEvent([
                name: 'thermostatMode',
                value: systemMode,      // off, heat, ......
                descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} thermostatMode is ${systemMode}"
            ], descMap, false)
            break
        case ['FFF8', 'FFF9', 'FFFA', 'FFFB', 'FFFC', 'FFFD', '00FE'] :
            logTrace "parseThermostat: ${getAttributeName(descMap)} = ${descMap.value}"
            break
        default :
            Map eventMap = [:]
            String attrName = getAttributeName(descMap)
            String fingerprintName = getFingerprintName(descMap)
            logDebug "parseThermostat: fingerprintName:${fingerprintName} attrName:${attrName}"
            if (state[fingerprintName] == null) { state[fingerprintName] = [:] }
            String eventName = attrName[0].toLowerCase() + attrName[1..-1]  // change the attribute name first letter to lower case
            if (attrName in ThermostatClusterAttributes.values().toList()) {
                String valueFormatted = descMap.value
                if (attrName in ['AbsMinHeatSetpointLimit', 'AbsMaxHeatSetpointLimit', 'MinHeatSetpointLimit', 'MaxHeatSetpointLimit']) {
                    valueFormatted = convertTemperature(descMap).toString()   
                }
                eventMap = [name: eventName, value:valueFormatted, descriptionText: "${eventName} is: ${valueFormatted}"]
                if (logEnable) { logInfo "parseThermostat: ${attrName} = ${valueFormatted}" }
            }
            else {
                logWarn "parseThermostat: unsupported: ${attrName} = ${descMap.value}"
            }
            if (eventMap != [:]) {
                eventMap.type = 'physical'; eventMap.isStateChange = true
                sendHubitatEvent(eventMap, descMap, true) // child events
            }
            break
    }
}


// Common code method for sending Hubitat events
void sendHubitatEvent(final Map<String, String> eventParams, DeviceWrapper dw, ignoreDuplicates = false) {
    String id = dw?.getDataValue('id') ?: '00'
    sendHubitatEvent(eventParams, [endpoint: id], ignoreDuplicates)
}

void sendHubitatEvent(final Map<String, String> eventParams, Map descMap = [:], ignoreDuplicates = false) {
    String name = eventParams['name']
    //String value = eventParams['value']
    Object value = eventParams['value']
    String descriptionText = eventParams['descriptionText']
    String unit = eventParams['unit']
    logTrace "sendHubitatEvent: name:${name} value:${value} descriptionText:${descriptionText} unit:${unit}"

    // Filter noisy Matter *events* (evtId present) shortly after (re)subscription.
    // Apply centrally so it affects all clusters, not just Switch.
    if (shouldFilterNoisyPostSubscribeEvent(descMap, 'sendHubitatEvent')) {
        logDebug "sendHubitatEvent: FILTERED noisy post-(re)subscription event for ${getDeviceDisplayName(descMap?.endpoint)} name:${name} value:${value}"
        return
    }

    String dni = ''
    // get the dni from the descMap eddpoint
    if (descMap != [:]) {
        dni = "${device.id}-${descMap.endpoint}"
    }
    if (descriptionText == null) {
        descriptionText = "${getDeviceDisplayName(descMap?.endpoint)} ${name} is ${value}"
    }
    ChildDeviceWrapper dw = getChildDevice(dni) // null if dni is null for the parent device
    Map eventMap = [name: name, value: value, descriptionText: descriptionText, unit: unit, type: 'physical']
    if (state.states['isRefresh'] == true) {
        eventMap.descriptionText += ' [refresh]'
        eventMap.isStateChange = true   // force the event to be sent
        eventMap.isRefresh = true
    }
    if (state.states['isDiscovery'] == true) {
        eventMap.descriptionText += ' [discovery]'
        eventMap.isStateChange = true   // force the event to be sent
        eventMap.isDiscovery = true
    }
    // TODO - use the child device wrapper to check the current value !!!!!!!!!!!!!!!!!!!!!

    // IMPORTANT: Never suppress Matter *events* (evtId present) as duplicates.
    // Button/Switch events often repeat the same payload (e.g. {"position":1}) and still represent a real action.
    if (ignoreDuplicates == true && state.states['isRefresh'] == false && descMap?.evtId == null) {
        boolean isDuplicate = false
        // Check child device currentState if available, otherwise check parent device for bridge events
        Object latestEvent = dw?.device?.currentState(name) ?: device.currentState(name)
        //latestEvent.properties.each { k, v -> logWarn ("$k: $v") }
        try {
            if (latestEvent != null) {
                if (latestEvent.value != null) {
                    if (latestEvent.dataType in ['NUMBER', 'DOUBLE', 'FLOAT'] ) {
                        isDuplicate = Math.abs(latestEvent.doubleValue - safeToDouble(value)) < 0.00001
                    }
                    else if (latestEvent.dataType == 'STRING' || latestEvent.dataType == 'ENUM' || latestEvent.dataType == 'DATE') {
                        isDuplicate = (latestEvent.stringValue == value.toString())
                    }
                    else if (latestEvent.dataType == 'JSON_OBJECT') {
                        isDuplicate = (latestEvent.jsonValue == value.toString())   // TODO - check this
                    }
                    else {
                        isDuplicate = false
                        logWarn "sendHubitatEvent: unsupported dataType:${latestEvent.dataType}"
                    }
                }
                else {
                    logTrace "sendHubitatEvent: latestEvent.value is null"
                }
            }
            else {
                logTrace "sendHubitatEvent: latestEvent is null (attribute not found in child or parent device)"
            }
        } catch (Exception e) {
            logWarn "sendHubitatEvent: error checking for duplicates: ${e}"
        }
        if (isDuplicate) {
            logTrace "sendHubitatEvent: IGNORED duplicate event: ${eventMap.descriptionText} (value:${value} dataType:${latestEvent?.dataType})"
            return
        }
        else {
            logTrace "sendHubitatEvent: NOT IGNORED event: ${eventMap.descriptionText} (value:${value} latestEvent.value = ${latestEvent?.value} dataType:${latestEvent?.dataType})"
        }
    }
    else {
        logTrace "sendHubitatEvent: <b>ignoreDuplicates=false</b> or isRefresh=${state.states['isRefresh'] } for event: ${eventMap.descriptionText} (value:${value})"
    }
    if (dw != null && dw?.disabled != true) {
        // Always route Matter *events* (evtId present) through the child parse() so component drivers can translate them.
        boolean isMatterEvent = (descMap?.evtId != null)
        if (isMatterEvent) {
            logDebug "sendHubitatEvent: sending Matter <b>Event</b> for parsing to the child device: dw:${dw} dni:${dni} name:${name} value:${value} descriptionText:${descriptionText}"
            dw.parse([eventMap])
        }
        // Route internal events through parse() without requiring attribute declaration
        else if (name in INTERNAL_EVENTS) {
            logDebug "sendHubitatEvent: routing internal event '${name}' through child parse(): dw:${dw} dni:${dni}"
            dw.parse([eventMap])
        }
        // For attributes, keep the existing behavior: if child doesn't declare the attribute, send it directly.
        else if (dw?.hasAttribute(name) != true) {
            logDebug "sendHubitatEvent: sending directly (attribute '${name}' not declared in child driver): dw:${dw} dni:${dni} value:${value}"
            dw.sendEvent(eventMap)
            logInfo "${eventMap.descriptionText}"
            // added 2024/10/02 - update the data value in the child device
            // dw.updateDataValue(name, value.toString())  // 2026/01/25    // commented out 2026-01-26 as it is not needed
        }
        else {
            // send events to child for parsing. Any filtering of duplicated Matter messages will be potentially done in the child device handler.
            logDebug "sendHubitatEvent: sending for parsing to the child device: dw:${dw} dni:${dni} name:${name} value:${value} descriptionText:${descriptionText}"
            dw.parse([eventMap])
        }
    } else if (descMap?.endpoint == null || descMap?.endpoint == '00') {
        // Bridge event
        sendEvent(eventMap)
        logInfo "${eventMap.descriptionText}"
    }
    else {
        // event intended to be sent to the parent device, but the dni is null ..
        if (state['states']['isDiscovery'] != true) { // do not log this event if the discovery is in progress
            logWarn "sendHubitatEvent: <b>cannot send </b> for parsing to the child device: dw:${dw} dni:${dni} name:${name} value:${value} descriptionText:${descriptionText}"
        }
    }
}

//capability commands

void identify() {
    String cmd
    Integer time = 10
    List<Map<String, String>> cmdFields = []
    cmdFields.add(matter.cmdField(0x05, 0x00, zigbee.swapOctets(HexUtils.integerToHexString(time, 2))))
    cmd = matter.invoke(device.endpointId, 0x0003, 0x0000, cmdFields)
    sendToDevice(cmd)
}

void setRefreshRequest()   { if (state.states == null) { state.states = [:] } ; state.states['isRefresh'] = true ; runInMillis(REFRESH_TIMER, clearRefreshRequest, [overwrite: true]) }                 // 3 seconds
void clearRefreshRequest() { if (state.states == null) { state.states = [:] } ; state.states['isRefresh'] = false }
void setDigitalRequest()   { if (state.states == null) { state.states = [:] } ; state.states['isDigital'] = true ; runInMillis(DIGITAL_TIMER, clearDigitalRequest, [overwrite: true]) }                 // 3 seconds
void clearDigitalRequest() { if (state.states == null) { state.states = [:] } ; state.states['isDigital'] = false }

void logRequestedClusterAttrResult(final Map data) {
    logDebug "logRequestedClusterAttrResult: data:${data}"
    String cluster = HexUtils.integerToHexString(data.cluster, 2)
    String endpoint = HexUtils.integerToHexString(data.endpoint as Integer, 1)
    String clusterAttr = "Cluster <b>${MatterClusters[data.cluster]}</b> (0x${cluster}) endpoint <b>0x${endpoint}</b> attributes and values list (0xFFFB)"
    String logMessage = state.tmp != null ? "${clusterAttr} : <br>${state.tmp}" : "${clusterAttr} <b>timeout</b>! :("

    state.tmp = null
    state.states = state.states ?: [:]
    state.states['isInfo'] = false
    state.states['cluster'] = null

    logInfo logMessage
}

/**
 * Requests the list of attributes for a Matter cluster (reads the 0xFFFB attribute of the cluster and endpoint in the data map)
 */
void requestMatterClusterAttributesList(final Map data) {
    state.states = state.states ?: [:]
    state.states['isInfo'] = true
    state.states['cluster'] = HexUtils.integerToHexString(data.cluster, 2)
    state.tmp = null

    Integer endpoint = data.endpoint as Integer
    Integer cluster = data.cluster as Integer

    logDebug "Requesting Attribute List for Cluster <b>${MatterClusters[data.cluster]}</b> (0x${HexUtils.integerToHexString(data.cluster, 2)}) endpoint <b>0x${HexUtils.integerToHexString(endpoint, 1)}</b> attributes list ..."

    List<Map<String, String>> attributePaths = [matter.attributePath(endpoint, cluster, 0xFFFB)]
    sendToDevice(matter.readAttributes(attributePaths))
}

/**
 * Requests the values of ALL ATRIBUTES for a Matter cluster and endpoint
 */
void requestMatterClusterAttributesValues(final Map data) {
    Integer endpoint = data.endpoint as Integer
    Integer cluster  = data.cluster  as Integer
    List<Map<String, String>> serverList = []
    String fingerprintName = getFingerprintName(endpoint)
    if (state[fingerprintName] == null) {
        logWarn "requestMatterClusterAttributesValues: state.${fingerprintName} is null !"
        return
    }
    logDebug "Requesting Attributes Values for endpoint <b>0x${HexUtils.integerToHexString(endpoint, 1)}</b>  cluster <b>${MatterClusters[data.cluster]}</b> (0x${HexUtils.integerToHexString(data.cluster, 2)}) attributes values ..."

    String listMapName = ''
    if (cluster == 0x001D) {
        listMapName = 'AttributeList'
    }
    else {
        listMapName = HexUtils.integerToHexString(data.cluster, 2) + '_' + 'FFFB'
    }

    attributeList = state[fingerprintName][listMapName]

    logDebug "requestMatterClusterAttributesValues: (requesting cluster 0x${HexUtils.integerToHexString(data.cluster, 2)}) fingerprintName=${fingerprintName} listMapName=${listMapName} attributeList=${attributeList}"
    if (attributeList == null) {
        logWarn 'requestMatterClusterAttributesValues: attrListString is null'
        return
    }
    logDebug "requestMatterClusterAttributesValues: cluster ${MatterClusters[data.cluster]} (0x${HexUtils.integerToHexString(data.cluster, 2)}) attributeList:${attributeList}"
    List<Map<String, String>> attributePaths = attributeList.collect { attr ->
        Integer attrInt = HexUtils.hexStringToInt(attr)
        if (attrInt == 0x0040 || attrInt == 0x0041) {
            logDebug "requestMatterClusterAttributesValues: skipping attribute 0x${HexUtils.integerToHexString(attrInt, 2)} (${attrInt})"
            return
        }
        matter.attributePath(endpoint, cluster, attrInt)
    }.findAll()
    if (!attributePaths.isEmpty()) {
        sendToDevice(matter.readAttributes(attributePaths))
    }
}

/**
 * Requests, collects and logs all attribute values for a given endpoint and cluster.
 */
void requestAndCollectAttributesValues(Integer endpoint, Integer cluster, Integer time=1, boolean fast=false) {
    state.states['isPing'] = false
    final int baseTime = (time as int) ?: 1
    final int scale = getDiscoveryTimeoutScale()
    runIn(baseTime, requestMatterClusterAttributesList,   [overwrite: false, data: [endpoint: endpoint, cluster: cluster] ])
    runIn(baseTime + ((fast ? 2 : 3) * scale),  requestMatterClusterAttributesValues, [overwrite: false, data: [endpoint: endpoint, cluster: cluster] ])
    runIn(baseTime + ((fast ? 6 : 12) * scale), logRequestedClusterAttrResult,        [overwrite: false, data: [endpoint: endpoint, cluster: cluster] ])
}

void scheduleRequestAndCollectServerListAttributesList(String endpointPar = '00', Integer time=1, boolean fast=false) {
    state.states['isPing'] = false
    runIn((time as int) ?: 1, requestAndCollectServerListAttributesList,   [overwrite: false, data: [endpointPar: endpointPar] ])
}

void requestAndCollectServerListAttributesList(Map data)
{
    Integer endpoint = safeNumberToInt(data.endpointPar)
    String fingerprintName = getFingerprintName(endpoint)
    List<String> serverList = state[fingerprintName]?.ServerList
    logDebug "requestAndCollectServerListAttributesList(): serverList:${serverList} endpoint=${endpoint} getFingerprintName = ${fingerprintName}"
    if (serverList == null) {
        logWarn 'requestAndCollectServerListAttributesList(): serverList is null!'
        return
    }
    serverList.each { cluster ->
        Integer clusterInt = HexUtils.hexStringToInt(cluster)
        //logTrace "requestAndCollectServerListAttributesList(): endpointInt:${endpoint} (0x${HexUtils.integerToHexString(safeToInt(endpoint), 1)}),  clusterInt:${clusterInt} (0x${cluster})"
        
        // Read AttributeList (existing)
        readAttribute(endpoint, clusterInt, 0xFFFB)
    }
}

/*
 *  Discover all the endpoints and clusters for the Bridge and all the Bridged Devices
 */
void _DiscoverAll() { _DiscoverAllPatched('All') }     // patch for HE platform version 2.4.0.x 
void _DiscoverAllPatched(String statePar/* = null*/) {
    logWarn "_DiscoverAll()"
    updated()   // 2026-02-09
    Integer stateSt = DISCOVER_ALL_STATE_INIT
    state.stateMachines = [:]
    // ['All', 'BasicInfo', 'PartsList']]
    if (statePar == null || statePar == 'All') { stateSt = DISCOVER_ALL_STATE_INIT }
    else if (statePar == 'BasicInfo') { stateSt = DISCOVER_ALL_STATE_BRIDGE_BASIC_INFO_ATTR_LIST }
    else if (statePar == 'PartsList') { stateSt = DISCOVER_ALL_STATE_GET_PARTS_LIST_START }
    else if (statePar == 'ChildDevices') { stateSt = DISCOVER_ALL_STATE_SUPPORTED_CLUSTERS_START }
    else if (statePar == 'Subscribe') { stateSt = DISCOVER_ALL_STATE_SUBSCRIBE_KNOWN_CLUSTERS }
    else {
        logWarn "_DiscoverAll(): unknown statePar:${statePar} !"
        return
    }

    discoverAllStateMachine([action: START, goToState: stateSt])
}

void readAttribute(Integer endpoint, Integer cluster, Integer attrId) {
    List<Map<String, String>> attributePaths = [matter.attributePath(endpoint as Integer, cluster as Integer, attrId as Integer)]
    sendToDevice(matter.readAttributes(attributePaths))
}

// not used
void configure() {
    log.warn 'configure...'
    sendToDevice(subscribeCmd())
    sendInfoEvent('configure()...', 'sent device subscribe command')
}

//lifecycle commands
void updated() {
    log.info 'updated...'
    checkDriverVersion()
    logInfo "debug logging is: ${logEnable == true} description logging is: ${txtEnable == true}"
    if (settings.logEnable)   { runIn(86400, logsOff) }   // 24 hours
    if (settings.traceEnable) { logTrace settings; runIn(7200, traceOff) }   // 7200 = 2 hours

    final int healthMethod = (settings.healthCheckMethod as Integer) ?: 0
    if (healthMethod == 1 || healthMethod == 2) {                            //    [0: 'Disabled', 1: 'Activity check', 2: 'Periodic polling']
        // schedule the periodic timer
        final int interval = (settings.healthCheckInterval as Integer) ?: 0
        if (interval > 0) {
            logTrace "healthMethod=${healthMethod} interval=${interval}"
            log.info "scheduling health check every ${interval} minutes by ${HealthcheckMethodOpts.options[healthCheckMethod as int]} method"
            scheduleDeviceHealthCheck(interval, healthMethod)
        }
    }
    else {
        unScheduleDeviceHealthCheck()        // unschedule the periodic job, depending on the healthMethod
        log.info 'Health Check is disabled!'
    }
    // compare state.preferences.minimizeStateVariables with settings.minimizeStateVariables was changed and call the minimizeStateVariables()
    if (state.preferences == null) { state.preferences = [:] }
    if ((state.preferences['minimizeStateVariables'] ?: false) != settings?.minimizeStateVariables && settings?.minimizeStateVariables == true) {
        minimizeStateVariables(['true'])
    }
    state.preferences['minimizeStateVariables'] = settings.minimizeStateVariables
    ensureNewParseFlag()
    
    if (settings?.minReportingTimeIllum == null) device.updateSetting("minReportingTimeIllum",  [value:10, type:"number"])
    resetStats2()
   
}

// delete all Preferences
void deleteAllSettings() {
    settings.each { it ->
        logDebug "deleting ${it.key}"
        device.removeSetting("${it.key}")
    }
    logInfo  'All settings (preferences) DELETED'
}

// delete all attributes
void deleteAllCurrentStates() {
    device.properties.supportedAttributes.each { it ->
        logDebug "deleting $it"
        device.deleteCurrentState("$it")
    }
    logInfo 'All current states (attributes) DELETED'
}

// delete all State Variables
void deleteAllStates() {
    state.each { it ->
        logDebug "deleting state ${it.key}"
    }
    state.clear()
    logInfo 'All States DELETED'
}

void deleteAllScheduledJobs() {
    unschedule()
    logInfo 'All scheduled jobs DELETED'
}

void deleteAllChildDevices() {
    logWarn 'deleteAllChildDevices : not implemented!'
}

void loadAllDefaults() {
    logInfo 'Loading All Defaults...'
    deleteAllSettings()
    deleteAllCurrentStates()
    deleteAllScheduledJobs()
    deleteAllStates()
    initializeVars(fullInit = true)
    updated()
    sendInfoEvent('All Defaults Loaded! F5 to refresh')
}

void initialize() {
    log.warn 'initialize()...'
    unschedule()
    if (state.states == null) { state.states = [:] }
    if (state.lastTx == null) { state.lastTx = [:] }
    if (state.stats == null)  { state.stats = [:] }
    if (state.pendingPings == null) { state.pendingPings = [:] }
    state.states['isInfo'] = false
    Integer timeSinceLastSubscribe   = (now() - (state.lastTx['subscribeTime']   ?: 0)) / 1000
    Integer timeSinceLastUnsubscribe = (now() - (state.lastTx['unsubscribeTime'] ?: 0)) / 1000
    logDebug "'isSubscribe'= ${state.states['isSubscribe']} timeSinceLastSubscribe= ${timeSinceLastSubscribe} 'isUnsubscribe' = ${state.states['isUnsubscribe']} timeSinceLastUnsubscribe= ${timeSinceLastUnsubscribe}"

    state.stats['initializeCtr'] = (state.stats['initializeCtr'] ?: 0) + 1
    if (state.deviceType == null || state.deviceType == '') {
        log.warn 'initialize(fullInit = true))...'
        initializeVars(fullInit = true)
        sendInfoEvent('initialize()...', 'full initialization - all settings are reset to default')
    }
    log.warn "initialize(): calling sendSubscribeList()! (last unsubscribe was more than ${timeSinceLastSubscribe} seconds ago)"
    state.lastTx['subscribeTime'] = now()
    state.states['isUnsubscribe'] = false
    state.states['isSubscribe'] = true  // should be set to false in the parse() method
    sendEvent([name: 'initializeCtr', value: state.stats['initializeCtr'], descriptionText: "${device.displayName} initializeCtr is ${state.stats['initializeCtr']}", type: 'digital'])
    scheduleCommandTimeoutCheck(delay = 55)
    // Starting from MAB version 1.6.0 we support only cleanSubscribe command. Hubitat platform versions older than '2.3.9.186' are not supported!
    String cleanCmd = cleanSubscribeCmd()
    if (cleanCmd != null) {
        sendToDevice(cleanCmd)
        sendInfoEvent('cleanSubscribeCmd()...Please wait.', 'sent device subscribe command')
    }
    updated()   // added 02/03/2024
}

void clearStates() {
    logWarn 'clearStates()...'
}

// device driver command 
void reSubscribe() {
    logDebug "reSubscribe() ...(${location.hub.firmwareVersionString >= '2.3.9.186'})"
    if (location.hub.firmwareVersionString >= '2.3.9.186') {
        String cleanCmd = cleanSubscribeCmd()
        if (cleanCmd != null) {
            state.lastTx['subscribeTime'] = now()
            sendToDevice(cleanCmd)
        }
        sendInfoEvent('cleanSubscribeCmd()...Please wait.', 'sent device reSubscribe command')
        runIn(3, 'clearInfoEvent')
    }
    else {
        unsubscribe()
        logWarn 'cleanSubscribe() is not supported for this Hub firmware version!'
    }
}

void unsubscribe() {
    sendInfoEvent('unsubscribe()...Please wait.', 'sent device unsubscribe command')
    sendToDevice(unSubscribeCmd())
}

String  unSubscribeCmd() {
    return matter.unsubscribe()
}

void clearSubscriptionsState() {
    state.subscriptions = []
}

String updateStateSubscriptionsList(String addOrRemove, Integer endpoint, Integer cluster, Integer attrId) {
    String cmd = ''
    logTrace "updateStateSubscriptionsList(action: ${addOrRemove} endpoint:${endpoint}, cluster:${cluster}, attrId:${attrId})"
    List<Map<String, String>> attributePaths = []
    attributePaths.add(matter.attributePath(endpoint as Integer, cluster as Integer, attrId as Integer))
    // format EP_CLUSTER_ATTRID
    List<String> newSub = [endpoint, cluster, attrId]
    List<List<String>> stateSubscriptionsList = state.subscriptions ?: []
    if (addOrRemove == 'add') {
        if (stateSubscriptionsList.contains(newSub)) {
            logTrace "updateStateSubscriptionsList(): subscription already exists: ${newSub}"
        } else {
            logTrace "updateStateSubscriptionsList(): adding subscription: ${newSub}"
            cmd = matter.subscribe(0, 0xFFFF, attributePaths)
            //sendToDevice(cmd)     // commented out 2024-02-17
            stateSubscriptionsList.add(newSub)
            state.subscriptions = stateSubscriptionsList
        }
    }
    else if (addOrRemove == 'remove') {
        if (stateSubscriptionsList.contains(newSub)) {
            stateSubscriptionsList.remove(newSub)
            state.subscriptions = stateSubscriptionsList
        } else {
            logWarn "updateStateSubscriptionsList(): subscription not found!: ${newSub}"
        }
    }
    else if (addOrRemove == 'show') {
        if (logEnable) {
            logInfo "updateStateSubscriptionsList(): state.subscriptions size is ${state.subscriptions?.size()}"
            logInfo "updateStateSubscriptionsList(): state.subscriptions = ${state.subscriptions}"
        }
    }
    else {
        logWarn "updateStateSubscriptionsList(): unknown action: ${addOrRemove}"
    }
    return cmd
}

void sendSubscribeList() {
    sendInfoEvent('sendSubscribeList()...Please wait.', 'sent device subscribe command')
    // Use cleanSubscribe for better reliability with event subscriptions (buttons, switches)
    if (location.hub.firmwareVersionString >= '2.3.9.186') {
        String cleanCmd = cleanSubscribeCmd()
        if (cleanCmd != null) {
            state.lastTx['subscribeTime'] = now()
            sendToDevice(cleanCmd)
            logDebug 'sendSubscribeList(): using cleanSubscribe'
        }
    }
    else {
        // Fallback to older subscribe method for firmware < 2.3.9.186
        List<String> cmds = getSubscribeOrRefreshCmdList('SUBSCRIBE_ALL')
        if (cmds != null && cmds != []) {
            state.lastTx['subscribeTime'] = now()
            logTrace "sendSubscribeList(): cmds = ${cmds}"
            sendToDevice(cmds)
        }
    }
}

List<String> getSubscribeOrRefreshCmdList(action='REFRESH') {
    // the state.subscriptions list is: subscriptions : [[0, 29, 3], [36, 6, 0], [36, 8, 0], [36, 768, 0], [36, 768, 1], [36, 768, 7], [54, 6, 0], [8, 1029, 0], [7, 1026, 0], [55, 6, 0], [15, 6, 0], [13, 513, 0]]
    List<List<Integer>>  stateSubscriptionsList = new ArrayList<List<Integer>>(state.subscriptions ) ?: []
    List<String> cmdsList = []
    logDebug "getSubscribeCmdList(): stateSubscriptionsList = ${stateSubscriptionsList}"

    LinkedHashMap<Integer, List<List<Integer>>>  groupedSubscriptionsByCluster = stateSubscriptionsList.groupBy { it[1] }
    logTrace "groupedSubscriptionsByCluster=${groupedSubscriptionsByCluster}"
    // sample groupedSubscriptionsByCluster:  768 : [[36, 768, 0], [36, 768, 1], [36, 768, 7]]
    for (Map.Entry<Integer, List<List<Integer>>> entry : groupedSubscriptionsByCluster.entrySet()) {
        Integer cluster = entry.getKey()
        Integer attribute = null
        List<List<Integer>> value = entry.getValue()
        //logTrace "Cluster:${cluster}, value:${value}"
        // check if the cluster is in the supported clusters list
        if (!SupportedMatterClusters.containsKey(cluster)) {
            logWarn "getSubscribeCmdList(): cluster 0x${HexUtils.integerToHexString(cluster, 2)} is not in the SupportedMatterClusters list!"
            continue  // do not subscribe to this cluster, continue with the next cluster
        }
        // Sample groupedSubscriptionsByAttribute Attribute 0 : [[36, 768, 0]]
        Map<Integer, List<List<Integer>>> groupedSubscriptionsByAttribute = value.groupBy { it[2] }
        //logTrace "groupedSubscriptionsByAttribute=${groupedSubscriptionsByAttribute}"
        for (Map.Entry<Integer, List<List<Integer>>> entry2 : groupedSubscriptionsByAttribute.entrySet()) {
            List<Map<String, String>> attributePaths = []       // individual attributePaths for each attribute
            attribute = entry2.getKey()
            List<List<Integer>> endpointsList = entry2.getValue()
            //logTrace "Cluster:${cluster}, Attribute:${attribute}, endpointsList:${endpointsList}"

            List<List<Map<Integer, Map<String, Integer>>>> supportedSubscriptions = SupportedMatterClusters[cluster]['subscriptions']
            //def supportedSubscriptions = SupportedMatterClusters[cluster]['subscriptions']
            // sample supportedSubscriptions=[[0:[min:0, max:65535, delta:0]], [1:[min:0, max:65535, delta:0]]]
            if (supportedSubscriptions == null || supportedSubscriptions == []) {
                logWarn "<b>getSubscribeCmdList(): supportedSubscriptions is null or empty for cluster:${cluster} attribute:${attribute}!</b>"
                continue  // do not subscribe to this attribute, continue with the next
            }
            // make a list of integer keys from  the supportedSubscriptions list
            List<Integer> supportedSubscriptionsKeys = supportedSubscriptions*.keySet().flatten()
            //logTrace "supportedSubscriptionsKeys=${supportedSubscriptionsKeys}"
            // check if the attribute is in the supportedSubscriptionsKeys list
            if (!supportedSubscriptionsKeys.contains(attribute)) {
                logWarn "getSubscribeCmdList(): attribute 0x${HexUtils.integerToHexString(attribute, 2)} is not in the supportedSubscriptionsKeys:${supportedSubscriptionsKeys} list! "
                continue  // do not subscribe to this attribute, continue with the next
            }
            // here we have a list of same cluster, same attribute, different endpoints
            endpointsList.each { endpointList ->
                Integer endpoint = endpointList[0]
                // Skip disabled child devices
                String dni = "${device.id}-${HexUtils.integerToHexString(endpoint, 1).toUpperCase()}"
                ChildDeviceWrapper childDevice = getChildDevice(dni)
                if (childDevice?.disabled == true) {
                    logDebug "getSubscribeOrRefreshCmdList(): skipping disabled device endpoint ${endpoint} (${childDevice.displayName})"
                    return  // continue to next endpoint
                }
                attributePaths.add(matter.attributePath(endpoint, cluster, attribute))
            }
            //logTrace "attribute: ${attribute} attributePaths:${attributePaths} supportedSubscriptions[attribute]:${supportedSubscriptions[attribute]}"
            // assume the min, max and delta values are the same for all endpoints
            def firstSupportedSubscription = supportedSubscriptions[attribute]?.get(0)
            //logTrace "firstSupportedSubscription = ${firstSupportedSubscription}"
            Integer min = firstSupportedSubscription?.get('min') ?: 0
            Integer max = firstSupportedSubscription?.get('max') ?: 0xFFFF
            //logTrace "min=${min}, max=${max}, delta=${delta}"
            if (action == 'REFRESH_ALL') {
                cmdsList.add(matter.readAttributes(attributePaths))
            }
            else if (action == 'SUBSCRIBE_ALL') {
                cmdsList.add(matter.subscribe(min, max, attributePaths))
            }
        } // for each attribute
        
        // Handle event subscriptions for this cluster (if configured)
        if (action == 'SUBSCRIBE_ALL' && SupportedMatterClusters[cluster]?.eventSubscriptions) {
            List<Integer> eventIds = SupportedMatterClusters[cluster].eventSubscriptions
            List<Map<String, String>> eventPaths = []
            
            // Get unique endpoints for this cluster from the grouped subscriptions
            Set<Integer> clusterEndpoints = [] as Set<Integer>
            value.each { endpointClusterAttr ->
                clusterEndpoints.add(endpointClusterAttr[0] as Integer)
            }
            
            // Build event paths for each endpoint (skip disabled devices)
            clusterEndpoints.each { endpoint ->
                String dni = "${device.id}-${HexUtils.integerToHexString(endpoint, 1).toUpperCase()}"
                ChildDeviceWrapper childDevice = getChildDevice(dni)
                if (childDevice?.disabled == true) {
                    logDebug "getSubscribeOrRefreshCmdList(): skipping disabled device events for endpoint ${endpoint} (${childDevice.displayName})"
                    return  // continue to next endpoint
                }
                
                eventIds.each { eventId ->
                    eventPaths.add(matter.eventPath(endpoint, cluster, eventId))
                }
            }
            
            // Add event subscription command if we have any event paths
            if (!eventPaths.isEmpty()) {
                String evtSubscribeCmd = matter.subscribe(0, 0xFFFF, eventPaths)
                cmdsList.add(evtSubscribeCmd)
                logDebug "getSubscribeOrRefreshCmdList(): added event subscriptions for cluster 0x${HexUtils.integerToHexString(cluster, 2)} endpoints=${clusterEndpoints} eventIds=${eventIds}"
            }
        }
        
        //logTrace "attribute:${attribute} cmdsList=${cmdsList}"
        //return cmdsList
    }   // for each cluster

    return cmdsList
}

String subscribeCmd() {
    log.warn 'subscribeCmd() is deprecated. Use cleanSubscribeCmd() instead.'
    return null
}

// availabe from HE platform version [2.3.9.186]
String cleanSubscribeCmd() {
    List<Map<String, String>> paths = []
    //paths.add(matter.attributePath(0, 0x001D, 0x03))   // Descriptor Cluster - PartsList
    // Build event paths first, then attribute paths (requested ordering)
    List<Map<String, String>> eventPaths = []

    // Add event subscriptions for clusters that have eventSubscriptions configured
    LinkedHashMap<Integer, List<List<Integer>>> groupedSubscriptionsByCluster = state.subscriptions?.groupBy { it[1] }
    groupedSubscriptionsByCluster?.each { Integer cluster, List<List<Integer>> endpointsList ->
        if (SupportedMatterClusters[cluster]?.eventSubscriptions) {
            List<Integer> eventIds = SupportedMatterClusters[cluster].eventSubscriptions
            // Get unique endpoints for this cluster
            Set<Integer> clusterEndpoints = endpointsList*.get(0) as Set<Integer>
            // Build event paths for each endpoint (skip disabled devices)
            clusterEndpoints.each { Integer endpoint ->
                String dni = "${device.id}-${HexUtils.integerToHexString(endpoint, 1).toUpperCase()}"
                ChildDeviceWrapper childDevice = getChildDevice(dni)
                if (childDevice?.disabled == true) {
                    logDebug "cleanSubscribeCmd(): skipping disabled device events for endpoint ${endpoint} (${childDevice.displayName})"
                    return  // continue to next endpoint
                }
                eventIds.each { Integer eventId ->
                    eventPaths.add(matter.eventPath(endpoint, cluster, eventId))
                }
            }
        }
    }

    // Filter out attribute subscriptions for disabled child devices
    List<Map<String, String>> attributePaths = state.subscriptions?.findAll { sub ->
        Integer endpoint = sub[0] as Integer
        String dni = "${device.id}-${HexUtils.integerToHexString(endpoint, 1).toUpperCase()}"
        ChildDeviceWrapper childDevice = getChildDevice(dni)
        if (childDevice?.disabled == true) {
            logDebug "cleanSubscribeCmd(): skipping disabled device endpoint ${endpoint} (${childDevice.displayName})"
            return false
        }
        return true
    }?.collect { sub ->
        matter.attributePath(sub[0] as Integer, sub[1] as Integer, sub[2] as Integer)
    } ?: []

    paths.addAll(eventPaths)
    paths.addAll(attributePaths)
    
    if (paths.isEmpty()) {
        logWarn 'cleanSubscribeCmd(): paths is empty!'
        return null
    }
    logDebug "paths for cleanSubscribe: ${paths}"
    List<Map<String, String>> minimizedPaths = []
    minimizedPaths = minimizeByWildcard(paths)
    logDebug "minimizedPaths for cleanSubscribe: ${minimizedPaths}"
    Integer cleanSubscribeMin = safeNumberToInt(settings?.cleanSubscribeMinInterval, CLEAN_SUBSCRIBE_MIN_INTERVAL_DEFAULT)
    Integer cleanSubscribeMax = safeNumberToInt(settings?.cleanSubscribeMaxInterval, CLEAN_SUBSCRIBE_MAX_INTERVAL_DEFAULT)
    if (cleanSubscribeMin < 1) { cleanSubscribeMin = 1 }
    if (cleanSubscribeMax < cleanSubscribeMin) { cleanSubscribeMax = cleanSubscribeMin }
    if (cleanSubscribeMax > CLEAN_SUBSCRIBE_MAX_ALLOWED_INTERVAL) { cleanSubscribeMax = CLEAN_SUBSCRIBE_MAX_ALLOWED_INTERVAL }
    logDebug "cleanSubscribe intervals: min=${cleanSubscribeMin} max=${cleanSubscribeMax}"
    return matter.cleanSubscribe(cleanSubscribeMin, cleanSubscribeMax, minimizedPaths)
}

List<Map<String, Object>> minimizeByWildcard(List<Map<String, Object>> paths) {
    if (!paths) return []

    // Group by "cluster + attr" or "cluster + evt"
    Map<String, List<Map>> groups = [:].withDefault { [] }

    paths.each { p ->
        // Filter out Descriptor cluster (0x001D) paths
        Integer clusterInt = (p.cluster instanceof Number) ? (p.cluster as Integer)
                          : Integer.decode(p.cluster.toString())
        if (clusterInt == 0x001D || clusterInt == 29) {
            logTrace "minimizeByWildcard: filtering out Descriptor cluster path: ${p}"
            return // skip this path
        }
        
        if (p.attr != null) {
            String key = "A:${p.cluster}:${p.attr}"
            groups[key] << p
        }
        else if (p.evt != null) {
            String key = "E:${p.cluster}:${p.evt}"
            groups[key] << p
        }
        else {
            // keep anything unknown as-is
            String key = "X:${p.cluster}:${p.ep}"
            groups[key] << p
        }
    }

    List<Map<String, Object>> result = []

    groups.each { key, items ->
        // Default behavior
        if (items.size() > 1) {
            // collapse to wildcard endpoint
            Map first = items[0]
            Map collapsed = [
                ep     : -1,
                cluster: first.cluster
            ]
            if (first.attr != null) collapsed.attr = first.attr
            if (first.evt  != null) collapsed.evt  = first.evt
            result << collapsed
        }
        else {
            // keep original single entry
            result << items[0]
        }
    }

    return result
}



// TODO - check if this is still needed ?
void checkSubscriptionStatus() {
    if (state.states == null) { state.states = [:] }
    if (state.states['isUnsubscribe'] == true) {
        logInfo 'checkSubscription(): unsubscribe() is completed.'
        sendInfoEvent('unsubscribe() is completed', 'something was received in the parse() method')
        state.states['isUnsubscribe'] = false
    }
    if (state.states['isSubscribe'] == true) {
        logInfo 'checkSubscription(): completed.'
        sendInfoEvent('completed', 'something was received in the parse() method')
        state.states['isSubscribe'] = false
    }
}

/**
 * This method is called at the end of the discovery process to update the state.subscriptions list of lists.
 * It collects the known clusters and attributes based on the state.fingerprintXX.Subscribe individual devoces lists.
 * It iterates through each fingerprint in the state and checks if the fingerprint has entries in the SupportedMatterClusters list.
 * If a match is found, it adds the corresponding entries to the state.subscriptions list of lists.
 * The number of the found subscriptions and the device count are logged and sent as info events.
 */
void fingerprintsToSubscriptionsList() {
    logDebug 'fingerprintsToSubscriptionsList:'
    sendInfoEvent('Subscribing for known clusters and attributes reporting ...')
    state.subscriptions = []
    // do NOT subscribe to the Descriptor cluster PartsList attribute - creates problems !!!!!!!!!!!!
    //updateStateSubscriptionsList('add', 0, 0x001D, 0x0003)

    // Bridge/node endpoint (00) subscriptions (not part of fingerprintXX)
    // Subscribe to General Diagnostics attrs only if exposed in the bridge attribute list.
    List bridgeAttrList0033 = state?.bridgeDescriptor?.get('0033_FFFB') as List
    if (bridgeAttrList0033) {
        List<Integer> bridgeAttrInts0033 = bridgeAttrList0033.collect { safeHexToInt(it) }
        if (bridgeAttrInts0033.contains(0x0001)) {
            logDebug 'fingerprintsToSubscriptionsList: adding bridge subscription endpoint 0 cluster 0x0033 attr 0x0001 (RebootCount)'
            updateStateSubscriptionsList(addOrRemove = 'add', endpoint = 0, cluster = 0x0033, attrId = 0x0001)
        }
        else {
            logDebug "fingerprintsToSubscriptionsList: bridge 0x0033 AttributeList does not contain 0x0001 (RebootCount): ${bridgeAttrInts0033}"
        }
        if (bridgeAttrInts0033.contains(0x0002)) {
            logDebug 'fingerprintsToSubscriptionsList: adding bridge subscription endpoint 0 cluster 0x0033 attr 0x0002 (UpTime)'
            updateStateSubscriptionsList(addOrRemove = 'add', endpoint = 0, cluster = 0x0033, attrId = 0x0002)
        }
        else {
            logDebug "fingerprintsToSubscriptionsList: bridge 0x0033 AttributeList does not contain 0x0002 (UpTime): ${bridgeAttrInts0033}"
        }
    }
    else {
        logTrace 'fingerprintsToSubscriptionsList: bridgeDescriptor 0033_FFFB attribute list not available'
    }

    // For each fingerprint in the state, check if the fingerprint has entries in the SupportedMatterClusters list. Then, add these entries to the state.subscriptions map
    Integer deviceCount = 0
    //Map stateCopy = state.clone()
    //Map stateCopy = state
    state.each { fingerprintName, fingerprintMap ->
        if (fingerprintName.startsWith('fingerprint')) {
            boolean knownClusterFound = false
            List subscribeList = fingerprintMap['Subscribe'] as List
            logTrace "fingerprintsToSubscriptionsList: fingerprintName:${fingerprintName} subscribeList:${subscribeList}"
            // Subscribe=[6, 8, 768]
            subscribeList.each { cluster  ->
                Integer clusterInt = safeToInt(cluster)
                List supportedClustersKeys = SupportedMatterClusters.keySet()?.collect { it as Integer }
                if (!supportedClustersKeys.contains(clusterInt)) {
                    logWarn "fingerprintsToSubscriptionsList: clusterInt:${clusterInt} is not in the SupportedMatterClusters list!"
                    return  // continue with the next cluster
                }
                def supportedSubscriptions = SupportedMatterClusters[clusterInt]['subscriptions']
                def supportedSubscriptionsKeys = supportedSubscriptions*.keySet()?.flatten()
                logDebug "fingerprintsToSubscriptionsList: clusterInt:${clusterInt} subscribeList=${subscribeList} supportedSubscriptions=${supportedSubscriptions} supportedSubscriptionsKeys=${supportedSubscriptionsKeys}"
                String endpointId = fingerprintName.substring(fingerprintName.length() - 2, fingerprintName.length())
                // Add the supported subscriptions to the state.subscriptions list
                supportedSubscriptionsKeys.each { attribute ->
                    // check whether the attribute_0xFFFB entry is in the fingerprintMap
                    String clusterListName = HexUtils.integerToHexString(clusterInt, 2) + '_FFFB'
                    List clusterAttrList = fingerprintMap[clusterListName]
                    // convert clusterAttrList from list of hex to list of integers
                    clusterAttrList = clusterAttrList.collect { safeHexToInt(it) }
                    logTrace "fingerprintsToSubscriptionsList: clusterInt:${clusterInt} attribute:${attribute} clusterListName=${clusterListName} clusterAttrList=${clusterAttrList}"
                    if (clusterAttrList != null && clusterAttrList != []) {
                        // 0006_FFFB=[00, 4000, 4001, 4002, 4003, FFF8, FFF9, FFFB, FFFC, FFFD]
                        // check if the attribute is in the clusterAttrList
                        if (!clusterAttrList.contains(attribute)) {
                            logDebug "fingerprintsToSubscriptionsList: clusterInt:${clusterInt} attribute:${attribute} is not in the clusterListName=${clusterListName} clusterAttrList ${clusterAttrList}!"    // downgraded to logDebug on 2026-01-26
                            return  // continue with the next attribute
                        }
                        logDebug "fingerprintsToSubscriptionsList: updateStateSubscriptionsList: adding endpointId=${endpointId} clusterInt:${clusterInt} attribute:${attribute} clusterListName=${clusterListName} to the state.subscriptions list!"
                        updateStateSubscriptionsList(addOrRemove = 'add', endpoint = safeHexToInt(endpointId), cluster = clusterInt, attrId = safeToInt(attribute))
                    }
                    else {  // changed to logDebug  on 2024-06-10  logTrace 2026-01-26
                        logTrace "fingerprintsToSubscriptionsList: clusterInt:${clusterInt} attribute:${attribute} clusterListName ${clusterListName} is not in the fingerprintMap!"
                    }
                }
                // done!
                knownClusterFound = true
            }
            if (knownClusterFound) { deviceCount ++ }
        }
    }
    int numberOfSubscriptions = state.subscriptions?.size() ?: 0
    sendInfoEvent("the number of subscriptions is ${numberOfSubscriptions}")
    sendEvent([name: 'deviceCount', value: deviceCount, descriptionText: "${device.displayName} subscribed for events from ${deviceCount} devices"])
}

void setSwitch(String commandPar, String deviceNumberPar/*, extraPar = null*/) {
    String command = commandPar.strip()
    Integer deviceNumber
    logDebug "setSwitch() command: ${command}, deviceNumber:${deviceNumberPar}, extraPar:${extraPar}"
    deviceNumber = safeNumberToInt(deviceNumberPar)
    if (deviceNumber == null || deviceNumber <= 0 || deviceNumber > 255) {
        logWarn "setSwitch(): deviceNumber ${deviceNumberPar} is not valid!"
        return
    }

    // find the command in the OnOffClusterCommands map
    Integer onOffcmd = OnOffClusterCommands.find { k, v -> v == command }?.key
    logTrace "setSwitch(): command = ${command}, onOffcmd = ${onOffcmd}, onOffCommandsList = ${onOffCommandsList}"
    if (onOffcmd == null) {
        logWarn "setSwitch(): command '${command}' is not valid for ${getDeviceDisplayName(deviceNumber)} !"
        return
    }

    String cmd = ''
    switch (command) {
        case ['Off', 'On', 'Toggle']:
            cmd = matter.invoke(deviceNumber, 0x0006, onOffcmd)
            break
        case 'OffWithEffect':
            cmd = matter.invoke(deviceNumber, 0x0006, 0x0040)
            break
        case 'OnWithRecallGlobalScene':
            cmd = matter.invoke(deviceNumber, 0x0006, 0x0041)
            break
        case 'OnWithTimedOff':
            cmd = matter.invoke(deviceNumber, 0x0006, 0x0042)
            break
        default:
            logWarn "setSwitch(): command '${command}' is not valid!"
            return
    }
    logTrace "setSwitch(): sending command '${cmd}'"
    sendToDevice(cmd)
}

void refresh() {
    logInfo'refresh() ...'
    checkDriverVersion()
    setRefreshRequest()    // 6 seconds
    List<String> cmdsList = getSubscribeOrRefreshCmdList('REFRESH_ALL')
    if (cmdsList != null && cmdsList != []) {
        logDebug "refresh(): cmdsList = ${cmdsList}"
        sendToDevice(cmdsList)
    }
    else {
        logWarn 'refresh(): cmdsList is null or empty!'
    }
}

String refreshCmd() {
    logInfo 'refreshCmd() ...'
    List<Map<String, String>> attributePaths = state.subscriptions?.collect { sub ->
        matter.attributePath(sub[0] as Integer, sub[1] as Integer, sub[2] as Integer)
    } ?: []
    if (state['bridgeDescriptor'] == null) { logWarn 'refreshCmd(): state.bridgeDescriptor is null!'; return null  }
    List<String> serverList = (state['bridgeDescriptor']['0033_FFFB'] as List)?.clone()  // new ArrayList<>(originalList)
    serverList?.removeAll(['FFF8', 'FFF9', 'FFFB', 'FFFC', 'FFFD', '00'])                // 0x0000  : 'NetworkInterfaces' - not supported
    if (serverList == null) { logWarn 'refreshCmd(): serverList is null!'; return null  }
    attributePaths.addAll(serverList?.collect { attr ->
        matter.attributePath(0, 0x0033, HexUtils.hexStringToInt(attr))
    })
    return matter.readAttributes(attributePaths)
}

void logsOff() {
    log.warn 'debug logging disabled...'
    device.updateSetting('logEnable', [value:'false', type:'bool'])
}

void traceOff() {
    logInfo 'trace logging disabled...'
    device.updateSetting('traceEnable', [value: 'false', type: 'bool'])
}


void sendToDevice(List<String> cmds, Integer delay = 300) {
    logDebug "sendToDevice (List): (${cmds})"
    if (state.stats != null) { state.stats['txCtr'] = (state.stats['txCtr'] ?: 0) + 1 } else { state.stats = [:] }
    if (state.lastTx != null) { state.lastTx['cmdTime'] = now() } else { state.lastTx = [:] }
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds, delay), hubitat.device.Protocol.MATTER))
}

/* groovylint-disable-next-line UnusedMethodParameter */
void sendToDevice(String cmd, Integer delay = 300) {
    logDebug "sendToDevice (String): (${cmd})"
    if (state.stats != null) { state.stats['txCtr'] = (state.stats['txCtr'] ?: 0) + 1 } else { state.stats = [:] }
    if (state.lastTx != null) { state.lastTx['cmdTime'] = now() } else { state.lastTx = [:] }
    sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))
}

List<String> commands(List<String> cmds, Integer delay = 300) {
    return delayBetween(cmds.collect { it }, delay)
}

// Component logging - allows child devices to log to parent with device context
void componentLog(DeviceWrapper dw, String level, String message) {
    String deviceInfo = "${dw.displayName} (#${dw.getDataValue('id')})"
    switch (level) {
        case 'info':
            logInfo "${deviceInfo}: ${message}"
            break
        case 'warn':
            logWarn "${deviceInfo}: ${message}"
            break
        case 'error':
            logError "${deviceInfo}: ${message}"
            break
        case 'debug':
        default:
            logDebug "${deviceInfo}: ${message}"
            break
    }
}

/* ============================= Child Devices code ================================== */
/* code segments 'borrowed' from Jonathan's 'Tuya IoT Platform (Cloud)' driver importUrl: 'https://raw.githubusercontent.com/bradsjm/hubitat-drivers/main/Tuya/TuyaOpenCloudAPI.groovy' */

/**
 * Helper function to extract device types from DeviceTypeList
 * DeviceTypeList format: [deviceType, revision, deviceType, revision, ...]
 * This function returns only the device types (even-indexed elements)
 */
 /*
 TOBEDEL !!!!!!!!!!!!!!!!
List<String> deviceTypesOnly(List<String> dtList) {
    if (!(dtList instanceof List)) return []
    return (0..<dtList.size()).findAll { (it % 2) == 0 }.collect { dtList[it] }
}
*/

/**
  *  Tuya Standard Instruction Set Category Mapping to Hubitat Drivers
  *  https://developer.tuya.com/en/docs/iot/standarddescription?id=K9i5ql6waswzq
  *  MATTER : https://developer.tuya.com/en/docs/iot-device-dev/Matter_Product_Feature_List?id=Kd2wjfpuhgmrw
  */
//private static Map mapMatterCategory(Map d) {
Map mapMatterCategory(Map d) {
    // check order is important!
    logDebug "mapMatterCategory: ServerList=${d.ServerList} DeviceType=${d.DeviceType}"
    
    // DeviceType is already normalized (device types only, no revision numbers)
    List<String> deviceTypes = d.DeviceType ?: []

    if ('0300' in d.ServerList) {
        // Check for Extended Color Light (0x010D or decimal 13/0x0D) or Color Temperature Light (0x010C)
        boolean isExtendedColor = deviceTypes.any { it.toUpperCase() in ['0D', '13', '10D', '010D'] }
        if (isExtendedColor) {
            return [ driver: 'Generic Component RGBW', product_name: 'RGB Extended Color Light' ]
        }
        else {
            return [ driver: 'Generic Component CT', product_name: 'Color Temperature Light' ]
        }
    }
    if ('0008' in d.ServerList) {   // Dimmer
        return [ driver: 'Generic Component Dimmer', product_name: 'Dimmer/Bulb' ]
    }
    if ('0045' in d.ServerList) {   //  Boolean State (Contact Sensor or Water Leak Sensor)
        // Use DeviceTypeList to determine sensor type
        if (deviceTypes.any { it.toUpperCase() in ['43', '0043'] }) {   // 0x0043 = Water Leak Detector
            return [ driver: 'Generic Component Water Sensor', product_name: 'Water Leak Sensor' ]
        }
        if (deviceTypes.any { it.toUpperCase() in ['15', '0015'] }) {   // 0x0015 = Contact Sensor
            return [ driver: 'Generic Component Contact Sensor', product_name: 'Contact Sensor' ]
        }
        
        // Default to Contact Sensor if DeviceType is not specified
        logWarn "mapMatterCategory: Boolean State cluster 0x0045 found but DeviceType is ambiguous: ${deviceTypes} - defaulting to Contact Sensor"
        return [ driver: 'Generic Component Contact Sensor', product_name: 'Contact Sensor' ]
    }
    if ('005B' in d.ServerList) {   // Air Quality Sensor
        return [ namespace: 'kkossev', driver: 'Matter Generic Component Air Purifier', product_name: 'Air Quality Sensor' ]
    }
    if ('0101' in d.ServerList) {   // Door Lock (since version 1.1.0)
        return [ namespace: 'kkossev', driver: 'Matter Generic Component Door Lock', product_name: 'Door Lock' ]
    }
    if ('0102' in d.ServerList) {   // Curtain Motor (uses custom driver)
        return [ namespace: 'kkossev', driver: 'Matter Generic Component Window Shade', product_name: 'Curtain Motor' ]
    }
    if ('0201' in d.ServerList) {   // Thermostat (since version 1.2.0)
        return [ driver: 'Generic Component Thermostat', product_name: 'Thermostat' ]
    }
    if ('0400' in d.ServerList) {   // Illuminance Sensor
        return [ driver: 'Generic Component Omni Sensor', product_name: 'Illuminance Sensor' ]
    }
    if ('0402' in d.ServerList) {   // TemperatureMeasurement
        return [ driver: 'Generic Component Omni Sensor', product_name: 'Temperature Sensor' ]
    }
    if ('0405' in d.ServerList) {   // HumidityMeasurement
        return [ driver: 'Generic Component Omni Sensor', product_name: 'Humidity Sensor' ]
    }
    if ('0406' in d.ServerList) {   // OccupancySensing (motion)
        return [ namespace: 'kkossev', driver: 'Matter Generic Component Motion Sensor', product_name: 'Motion Sensor' ]
    }
    if ('042A' in d.ServerList) {   // Concentration Measurement Sensor
        return [ namespace: 'kkossev', driver: 'Matter Generic Component Air Purifier', product_name: 'Air Quality Sensor' ]
    }
    if ('0090' in d.ServerList) {   // Electrical Power Measurement Cluster
        return [ namespace: 'kkossev', driver: 'Matter Custom Component Power Energy', product_name: 'Power Measurement' ]
    }
    if ('0006' in d.ServerList) {   // OnOff
        return [ namespace: 'kkossev', driver: 'Matter Generic Component Switch', product_name: 'Switch' ]
    }
    if ('003B' in d.ServerList) {   // Switch / Button - TODO !
        return [ namespace: 'kkossev', driver: 'Matter Generic Component Button', product_name: 'Button' ]
    }
    if ('002F' in d.ServerList) {   // Power Source
        return [ namespace: 'kkossev', driver: 'Matter Generic Component Battery', product_name: 'Battery' ]
    }

    return [ driver: 'Generic Component Switch', product_name: 'Unknown' ]
}

/* --------------------------------------------------------------------------------------------------------------
 * Implementation of component commands from child devices
 */

// Component command to refresh device
void componentRefresh(DeviceWrapper dw) {
    String id = dw.getDataValue('id')       // in hex
    // find the id in the state.subscriptions list of lists - this is the first element of the lists
    List<List<Integer>> stateSubscriptionsList = state.subscriptions ?: []
    List<List<Integer>> deviceSubscriptionsList = stateSubscriptionsList.findAll { it[0] == HexUtils.hexStringToInt(id) }
    logDebug "componentRefresh(${dw}) id=${id} deviceSubscriptionsList=${deviceSubscriptionsList}"
    if (deviceSubscriptionsList == null || deviceSubscriptionsList == []) {
        logWarn "componentRefresh(${dw}) id=${id} deviceSubscriptionsList is empty!"
        return
    }
    // for deviceSubscriptionsList, readAttributes
    List<Map<String, String>> attributePaths = deviceSubscriptionsList.collect { sub ->
        matter.attributePath(sub[0] as Integer, sub[1] as Integer, sub[2] as Integer)
    }
    if (!attributePaths.isEmpty()) {
        setRefreshRequest()    // 6 seconds
        sendToDevice(matter.readAttributes(attributePaths))
        logDebug "componentRefresh(${dw}) id=${id} : refreshing attributePaths=${attributePaths}"
    }
}

void componentIdentify(DeviceWrapper dw) {
    if (!dw.hasCommand('identify')) { logError "componentIdentify(${dw}) driver '${dw.typeName}' does not have command 'identify' in ${dw.supportedCommands}"; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    logDebug "sending Identify command to device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    String cmd
    Integer time = 10
    List<Map<String, String>> cmdFields = []
    cmdFields.add(matter.cmdField(0x05, 0x00, zigbee.swapOctets(HexUtils.integerToHexString(time, 2))))
    cmd = matter.invoke(deviceNumber, 0x0003, 0x0000, cmdFields)
    sendToDevice(cmd)
}

// Component command to ping the device
void componentPing(DeviceWrapper dw) {
    String id = dw.getDataValue('id')
    Integer deviceNumber = HexUtils.hexStringToInt(id)
    if (deviceNumber == null || deviceNumber <= 0 || deviceNumber > 255) {
        logWarn "componentPing(${dw}) id=${id} is not valid!"
        return
    }
    
    // Initialize pending pings state if needed
    if (state.pendingPings == null) { state.pendingPings = [:] }
    
    Long startTime = now()
    // Create unique ping ID using device number and timestamp
    String pingId = "${deviceNumber}-${startTime}"
    
    // Store ping tracking information
    state.pendingPings[pingId] = [
        deviceNumber: deviceNumber,
        startTime: startTime,
        dni: dw.deviceNetworkId,
        cluster: '001D',      // Descriptor cluster (universal)
        attrId: '0000'        // DeviceTypeList attribute
    ]
    
    // Send read request for lightweight Descriptor cluster attribute
    List<Map<String, String>> attributePaths = []
    attributePaths.add(matter.attributePath(deviceNumber, 0x001D, 0x0000))
    sendToDevice(matter.readAttributes(attributePaths))
    
    // Schedule timeout cleanup
    Integer timeoutSeconds = MAX_PING_MILISECONDS / 1000
    runIn(timeoutSeconds, 'pingTimeout', [data: [pingId: pingId]])
    
    logDebug "componentPing(${dw}) id=${id}: ping sent, tracking as ${pingId}"
}

// Handle ping timeout when device doesn't respond
void pingTimeout(Map data) {
    String pingId = data?.pingId
    if (pingId == null || state.pendingPings == null) { return }
    
    if (state.pendingPings.containsKey(pingId)) {
        Map pingEntry = state.pendingPings[pingId]
        String endpointHex = HexUtils.integerToHexString(pingEntry.deviceNumber, 1).padLeft(2, '0')
        
        // Send ping timeout event to child device
        sendHubitatEvent([
            name: 'rtt',
            value: -1,
            descriptionText: "Ping timeout - no response after ${MAX_PING_MILISECONDS}ms",
            type: 'digital'
        ], [endpoint: endpointHex], false)
        
        logTrace "Ping timeout for device ${pingEntry.deviceNumber} (${pingEntry.dni})"
        
        state.pendingPings.remove(pingId)
    }
}

/**
 * Get ServerList for a child device from fingerprintData
 * @param dw Child device wrapper
 * @return List of cluster hex strings (normalized to 4 characters), or empty list if not found
 */
private List<String> getDeviceServerList(DeviceWrapper dw) {
    // Try child's fingerprintData first
    String fingerprintJson = dw.getDataValue('fingerprintData')
    if (fingerprintJson) {
        try {
            Map fingerprint = new groovy.json.JsonSlurper().parseText(fingerprintJson)
            List<String> serverList = fingerprint?.ServerList
            if (serverList != null && !serverList.isEmpty()) {
                // Normalize cluster IDs to 4-character hex strings (e.g., "06" -> "0006")
                return serverList.collect { it.toString().toUpperCase().padLeft(4, '0') }
            }
        } catch (Exception e) {
            logDebug "getDeviceServerList: failed to parse fingerprintData for ${dw.displayName}: ${e.message}"
        }
    }
    
    // Fallback: try parent's state fingerprint data
    String dni = dw.deviceNetworkId
    String endpoint = dni?.split('-')?.last()
    if (endpoint) {
        String fingerprintName = getFingerprintName([endpoint: endpoint])
        if (state[fingerprintName]) {
            Map parentFingerprint = state[fingerprintName]
            List<String> serverList = parentFingerprint?.ServerList
            if (serverList != null && !serverList.isEmpty()) {
                logDebug "getDeviceServerList: using parent state fingerprint ${fingerprintName} for ${dw.displayName}"
                // Normalize cluster IDs to 4-character hex strings (e.g., "06" -> "0006")
                return serverList.collect { it.toString().toUpperCase().padLeft(4, '0') }
            }
        }
    }
    
    logDebug "getDeviceServerList: ServerList not found for ${dw.displayName}, will attempt command anyway"
    return []
}

// Component command to turn on device
void componentOn(DeviceWrapper dw) {
    if (!dw.hasCommand('on')) { 
        logError "componentOn(${dw}) driver '${dw.typeName}' does not have command 'on' in ${dw.supportedCommands}"
        return
    }
    
    // Extract endpoint from device network ID (format: "parentId-endpoint")
    String dni = dw.deviceNetworkId
    String endpoint = dni?.split('-')?.last()
    if (endpoint == null) {
        logError "componentOn(${dw}) cannot extract endpoint from DNI: ${dni}"
        return
    }
    
    List<String> serverList = getDeviceServerList(dw)
    String deviceId = '0x' + endpoint
    
    if ('0006' in serverList) {
        logDebug "componentOn(${dw}) ServerList contains OnOff cluster (0006) - turning on"
        setSwitch('On', deviceId)
    }
    else if ('0201' in serverList) {
        logDebug "componentOn(${dw}) ServerList contains Thermostat cluster (0201) - calling heat()"
        componentSetThermostatMode(dw, 'heat')
    }
    else {
        // ServerList is empty/missing OR doesn't contain known clusters
        if (serverList.isEmpty()) {
            logDebug "componentOn(${dw}) ServerList unavailable - attempting On command anyway"
        } else {
            logDebug "componentOn(${dw}) ServerList ${serverList} does not contain OnOff (0006) or Thermostat (0201) - attempting On command anyway"
        }
        setSwitch('On', deviceId)
    }
}

// Component command to turn off device
void componentOff(DeviceWrapper dw) {
    if (!dw.hasCommand('off')) { 
        logError "componentOff(${dw}) driver '${dw.typeName}' does not have command 'off' in ${dw.supportedCommands}"
        return
    }
    
    // Extract endpoint from device network ID (format: "parentId-endpoint")
    String dni = dw.deviceNetworkId
    String endpoint = dni?.split('-')?.last()
    if (endpoint == null) {
        logError "componentOff(${dw}) cannot extract endpoint from DNI: ${dni}"
        return
    }
    
    List<String> serverList = getDeviceServerList(dw)
    String deviceId = '0x' + endpoint
    
    if ('0006' in serverList) {
        logDebug "componentOff(${dw}) ServerList contains OnOff cluster (0006) - turning off"
        setSwitch('Off', deviceId)
    }
    else if ('0201' in serverList) {
        logDebug "componentOff(${dw}) ServerList contains Thermostat cluster (0201) - calling off()"
        componentSetThermostatMode(dw, 'off')
    }
    else {
        // ServerList is empty/missing OR doesn't contain known clusters
        if (serverList.isEmpty()) {
            logDebug "componentOff(${dw}) ServerList unavailable - attempting Off command anyway"
        } else {
            logDebug "componentOff(${dw}) ServerList ${serverList} does not contain OnOff (0006) or Thermostat (0201) - attempting Off command anyway"
        }
        setSwitch('Off', deviceId)
    }
}

//  '[close, open, refresh, setPosition, startPositionChange, stopPositionChange]'
// Component command to open device
void componentOpen(DeviceWrapper dw) {
    if (!dw.hasCommand('open')) { logError "componentOpen(${dw}) driver '${dw.typeName}' does not have command 'open' in ${dw.supportedCommands}"; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    logDebug "sending Open command to device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    if (deviceNumber == null || deviceNumber <= 0 || deviceNumber > 255) { logWarn "componentOpen(): deviceNumber ${deviceNumberPar} is not valid!"; return }
    String cmd = matter.invoke(deviceNumber, 0x0102, 0x00) // 0x0102 = Window Covering Cluster, 0x00 = UpOrOpen
    logTrace "componentOpen(): sending command '${cmd}'"
    sendToDevice(cmd)
}

// Component command to close device
void componentClose(DeviceWrapper dw) {
    if (!dw.hasCommand('close')) { logError "componentClose(${dw}) driver '${dw.typeName}' does not have command 'close' in ${dw.supportedCommands}"; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    logDebug "sending Close command to device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    if (deviceNumber == null || deviceNumber <= 0 || deviceNumber > 255) { logWarn "componentClose(): deviceNumber ${deviceNumberPar} is not valid!"; return }
    String cmd = matter.invoke(deviceNumber, 0x0102, 0x01) // 0x0102 = Window Covering Cluster, 0x01 = DownOrClose
    logTrace "componentClose(): sending command '${cmd}'"
    sendToDevice(cmd)
}

// prestage level : https://community.hubitat.com/t/sengled-element-color-plus-driver/21811/2

// Component command to set level
void componentSetLevel(DeviceWrapper dw, BigDecimal levelPar, BigDecimal durationPar=null) {
    if (!dw.hasCommand('setLevel')) { logError "componentSetLevel(${dw}) driver '${dw.typeName}' does not have command 'setLevel' in ${dw.supportedCommands}" ; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    if (levelPar == null) { logWarn "componentSetLevel(): levelPar is null!"; return }
    int level = levelPar as int
    level = level < 0 ? 0 : level > 100 ? 100 : level
    int duration = (durationPar ?: 0) * 10
    logDebug "Setting level ${level} durtion ${duration} for device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    Integer levelHex = Math.round(level * 2.54)
    List<Map<String, String>> cmdFields = []
    cmdFields.add(matter.cmdField(0x04, 0x00, HexUtils.integerToHexString(levelHex, 1)))
    cmdFields.add(matter.cmdField(0x05, 0x01, zigbee.swapOctets(HexUtils.integerToHexString(duration, 2))))
    cmd = matter.invoke(deviceNumber, 0x0008, 0x04, cmdFields)  // 0x0008 = Level Control Cluster, 0x04 = MoveToLevelWithOnOff
    def stock = matter.setLevel(level, duration)                             //    {152400 0C2501 0A0018}'
    sendToDevice(cmd)
}

// Component command to start level change (up or down)
void componentStartLevelChange(DeviceWrapper dw, String direction) {
    if (!dw.hasCommand('startLevelChange')) { logError "componentStartLevelChange(${dw}) driver '${dw.typeName}' does not have command 'startLevelChange' in ${dw.supportedCommands}"; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    if (direction == null) { logWarn "componentStartLevelChange(): direction is null!"; return }
    String moveMode = direction == 'up' ? '00' : '01'
    Integer rateInt = 5  // seconds
    //String moveRate = zigbee.swapOctets(HexUtils.integerToHexString(rateInt as int, 1))   // TODO - errorjava.lang.StringIndexOutOfBoundsException: begin 2, end 4, length 2 on line 1684 (method componentStartLevelChange)
    String moveRate = '50'
    List<Map<String, String>> cmdFields = []
    logDebug "Starting level change UP for device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    cmdFields.add(matter.cmdField(DataType.UINT8, 0x00, moveMode))   // MoveMode
    cmdFields.add(matter.cmdField(DataType.UINT8, 0x01, moveRate))   // MoveRate    // TODO - configurable ??
    String cmd = matter.invoke(deviceNumber, 0x0008, 0x01, cmdFields)       // 0x01 = Move
    sendToDevice(cmd)
}

// Component command to stop level change
void componentStopLevelChange(DeviceWrapper dw) {
    if (!dw.hasCommand('stopLevelChange')) { logError "componentStopLevelChange(${dw}) driver '${dw.typeName}' does not have command 'stopLevelChange' in ${dw.supportedCommands}"; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    List<Map<String, String>> cmdFields = []
    cmdFields.add(matter.cmdField(DataType.UINT8, 0x00, '00'))      // OptionsMask - map8 = 0x18
    cmdFields.add(matter.cmdField(DataType.UINT8, 0x01, '00'))      // OptionsOverride
    String cmd = matter.invoke(deviceNumber, 0x0008, 0x03, cmdFields)      // 0x03 = Stop
    sendToDevice(cmd)
}

void componentSetColorTemperature(DeviceWrapper dw, BigDecimal colorTemperature, BigDecimal level=null, BigDecimal duration=null) {
    if (!dw.hasCommand('setColorTemperature')) { logError "componentSetColorTemperature(${dw}) driver '${dw.typeName}' does not have command 'setColorTemperature' in ${dw.supportedCommands}"; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    logDebug "Setting color temperature ${colorTemperature} for device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    if (dw.currentValue('switch') == 'off') {
        logDebug "componentSetColorTemperature(): device is off, turning it on"
        componentOn(dw)
    }
    if (dw.currentValue('colorMode') != 'CT') {
        logDebug "componentSetColor(): setting color mode to CT"
        sendHubitatEvent([name: 'colorMode', value: 'CT', isStateChange: true, displayed: false], dw, true)
    }
    String colorTemperatureMireds = byteReverseParameters(HexUtils.integerToHexString(ctToMired(colorTemperature as int), 2))
    String transitionTime = zigbee.swapOctets(HexUtils.integerToHexString((duration ?: 0) as int, 2))
    List<Map<String, String>> cmdFields = []
    cmdFields.add(matter.cmdField(DataType.UINT16, 0, colorTemperatureMireds))
    cmdFields.add(matter.cmdField(DataType.UINT16, 1, transitionTime))
    String cmd = matter.invoke(deviceNumber, 0x0300, 0x0A, cmdFields)  // 0x0300 = Color Control Cluster, 0x0A = MoveToColorTemperature
    sendToDevice(cmd)
    if (level != null || duration != null) {
        componentSetLevel(dw, level, duration)
    }
}

void componentSetHue(DeviceWrapper dw, BigDecimal hue) {
    if (!dw.hasCommand('setHue')) { logError "componentSetHue(${dw}) driver '${dw.typeName}' does not have command 'setHue' in ${dw.supportedCommands}"; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    logDebug "Setting hue ${hue} for device ${dw.getDataValue('id')} ${dw}"
    Integer hueScaled = Math.min(Math.max(Math.round(hue * 2.54), 0), 254)
    String hueHex = byteReverseParameters(HexUtils.integerToHexString(hueScaled, 1))
    String transitionTimeHex = zigbee.swapOctets(HexUtils.integerToHexString(1, 2))
    List<Map<String, String>> cmdFields = [
        matter.cmdField(DataType.UINT8, 0, hueHex),
        matter.cmdField(DataType.UINT8, 1, "00"), // Direction 00 = Shortest
        matter.cmdField(DataType.UINT16, 2, transitionTimeHex) // TransitionTime in 0.1 seconds, uint16 0-65534, byte swapped
    ]
    String cmd = matter.invoke(HexUtils.hexStringToInt(dw.getDataValue('id')), 0x0300, 0x00, cmdFields) // 0x0300 = Color Control Cluster, 0x00 = MoveToHue
    sendToDevice(cmd)
}

void componentSetSaturation(DeviceWrapper dw, BigDecimal saturation) {
    if (!dw.hasCommand('setSaturation')) { logError "componentSetSaturation(${dw}) driver '${dw.typeName}' does not have command 'setSaturation' in ${dw.supportedCommands}"; return }
    Integer saturationScaled = Math.min(Math.max(Math.round(saturation * 2.54), 0), 254)
    String saturationHex = byteReverseParameters(HexUtils.integerToHexString(saturationScaled as int, 1))
    String transitionTimeHex = zigbee.swapOctets(HexUtils.integerToHexString(1, 2))
    List<Map<String, String>> cmdFields = [
        matter.cmdField(DataType.UINT8, 0, saturationHex),
        matter.cmdField(DataType.UINT16, 1, transitionTimeHex)
    ]
    String cmd = matter.invoke(HexUtils.hexStringToInt(dw.getDataValue('id')), 0x0300, 0x03, cmdFields)
    sendToDevice(cmd)
}

void componentSetColor(DeviceWrapper dw, Map colormap) {
    if (!dw.hasCommand('setColor')) { logError "componentSetColor(${dw}) driver '${dw.typeName}' does not have command 'setColor' in ${dw.supportedCommands}"; return }
    logDebug "Setting color hue ${colormap.hue} saturation ${colormap.saturation} level ${colormap.level} for device# ${dw.getDataValue('id')} ${dw}"
    if (dw.currentValue('switch') == 'off') {
        logDebug "componentSetColor(): device is off, turning it on"
        componentOn(dw)
    }
    if (dw.currentValue('colorMode') != 'RGB') {
        logDebug "componentSetColor(): setting color mode to RGB"
        sendHubitatEvent([name: 'colorMode', value: 'RGB', isStateChange: true, displayed: false], dw, true)
    }
    Integer hueScaled = Math.round(Math.max(0, Math.min((double)(colormap.hue * 2.54), 254.0)))
    Integer saturationScaled = Math.round(Math.max(0, Math.min((colormap.saturation * 2.54).toInteger(), 254)))
    Integer levelScaled = Math.round(Math.max(0, Math.min((colormap.level * 2.54).toInteger(), 254)))
    Integer transitionTime = 1
    logDebug    "Setting color hue ${hueScaled} saturation ${saturationScaled} level ${levelScaled} for device# ${dw.getDataValue('id')} ${dw}"
    List<Map<String, String>> cmdFields = [
        matter.cmdField(DataType.UINT8, 0, byteReverseParameters(HexUtils.integerToHexString(hueScaled as int, 1))),
        matter.cmdField(DataType.UINT8, 1, byteReverseParameters(HexUtils.integerToHexString(saturationScaled as int, 1))),
        matter.cmdField(DataType.UINT16, 2, zigbee.swapOctets(HexUtils.integerToHexString(transitionTime as int, 2)))
    ]
    String cmd = matter.invoke(HexUtils.hexStringToInt(dw.getDataValue('id')), 0x0300, 0x06, cmdFields)  // 0x0300 = Color Control Cluster, 0x06 = MoveToHueAndSaturation ;0x07 = MoveToColor
    sendToDevice(cmd)
}

void componentSetEffect(DeviceWrapper dw, BigDecimal effect) {
    String id = dw.getDataValue('id')
    logWarn "componentSetEffect(${dw}) id=${id} (TODO: not implemented!)"
}

void componentSetNextEffect(DeviceWrapper dw) {
    String id = dw.getDataValue('id')
    logWarn "componentSetNextEffect(${dw}) id=${id} (TODO: not implemented!)"
}

void componentSetPreviousEffect(DeviceWrapper dw) {
    String id = dw.getDataValue('id')
    logWarn "componentSetPreviousEffect(${dw}) id=${id} (TODO: not implemented!)"
}


// Component command to set position  (used by Window Shade)
void componentSetPosition(DeviceWrapper dw, BigDecimal positionPar) {
    if (!dw.hasCommand('setPosition')) { logError "componentSetPosition(${dw}) driver '${dw.typeName}' does not have command 'setPosition' in ${dw.supportedCommands}"; return }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    int position = positionPar as int
    if (position < 0) { position = 0 }
    if (position > 100) { position = 100 }
    logDebug "Setting position ${position} for device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    List<Map<String, String>> cmdFields = []
    cmdFields.add(matter.cmdField(0x05, 0x00, zigbee.swapOctets(HexUtils.integerToHexString(position * 100, 2))))
    cmd = matter.invoke(deviceNumber, 0x0102, 0x05, cmdFields)  // 0x0102 = Window Covering Cluster, 0x05 = GoToLiftPercentage
    sendToDevice(cmd)
}

// Component command to set position direction (not used by Window Shade !)
void componentStartPositionChange(DeviceWrapper dw, String direction) {
    logDebug "componentStartPositionChange(${dw}, ${direction})"
    switch (direction) {
        case 'open': componentOpen(dw); break
        case 'close': componentClose(dw); break
        default:
            logWarn "componentStartPositionChange not implemented! direction ${direction} for ${dw}"
            break
    }
}

// Component command to stop position change
void componentStopPositionChange(DeviceWrapper dw) {
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    logDebug "Stopping position change for device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    if (deviceNumber == null || deviceNumber <= 0 || deviceNumber > 255) { logWarn "componentStopPositionChange(): deviceNumber ${deviceNumberPar} is not valid!"; return; }
    String cmd = matter.invoke(deviceNumber, 0x0102, 0x02) // 0x0102 = Window Covering Cluster, 0x02 = StopMotion
    logTrace "componentStopPositionChange(): sending command '${cmd}'"
    sendToDevice(cmd)
}

void componentSetThermostatMode(DeviceWrapper dw, String mode) {
    if (dw.currentValue('supportedThermostatModes') == null) { initializeThermostat(dw) }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    Integer key = ThermostatSystemModes.find { k, v -> v == mode }?.key
    Boolean isSupportedMode = dw.currentValue('supportedThermostatModes')?.contains(mode)
    logDebug "componentSetThermostatMode: setting thermostatMode to <b>${mode}</b> (key=${key}) for device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    if (deviceNumber == null || deviceNumber <= 0 || deviceNumber > 255) { logWarn "componentSetThermostatMode(): deviceNumber ${deviceNumberPar} is not valid!"; return; }
    if (key == null || !isSupportedMode) { logWarn "componentSetThermostatMode(): mode '${mode}' is not valid!" ; return }
    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest(deviceNumber, 0x201, 0x001C, DataType.UINT8, intToHexStr(key,1))) // 0x0201 = Thermostat Cluster, 0x001C = SetThermostatMode
    String cmd = matter.writeAttributes(attrWriteRequests)
    sendToDevice(cmd)
}

void componentSetThermostatFanMode(DeviceWrapper dw, String mode) {
    if (dw.currentValue('supportedThermostatFanModes') == null) { initializeThermostat(dw) }
    logWarn "componentSetThermostatFanMode(${dw}, ${mode}) is not implemented!"
    return
}

void componentSetHeatingSetpoint(DeviceWrapper dw, BigDecimal temperaturePar) {
    if (dw.currentValue('heatingSetpoint') == null) { initializeThermostat(dw) }
    Integer deviceNumber = HexUtils.hexStringToInt(dw.getDataValue('id'))
    logDebug "componentSetHeatingSetpoint: setting heatingSetpoint to <b>${temperaturePar}</b> for device# ${deviceNumber} (${dw.getDataValue('id')}) ${dw}"
    if (deviceNumber == null || deviceNumber <= 0 || deviceNumber > 255) { logWarn "componentSetThermostatMode(): deviceNumber ${deviceNumberPar} is not valid!"; return; }
    Double temperature = temperaturePar as Double
    Double minHeatSetpoint = safeToDouble(dw?.getDataValue('minHeatSetpointLimit')) ?: safeToDouble(dw?.getDataValue('absMinHeatSetpointLimit')) ?: 5.0
    Double maxHeatSetpoint = safeToDouble(dw?.getDataValue('maxHeatSetpointLimit')) ?: safeToDouble(dw?.getDataValue('absMaxHeatSetpointLimit')) ?: 35.0
    //log.trace "parseThermostat: minHeatSetpoint:${minHeatSetpoint} maxHeatSetpoint:${maxHeatSetpoint}"
    if (temperature < minHeatSetpoint) { temperature = minHeatSetpoint }
    if (temperature > maxHeatSetpoint) { temperature = maxHeatSetpoint }
    Integer temperatureScaled = temperature * 100
    List<Map<String, String>> attrWriteRequests = []
    attrWriteRequests.add(matter.attributeWriteRequest(deviceNumber, 0x201, 0x0012, DataType.INT16, intToHexStr(temperatureScaled,2))) // 0x0201 = Thermostat Cluster, 0x0012 = OccupiedHeatingSetpoint
    String cmd = matter.writeAttributes(attrWriteRequests)
    sendToDevice(cmd)
}

void componentSetCoolingSetpoint(DeviceWrapper dw, BigDecimal temperature) {
    if (dw.currentValue('coolingSetpoint') == null) { initializeThermostat(dw) }
    logWarn "componentSetCoolingSetpoint(${dw}, ${temperature}) is not implemented!"
    return
}

/*
 * NOTE: Lock commands have been moved to the child driver (Matter_Generic_Component_Door_Lock)
 * The child driver now builds Matter commands directly and calls parent.sendToDevice()
 * This keeps all lock-specific logic in the lock driver component.
 */

 
void initializeThermostat(DeviceWrapper dw) {
    logWarn "initializeThermostat(${dw}) is not implemented!"
    def supportedThermostatModes = []
    supportedThermostatModes = ["off", "heat"]  // removed "auto" 2024/10/02
    logInfo "supportedThermostatModes: ${supportedThermostatModes}"
    if (dw.currentValue('supportedThermostatModes') == null) { sendHubitatEvent([name: "supportedThermostatModes", value:  JsonOutput.toJson(supportedThermostatModes), isStateChange: true], dw) }
    if (dw.currentValue('supportedThermostatFanModes') == null) { sendHubitatEvent([name: "supportedThermostatFanModes", value: JsonOutput.toJson(["auto"]), isStateChange: true], dw)  }
    if (dw.currentValue('thermostatMode') == null) { sendHubitatEvent([name: "thermostatMode", value: "heat", isStateChange: true, description: "inital attribute setting"], dw) }
    if (dw.currentValue('thermostatFanMode') == null) { sendHubitatEvent([name: "thermostatFanMode", value: "auto", isStateChange: true, description: "inital attribute setting"], dw) }
    if (dw.currentValue('thermostatOperatingState') == null) { sendHubitatEvent([name: "thermostatOperatingState", value: "idle", isStateChange: true, description: "inital attribute setting"], dw) }
    //if (dw.currentValue('thermostatSetpoint') == null) { sendHubitatEvent([name: "thermostatSetpoint", value:  12.3, unit: "\u00B0"+"C", isStateChange: true, description: "inital attribute setting"], dw) }       // Google Home compatibility
    if (dw.currentValue('heatingSetpoint') == null) { sendHubitatEvent([name: "heatingSetpoint", value: 12.3, unit: "\u00B0"+"C", isStateChange: true, description: "inital attribute setting"], dw) }
    if (dw.currentValue('coolingSetpoint') == null) { sendHubitatEvent([name: "coolingSetpoint", value: 34.5, unit: "\u00B0"+"C", isStateChange: true, description: "inital attribute setting"], dw) }
    if (dw.currentValue('temperature') == null) { sendHubitatEvent([name: "temperature", value: 23.4, unit: "\u00B0"+"C", isStateChange: true, description: "inital attribute setting"], dw) }
}

/*
* Retrieves a map of functions from a cache.
* If the map is not already in the cache, it computes the map by parsing a JSON string and adds it to the cache.
* The JSON string is obtained from the DeviceWrapper instance.
* If the DeviceWrapper instance is null or does not contain a 'functions' data value, the method uses an empty JSON string.
*/
private static Map<String, Map> getFunctions(DeviceWrapper dw) {
    return jsonCache.computeIfAbsent(dw?.getDataValue('functions') ?: '{}') {
        k -> jsonParser.parseText(k)
    }
}

/*
* Returns the value of the first key-value pair in the functions map where the key is also an element in the codes list.
* If no such key-value pair is found, or if functions is null, the method returns null.
*/
/* groovylint-disable-next-line UnusedPrivateMethod */
private static Map getFunction(Map functions, List codes) {
    return functions?.find { f -> f.key in codes }?.value
}

/*
* Returns the first code in the codes list that is also a key in the functions map.
* If no such code is found, or if codes or functions is null, the method returns null.
*/
/* groovylint-disable-next-line UnusedPrivateMethod */
private static String getFunctionCode(Map functions, List codes) {
    return codes?.find { c -> functions?.containsKey(c) }
}

/*
* Retrieves a map of status sets from a cache. If the map is not already in the cache, it computes the map by parsing a JSON string and adds it to the cache.
* The JSON string is obtained from the DeviceWrapper instance.
* If the DeviceWrapper instance is null or does not contain a 'statusSet' data value, the method uses an empty JSON string.
*/
/* groovylint-disable-next-line UnusedPrivateMethod */
private static Map<String, Map> getStatusSet(DeviceWrapper dw) {
    return jsonCache.computeIfAbsent(dw?.getDataValue('statusSet') ?: '{}') {
        k -> jsonParser.parseText(k)
    }
}

// Command to remove all the child devices
void removeAllDevices() {
    logInfo 'Removing all child devices'
    childDevices.each { device -> deleteChildDevice(device.deviceNetworkId) }
    sendEvent(name: 'deviceCount', value: '0', descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} All child devices removed")
    sendInfoEvent('All child devices removed')
}

/**
 *  Driver Capabilities Implementation
 */

Map fingerprintToData(String fingerprint) {
    Map data = [:]
    Map fingerprintMap = state[fingerprint]
    if (fingerprintMap != null) {
        data['id'] = fingerprint.substring(fingerprint.length() - 2, fingerprint.length())  // Device Number
        data['name'] = getDeviceDisplayName(data['id'])
        data['fingerprintName'] = fingerprint
        data['ServerList'] = fingerprintMap['ServerList']
        data['ProductName'] = fingerprintMap['ProductName']
        data['VendorName'] = fingerprintMap['VendorName']
        data['ProductLabel'] = fingerprintMap['ProductLabel']
        data['NodeLabel'] = fingerprintMap['NodeLabel']
        data['DeviceType'] = fingerprintMap['DeviceTypeList'] ?: []
        
        // Fallback: If ProductName, ProductLabel, and NodeLabel are all empty/null, use bridge's ProductName
        if (!data['ProductName'] && !data['ProductLabel'] && !data['NodeLabel']) {
            String bridgeProductName = state.bridgeDescriptor?.ProductName ?: state.ProductName
            if (bridgeProductName) {
                logDebug "fingerprintToData(): using bridge ProductName '${bridgeProductName}' for endpoint ${data['id']}"
                data['ProductName'] = bridgeProductName
            }
        }
        
        logDebug "fingerprintToData(): rawDeviceTypeList=${rawDeviceTypeList} extracted deviceTypeIds=${deviceTypeIds} fingerprintMap=${fingerprintMap} data=${data}"

        Map productName = mapMatterCategory(data)

        data['product_name'] = fingerprintMap['ProductName'] ?: productName['product_name']           // Device Name
    }
    return data
 }

private Integer createChildDevices() {
    logDebug 'createChildDevices(): '
    boolean result = false
    Integer deviceCount = 0
    // Exclude node/bridge-only clusters from being treated as a child-device support signal.
    List<Integer> supportedClusters = SupportedMatterClusters.collect { it.key }?.findAll { it != 0x0033 }
    logDebug "createChildDevices(): supportedClusters=${supportedClusters}"
    state.each { fingerprintName, fingerprintMap ->
        if (fingerprintName.startsWith('fingerprint')) {
            List<String> serverListStr = fingerprintMap['ServerList']
            List<Integer> serverListInt = serverListStr.collect { safeHexToInt(it) }
            if (supportedClusters.any { it in serverListInt }) {
                logDebug "createChildDevices(): creating child device for fingerprintName: ${fingerprintName} ProductName: ${fingerprintMap['ProductName']}"
                result = createChildDevices(fingerprintToData(fingerprintName))
                if (result) { deviceCount++ }
            }
            else {
                logWarn "createChildDevices(): fingerprintName: ${fingerprintName} ProductName: ${fingerprintMap['ProductName']} <b>ServerList: ${fingerprintMap['ServerList']}</b> is not supported yet!"
            }
        }
        else {
            logTrace "createChildDevices(): fingerprintName: ${fingerprintName} SKIPPED"
        }
    }
    sendHubitatEvent([name: 'deviceCount', value: deviceCount, descriptionText: "${getDeviceDisplayName(descMap?.endpoint)} number of devices exposing known clusters is ${deviceCount}"])
    return deviceCount
}

private boolean createChildDevices(Map d) {
    logDebug "createChildDevices(Map d): d=${d}"
    Map mapping = mapMatterCategory(d)
    logDebug "createChildDevices(Map d): product_name ${d.product_name} driver ${mapping}"

    if (mapping.driver != null) {
        logDebug "createChildDevices(Map d): mapping.driver is ${mapping.driver}, device.id is ${device.id}"
        logDebug "createChildDevices(Map d): createChildDevice '${device.id}-${d.id}' ${mapping} ${d}   "
        createChildDevice("${device.id}-${d.id}", mapping, d)
    } else {
        logWarn "createChildDevices(Map d): mapping.driver is ${mapping.driver} !"
    }
    return true
}

private ChildDeviceWrapper createChildDevice(String dni, Map mapping, Map d) {
    ChildDeviceWrapper dw = getChildDevice(dni)
    logDebug "createChildDevice(String dni, Map mapping, Map d): dni:${dni} mapping:${mapping} d:${d} dw:${dw}"

    if (dw == null) {
        logInfo "Creating device ${d.name} using ${mapping.driver} driver (name: ${d.product_name}, label: ${d.name})"
        try {
            dw = addChildDevice(mapping.namespace ?: 'hubitat', mapping.driver, dni,
                [
                    name: d.product_name ?: d.ProductLabel ?: d.ProductName ?:  d.name ?: 'Matter Device' as String,
                    label: d.NodeLabel /*?: d.ProductName ?: d.product_name ?: d.name*/ ?: '' as String

                    //name: d.ProductLabel ?: d.ProductName ?: d.product_name ?: d.name ?: 'Matter Device',
                    //label: d.NodeLabel ?: d.ProductName ?: d.product_name ?: d.name ?: 'Matter Device'

                    //name: d.name      // was  d.product_name; was d.name; 
                    //label: null     // do not set the label here, it will be set by the user!
                ]
            )
        } catch (UnknownDeviceTypeException e) {
            if (mapping.namespace == 'kkossev') {
                logError "${d.name} driver not found, try downloading from " +
                          "https://raw.githubusercontent.com/kkossev/Hubitat/development/Drivers/Matter%20Advanced%20Bridge/${mapping.driver}"
            } else {
                logError("${d.name} device creation failed", e)
            }
        }
    }
    /*
    String functionJson = JsonOutput.toJson(d.functions)
    jsonCache.put(functionJson, d.functions)
    */
    dw?.with {
        //label = label ?: d.name
        updateDataValue 'id', d.id
        updateDataValue 'fingerprintName', d.fingerprintName
        updateDataValue 'product_name', d.product_name
        // ServerList is now stored in fingerprintData, not as separate device data
    }

    return dw
}

/**
 * Copy entire fingerprint data from parent state to child device data
 * This preserves critical configuration even after state.fingerprint* is minimized
 * @param fingerprintName The fingerprint key (e.g., 'fingerprint08')
 * @param dni The child device network ID
 */
void copyEntireFingerprintToChild(String fingerprintName, String dni) {
    ChildDeviceWrapper childDevice = getChildDevice(dni)
    if (childDevice == null) {
        logWarn "copyEntireFingerprintToChild: child device ${dni} not found"
        return
    }
    
    Map fingerprint = state[fingerprintName]
    if (fingerprint == null || fingerprint.isEmpty()) {
        logWarn "copyEntireFingerprintToChild: fingerprint ${fingerprintName} is null or empty"
        return
    }
    
    try {
        // Store complete fingerprint as JSON string in child device data
        String fingerprintJson = JsonOutput.toJson(fingerprint)
        childDevice.updateDataValue('fingerprintData', fingerprintJson)
        
        logDebug "copyEntireFingerprintToChild: copied ${fingerprintJson.length()} bytes of fingerprint data to ${childDevice.displayName}"
        logTrace "copyEntireFingerprintToChild: fingerprint keys = ${fingerprint.keySet()}"
    } catch (Exception e) {
        logWarn "copyEntireFingerprintToChild: failed to copy fingerprint ${fingerprintName} to ${dni}: ${e.message}"
    }
}

/**
 * Update child device fingerprint data with newly discovered cluster attributes
 * Called when cluster-specific data (like AttributeLists) is received
 * @param fingerprintName The fingerprint key (e.g., 'fingerprint08')
 * @param attributeName The cluster-specific key (e.g., '0101_FFFB')
 * @param value The value to update
 */
void updateChildFingerprintData(String fingerprintName, String attributeName, Object value) {
    // Get the endpoint from fingerprintName (e.g., 'fingerprint08' -> '08')
    String endpointHex = fingerprintName.replaceFirst('fingerprint', '')
    String dni = "${device.id}-${endpointHex.toUpperCase()}"
    
    ChildDeviceWrapper childDevice = getChildDevice(dni)
    if (childDevice == null) {
        // Child device doesn't exist or not created yet
        return
    }
    
    try {
        // Get existing fingerprint data
        String fingerprintJson = childDevice.getDataValue('fingerprintData')
        Map fingerprint
        
        if (fingerprintJson) {
            fingerprint = new groovy.json.JsonSlurper().parseText(fingerprintJson)
        } else {
            fingerprint = [:]
        }
        
        // Update with new cluster-specific data
        fingerprint[attributeName] = value
        
        // Save back to child device
        String updatedJson = JsonOutput.toJson(fingerprint)
        childDevice.updateDataValue('fingerprintData', updatedJson)
        
        logTrace "updateChildFingerprintData: updated ${childDevice.displayName} ${attributeName} = ${value}"
    } catch (Exception e) {
        logDebug "updateChildFingerprintData: failed to update ${dni} ${attributeName}: ${e.message}"
    }
}

/**
 * Mark a cluster attribute as received for the discovery state machine wait logic
 * @param endpoint Endpoint hex string (e.g., "01")
 * @param cluster Cluster hex string (e.g., "0101")
 * @param attrId Attribute ID hex string (e.g., "FFFB")
 */
void markClusterDataReceived(String endpoint, String cluster, String attrId) {
    // Only process global cluster attributes we're tracking
    if (!(attrId in ['FFFB', 'FFF8', 'FFF9', 'FFFC'])) {
        return
    }
    
    Map<String, Boolean> expected = state['stateMachines']?.clusterDataExpected
    if (expected == null || expected.isEmpty()) {
        // Not in cluster data wait state
        return
    }
    
    // Normalize to uppercase
    String endpointHex = endpoint?.toUpperCase()?.padLeft(2, '0')
    String clusterHex = cluster?.toUpperCase()?.padLeft(4, '0')
    String attrHex = attrId?.toUpperCase()
    
    String key = "${endpointHex}_${clusterHex}_${attrHex}"
    
    if (expected.containsKey(key)) {
        expected[key] = true
        state['stateMachines']['clusterDataExpected'] = expected
        
        Integer receivedCount = expected.values().count { it == true }
        Integer totalCount = expected.size()
        //logDebug "markClusterDataReceived: ${key} received (${receivedCount}/${totalCount})"
    }
}

/**
 * Update a specific data value in a child device
 * @param fingerprintName The fingerprint key (e.g., 'fingerprint01')
 * @param dataName The data value name (e.g., 'ProductName', 'VendorName')
 * @param value The value to set
 */
void updateChildDeviceDataValue(String fingerprintName, String dataName, Object value) {
    // Get the endpoint from fingerprintName (e.g., 'fingerprint08' -> '08')
    String endpointHex = fingerprintName.replaceFirst('fingerprint', '')
    String dni = "${device.id}-${endpointHex.toUpperCase()}"
    
    ChildDeviceWrapper childDevice = getChildDevice(dni)
    if (childDevice == null) {
        // Child device doesn't exist yet
        return
    }
    
    try {
        childDevice.updateDataValue(dataName, value?.toString())
        logDebug "updateChildDeviceDataValue: updated ${childDevice.displayName} ${dataName} = ${value}"
    } catch (Exception e) {
        logDebug "updateChildDeviceDataValue: failed to update ${dni} ${dataName}: ${e.message}"
    }
}

/* ================================================================================================================================================================================ */

void clearInfoEvent()      { sendInfoEvent('clear') }

String getStateDriverVersion() { return state.driverVersion }
void setStateDriverVersion(String version) { state.driverVersion = version }

@CompileStatic
void checkDriverVersion() {
    if (getStateDriverVersion() == null || driverVersionAndTimeStamp() != getStateDriverVersion()) {
        logDebug "updating the settings from the current driver version ${getStateDriverVersion()} to the new version ${driverVersionAndTimeStamp()}"
        sendInfoEvent("Updated to version ${driverVersionAndTimeStamp()}")
        setStateDriverVersion(driverVersionAndTimeStamp())
        final boolean fullInit = false
        initializeVars(fullInit)
        // forceNewParseFlag() // uncomment to force re-parsing all child devices on driver update
    }
}

// credits @thebearmay
String getModel() {
    try {
        /* groovylint-disable-next-line UnnecessaryGetter, UnusedVariable */
        String model = getHubVersion() // requires >=2.2.8.141
    } catch (ignore) {
        try {
            httpGet("http://${location.hub.localIP}:8080/api/hubitat.xml") { res ->
                model = res.data.device.modelName
                return model
            }
        } catch (ignore_again) {
            return ''
        }
    }
}

void sendInfoEvent(info = null, descriptionText = null) {
    if (info == null || info == 'clear') {
        logDebug 'clearing the Status event'
        sendEvent(name: 'Status', value: 'clear', descriptionText: 'last info messages auto cleared', type: 'digital')
    }
    else {
        logInfo "${info}"
        sendEvent(name: 'Status', value: info, descriptionText:descriptionText ?: '',  type: 'digital')
        runIn(INFO_AUTO_CLEAR_PERIOD, 'clearInfoEvent')            // automatically clear the Info attribute after 1 minute
    }
}

void delayedInfoEvent(Map data) {
    sendInfoEvent(data.info, data.descriptionText)
}

private void scheduleDeviceHealthCheck(final int intervalMins, final int healthMethod) {
    if (healthMethod == 1 || healthMethod == 2)  {
        String cron = getCron(intervalMins * 60)
        schedule(cron, 'deviceHealthCheck')
        logDebug "deviceHealthCheck is scheduled every ${intervalMins} minutes"
    }
    else {
        logWarn 'deviceHealthCheck is not scheduled!'
        unschedule('deviceHealthCheck')
    }
}

private void unScheduleDeviceHealthCheck() {
    unschedule('deviceHealthCheck')
    device.deleteCurrentState('healthStatus')
    logWarn 'device health check is disabled!'
}

// called when any event was received from the device in the parse() method.
void setHealthStatusOnline() {
    if (state.health == null) { state.health = [:] }
    state.health['checkCtr3']  = 0
    if (((device.currentValue('healthStatus') ?: 'unknown') != 'online')) {
        sendHealthStatusEvent('online')
        logInfo 'is now online!'
    }
}

void checkHealthStatusForOffline() {
    if (state.health == null) { state.health = [:] }
    Integer ctr = state.health['checkCtr3'] ?: 0
    String healthStatus = device.currentValue('healthStatus') ?: 'unknown'
    logDebug "checkHealthStatusForOffline: healthStstus = ${healthStatus} checkCtr3=${ctr}"
    if (ctr  >= PRESENCE_COUNT_THRESHOLD) {
        state.health['offlineCtr'] = (state.health['offlineCtr'] ?: 0) + 1      // increase the offline counter even if the device is already not present - changed 02/1/2024
        if (healthStatus != 'offline') {
            logWarn 'not present!'
            sendHealthStatusEvent('offline')
        }
    }
    else {
        logDebug "checkHealthStatusForOffline: ${healthStatus} (checkCtr3=${ctr}) offlineCtr=${state.health['offlineCtr']}"
    }
    state.health['checkCtr3'] = ctr + 1
}

// a periodic cron job, increasing the checkCtr3 each time called.
// checkCtr3 is cleared when some event is received from the device.
void deviceHealthCheck() {
    checkDriverVersion()
    checkHealthStatusForOffline()
    if (((settings.healthCheckMethod as Integer) ?: 0) == 2) { //    [0: 'Disabled', 1: 'Activity check', 2: 'Periodic polling']
        ping()          // TODO - ping() results in initialize() call if the device is switched off !
    }
}

void sendHealthStatusEvent(String value) {
    String descriptionText = "${device.displayName} healthStatus changed to ${value}"
    sendEvent(name: 'healthStatus', value: value, descriptionText: descriptionText, isStateChange: true,  type: 'digital')
    if (value == 'online') { logInfo "${descriptionText}" }
    else if (value == 'offline') { if (settings?.txtEnable) { log.warn "${device.displayName}} <b>${descriptionText}</b>" } }
    else { logDebug "${descriptionText}" }
}

String getCron(int timeInSeconds) {
    final Random rnd = new Random()
    int minutes = (timeInSeconds / 60) as int
    int hours = (minutes / 60) as int
    hours = Math.min(hours, 23)
    String cron
    if (timeInSeconds < 60) {
        cron = "*/$timeInSeconds * * * * ? *"
    } else if (minutes < 60) {
        cron = "${rnd.nextInt(59)} */$minutes * ? * *"
    } else {
        cron = "${rnd.nextInt(59)} ${rnd.nextInt(59)} */$hours ? * *"
    }
    return cron
}

void ping() {
    if (state.lastTx != null) { state.lastTx['pingTime'] = new Date().getTime() } else { state.lastTx = [:] }
    if (state.states != null) { state.states['isPing'] = true } else { state.states = [:] }
    scheduleCommandTimeoutCheck()
    sendToDevice(pingCmd())
    logDebug 'ping...'
}

String pingCmd() {
    return matter.readAttributes([
        matter.attributePath(0, 0x0028, 0x00) // Basic Information Cluster : DataModelRevision
    ])
}

void scheduleCommandTimeoutCheck(int delay = COMMAND_TIMEOUT) {
    runIn(delay, 'deviceCommandTimeout')
}

// scheduled job to check if the device responded to the last command
// increases the checkCtr3 each time called.
void deviceCommandTimeout() {
    checkDriverVersion()
    if (state.health == null) { state.health = [:] }
    logWarn "no response received (sleepy device or offline?) checkCtr3 = ${state.health['checkCtr3']} offlineCtr = ${state.health['offlineCtr']} "
    if (state.states['isPing'] == true) {
        sendRttEvent('timeout')
        state.states['isPing'] = false
        if (state.stats != null) { state.stats['pingsFail'] = (state.stats['pingsFail'] ?: 0) + 1 } else { state.stats = [:] }
    } else {
        sendInfoEvent('timeout!', "no response received on the last matter command! (checkCtr3 = ${state.health['checkCtr3']} offlineCtr = ${state.health['offlineCtr']})")
    }
    // added 02/11/2024 - checkHealthStatusForOffline() will increase the check3Ctr and will send the healthStatus event if the device is offline
    checkHealthStatusForOffline()
}

void sendRttEvent(String value=null) {
    Long now = new Date().getTime()
    if (state.lastTx == null) { state.lastTx = [:] }
    Integer timeRunning = now.toInteger() - (state.lastTx['pingTime'] ?: now).toInteger()
    // Calculate rxCtr delta
    Integer rxCtrAtPing = state.lastTx['rxCtrAtPing'] ?: 0
    Integer rxCtrNow = (state.stats != null && state.stats['rxCtr'] != null) ? state.stats['rxCtr'] : 0
    Integer rxCtrDelta = rxCtrNow - rxCtrAtPing
    String descriptionText = "${device.displayName} Round-trip time is ${timeRunning} ms (rxCtr delta=${rxCtrDelta}), min=${state.stats['pingsMin']} max=${state.stats['pingsMax']} average=${state.stats['pingsAvg']} (HE uptime: ${formatUptime()})"
    if (value == null) {
        logInfo "${descriptionText}"
        sendEvent(name: 'rtt', value: timeRunning, descriptionText: descriptionText, unit: 'ms', type: 'physical')
    }
    else {
        descriptionText = "${device.displayName} Round-trip time : ${value} (rxCtr delta=${rxCtrDelta}, healthStatus=<b>${device.currentValue('healthStatus')}</b> offlineCtr=${state.health['offlineCtr']} checkCtr3=${state.health['checkCtr3']})"
        logInfo "${descriptionText}"
        sendEvent(name: 'rtt', value: value, descriptionText: descriptionText, type: 'digital')
    }
    // Update rxCtrAtPing for the next cycle
    state.lastTx['rxCtrAtPing'] = rxCtrNow
}

// credits @thebearmay
String formatUptime() {
    try {
        Long ut = location.hub.uptime.toLong()
        Integer days = Math.floor(ut/(3600*24)).toInteger()
        Integer hrs = Math.floor((ut - (days * (3600*24))) /3600).toInteger()
        Integer min = Math.floor((ut -  ((days * (3600*24)) + (hrs * 3600))) /60).toInteger()
        Integer sec = Math.floor(ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))).toInteger()
        String attrval = "${days.toString()}d, ${hrs.toString()}h, ${min.toString()}m, ${sec.toString()}s"
        return attrval
    } catch(ignore) {
        return UNKNOWN
    }
}

String driverVersionAndTimeStamp() { version() + ' ' + timeStamp() + ((_DEBUG) ? ' (debug version!) ' : ' ') + "(${device.getDataValue('model') } ${device.getDataValue('manufacturer') }) (${getModel()} ${location.hub.firmwareVersionString}) " }

String getDeviceInfo() {
    return "model=${device.getDataValue('model')} manufacturer=${device.getDataValue('manufacturer')} <b>deviceProfile=${state.deviceProfile ?: UNKNOWN}</b>"
}

void resetStats() {
    logDebug 'resetStats...'
    state.stats  = [:]
    state.states = [:]
    state.lastRx = [:]
    state.lastTx = [:]
    state.lastTx['pingTime'] = now() ; state.lastTx['cmdTime'] = now()
    state.lastTx['subscribeTime'] = now() ; state.lastTx['unsubscribeTime'] = now()
    state.health = [:]
    state.bridgeDescriptor  = [:]   // driver specific
    state.subscriptions = []        // driver specific, format EP_CLUSTER_ATTR
    state.stateMachines = [:]       // driver specific
    state.preferences = [:]         // driver specific
    state.stats['rxCtr'] = 0 ; state.stats['txCtr'] = 0
    state.stats['initializeCtr'] = 0 ; state.stats['duplicatedCtr'] = 0
    state.states['isDigital'] = false ; state.states['isRefresh'] = false ; state.states['isPing'] = false ; state.states['isInfo']  = false
    state.states['isSubscribing'] = 0
    state.states['isUnsubscribing']  = false
    state.health['offlineCtr'] = 0 ; state.health['checkCtr3']  = 0
}

void initializeVars(boolean fullInit = false) {
    logDebug "InitializeVars()... fullInit = ${fullInit}"
    if (fullInit == true || state.deviceType == null) {
        logDebug 'forcing fullInit = true'
        state.clear()
        unschedule()
        resetStats()
        resetStats2()
        state.comment = 'Experimental Matter Bridge Driver'
        logInfo 'all states and scheduled jobs cleared!'
        state.driverVersion = driverVersionAndTimeStamp()
        // TEMP until we discover: unknown
        state.deviceType = 'UNKNOWN'
        logInfo "DEVICE_TYPE (initial) = ${state.deviceType}"
        sendInfoEvent('Initialized (fullInit = true)', 'full initialization - loaded all defaults!')
        sendEvent([ name: 'endpointsCount', value: 0, type: 'digital'])
        sendEvent([ name: 'deviceCount', value: 0, type: 'digital'])
        sendEvent([ name: 'initializeCtr', value: 0, type: 'digital'])
    }

    if (state.stats == null)  { state.stats  = [:] }
    if (state.states == null) { state.states = [:] }
    if (state.lastRx == null) { state.lastRx = [:] }
    if (state.lastTx == null) { state.lastTx = [:] }
    if (state.health == null) { state.health = [:] }

    if (fullInit || settings?.txtEnable == null) { device.updateSetting('txtEnable', true) }
    if (fullInit || settings?.logEnable == null) { device.updateSetting('logEnable', DEFAULT_LOG_ENABLE) }
    if (fullInit || settings?.traceEnable == null) { device.updateSetting('traceEnable', false) }
    if (settings?.advancedOptions == null) { device.updateSetting('advancedOptions', [value:false, type:'bool']) }
    if (settings?.discoveryTimeoutScale == null) { device.updateSetting('discoveryTimeoutScale', [value: '1', type: 'enum']) }
    if (fullInit || settings?.healthCheckMethod == null) { device.updateSetting('healthCheckMethod', [value: HealthcheckMethodOpts.defaultValue.toString(), type: 'enum']) }
    if (fullInit || settings?.healthCheckInterval == null) { device.updateSetting('healthCheckInterval', [value: HealthcheckIntervalOpts.defaultValue.toString(), type: 'enum']) }
    if (settings?.minimizeStateVariables == null) { device.updateSetting('minimizeStateVariables', [value: MINIMIZE_STATE_VARIABLES_DEFAULT, type: 'bool']) }
    if (fullInit || settings?.newParse == null) { device.updateSetting('newParse', [value: true, type: 'bool']) }
    if (settings?.cleanSubscribeMinInterval == null) { device.updateSetting('cleanSubscribeMinInterval', [value: CLEAN_SUBSCRIBE_MIN_INTERVAL_DEFAULT, type: 'number']) }
    if (settings?.cleanSubscribeMaxInterval == null) { device.updateSetting('cleanSubscribeMaxInterval', [value: CLEAN_SUBSCRIBE_MAX_INTERVAL_DEFAULT, type: 'number']) }
    ensureNewParseFlag()
    if (device.currentValue('healthStatus') == null) { sendHealthStatusEvent('unknown') }
  
}


Integer getDiscoveryTimeoutScale() {
    Integer scale = safeToInt(settings?.discoveryTimeoutScale, 1)
    if (scale < 1) { scale = 1 }
    if (scale > 3) { scale = 3 }
    return scale
}

@Field static final int ROLLING_AVERAGE_N = 10

double approxRollingAverage(double avg, double newSample) {
    if (avg == null || avg == 0) { return newSample }
    return (avg * (ROLLING_AVERAGE_N - 1) + newSample) / ROLLING_AVERAGE_N
}

@Field static final String DRIVER = 'Matter Advanced Bridge'
@Field static final String WIKI   = 'Wiki page:'

// credits @jtp10181
String fmtHelpInfo(String str) {
	String info = "${DRIVER} v${version()}"
	String prefLink = "<a href='${GITHUB_LINK}' target='_blank'>${WIKI}<br><div style='font-size: 70%;'>${info}</div></a>"
    String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid green; border-radius: 6px; color: green;'"
    String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"

	return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
		"<div style='text-align: center; position: absolute; top: 46px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
}


void parseTest(par) {
    log.warn "parseTest(${par})"
    parse(par)
}

void updateStateStats(Map descMap) {
    if (state.stats  != null) { state.stats['rxCtr'] = (state.stats['rxCtr'] ?: 0) + 1 } else { state.stats =  [:] }
    if (state.lastRx != null) { state.lastRx['checkInTime'] = new Date().getTime() }     else { state.lastRx = [:] }
}

//@Field static final Map<String, Map> stateCache = new ConcurrentHashMap<>()

//@Field volatile static Map<String,Long> TimeStamps = [:]

/* groovylint-disable-next-line UnusedMethodParameter */
void test(par) {
    par = "16152400432401011818"
    def x = decodeTLVToHex(par)
    log.warn "decodeTLVToHex(${par} -> ${x})"
}

// - to be moved to matterLib.groovy when tested ! - 

@Field static final Map<Integer, String> MATTER_DEVICE_TYPE_NAMES_SUPPORTED = [
    // Utility / node
    0x0016: 'Root Node',
    0x0011: 'Power Source',
    0x000E: 'Aggregator',
    0x0013: 'Bridged Node',

    // Lighting
    0x0100: 'On/Off Light',
    0x0101: 'Dimmable Light',
    0x010C: 'Color Temperature Light',
    0x010D: 'Extended Color Light',

    // Plugs / outlets
    0x010A: 'On/Off Plug-in Unit',
    0x010B: 'Dimmable Plug-In Unit',

    // Switches / controls
    0x0103: 'On/Off Light Switch',
    0x0104: 'Dimmer Switch',
    0x0105: 'Color Dimmer Switch',
    0x000F: 'Generic Switch',

    // Closures
    0x000A: 'Door Lock',
    0x0202: 'Window Covering',

    // HVAC
    0x0301: 'Thermostat',

    // Sensors
    0x0015: 'Contact Sensor',
    0x0106: 'Light Sensor',
    0x0107: 'Occupancy Sensor',
    0x0302: 'Temperature Sensor',
    0x0307: 'Humidity Sensor',
    0x002C: 'Air Quality Sensor',
]

// Normalizes a single element like "0016", "0x0016", "16" to int 0x0016
private Integer normalizeDeviceTypeId(Object raw) {
    if (raw == null) return null
    String s = raw.toString().trim()
    if (s.startsWith('0x') || s.startsWith('0X')) s = s.substring(2)
    if (s == '') return null
    // If user passed decimal, this will throw; assume hex by default (your data is hex)
    try {
        return Integer.parseInt(s, 16)
    } catch (Exception ignored) {
        try { return Integer.parseInt(raw.toString().trim(), 10) } catch (Exception ignored2) { return null }
    }
}

/**
 * Converts normalized DeviceTypeList (type IDs only) to human readable names.
 * Returns both a list and a compact summary string.
 */
Map deviceTypeNames(List deviceTypeList) {
    List<String> names = []
    List<String> unknown = []

    (deviceTypeList ?: []).each { dt ->
        Integer id = normalizeDeviceTypeId(dt)
        if (id == null) return
        String name = MATTER_DEVICE_TYPE_NAMES_SUPPORTED[id]
        if (name != null) {
            names << name
        } else {
            unknown << String.format('0x%04X', id)
        }
    }

    // Optional: a “best label” heuristic for logging
    // Prefer application types over utility types if present
    List<String> utility = ['Root Node','Power Source','OTA Requestor','Bridged Node']
    String best =
        (names.find { !(it in utility) } ?:  // first non-utility
         (names.contains('Aggregator') ? 'Aggregator' :
          (names[0] ?: 'Unknown')))

    String summary = names.join(', ')
    if (unknown) summary = summary ? "${summary}, Unknown(${unknown.join(', ')})" : "Unknown(${unknown.join(', ')})"

    return [best: best, names: names, unknown: unknown, summary: summary]
}

private boolean isAggregatorDevice(Map bd) {
    if (!bd) return false

    List<String> dt = (bd.DeviceTypeList ?: [])*.toUpperCase()
    List<String> parts = (bd.PartsList ?: [])*.toUpperCase()
    List<String> server = (bd.ServerList ?: [])*.toUpperCase()

    boolean hasAggregatorType = dt.contains('000E') || dt.contains('0E')
    boolean hasParts = parts && parts.size() > 0

    // Strong signal: Aggregator type + parts
    if (hasAggregatorType && hasParts) return true

    // Fallback: parts exist and descriptor/basic patterns typical for bridges
    // Many bridges expose Bridged Device Basic Information (0x0039) on child endpoints.
    // If endpoint0 already claims 0x0039, that's suspicious (some do), but still:
    boolean hasBridgedBasicSomewhere = false
    parts?.each { epHex ->
        String fp = getFingerprintName(HexUtils.hexStringToInt(epHex))
        List<String> sl = (state[fp]?.ServerList ?: [])*.toUpperCase()
        if (sl.contains('0039')) hasBridgedBasicSomewhere = true
    }
    logDebug "isAggregatorDevice(): hasAggregatorType=${hasAggregatorType} hasParts=${hasParts} hasBridgedBasicSomewhere=${hasBridgedBasicSomewhere}"
    return hasParts && hasBridgedBasicSomewhere
}


private boolean isMatterBridgeByAnyEndpoint() {
    // scan all fingerprints (endpoint descriptor snapshots)
    List<Map> fps = state.findAll { k, v -> (k as String).startsWith('fingerprint') && (v instanceof Map) }
                         .collect { it.value as Map }

    for (Map fp : fps) {
        List<String> dt    = ((fp.DeviceTypeList ?: []) as List)*.toString()*.toUpperCase()
        List<String> parts = ((fp.PartsList ?: []) as List)*.toString()*.toUpperCase()

        if (dt.contains('000E') && parts && parts.size() > 0) {
            return true
        }
    }
    return false
}

private void finalizeDeviceType() {
    boolean isBridge = isMatterBridgeByAnyEndpoint()
    state.deviceType = isBridge ? 'MATTER_BRIDGE' : 'MATTER_DEVICE'
    logInfo "DEVICE_TYPE (detected) = ${state.deviceType}"
}


// -------- libraries here --------
/* groovylint-disable-next-line NglParseError */

// lgk 03/26 add delayed illum


def illumEvent( illum, descMap) {
    logDebug "In lgk illum event"
    def map = [:] 
    //def newMap = [:]
    Map statsMap = stringToJsonMap(state.stats2); try {statsMap['illumCtr']++ } catch (e) {statsMap['illumCtr']=1}; state.stats2 = mapToJsonString(statsMap)
    int lux = illum
    if (lux <= 0) {
        log.warn "ignored invalid illum/lux ${lux}"
        return
    }
    map.value = lux
    map.name = "illumination"
    map.unit = "lx"
    map.type = "digital"
    map.isStateChange = true
    map.descriptionText = "${map.name} is ${lux} ${map.unit}"
    Map lastRxMap = stringToJsonMap(state.lastRx2)
    def timeElapsed = Math.round((now() - lastRxMap['illumTime'])/1000)
    Integer timeRamaining = (minReportingTimeIllum - timeElapsed) as Integer
    if (timeElapsed >= minReportingTimeIllum) {
       // if (settings?.txtEnable) {log.info "${device.displayName} ${map.descriptionText}"}
        unschedule("sendDelayedEventIllum")
        lastRxMap['illumTime'] = now()
        logDebug "Not delaying sending $map"
        
       sendHubitatEvent([ 
            name: 'illuminance',
            value: illum,
            unit: 'lx',
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)}  illuminance is ${illum} lux"
            ], descMap, true)       
    }
    else { // queue the event 
    	map.type = "delayed"
        logDebug "${device.displayName} DELAYING ${timeRamaining} seconds event : ${map}"   
        
        map.descMap = descMap
        // [callbackType:Report, endpointInt:9, clusterInt:1024, attrInt:0, data:[0:UINT:13586], value:13586, cluster:0400, endpoint:09, attrId:0000]
        runIn(timeRamaining, 'sendDelayedEventIllum',  [overwrite: true, data: map ])
    }
    state.lastRx2 = mapToJsonString(lastRxMap)
}

private void sendDelayedEventIllum(Map map) {
    def descMap = [:]
 
    Map lastRxMap = stringToJsonMap(state.lastRx2); try {lastRxMap['illumTime'] = now()} catch (e) {lastRxMap['illumTime']=now()-(minReportingTimeIllum * 2000)}; state.lastRx2 = mapToJsonString(lastRxMap)
    logInfo "In Send/Processing delayed map = $map"
   
    int illum = map.value
    descMap = map.descMap 
    
    sendHubitatEvent([
            name: 'illuminance',
            value: illum,
            unit: 'lx',
            descriptionText: "${getDeviceDisplayName(descMap?.endpoint)}  illuminance is ${illum} lux"
            ], descMap, true)
}

def resetStats2() {
    Map stats2 = [
        date : new Date().format('yyyy-MM-dd', location.timeZone),
        rxCtr : 0,
        txCtr : 0,
        rejoins: 0
    ]
    
    Map lastRx2 = [
        illumTime : now() - defaultMinReportingTime * 1000,
        illumCfg : '-1,-1,-1'
    ]
    
 
    state.stats2  =  mapToJsonString( stats2 )
    state.lastRx2 =  mapToJsonString( lastRx2 )
    log.info "${device.displayName} Statistics were reset."
}

String mapToJsonString( Map map) {
    if (map==null || map==[:]) return ""
    String str = JsonOutput.toJson(map)
    return str
}

Map stringToJsonMap( String str) {
    if (str==null) return [:]
    def jsonSlurper = new JsonSlurper()
    def map = jsonSlurper.parseText( str )
    return map
}


#include kkossev.matterCommonLib
#include kkossev.matterLib
#include kkossev.matterUtilitiesLib
#include kkossev.matterStateMachinesLib
