/**
 *  Drayton Wiser
 *
 *  Copyright 2018 Colin Chapman
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
 */
definition(
    name: "Drayton Wiser (Connect)",
    namespace: "colc1705",
    author: "Colin Chapman",
    description: "Connect Drayton Wiser to Smartthings",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true)


preferences {
    
    
    page(name: "mainPage", title:"Drayton Wiser Setup", content:"mainPage", install: true)
    
    
}

def installed() {
	if (showDebug) log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	if (showDebug) log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	if (showDebug) log.debug "initialize()"
    getHubConfig()
    runEvery1Minute("getHubConfig")
    //getHubUrl("/data/domain/Room")
    //sendMessageToHeatHub("/data/domain/", "GET", "")
}

def mainPage() {
	if (showDebug) log.debug "mainPage"
    return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        section("Title") {
            input("hubIP", "string", title: "Hub IP address", description: "", required: true)
            input("systemSecret", "string", title: "Hub Secret", required: true)
            input("heatingBoost", "number", title: "Minutes for heating boost", required: false, defaultValue: 30)
            input("waterBoost", "number", title: "Minutes for hot water boost", required: false, defaultValue: 60)
            input("showDebug","bool", title: "Show debugging info", required: true, defaultValue: false)
        }
    }
  }
  
def showDebugInfo() {
	return showDebug
    }
  
def test(dni = null) {
	if (showDebug) log.debug "smartapp test($dni)"
    //refreshAllChildren()
    state.action = "test"
    getHubUrl("/data/v2/domain/")
}

def refreshHub(dni) {
	if (showDebug) log.debug "refreshHub()"
    if (dni == null) dni = app.id + ":HUB"
    def child = getChildDevice(dni)
    //log.debug state.hubConfig.System.EcoModeEnabled
    //log.debug state.hubConfig.System.OverrideType
    child.setMode(state.hubConfig.System.OverrideType)
    child.setEco(state.hubConfig.System.EcoModeEnabled)
    child.setComfort(state.hubConfig.System.ComfortModeEnabled)
}

def refreshChild(dni) {
	if (showDebug) log.debug "refreshChild($dni)"
	def roomId = dni.split(":")[1]
    def child = getChildDevice(dni)
    def roomStatId
    //log.debug roomId
    
	if (roomId == "HW") {
    	//if (showDebug) log.debug "Update Hot Water"
        def hotWater = state.hubConfig.HotWater[0]
        child.setState(hotWater.WaterHeatingState)
        child.setMode(hotWater.Mode)
        if (hotWater.OverrideTimeoutUnixTime) {
        	child.setBoost("On")
        } else {
        	child.setBoost("Off")
        }
    } else {
    	//if (showDebug) log.debug "Update room id{$roomId}"
        def rooms = state.hubConfig.Room
        rooms.each { room ->
            	if (roomId == room.id.toString()) {
                	
                    child.setTemp(room.CalculatedTemperature/10, room.CurrentSetPoint/10)
                    child.setMode(room.Mode)
                    //child.setOutputState(room.ControlOutputState)
                    child.setDemand(room.PercentageDemand)
                    child.setWindowState(room.WindowState)
                    if (room.OverrideTimeoutUnixTime) {
                    	child.setBoost("On")
                    } else {
                    	child.setBoost("Off")
                    }
                    roomStatId = room.RoomStatId
                    if (roomStatId) child.setHumidity(getHumidity(roomStatId))
                }
            }
    }
}

def refreshAllChildren() {
	if (showDebug) log.debug "refreshAllChildren()"
    def children = getChildDevices()
    
    children.each { child ->
    		
    		refreshChild(child.deviceNetworkId)
        
    }
    

}

def getHubConfig() {
	if (showDebug) log.debug "getHubConfig()"
    
    def result = new hubitat.device.HubAction(
    	method: "GET",
        path: "/data/domain/",
        headers: [
        	HOST: hubIP+":80",
            SECRET: systemSecret
        ],
        null,
        [callback: calledBackHandler]
        )
    state.action = "Hub Config"
    sendHubCommand(result)
}

def getHubUrl(path) {
	if (showDebug) log.debug "getHubUrl($path)"
    
    def result = new hubitat.device.HubAction(
    	method: "GET",
        path: path,
        headers: [
        	HOST: hubIP+":80",
            SECRET: systemSecret
        ],
        null,
        [callback: calledBackHandler]
        )
    //state.action = "Hub Config"
    sendHubCommand(result)
}

