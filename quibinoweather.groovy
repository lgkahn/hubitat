/**
 *  Qubino Weatherstation
 *	Device Handler 
 *	Version 1.08
 *  Date: 27.6.2017
 *	Author: Kristjan Jam&scaron;ek (Kjamsek), Goap d.o.o.
 *    laurence kahn, kahn@lgk.com
 *  Copyright 2017 Kristjan Jam&scaron;ek
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
 * |---------------------------- DEVICE HANDLER FOR QUBINO WEATHERSTATION Z-WAVE DEVICE --------------------------------------------------------|  
 *	The handler supports all unsecure functions of the Qubino Weatherstation device. Configuration parameters and
 *	association groups can be set in the device's preferences screen, but they are applied on the device only after
 *	pressing the 'Set configuration' and 'Set associations' buttons on the bottom of the details view. 
 *
 *	This device handler supports data values that are currently not implemented as capabilities, so custom attribute 
 *	states are used. Please use a SmartApp that supports custom attribute monitoring with this device in your rules.
 *	By default the 'Temperature Measurement' and 'Relative Humidity Measurement' capability states are mirrored from
 *	the custom attribute state values 'temperatureCh1' and 'humidityCh1'.
 *
 *	This device handler uses hubAction in configure() method in order to set a MultiChannel Lifeline association to the
 *	device, so reports from all endpoints can be received. 
 * |--------------------------------------------------------------------------------------------------------------------------------------------|
 *
 *
 *	TO-DO:
 *	- Implement Multichannel Association Command Class fully to add MC Association functionality.
 *  - Implement secure mode
 *
 *	CHANGELOG:
 *	0.99: Final release code cleanup and commenting
 *	1.00: Added comments to code for readability
 *  1.01: Reduced the tile labels to eliminate truncating texts on smaller device screens.
 *		  Added configuration parameter description to selection menus.
 *	1.02: Corrected the form of association removal from a specific group.
 *	1.03: Added MultiChannel Association Lifeline setting using the defined classes. The handler assumes this user would then prefer imperial units.
 *  1.04 changes by l.g. kahn, added colors to many tiles, added last update times for all channels ie wind, rain , ch1 an ch2, as depending on placement
 								it is common to not have updates. Later may add alerts for missed updates.
                               Also, added offsets for adjust for incorrect temps and humidities as seeing quite a bit of variation from known good sensors.
                               Cannot do adjustments to the outside/wind temp as this would screw up the wind chill calculations which are not done in this 
                               device type, but done directly in the system and sent as a message.
                               Also rounding off the ch1 and ch2 temps to 1 digit passed the decimal as we don't really have 100th of a degree accuracy anyway.
 							   Also make primary tile multi attirbute showing outside/ch1 temp and below showing windchill, as I primarily use this for outside temp.

*   1.05 lgk add max and min temps (outdoor), add max rain/hr and wind gust. Also add reset max buttons.
*    1.06 lgk ignore artficial readings (no contact) of wind 192.12 and temp -326.2 for min max. 
* 1.07 Add date/time for min max readings.
* 1.08 Fixed problem with rain max date/time typo.
*      removed max date/time tiles and got it working showing under the max amount using concatenated string tile.
*      Also added some more error checking for extraneous values that will be ignored like rain >= 12 inches per hour, temps under -80 etc etc.
*/
 
