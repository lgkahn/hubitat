/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *
 *  ZigBee White Color Temperature Bulb
 *
 *  Author: SmartThings
 *  Date: 2015-09-22
 */

metadata {
    definition (name: "ZigBee White Color Temperature Bulb - LGK Modified", namespace: "smartthings", author: "SmartThings") {

        capability "Actuator"
        capability "Color Temperature"
        capability "Configuration"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"

        attribute "colorName", "string"
        command "setGenericName"
           
        attribute "CoolWhite", "string"
        attribute "SoftWhite", "string"
        attribute "Moonlight", "string"
        attribute "Daylight", "string"
         
        command "setColorSoftWhite"
        command "setColorMoonlight"
        command "setColorCoolWhite"
        command "setColorDaylight"

        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300", outClusters: "0019"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04", outClusters: "0019"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY BR Tunable White", deviceJoinName: "OSRAM LIGHTIFY LED Flood BR30 Tunable White"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY RT Tunable White", deviceJoinName: "OSRAM LIGHTIFY LED Recessed Kit RT 5/6 Tunable White"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "Classic A60 TW", deviceJoinName: "OSRAM LIGHTIFY LED Tunable White 60W"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY A19 Tunable White", deviceJoinName: "OSRAM LIGHTIFY LED Tunable White 60W"
    }

    // UI tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
            // lgk color name not working in latest android version as secondary control so add as std tile for now
            // this is why it was not added as a tile below in default driver
//            tileAttribute ("device.colorName", key: "SECONDARY_CONTROL") {
  //              attributeState "colorName", label:'${currentValue}'
    //        }
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 2, inactiveLabel: false, range:"(2700..6500)") {
            state "colorTemperature", action:"color temperature.setColorTemperature"
        }
        valueTile("colorTemp", "device.colorTemperature", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "colorTemperature", label: '${currentValue} K'
        }
        valueTile("colorName", "device.colorName", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "colorName", label: '${currentValue}'
        }




 // lgk new tiles
    valueTile("SoftWhite", "device.SoftWhite",  width: 2, height: 2) {
			state "unlite", label:'Soft White',backgroundColor: "#ffffff", action: "setColorSoftWhite"
            state "lite", label:'Soft White',backgroundColor: "#4f9558"
	}
    
    valueTile("Moonlight", "device.Moonlight",  width: 2, height: 2) {
			state "unlite", label:'Moonlight',backgroundColor: "#ffffff", action: "setColorMoonlight"
            state "lite", label:'Moonlight',backgroundColor: "#4f9558"
	}	
    
    valueTile("CoolWhite", "device.CoolWhite",  width: 2, height: 2) {
			state "unlite", label:'Cool White',backgroundColor: "#ffffff", action: "setColorCoolWhite"
            state "lite", label:'Cool White',backgroundColor: "#4f9558"
	}
    
    valueTile("Daylight", "device.Daylight",  width: 2, height: 2) {
			state "unlite", label:'Daylight',backgroundColor: "#ffffff", action: "setColorDaylight"
            state "lite", label:'Daylight',backgroundColor: "#4f9558"
	}

// lgk end new tiles
        main(["switch"])
        details(["switch", "colorTempSliderControl", "colorTemp","colorName","SoftWhite","Moonlight",
        "refresh","CoolWhite","Daylight"])
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
  //  log.debug "description is $description"
    def event = zigbee.getEvent(description)
    if (event) {
        sendEvent(event)
    }
    else {
        log.warn "DID NOT PARSE MESSAGE for description : $description"
        //log.debug zigbee.parseDescriptionAsMap(description)
    }
}

def off() {
    log.debug "turned off"
    zigbee.off()
}

def on() {
    log.debug "turned on"
    zigbee.on()
}

def setLevel(value) {
    log.debug "set level"
    zigbee.setLevel(value)
}

def refresh() {
    zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.colorTemperatureRefresh() + zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.colorTemperatureConfig()
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.colorTemperatureConfig() + zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.colorTemperatureRefresh()
}

def setColorTemperature(value) {
    log.debug "in set color temp value = $value"
    sendEvent(name: "colorTemperature", value: value, displayed:false)
    setGenericName(value)
    zigbee.setColorTemperature(value)
    
}

//Naming based on the wiki article here: http://en.wikipedia.org/wiki/Color_temperature
def setGenericName(value){
    if (value != null) {
        def genericName = "White"
        if (value < 3300) {
            genericName = "Soft White"
        } else if (value < 4150) {
            genericName = "Moonlight"
        } else if (value <= 5000) {
            genericName = "Cool White"
        } else if (value >= 5000) {
            genericName = "Daylight"
        }
        log.debug "in set generic name setting colorname to $genericName"
        
        sendEvent(name: "colorName", value: genericName)
    }
}


// lgk new code
def setColorSoftWhite()
{
	log.debug "Soft White tile Pressed!"
	setColorTemperature(3000)

	sendEvent(name: "CoolWhite", value: "unlite")
    sendEvent(name: "SoftWhite", value: "lite")
    sendEvent(name: "Moonlight", value: "unlite")
    sendEvent(name: "Daylight", value: "unlite")
}

def setColorMoonlight()
{
	log.debug "Moonlight tile Pressed!"
	setColorTemperature(4100)

	sendEvent(name: "CoolWhite", value: "unlite")
    sendEvent(name: "SoftWhite", value: "unlite")
    sendEvent(name: "Moonlight", value: "lite")
    sendEvent(name: "Daylight", value: "unlite")
}

def setColorCoolWhite()
{
	log.debug "Cool White tile Pressed!"
	setColorTemperature(4900)

	sendEvent(name: "CoolWhite", value: "lite")
    sendEvent(name: "SoftWhite", value: "unlite")
    sendEvent(name: "Moonlight", value: "unlite")
    sendEvent(name: "Daylight", value: "unlite")
}

def setColorDaylight()
{
	log.debug "Daylight tile Pressed!"
	setColorTemperature(6400)

	sendEvent(name: "CoolWhite", value: "unlite")
    sendEvent(name: "SoftWhite", value: "unlite")
    sendEvent(name: "Moonlight", value: "unlite")
    sendEvent(name: "Daylight", value: "lite")
}