def sendMessageToHeatHub(path, method, content) {
	if (showDebug) log.debug "sendMessageToHeatHub($path, $method, $content)"
    
   def result = new hubitat.device.HubAction(
    	method: method,
        path: path,
        headers: [
        	HOST: hubIP+":80",
            SECRET: systemSecret
        ],
        body: content,
        null,
        [callback: calledBackHandler]
        )
    //state.action = "Hub Config"
    sendHubCommand(result)
}


void calledBackHandler(hubitat.device.HubResponse hubResponse) {
    if (showDebug) log.debug "Entered calledBackHandler()..."
    if (showDebug) log.debug hubResponse.status
    //if (showDebug) log.debug "entering action: " + state.action
    
    if (state.action == "test") log.debug hubResponse.json
    
    if (state.action == "Hub Config") {
    	state.hubConfig = hubResponse.json
        
		def rooms = state.hubConfig.Room   
   
   		if (state.hubConfig.HotWater) {
  		  	if (showDebug) log.debug "Got hot water"
   		    createChildDevices(rooms, true)
  		} else {
    		if (showDebug) log.debug "No hot water"
    		createChildDevices(rooms, false)
    	}
    
	
    }
    
    if (state.action == "refreshChildren" ) {
    	state.hubConfig = hubResponse.json
        refreshAllChildren()
    }
    	
    
    if (hubResponse.status == 200) {
    	if (state.action.contains("setPoint")) runIn(3, "getHubConfig")
    	if (state.action == "ecoOn") getChildDevice(app.id + ":HUB").setEco(true)
    	if (state.action == "ecoOff") getChildDevice(app.id + ":HUB").setEco(false)
        if (state.action == "comfortOn") getChildDevice(app.id + ":HUB").setComfort(true)
        if (state.action == "comfortOff") getChildDevice(app.id + ":HUB").setComfort(false)
        if (state.action.contains("changeroomManualMode")) {
        	def roomId = state.action.split(":")[1]
        	getChildDevice(app.id + ":$roomId").setMode("Manual")
            state.action = ""
        }
        if (state.action.contains("changeroomAutoMode")) {
        	def roomId = state.action.split(":")[1]
        	getChildDevice(app.id + ":$roomId").setMode("Auto")
            state.action = ""
        }
        if (state.action.contains("roomManualMode")) state.action = "change" + state.action
        if (state.action.contains("roomAutoMode")) state.action = "change" + state.action
        
        if (state.action == "hwManualChange") getChildDevice(app.id + ":HW").setMode("Manual")
        if (state.action == "hwAutoChange") getChildDevice(app.id + ":HW").setMode("Auto")
        if (state.action == "hwManual") state.action = "hwManualChange"
        if (state.action == "hwAuto") state.action = "hwAutoChange"
        
        if (state.action == "hwBoostOn") {
        	getChildDevice(app.id + ":HW").setBoost("on")
            getChildDevice(app.id + ":HW").setState("on")
            }
        if (state.action == "hwBoostOff") {
        	getChildDevice(app.id + ":HW").setBoost("off")
            getChildDevice(app.id + ":HW").setState("off")
            }
        if (state.action == "HotWaterOn") getChildDevice(app.id + ":HW").setState("on")
        if (state.action == "HotWaterOff") getChildDevice(app.id + ":HW").setState("off")
        
        if (state.action == "roomBoostOn") {
        	state.action = "refreshChildren"
            getHubUrl("/data/domain/")
        }
        if (state.action == "roomBoostOff") {
        	state.action = "refreshChildren"
            getHubUrl("/data/domain/")
        }
        
    }
    if (hubResponse.status == 403) {
    	if (state.action == "homeModeChange") {
        	getChildDevice(app.id + ":HUB").setMode("Home")
            state.action = "refreshChildren"
            getHubUrl("/data/domain/")
            
        }
        if (state.action == "awayModeChange") {
        	getChildDevice(app.id + ":HUB").setMode("Away")
            state.action = "refreshChildren"
            getHubUrl("/data/domain/")
        }
        if (state.action == "homeMode") state.action = "homeModeChange"
        if (state.action == "awayMode") state.action = "awayModeChange"
 		
        
        
    }
    
    //if (showDebug) log.debug "exiting action " + state.action
}