metadata {
	definition (name: "Qubino Weatherstation", namespace: "LGK-Apps", author: "Kristjan Jam&scaron;ek, kahn@lgk.com") {
		capability "Configuration" //Needed for configure() function to set MultiChannel Lifeline Association Set
		capability "Temperature Measurement" //Used on main tile, needed in order to have the device appear in capabilities lists, mirrors temperatureCh1 attribute states
		capability "Relative Humidity Measurement" //Needed in order to have the device appear in capabilities lists, mirrors humidityCh1 attribute states
		capability "Sensor" // - Tagging capability
        capability "Zw Multichannel"
		
		attribute "temperatureCh1", "number" // temperature attribute for Ch1 Temperature reported by device's endpoint 1
		attribute "windDirection", "number" // wind direction attribute for Wind Gauge - Direction reported by device's endpoint 2
		
		attribute "windVelocity", "number" // wind velocity attribute for Wind Gauge - Velocity reported by device's endpoint 3
		attribute "windGust", "number" // wind gust velocity attribute for Wind Gauge - Wind gust reported by device's endpoint 4
		attribute "windTemperature", "number" // wind temperature attribute for Wind Gauge - Temperature reported by device's endpoint 5
		attribute "windChillTemperature", "number" // wind chill temperature attribute for Wind Gauge - Wind Chill reported by device's endpoint 6
		attribute "rainRate", "number" // rain rate attribute for Rain Sensor data reported by device's endpoint 7
		attribute "humidityCh1", "number" // humidity attribute for Ch1 Humidity reported by device's endpoint 8
		attribute "temperatureCh2", "number" // temperature attribute for Ch2 Temperature reported by device's endpoint 9
		attribute "humidityCh2", "number" // humidity attribute for Ch2 Humidity reported by device's endpoint 10
		
		attribute "setConfigParams", "string" // attribute for tile element for setConfigurationParams command
		attribute "setAssocGroups", "string" // attribute for tile element for setAssociations command
		
        /* lgk last update times. */
        attribute "lastUpdateWind", "string"
		attribute "lastUpdateCh1", "string"
		attribute "lastUpdateCh2", "string"
		attribute "lastUpdateRain", "string"
        
        attribute "windMaxDate", "string"
		attribute "tempMaxDate", "string"
		attribute "tempMinDate", "string"
		attribute "rainMaxDate", "string"
        
        attribute "maxOutdoorTemp", "number"
		attribute "minOutdoorTemp", "number"
        attribute "maxRain", "number"
        attribute "maxWindGust", "number"
        
        attribute "windMaxDateStr", "string"
		attribute "tempMaxDateStr", "string"
		attribute "tempMinDateStr", "string"
		attribute "rainMaxDateStr", "string"

		command "setConfigurationParams" // command to issue Configuration Set command sequence according to user's preferences
		command "setAssociations" // command to issue Association Set command sequence according to user's preferences
		command "resetMaxMin" // lgk reset them

        fingerprint mfr:"0159", prod:"0007", model:"0053"  //Manufacturer Information values for Qubino Weatherstation
	}


	simulator {
		// TESTED WITH PHYSICAL DEVICE - UNNEEDED
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"temperature", type:"generic", width:6, height:4, canChangeIcon: false) {
  			tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
    			attributeState("default", label:'${currentValue} °F', unit: "°F", icon: "st.Weather.weather2",
                     backgroundColors:[
                	[value: 1,  color: "#c8e3f9"],
                	[value: 10, color: "#dbdee2"],
                	[value: 20, color: "#c0d2e4"],
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
                    [value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
			])
            }
             tileAttribute("device.windChillTemperature", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Wind Chill ${currentValue} °F', unit:"°F")
                }
  		
        }   
    
    /*
    tiles(scale: 2) {
		valueTile("temperature", "device.temperature") {
			state("temperature", label:'${currentValue} °F', unit:"°F", icon: "st.Weather.weather2", backgroundColors: [
			 	// Celsius Color Range
				[value: 0, color: "#153591"],
				[value: 7, color: "#1e9cbb"],
				[value: 15, color: "#90d2a7"],
				[value: 23, color: "#44b621"],
				[value: 29, color: "#f1d801"],
				[value: 33, color: "#d04e00"],
				[value: 36, color: "#bc2323"],
				// Fahrenheit Color Range
				[value: 40, color: "#153591"],
				[value: 44, color: "#1e9cbb"],
				[value: 59, color: "#90d2a7"],
				[value: 74, color: "#44b621"],
				[value: 84, color: "#f1d801"],
				[value: 92, color: "#d04e00"],
				[value: 96, color: "#bc2323"]
			])
		}
        */
     
		valueTile("humidity", "device.humidity") {
			state("humidity", label:'${currentValue} %', unit:"%", display:false)
		}	
        
		standardTile("temperatureCh1", "device.temperatureCh1", width: 3, height: 2) {
			state("temperatureCh1", label:'Temp. Ch1:\n${currentValue} °F', unit:'°F', icon: 'st.Weather.weather2',
            	backgroundColors:[
                	[value: 1,  color: "#c8e3f9"],
                	[value: 10, color: "#dbdee2"],
                	[value: 20, color: "#c0d2e4"],
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
                    [value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
	
				])
		}
		standardTile("windDirection", "device.windDirection", width: 3, height: 2) {
			state("windDirection", label:'Wind Dir.:\n${currentValue}', unit: "", icon: 'st.Outdoor.outdoor20')
		}

		standardTile("windVelocity", "device.windVelocity", width: 3, height: 2) {
			state("windVelocity", label:'Wind Vel.:\n${currentValue} mph', unit:"mph", icon: 'st.Weather.weather1',
              backgroundColors: [
            	[value: 0, color: "#90d2a7"],
				[value: 5, color: "#44b621"],
				[value: 10, color: "#f1d801"],
				[value: 20, color: "#d04e00"],
				[value: 30, color: "#bc2323"]
                ]
                )
		}
		
		standardTile("windGust", "device.windGust", width: 3, height: 2) {
			state("windGust", label:'Wind Gust:\n${currentValue} mph', unit:"mph", icon: 'st.Weather.weather1',
            backgroundColors: [
            	[value: 0, color: "#90d2a7"],
				[value: 5, color: "#44b621"],
				[value: 10, color: "#f1d801"],
				[value: 20, color: "#d04e00"],
				[value: 30, color: "#bc2323"]
                ]
                )
		}
		standardTile("windTemperature", "device.windTemperature", width: 3, height: 2) {
			state("windTemperature", label:'Outdoor Temp.\n${currentValue} °F', unit:'°F', icon: 'st.Weather.weather2',
         backgroundColors:[
                	[value: 1,  color: "#c8e3f9"],
                	[value: 10, color: "#dbdee2"],
                	[value: 20, color: "#c0d2e4"],
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
                    [value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
	
				])   
		}
		standardTile("windChillTemperature", "device.windChillTemperature", width: 3, height: 2) {
			state("windChillTemperature", label:'Wind Chill:\n${currentValue} °F', unit:'°F', icon: 'st.Weather.weather2',
            backgroundColors:[
                	[value: 1,  color: "#c8e3f9"],
                	[value: 10, color: "#dbdee2"],
                	[value: 20, color: "#c0d2e4"],
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
                    [value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
	
				])
		}
		standardTile("rainRate", "device.rainRate", width: 3, height: 2) {
			state("rainRate", label:'Rain: ${currentValue} inches/h', unit:"inches/h", icon: 'st.Weather.weather10')
		}
		standardTile("humidityCh1", "device.humidityCh1", width: 3, height: 2) {
			state("humidityCh1", label:'Hum. Ch1:\n${currentValue} %', unit:"%", icon: 'st.Weather.weather12',
              backgroundColors : [
                    [value: 01, color: "#724529"],
                    [value: 11, color: "#724529"],
                    [value: 21, color: "#724529"],
                    [value: 35, color: "#44b621"],
                    [value: 49, color: "#44b621"],
                    [value: 50, color: "#1e9cbb"]
         ]        
         )	}
		standardTile("temperatureCh2", "device.temperatureCh2", width: 3, height: 2) {
			state("temperatureCh2", label:'Temp. Ch2:\n${currentValue} °F', unit:'°F', icon: 'st.Weather.weather2',
            	backgroundColors:[
                	[value: 1,  color: "#c8e3f9"],
                	[value: 10, color: "#dbdee2"],
                	[value: 20, color: "#c0d2e4"],
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
                    [value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
	
				])
		}
		standardTile("humidityCh2", "device.humidityCh2", width: 3, height: 2) {
			state("humidityCh2", label:'Hum. Ch2:\n${currentValue} %', unit:"%", icon: 'st.Weather.weather12',\
              backgroundColors : [
                    [value: 01, color: "#724529"],
                    [value: 11, color: "#724529"],
                    [value: 21, color: "#724529"],
                    [value: 35, color: "#44b621"],
                    [value: 49, color: "#44b621"],
                    [value: 50, color: "#1e9cbb"]
         ] )
		}
        
        // lgk reset max/min
        
        standardTile("resetMaxMin", "device.resetMaxMin", decoration: "flat", width: 2, height: 2) {
			state("resetMaxMin", label:'reset Max/Min', action:'resetMaxMin', icon: "st.secondary.tools")
		}
		standardTile("setConfigParams", "device.setConfigParams", decoration: "flat", width: 2, height: 2) {
			state("setConfigParams", label:'Set configuration', action:'setConfigurationParams', icon: "st.secondary.tools")
		}
		standardTile("setAssocGroups", "device.setAssocGroups", decoration: "flat", width: 2, height: 2) {
			state("setAssocGroups", label:'Set associations', action:'setAssociations', icon: "st.secondary.tools")
		}
		
        
        
            // lgk main max
         
            valueTile("maxWindGust", "device.maxWindGust", width: 3, height: 1) {
            state "default", label: 'Max Wind Gust:\n ${currentValue} mph' }
         
            valueTile("maxOutdoorTemp", "device.maxOutdoorTemp", width: 3, height: 1) {
            state "default", label: 'Max Outdoor Temp:\n ${currentValue} °F' }
      
            valueTile("minOutdoorTemp", "device.minOutdoorTemp", width: 3, height: 1) {
            state "default", label: 'Min Outdoor Temp:\n ${currentValue} °F' }
            
            valueTile("maxRain", "device.maxRain", width: 3, height: 1) {
            state "default", label: 'Max Rain:\n ${currentValue} inches/h' }
            
            
        	// lgk status tiles
        	valueTile("windMaxDate", "device.windMaxDate", width: 3, height: 1, decoration: "flat") {
			state "default", label: 'Date/Time:\n ${currentValue}'
					}
        	valueTile("tempMaxDate", "device.tempMaxDate", width: 3, height: 1, decoration: "flat") {
			state "default", label: 'Date/Time:\n ${currentValue}'
					}
        	valueTile("tempMinDate", "device.tempMinDate", width: 3, height: 1, decoration: "flat") {
			state "default", label: 'Date/Time:\n ${currentValue}'
					}
            valueTile("rainMaxDate", "device.rainMaxDate", width: 3, height: 1, decoration: "flat") {
			state "default", label: 'Date/Time:\n ${currentValue}'
					}
                    
                     	// lgk status tiles
        	valueTile("windMaxDateStr", "device.windMaxDateStr", width: 3, height: 1, decoration: "flat") {
			state "default", label: '${currentValue}'
					}
        	valueTile("tempMaxDateStr", "device.tempMaxDateStr", width: 3, height: 1, decoration: "flat") {
			state "default", label: '${currentValue}'
					}
        	valueTile("tempMinDateStr", "device.tempMinDateStr", width: 3, height: 1, decoration: "flat") {
			state "default", label: '${currentValue}'
					}
            valueTile("rainMaxDateStr", "device.rainMaxDateStr", width: 3, height: 1, decoration: "flat") {
			state "default", label: '${currentValue}'
					}
        
        
        // lgk status tiles
        	valueTile("statusWind", "device.lastUpdateWind", width: 3, height: 1, decoration: "flat") {
			state "default", label: 'Wind Last Update: ${currentValue}'
					}
        	valueTile("statusCh1", "device.lastUpdateCh1", width: 3, height: 1, decoration: "flat") {
			state "default", label: 'Ch1 Last Update: ${currentValue}'
					}
        	valueTile("statusCh2", "device.lastUpdateCh2", width: 3, height: 1, decoration: "flat") {
			state "default", label: 'Ch2 Last Update: ${currentValue}'
					}
            valueTile("statusRain", "device.lastUpdateRain", width: 3, height: 1, decoration: "flat") {
			state "default", label: 'Rain Last Update: ${currentValue}'
					}
        
		main("temperature")
		
		details(["temperatureCh1", "humidityCh1", "temperatureCh2", "humidityCh2","windDirection", "windVelocity", "windGust", "windTemperature", 
            "windChillTemperature", "rainRate", "tempMinDateStr", "tempMaxDateStr", "windMaxDateStr", "rainMaxDateStr",
            "statusCh1", "statusCh2", "statusWind", "statusRain", "resetMaxMin","setConfigParams", "setAssocGroups"])

	}
	preferences {
/**
*			--------	CONFIGURATION PARAMETER SECTION	--------
*/
				input name: "windvane", type: "bool", required: false,
					title: "Wind direction in degrees if selected, \n"+
							"otherwise wind directions shown in cardinal directions."
				input name: "param1", type: "number", range: "0..8800", required: false,
					title: "1. Wind Gauge, Wind Gust Top Value. " +
						   "If the Wind Gust is higher than the parameter value, the device triggers an association.\n" +
						   "Available settings:\n" +
						   "0 ... 8800 = value from 0,00 m/s to 88,00 m/s,\n" +
						   "Default value: 1000 (10,00 m/s)."
				input name: "param2", type: "number", range: "0..30000", required: false,
					title: "2. Rain Gauge, Rain Rate Top Value. " +
						   "If the Rain Sensor Rain Rate is higher than the parameter value, the device triggers an association.\n" +
						   "Available settings:\n" +
						   "0 ... 30000 = value from 0,00 mm/h to 300,00 mm/h,\n" +
						   "Default value: 200 (2,00 mm/h)."          
				input name: "param3", type: "enum", required: false,
					options: ["0" : "0 = If the Wind Gust is higher than the parameter number 1 value, then the device sends a Basic Set 0x00",
							  "1" : "1 = If the Wind Gust is higher than the parameter number 1 value, then the device sends a Basic Set 0xFF"],
					title: "3. Wind Gauge, Wind Gust. " +
						   "Available settings:\n" +
						   "0 = If the Wind Gust is higher than the parameter number 1 value, then the device sends a Basic Set 0x00,\n" +
						   "1 = If the Wind Gust is higher than the parameter number 1 value, then the device sends a Basic Set 0xFF,\n" +
						   "Default value: 1."
				input name: "param4", type: "enum", required: false,
					options: ["0" : "0 = If the Rain Rate is higher than the parameter number 2 value, then the device sends a Basic Set 0x00",
							  "1" : "1 = If the Rain Rate is higher than the parameter number 2 value, then the device sends a Basic Set 0xFF"],
					title: "4. Rain Gauge, Rain Rate. " +
						   "Available settings:\n" +
						   "0 = If the Rain Rate is higher than the parameter number 2 value, then the device sends a Basic Set 0x00,\n" +
						   "1 = If the Rain Rate is higher than the parameter number 2 value, then the device sends a Basic Set 0xFF,\n" +
						   "Default value: 1."	
				input name: "param5", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "5. Endpoint 1 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param6", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "6. Endpoint 2 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param7", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "7. Endpoint 3 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param8", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "8. Endpoint 4 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param9", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "9. Endpoint 5 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param10", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "10. Endpoint 6 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param11", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "11. Endpoint 7 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param12", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "12. Endpoint 8 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param13", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "13. Endpoint 9 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param14", type: "enum", required: false,
					options: ["0" : "0 = Unsolicited Report disabled",
							  "1" : "1 = Unsolicited Report enabled"],
					title: "14. Endpoint 10 - Unsolicited Report enable/disable. " +
						   "Available settings:\n" +
						   "0 = Unsolicited Report disabled,\n" +
						   "1 = Unsolicited Report enabled,\n" +
						   "Default value: 1."
				input name: "param15", type: "enum", required: false,
					options: ["0" : "0 = Random ID disabled",
							  "1" : "1 = Random ID enabled"],
					title: "15. Random ID enable/disable " +
						   "Available settings:\n" +
						   "0 = Random ID disabled,\n" +
						   "1 = Random ID enabled,\n" +
						   "Default value: 0."
                           

				// lgk offsets 
                input("TempOffsetCh1", "number", title: "Ch1 Temperature Offset/Adjustment -10 to +10 in Degrees?",range: "-10..10", description: "If your Ch1 temperature is innacurate this will offset/adjust it by this many degrees.", defaultValue: 0, required: false)
  				input("HumidOffsetCh1", "number", title: "Ch1 Humidity Offset/Adjustment -10 to +10 in percent?",range: "-10..10", description: "If your Ch1 umidty is innacurate this will offset/adjust it by this percent.", defaultValue: 0, required: false)
                input("TempOffsetCh2", "number", title: "Ch2 Temperature Offset/Adjustment -10 to +10 in Degrees?",range: "-10..10", description: "If your Ch1 temperature is innacurate this will offset/adjust it by this many degrees.", defaultValue: 0, required: false)
  				input("HumidOffsetCh2", "number", title: "Ch2 Humidity Offset/Adjustment -10 to +10 in percent?",range: "-10..10", description: "If your Ch1 umidty is innacurate this will offset/adjust it by this percent.", defaultValue: 0, required: false)
              	
				
/**
*			--------	ASSOCIATION GROUP SECTION	--------
*/
				input name: "assocGroup2", type: "text", required: false,
					title: "Association group 2: \n" +
						   "Basic On/Off command will be sent to associated nodes when the Wind Gust of the Wind Gauge exceeds the Configuration parameter 1 value. \n" +
						   "NOTE: Insert the node Id value of the devices you wish to associate this group with. Multiple nodeIds can also be set at once by separating individual values by a comma (2,3,...)."
						   
				input name: "assocGroup3", type: "text", required: false,
					title: "Association group 3: \n" +
						   "Basic On/Off command will be sent to associated nodes when the Rain Rate exceeds the Configuration parameter 2 value. \n" +
						   "NOTE: Insert the node Id value of the devices you wish to associate this group with. Multiple nodeIds can also be set at once by separating individual values by a comma (2,3,...)."
	}
}
/**
*	--------	HELPER METHODS SECTION	--------
*/
/**
 * Converts a list of String type node id values to Integer type.
 *
 * @param stringList - a list of String type node id values.
 * @return stringList - a list of Integer type node id values.
 lgk this function does not work as not id are not integer but hex..
 
*/
def convertStringListToIntegerList(stringList){
	if(stringList != null){
		for(int i=0;i<stringList.size();i++){
			stringList[i] = stringList[i].toInteger()
		}
	}
	return stringList
}
/**
 * Converts temperature values to fahrenheit or celsius scales accordign to user's setting.
 *
 * @param scaleParam user set scale parameter.
 * @param encapCmd received temperature parsed value.
 * @return String type value of the converted temperature value.
*/
def convertDegrees(scaleParam, encapCmd){
	switch (scaleParam) {
		default:
				break;
		case "F":
				if(encapCmd.scale == 1){
					return encapCmd.scaledSensorValue.toString()
				}else{
					return (encapCmd.scaledSensorValue * 9 / 5 + 32).toString()
				}
				break;
		case "C":
				if(encapCmd.scale == 0){
					return encapCmd.scaledSensorValue.toString()
				}else{
					return (encapCmd.scaledSensorValue * 9 / 5 + 32).toString()
				}
				break;
	}
}

/**
 * Converts rain values to imperial.
 *
 * @param encapCmd received rain parsed value.
 * @return String type value of the converted rain value.
*/
def convertRain(encapCmd){
	if(encapCmd.scaledSensorValue != 0){
		def convertedValue = encapCmd.scaledSensorValue * 0.04
		return String.format("%5.2f",convertedValue)
	}else{
		def convertedValue = encapCmd.scaledSensorValue
		return String.format("%5.2f",convertedValue)
	}
}
/**
 * Converts velocity values to imperial scales.
 *
 * @param encapCmd received velocity parsed value.
 * @return String type value of the converted velocity value.
*/
def convertVelocity(encapCmd){
	if(encapCmd.scaledSensorValue != 0){
		def convertedValue = encapCmd.scaledSensorValue * 2.24
		return String.format("%5.2f",convertedValue)
	}else{
		def convertedValue = encapCmd.scaledSensorValue
		return String.format("%5.2f",convertedValue)
	}
}
/**
 * Converts direction degree values to cardinal directions.
 *
 * @param encapCmd received wind direction value.
 * @return String type value of the converted velocity value.
*/
def convertDirection(encapCmd){
	def cardinals = [ "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW", "N" ]
	def sensorValInDeg = encapCmd.scaledSensorValue * 10
	def sensorValInDir = sensorValInDeg / 225
	return cardinals[sensorValInDir.toInteger()]
}
/*
*	--------	HANDLE COMMANDS SECTION	--------
*/
/**
 * Configuration capability command handler that executes after module inclusion to remove existing singlechannel association Lifeline and replace it
 * with a multichannel Lifeline association setting for node id 1 and endpoint 1, which enables modules to report multichannel encapsulated frames.
 *
 * @param void
 * @return List of commands that will be executed in sequence with 500 ms delay inbetween.
*/
def configure() {
	log.debug "Qubino Weatherstation: configure()"
	/** In this method we first clear the association group 1 that is set by SmartThings automatically.
	* Afterwards we set a MultiChannel LifelineAssociation, so the device can report it's data MC Encapsulated.
	*/
	def assocCmds = []
	assocCmds << zwave.associationV1.associationRemove(groupingIdentifier:1, nodeId:zwaveHubNodeId).format()
	assocCmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 1, nodeId: [0,zwaveHubNodeId,1]).format()
	return delayBetween(assocCmds, 500)
}
/**
 * setAssociations command handler that sets user selected association groups. In case no node id is insetred the group is instead cleared.
 * Lifeline association hidden from user influence by design.
 *
 * @param void
 * @return List of Association commands that will be executed in sequence with 500 ms delay inbetween.
*/
def setAssociations() {
	log.debug "Qubino Weatherstation: setAssociations()"
	def assocSet = []
    log.debug "assoc node =  $settings.assocGroup2"
	if(settings.assocGroup2 != null){
		def group2parsed = settings.assocGroup2.tokenize(",")
		if(group2parsed == null){
      //  log.debug "not list"
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:settings.assocGroup2).format()
		}else{
       // log.debug "list"
			group2parsed = convertStringListToIntegerList(group2parsed)
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:group2parsed).format()
		}
	}else{
		assocSet << zwave.associationV1.associationRemove(groupingIdentifier:2).format()
	}
	if(settings.assocGroup3 != null){
		def group3parsed = settings.assocGroup3.tokenize(",")
		if(group3parsed == null){
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:settings.assocGroup3).format()
		}else{
			group3parsed = convertStringListToIntegerList(group3parsed)
			assocSet << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:group3parsed).format()
		}
	}else{
		assocSet << zwave.associationV1.associationRemove(groupingIdentifier:3).format()
	}
	if(assocSet.size() > 0){
		return delayBetween(assocSet, 500)
	}	
}
/**
 * setConfigurationParams command handler that sets user selected configuration parameters on the device. 
 * In case no value is set for a specific parameter the method skips setting that parameter.
 * Secure mdoe setting hidden from user influence by design.
 *
 * @param void
 * @return List of Configuration Set commands that will be executed in sequence with 500 ms delay inbetween.
*/
def setConfigurationParams() {
	log.debug "Qubino Weatherstation: setConfigurationParams()"
	def configSequence = []
	if(settings.param1 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 1, size: 2, scaledConfigurationValue: settings.param1.toInteger()).format()
	}
	if(settings.param2 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 2, size: 2, scaledConfigurationValue: settings.param2.toInteger()).format()
	}
	if(settings.param3 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: settings.param3.toInteger()).format()
	}
	if(settings.param4 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 4, size: 1, scaledConfigurationValue: settings.param4.toInteger()).format()
	}
	if(settings.param5 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 5, size: 1, scaledConfigurationValue: settings.param5.toInteger()).format()
	}
	if(settings.param6 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 6, size: 1, scaledConfigurationValue: settings.param6.toInteger()).format()
	}
	if(settings.param7 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 7, size: 1, scaledConfigurationValue: settings.param7.toInteger()).format()
	}
	if(settings.param8 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, scaledConfigurationValue: settings.param8.toInteger()).format()
	}
	if(settings.param9 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 9, size: 1, scaledConfigurationValue: settings.param9.toInteger()).format()
	}
	if(settings.param10 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 10, size: 1, scaledConfigurationValue: settings.param10.toInteger()).format()
	}
	if(settings.param11 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 11, size: 1, scaledConfigurationValue: settings.param11.toInteger()).format()
	}
	if(settings.param12 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 12, size: 1, scaledConfigurationValue: settings.param12.toInteger()).format()
	}
	if(settings.param13 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 13, size: 1, scaledConfigurationValue: settings.param13.toInteger()).format()
	}
	if(settings.param14 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 14, size: 1, scaledConfigurationValue: settings.param14.toInteger()).format()
	}
	if(settings.param15 != null){
		configSequence << zwave.configurationV1.configurationSet(parameterNumber: 15, size: 1, scaledConfigurationValue: settings.param15.toInteger()).format()
	}	
	if(configSequence.size() > 0){
		return delayBetween(configSequence, 500)
	}
}
/*
*	--------	EVENT PARSER SECTION	--------
*/
/**
 * parse function takes care of parsing received bytes and passing them on to event methods.
 *
 * @param description String type value of the received bytes.
 * @return Parsed result of the received bytes.
*/
def parse(String description) {
    

	log.debug "Qubino Weatherstation: Parsing '${description}'"
	def result = null
    def cmd = zwave.parse(description)
    if (cmd) {
		result = zwaveEvent(cmd)
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
		log.debug "Non-parsed event: ${description}"
    }
    return result
}
/**
 * Event method for MultiChannelCmdEncap encapsulation frames.
 *
 * @param cmd Type physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap received command.
 * @return List of events that will update UI elements for data display.
*/
def zwaveEvent(hubitat.zwave.commands.multiinstancev1.MultiInstanceCmdEncap cmd) {
//def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
//def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiInstanceCmdEncap cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x30: 1, 0x31: 1])
	def tempScale = location.temperatureScale
	def resultEvents = []
    def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
   
	switch (cmd.sourceEndPoint) {
		default:
				break;
		case 1:
	            sendEvent(name: "lastUpdateCh1", value: now, descriptionText: "Ch1 Last Update: $now")
                BigDecimal offset = settings.TempOffsetCh1
                def startval = convertDegrees(tempScale,encapsulatedCommand) 
                def thetemp = startval as BigDecimal
                BigDecimal adjval = (thetemp + offset)
                def dispval =  String.format("%5.1f", adjval)
                def finalval = dispval as BigDecimal
                
                resultEvents << createEvent(name:"temperatureCh1", value: finalval, unit:" °"+location.temperatureScale, descriptionText: "Temperature Ch1: "+convertDegrees(tempScale,encapsulatedCommand)+" °F")
				resultEvents << createEvent(name:"temperature", value: finalval, unit:" °"+location.temperatureScale, descriptionText: "Temperature Ch1: "+convertDegrees(tempScale,encapsulatedCommand)+" °F", displayed: false)
			    // resultEvents << createEvent(name:"temperatureCh1", value: convertDegrees(tempScale,encapsulatedCommand) + settings.TempOffsetCh1, unit:" °"+location.temperatureScale, descriptionText: "Temperature Ch1: "+convertDegrees(tempScale,encapsulatedCommand)+" °F")
				//resultEvents << createEvent(name:"temperature", value: convertDegrees(tempScale,encapsulatedCommand) + settings.TempOffsetCh1, unit:" °"+location.temperatureScale, descriptionText: "Temperature Ch1: "+convertDegrees(tempScale,encapsulatedCommand)+" °F", displayed: false)
				break;
		case 2:
		        sendEvent(name: "lastUpdateWind", value: now, descriptionText: "Wind Last Update: $now")

				if(settings.windvane){
					resultEvents << createEvent(name:"windDirection", value: encapsulatedCommand.scaledSensorValue.toString(), unit:"°", descriptionText: "Wind Direction: "+encapsulatedCommand.scaledSensorValue.toString()+"°")
				}else{
					resultEvents << createEvent(name:"windDirection", value: convertDirection(encapsulatedCommand), unit:"", descriptionText: "Wind Direction: "+convertDirection(encapsulatedCommand))
				}			
				break;
		case 3:
        		sendEvent(name: "lastUpdateWind", value: now, descriptionText: "Wind Last Update: $now")

				def lvalue =  convertVelocity(encapsulatedCommand)
				checkMaxWindGust(lvalue)
                
				resultEvents << createEvent(name:"windVelocity", value: convertVelocity(encapsulatedCommand), unit:"mph", descriptionText: "Wind Velocity: "+convertVelocity(encapsulatedCommand)+" mph")
				break;
		case 4:
				sendEvent(name: "lastUpdateWind", value: now, descriptionText: "Wind Last Update: $now")

                def lvalue =  convertVelocity(encapsulatedCommand)
				checkMaxWindGust(lvalue)

				resultEvents << createEvent(name:"windGust", value: convertVelocity(encapsulatedCommand), unit:"mph", descriptionText: "Wind Gust: "+convertVelocity(encapsulatedCommand)+" mph")
				break;
		case 5:
		        sendEvent(name: "lastUpdateWind", value: now, descriptionText: "Wind Last Update: $now")

				def lvalue =  convertDegrees(tempScale,encapsulatedCommand)
                checkMinOutdoorTemp(lvalue)
                checkMaxOutdoorTemp(lvalue)

				resultEvents << createEvent(name:"windTemperature", value: convertDegrees(tempScale,encapsulatedCommand), unit:"°"+location.temperatureScale, descriptionText: "Wind Gauge - Out Temperature: "+convertDegrees(tempScale,encapsulatedCommand)+" °"+location.temperatureScale)
				break;
		case 6:
		        sendEvent(name: "lastUpdateWind", value: now, descriptionText: "Wind Last Update: $now")

				resultEvents << createEvent(name:"windChillTemperature", value: convertDegrees(tempScale,encapsulatedCommand), unit:"°"+location.temperatureScale, descriptionText: "Wind Chill: "+convertDegrees(tempScale,encapsulatedCommand)+" °"+location.temperatureScale)
				break;
		case 7:	
		        sendEvent(name: "lastUpdateRain", value: now, descriptionText: "Rain Last Update: $now")

				def lvalue =  convertRain(encapsulatedCommand)
                checkMaxRain(lvalue)

				resultEvents << createEvent(name:"rainRate", value: convertRain(encapsulatedCommand), unit:"inches/h", descriptionText: "Rain Sensor: "+convertRain(encapsulatedCommand)+" inches/h")
				break;
		case 8:
		        sendEvent(name: "lastUpdateCh1", value: now, descriptionText: "Ch1 Last Update: $now")
                log.debug "raw humidity = $encapsulatedCommand.scaledSensorValue"
                
				resultEvents << createEvent(name:"humidity", value: (encapsulatedCommand.scaledSensorValue.toInteger() + settings.HumidOffsetCh1).toString(), unit:"%", descriptionText: "Humidity Ch1: "+(encapsulatedCommand.scaledSensorValue.toInteger() + settings.HumidOffsetCh1).toString() +" %", displayed: false)
				resultEvents << createEvent(name:"humidityCh1", value: (encapsulatedCommand.scaledSensorValue.toInteger() + settings.HumidOffsetCh1).toString(), unit:"%", descriptionText: "Humidity Ch1: "+(encapsulatedCommand.scaledSensorValue.toInteger() + settings.HumidOffsetCh1).toString() +" %")
				//resultEvents << createEvent(name:"humidity", value: encapsulatedCommand.scaledSensorValue.toString(), unit:"%", descriptionText: "Humidity Ch1: "+encapsulatedCommand.scaledSensorValue.toString()+" %", displayed: false)
				//resultEvents << createEvent(name:"humidityCh1", value: encapsulatedCommand.scaledSensorValue.toString(), unit:"%", descriptionText: "Humidity Ch1: "+encapsulatedCommand.scaledSensorValue.toString()+" %")
				break;
		case 9:
		        sendEvent(name: "lastUpdateCh2", value: now, descriptionText: "Ch2 Last Update: $now")
    
                BigDecimal offset = settings.TempOffsetCh2
                def startval = convertDegrees(tempScale,encapsulatedCommand) 
                def thetemp = startval as BigDecimal
                BigDecimal adjval = (thetemp + offset)
                def dispval =  String.format("%5.1f", adjval)
                def finalval = dispval as BigDecimal

    		    resultEvents << createEvent(name:"temperatureCh2", value: finalval, unit:"°"+location.temperatureScale, descriptionText: "Temperature Ch2: "+convertDegrees(tempScale,encapsulatedCommand)+" °"+location.temperatureScale)
				//resultEvents << createEvent(name:"temperatureCh2", value: convertDegrees(tempScale,encapsulatedCommand) + settings.TempOffsetCh2, unit:"°"+location.temperatureScale, descriptionText: "Temperature Ch2: "+convertDegrees(tempScale,encapsulatedCommand)+" °"+location.temperatureScale)
				break;
		case 10:
		        sendEvent(name: "lastUpdateCh2", value: now, descriptionText: "Ch2 Last Update: $now")
				resultEvents << createEvent(name:"humidityCh2", value: (encapsulatedCommand.scaledSensorValue.toInteger() + settings.HumidOffsetCh2).toString(), unit:"%", descriptionText: "Humidity Ch2: "+(encapsulatedCommand.scaledSensorValue.toInteger() + settings.HumidOffsetCh2).toString()+" %")
//				resultEvents << createEvent(name:"humidityCh2", value: encapsulatedCommand.scaledSensorValue.toString(), unit:"%", descriptionText: "Humidity Ch2: "+encapsulatedCommand.scaledSensorValue.toString()+" %")
				break;
	}
	return resultEvents
}

 // lgk reset
 
 def resetMaxMin()
 
 {
 
  log.debug "Resetting Min/Max values!"
  
     sendEvent(name: "maxRain", value: null, descriptionText: "Max Rain Reset", isStateChange: true)
	 sendEvent(name: "maxWindGust", value: null, descriptionText: "Max Wind Gust Reset", isStateChange: true)
     sendEvent(name: "maxOutdoorTemp", value: null, descriptionText: "Max OutDoor Temp Reset", isStateChange: true)
	 sendEvent(name: "minOutdoorTemp", value: null, descriptionText: "Min Outdoor Temp Reset", isStateChange: true)
    
     sendEvent(name: "windMaxDate", value: null, descriptionText: "Max Wind Date Reset", isStateChange: true)
	 sendEvent(name: "tempMaxDate", value: null, descriptionText: "Max Temp Date Reset", isStateChange: true)
     sendEvent(name: "tempMinDate", value: null, descriptionText: "Min Temp Date Reset", isStateChange: true)
	 sendEvent(name: "rainMaxDate", value: null, descriptionText: "Max Rain Date Reset", isStateChange: true)
     
     sendEvent(name: "windMaxDateStr", value: null, descriptionText: "Max Wind String Reset", isStateChange: true)
	 sendEvent(name: "tempMaxDateStr", value: null, descriptionText: "Max Temp String Reset", isStateChange: true)
     sendEvent(name: "tempMinDateStr", value: null, descriptionText: "Min Temp String Reset", isStateChange: true)
	 sendEvent(name: "rainMaxDateStr", value: null, descriptionText: "Max Rain String Rest", isStateChange: true)
 }
 
 
 def checkMinOutdoorTemp(cval)
 
 {
  def lval = cval as BigDecimal
  def mint = device.currentValue('minOutdoorTemp') as BigDecimal
  def now1 = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)


   if (mint == null)
     mint = 100.0
     
   log.debug "in check min outdoor temp, current value = $lval , current minimum = $mint"  
     
   if ((lval < mint) && (lval != -326.2) && (lval > -80.0))
   {
     def valstr = String.format("%5.1f",lval)
     def minStr =  "Min Outdoor Temp: ${valstr} °F\n ${now1}"
     
     sendEvent(name: "minOutdoorTemp", value: lval, descriptionText: "Min Outdoor Temp reset to $cval ", isStateChange: true)
     sendEvent(name: "tempMinDate", value: now1, descriptionText: "Outdor Temp Min Date Update: $now1")
	 sendEvent(name: "tempMinDateStr", value: minStr, descriptionText: "Min Temp String: $minStr", isStateChange: true)

   }
}


 def checkMaxOutdoorTemp(cval)
 
 {
   def lval = cval as BigDecimal
   def maxt = device.currentValue('maxOutdoorTemp') as BigDecimal
   def now1 = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)

   
   if (maxt == null)
     maxt = 0.0
     
   log.debug "in check max outdoor temp, current value = $lval , current maximum = $maxt"  
     
   if ((lval > maxt) && (lval != -326.2) && (lval < 140.0))
   {
  
     def valstr = String.format("%5.1f",lval)
     def maxStr =  "Max Outdoor Temp: ${valstr} °F\n ${now1}"
    
     sendEvent(name: "maxOutdoorTemp", value: lval, descriptionText: "Max Outdoor Temp reset to $cval ", isStateChange: true)
     sendEvent(name: "tempMaxDate", value: now1, descriptionText: "Outdor Temp Max Date Update: $now1")
	 sendEvent(name: "tempMaxDateStr", value: maxStr, descriptionText: "Max Temp String: $maxStr", isStateChange: true) 
   }
}


 def checkMaxWindGust(cval)
 
 {
   def lval = cval as BigDecimal
   def maxw = device.currentValue('maxWindGust') as BigDecimal
   def now1 = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
   
   if (maxw == null)
     maxw = 0.0
     
   log.debug "in check max wind gust, current value = $lval , current maximum  = $maxw"  
     
   if ((lval > maxw) && (lval != 197.12) && (lval < 100.0))
   {   
     def valstr = String.format("%5.1f",lval)
     def maxStr =  "Max Wind Gust: ${valstr} mph\n ${now1}"
    
     sendEvent(name: "maxWindGust", value: lval, descriptionText: "Max Wind Gust reset to $cval ", isStateChange: true)
     sendEvent(name: "windMaxDate", value: now1, descriptionText: "Wind Max Date Update: $now1")
     sendEvent(name: "windMaxDateStr", value: maxStr, descriptionText: "Max Wind Gust String: $maxStr", isStateChange: true) 
   }
}


 def checkMaxRain(cval)
 
 {
   def lval = cval as BigDecimal
   def maxr = device.currentValue('maxRain') as BigDecimal
   def now1 = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)

   
   if (maxr == null)
     maxr = 0.0
     
   log.debug "in check max rain, current value = $lval , current maximum  = $maxr"  
     
   if ((lval > maxr) && (lval < 12.0))
   {
     def valstr = String.format("%5.1f",lval)
     def maxStr =  "Max Rain: ${valstr} inches/hr\n ${now1}"
    
     sendEvent(name: "maxRain", value: lval, descriptionText: "Max Rain reset to $cval ", isStateChange: true)
     sendEvent(name: "rainMaxDate", value: now1, descriptionText: "Rain Max Date Update: $now1")
     sendEvent(name: "rainMaxDateStr", value: maxStr, descriptionText: "Max Rain String: $maxStr", isStateChange: true) 
 
    }

}
