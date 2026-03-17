metadata {
	definition (name: "Host Ping Device", namespace: "ilkeraktuna", author: "ilkeraktuna") {
    capability "Switch"
    capability "Refresh"    
    capability "PresenceSensor"
    command "xon"
    command "xoff"
    command "refresh"
	attribute "state", "string"
    attribute "percentLoss", "number"
	}
    
preferences {
		section {
			input title: "", description: "Pingable Device", displayDuringSetup: true, type: "paragraph", element: "paragraph"
			input("name", "string", title:"Name", description: "Name", required: true, displayDuringSetup: true)
			input("ip", "string", title:"LAN IP address", description: "LAN IP address", required: true, displayDuringSetup: true)
			input("pingPeriod", "number", title: "Ping Repeat in Seconds\n Zero to disable", defaultValue: 60, required:true, submitOnChange: true)
            input("wol", "bool", title: "Enable Wake On LAN?")
            input("myMac", "string", title: "MAC address", required: false)
			input("debugEnable", "bool", title: "Enable debug logging?")
            
			if (security) { 
				input("username", "string", title: "Hub Security Username", required: false)
				input("password", "password", title: "Hub Security Password", required: false)
			}
    }
}

}

def on() {
if(debugEnable)log.debug "turning on"
sendEvent(name: "switch", value: "on");
if(wol) sendHubCommand(createWOL())
}

def off() {
if(debugEnable)log.debug "turning off"
sendEvent(name: "switch", value: "off");
}

def xon() {
if(debugEnable)log.debug "on new"
sendEvent(name: "switch", value: "on");
sendEvent(name: "presence", value: "present");
}

def xoff() {
if(debugEnable)log.debug "off new"
sendEvent(name: "switch", value: "off");
sendEvent(name: "presence", value: "not present");
}

def installed() {
initialize()
}

def updated() {
initialize()
}

def poll(){
refresh()
}

def initialize() {
	log.debug "in initialize"
unschedule()
	//if(pingPeriod > 0) runIn(pingPeriod, "sendPing", [data:ipAddress])
	if(pingPeriod > 0) runIn(pingPeriod, "refresh")
	//runEvery1Minute(refresh)
}

def refresh() {
def host = ip
	if(pingPeriod > 0) runIn(pingPeriod, "refresh")
sendPing(ip)
}

def sendPing(ipAddress){
if(ipAddress == null) ipAddress = data.ip
// start - Modified from dman2306 Rebooter app
if(security) {
    httpPost(
        [
            uri: "http://127.0.0.1:8080",
            path: "/login",
            query: [ loginRedirect: "/" ],
            body: [
                username: username,
                password: password,
                submit: "Login"
            ]
        ]
    ) { resp -> cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0) }
}
// End - Modified from dman2306 Rebooter app

params = [
    uri: "http://${location.hub.localIP}:8080",
    path:"/hub/networkTest/ping/"+ipAddress,
    headers: [ "Cookie": cookie ]
]
if(debugEnable)log.debug params
asynchttpGet("sendPingHandler", params)
//updateAttr("responseReady",false)
//updateAttr("pingReturn","Pinging $ipAddress")  
//if(pingPeriod > 0) runIn(pingPeriod, "sendPing", [data:ipAddress])

}

def sendPingHandler(resp, data) {
def errFlag = 0
try {
	    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		    strWork = resp.data.toString()
		//if(debugEnable)
           // log.debug "here"
            log.debug strWork
			//xon()
  	    }
} catch(Exception ex) { 
    errFlag = 1
    respStatus = resp.getStatus()
    sendEvent(name:"pingReturn", value: "httpResp = $respStatus but returned invalid data")
    xoff()
		log.warn "sendPing httpResp = $respStatus but returned invalid data"
} 
if (errFlag==0) extractValues(strWork)
}

def extractValues(strWork) {
startInx = strWork.indexOf("%")
if(debugEnable)log.debug startInx
percentLossX=0
if (startInx == -1){
    percentLossX=100
    updateAttr("percentLoss",100)      
} else {
    startInx -=3
    strWork=strWork.substring(startInx)
    if(strWork.substring(0,1)==","){
        percentLossX = strWork.substring(1,3).toInteger()
    } else
        percentLossX = strWork.substring(0,3).toInteger()
    updateAttr("percentLoss",percentLossX)        
    startInx = strWork.indexOf("=")
}
if(debugEnable)log.debug percentLossX
if (percentLossX < 100 ) xon()
else xoff()
}

def updateAttr(aKey, aValue){
if(debugEnable)log.debug "update percentLoss"
sendEvent(name:aKey, value:aValue)
}

def updateAttr(aKey, aValue, aUnit){
sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def createWOL() {
	def newMac = myMac.replaceAll(":","").replaceAll("-","")
if(debugEnable) log.debug "Sending Magic Packet to: $newMac"
def result = new hubitat.device.HubAction (
   	"wake on lan $newMac",
   	hubitat.device.Protocol.LAN,
   	null
)    
return result
}