private void createChildDevices(rooms, hotwater) {
	if (showDebug) log.debug "createChildDevices()"
    def children = getChildDevices().deviceNetworkId
    def child
    def dni
    
    dni = app.id + ":HUB"
    if (children.contains(dni)) {
    	if (showDebug) log.debug "Device ${dni} already exists"
        refreshHub(dni)
    } else {
    	try {
        	child = addChildDevice(app.namespace, "Drayton Wiser Hub", dni, null, ["label": "Drayton Hub"]) 
            refreshHub(dni)
        } catch (e) {
        	if (showDebug) log.debug "Error creating child device ${e}"
        }
    }
    
    
    for (HashMap room : rooms) {
    	def dh
    	dni = app.id + ":" + room.id
        if (children.contains(dni)) {
        	if (showDebug) log.debug "Device ${dni} already exists"
            refreshChild(dni)
        } else {
        	try {
            	if (room.RoomStatId) dh = "Drayton Wiser Room"
                if (room.SmartValveIds) dh = "Drayton Wiser TRV"
                child = addChildDevice(app.namespace, dh, "$dni", null, ["label": "Drayton (${room.Name})"])
                //child.setTemp(room.CalculatedTemperature/10, room.CurrentSetPoint/10)
                refreshChild(dni)
        	} catch(e) {
        		if (showDebug) log.debug "Error creating child device ${e}"
        	}
        }
    }
    
    //add hot water
    if (hotwater) {
    	dni = app.id + ":HW"
        if (children.contains(dni)) {
        	if (showDebug) log.debug "Device ${dni} already exists"
            refreshChild(dni)
        } else {
    		try {
                child = addChildDevice(app.namespace, "Drayton Wiser Hot Water", "$dni", null, ["label": "Drayton Hot Water"])
                //child.setState(state.json.HotWater.WaterHeatingState)
                refreshChild(dni)
			} catch(e) {
    			if (showDebug) log.debug "Error creating child device ${e}"
    		}
        }
    }
}

void setPoint(dni, setPoint) {
	if (showDebug) log.debug "setPoint($dni, $setPoint)"
	def roomId = dni.split(":")[1]
    //if (showDebug) log.debug roomId
    def newSP = setPoint * 10
    if (roomId == "HW" ) {
      	if (showDebug) log.debug "This is the hotwater"
        
    } else {
    	state.action = "setPoint:" + roomId
        def payload
        payload = "{\"RequestOverride\":{\"Type\":\"Manual\", \"SetPoint\":" + newSP.toInteger().toString() + "}}"
        sendMessageToHeatHub(getRoomsEndpoint() + roomId.toString(), "PATCH", payload)
    }
    
}

def setAwayMode(awayMode) {
	if (showDebug) log.debug "setAwayMode($awayMode)"
	def payload
    def payload2
	payload = "{\"Type\":" + (awayMode ? "2" : "0") + ",\"Originator\": \"App\", \"setPoint\":" + (awayMode ? "50" : "0") + "}"
	payload2 = "{\"Type\":" + (awayMode ? "2" : "0") + ", \"setPoint\":" + (awayMode ? "-200" : "0") + "}"
    state.action = (awayMode ? "awayMode" : "homeMode")
    return [sendMessageToHeatHub(getSystemEndpoint() + "RequestOverride", "PATCH", payload), delayAction(1000), sendMessageToHeatHub(getHotwaterEndpoint() + "2/RequestOverride", "PATCH", payload2)]
}

def setHotWaterManualMode(manualMode) {
	if (showDebug) log.debug "setHotWaterManualMode($manualMode)"
	def payload
    def payload2
    payload = "{\"Mode\":\"" + (manualMode ? "Manual" : "Auto") + "\"}"
    payload2 = "{\"RequestOverride\":{\"Type\":\"None\",\"Originator\" :\"App\",\"DurationMinutes\":0,\"SetPoint\":0}}"
    state.action = (manualMode ? "hwManual" : "hwAuto")
    return [sendMessageToHeatHub(getHotwaterEndpoint() + "2", "PATCH", payload), delayAction(1000), sendMessageToHeatHub(getHotwaterEndpoint() + "2", "PATCH", payload2)]
}

def setEcoMode(ecoMode) {
		if (showDebug) log.debug "setEcoMode($ecoMode)"
		def payload
        payload = "{\"EcoModeEnabled\":" + ecoMode + "}";
        state.action = (ecoMode ? "ecoOn" : "ecoOff")
        sendMessageToHeatHub(getSystemEndpoint(), "PATCH", payload);
        //refresh();
}

def setComfort(comfort) {
	if (showDebug) log.debug "setComfort($comfort)"
    def payload
    payload = "$comfort";
    state.action = (comfort ? "comfortOn" : "comfortOff")
    sendMessageToHeatHub(getSystemEndpointv2() + "ComfortModeEnabled", "PATCH", payload);
}

def getHumidity(roomStatId) {
	if (showDebug) log.debug "getHumidity($roomStatId)"
    for (HashMap roomStat : state.hubConfig.RoomStat) {
    	if (roomStatId.toString().equals(roomStat.id.toString())) {
        	return roomStat.MeasuredHumidity
        } else {
        	return 0
        }
    }
}

def setRoomManualMode(dni, manualMode) {
        if (showDebug) log.debug "setRoomManualMode($dni, $manualMode)"
		def roomId = dni.split(":")[1]
        def payload
        def payload2
        payload = "{\"Mode\":\"" + (manualMode ? "Manual" : "Auto") + "\"}"
        payload2 = "{\"RequestOverride\":{\"Type\":\"None\",\"Originator\" :\"App\",\"DurationMinutes\":0,\"SetPoint\":0}}"
        state.action = (manualMode ? "roomManualMode:$roomId" : "roomAutoMode:$roomId")
        return [sendMessageToHeatHub(getRoomsEndpoint() + roomId.toString(), "PATCH", payload), delayAction(1000), sendMessageToHeatHub(getRoomsEndpoint() + roomId.toString(), "PATCH", payload)]
}

def setRoomBoost(dni, boostTime, temp) {
    def roomId = dni.split(":")[1]
    def payload
    if (boostTime == 0) {
    	state.action = "roomBoostOff"
        payload = "{\"RequestOverride\":{\"Type\":\"None\",\"Originator\":\"App\",\"DurationMinutes\":0,\"SetPoint\":0}}"
    } else {
    	boostTime = heatingBoost
    	state.action = "roomBoostOn"
        payload = "{\"RequestOverride\":{\"Type\":\"Manual\",\"Originator\":\"App\", \"DurationMinutes\":" + boostTime + ", \"SetPoint\":"+ (temp * 10).toInteger().toString() + "}}"
    }
    if (showDebug) log.debug "setRoomBoost($dni, $boostTime, $temp)"
    sendMessageToHeatHub(getRoomsEndpoint() + roomId.toString(), "PATCH", payload)
}

def setHotWaterBoost(boostTime) {
    def payload
    if (boostTime == 0) {
    	state.action = "hwBoostOff"
    	payload = "{\"RequestOverride\":{\"Type\":\"None\",\"Originator\":\"App\",\"DurationMinutes\":" + boostTime + ",\"SetPoint\":0}}"	
    } else {
    	boostTime = waterBoost
    	state.action = "hwBoostOn"
    	payload = "{\"RequestOverride\":{\"Type\":\"Manual\",\"Originator\":\"App\",\"DurationMinutes\":" + boostTime + ",\"SetPoint\":1100}}"
    }
    if (showDebug) log.debug "setHotWaterBoost($dni, $boostTime)"
    sendMessageToHeatHub(getHotwaterEndpoint() + "2", "PATCH", payload)
}

def turnHotWaterOn() {
	if (showDebug) log.debug "turnHotWateOn()"
    def payload
    state.action = "HotWaterOn"
    payload = "{\"RequestOverride\":{\"Type\":\"Manual\",\"SetPoint\":1100}}"
    sendMessageToHeatHub(getHotwaterEndpoint() + "2", "PATCH", payload)
}

def turnHotWaterOff() {
	if (showDebug) log.debug "turnHotWateOff()"
    def payload
    state.action = "HotWaterOff"
    payload = "{\"RequestOverride\":{\"Type\":\"Manual\",\"SetPoint\":-200}}"
    sendMessageToHeatHub(getHotwaterEndpoint() + "2", "PATCH", payload)
}


private delayAction(long time) {
	new hubitat.device.HubAction("delay $time")
}

def getDeviceEndpoint() {
	return "/data/domain/Device/"
}

def getRoomstatsEndpoint() {
    return "/data/domain/RoomStat/"
    }
    
def getTRVsEndpoint() {
    return "/data/domain/SmartValve/"
    }
    
def getRoomsEndpoint() {
    return "/data/domain/Room/"
    }
    
def getSchedulesEndpoint() {
    return "/data/domain/Schedule/";
    }
    
def getHeatChannelsEndpoint() { 
    return "/data/domain/HeatingChannel/"
    }
    
def getSystemEndpoint() {
    return "/data/domain/System/"
    }
    
def getSystemEndpointv2() {
	return "/data/v2/domain/System/"
    }
    
def getStationEndpoint() {
    return "/data/network/Station/"
    }
    
def getDomainEndpoint() {
    return "/data/domain/"
    }
    
def getHotwaterEndpoint() {
    return "/data/domain/HotWater/"
    }
