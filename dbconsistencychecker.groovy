
/* lgkahn kahn@lgk.com
* device to enumerate through all devices in the system and check that there are no zwave/zigbee devices that are not in their corresponding zwave/zigbee tables.
* also checks in the other direction to make sure there are no zwave devices in the zwave table that are not in the device list (ghosts).
* v 1.1
 
*/

preferences()
{  
    input "debug", "bool", title: "Enable debugging?", required: true, defaultValue: false
    input "descLog", "bool", title: "Enable description Text logging", required: true, defaultValue: true
    input "runSchedule", "enum", title: "Schedule to run every X days?",options: ["Disabled", "1", "2", "3", "5", "7","14","21","28","30"],  required: true, defaultValue: "Disabled"
    input "runHour","enum",title: "If scheduled to run, run at which hour?",options: ["00","01", "02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20"," 21","22","23"], required: true, defaultValue: "00"
    input "clearTableStatesAfterRun","bool",title: "Clear out the table/state variables after run to not cluter the device status page?", required: true, defaultValue: true
}

metadata {
    definition (name: "LGK Zwave table/devices comparison.", namespace: "lgkapps", author: "larry kahn kahn@lgk.com",  singleThreaded: false) {
    capability "Actuator"
      
    command "compare"
    }
}

attribute "numberOfZwaveNodes", "number"
attribute "numberOfZigbeeNodes", "number"
attribute "numberOfDevices", "number"
attribute "devicesNotInZwaveTable" , "number"
attribute "devicesNotInZigbeeTable" , "number"
attribute "ghosts", "number"
attribute "zwMatchesFound", "number"
attribute "zbMatchesFound", "number"
attribute "lastUpdate", "string"
attribute "lastRunStart", "string"
attribute "zzrunResults", "string"

void installed() {
   log.info "in installed"
}

void updated() {
    
    log.info "LGKahn Device/Zwave/Zigbee Database consistency checker version: ${getVersion()}"
     
    if (runSchedule != "Disabled")
    {
      log.info "Schedule to run every $runSchedule Days at $runHour."
      def chronEntry = "0 0 " + runHour + " */" + runSchedule + " * ?"
      if (debug) log.debug "chronEntry = $chronEntry"
      unschedule()
      schedule(chronEntry,"compare")     
    }
    else
    {
      log.info "Run Schedule is disabled."
      unschedule()
    }   
    
   if (clearTableStatesAfterRun) log.info "Will clear state variables after run."
}

void initialize()
{
   log.info "in initialize"      
}

void getDeviceList()
{
    if (debug) log.info "In get Device List"
    
   def params = [ uri: "http://127.0.0.1:8080/device/listJson?capability=capability.*",
                 contentType: "application/json",
                 timeout: 20]
    
    try {
	     httpGet(params) { resp ->   
           if (debug) log.warn "resp = ${resp.data}"
           
           def devices1 = resp.data  as ArrayList
           log.info "Device table items loaded: ${devices1.size()}"
      
           sendEvent(name: "numberOfDevices", value: devices1.size())
           state.hubDevices = devices1
   
         }
    }
      catch (Exception e) {
        log.error "EXCEPTION CAUGHT: ${e.message} ON LINE ${e.stackTrace.find{it.className.contains("user_")}?.lineNumber}"   
      }
}

void getZwaveTable()
{
    if (debug) log.info "in get zwave table" 
    Map respData = [:]
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/hub/zwaveDetails/json",      
           timeout: 20]
  
    try {
        
    httpGet(params) { resp ->
        if (debug) log.warn "data = ${resp.data}"
       respData = resp.data as Map    
    }
    }
   catch (Exception e) {
        log.error "EXCEPTION CAUGHT: ${e.message} ON LINE ${e.stackTrace.find{it.className.contains("user_")}?.lineNumber}"
    }
    
    state.zwDevices = respData.zwDevices as Map
    sendEvent(name: "numberOfZwaveNodes", value: state.zwDevices.size()) 
    log.info "Zwave table Items Loaded: ${state.zwDevices?.size() ?: 0} nodes" 
}

void getZigbeeTable()
{
    def respData
    if (debug)log.info "in get zigbee table" 
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/hub/zigbeeDetails/json",         
           timeout: 20]
  
    try {
        
    httpGet(params) { resp ->
        if (debug) log.warn "devices = ${resp.data.devices}"
        respData = resp.data.devices 
      
    }
    }
   catch (Exception e) {
        log.error "EXCEPTION CAUGHT: ${e.message} ON LINE ${e.stackTrace.find{it.className.contains("user_")}?.lineNumber}"
    }
   
    state.zigbeeDevices = respData
    sendEvent(name: "numberOfZigbeeNodes", value: state.zigbeeDevices.size()) 
    log.info "Zigbee table items loaded: ${state.zigbeeDevices?.size() ?: 0} nodes" 
}

void listZWaveTable()
{
 if (debug) log.info "In list zwave Table"
    
 def howmany = state.zwDevices.size()
 def zwtable = state.zwDevices
    
  log.debug "size = ${howmany}"

  zwtable?.each 
    { it ->
        log.info "got node ${it.id}, value = ${it.value}"
    }

}

void listZigbeeTable()
{
 if (debug) log.info "In list zigbee Table"
    
 def howmany = state.zigbeeDevices.size()
 def zbtable = state.zigbeeDevices
    
  //log.debug "zbtabe = $zbtable"  
  log.debug "size = ${howmany}"

  zbtable?.each 
    { it ->
       log.info "got node ${it.id}, value = ${it.name}, ${it.zigbeeId}"
    }

}

Number compareZWaveTable()
{
   def ghosts = 0
   def matches = 0
    
   def zwdevices = state.zwDevices
   if (debug) log.debug "in compare zwave table size = ${zwdevices.size()}"
    
   for (it in zwdevices)
    {
        if (debug) log.debug "id = ${it.value.id}, ${it.value.displayName}"
        
        // check if id is in the devics
        def deviceExits = findZWNodeInDevices(it.value.id)   
        if (debug) log.debug "deviceExits = $deviceExits"
        
      if (deviceExits)
        {            
           if (debug) log.debug "found it"
            found = true
            ++matches
        }
       else
       {
        log.error "WARNING device ${it.value.displayName}, id:${it.id} is in the ZWave node table but does not appear to exist as a device... most likely a GHOST!"
         ++ghosts
       }
    }
    if (debug) log.info "Matches Found: $matches"
    if (debug) log.info "Ghosts: $ghosts"
    
    sendEvent(name: "ghosts", value: ghosts)
}

void compareDeviceTable()
{
    
    def missingZWave = 0
    def missingZigbee = 0
    def zwmatches = 0
    def zbmatches = 0  
         
 def howmany = state.hubDevices.size()
 def deviceTable = state.hubDevices
 // log.debug "device table = $deviceTable"
    
  if (debug) log.debug "size = ${howmany}"

  deviceTable?.each
    { it ->
        if (debug) log.info "got device ${it.displayName}, value = ${it.id}"
        // get type data
        def dtype = getDeviceType(it.id)
        if (debug) log.debug "deviceType: $dtype"
        
        if (dtype == "zwave")
        {
         boolean intable = isInZwaveTable(it.id)
         if (debug) log.debug "in zwave table = $intable"          
         if (intable == false)
          {
            log.error "WARNING device ${it.displayName}, id:${it.id} is a ZWave device but is not in the ZWave Table!"
            ++missingZWave
          }
         else ++zwmatches
        }
        
       else if (dtype == "zigbee")
       {
         boolean intable = isInZigbeeTable(it.id)
         if (debug) log.debug "in zigbee table = $intable"
         if (intable == false)
          {
            log.error "WARNING device ${it.displayName}, id:${it.id} is a Zigbee device but is not in the Zigbee Table!"
            ++missingZigbee
          }
         else ++zbmatches   
       }
    }
    
    // now look for ghosts
    
    sendEvent(name: "devicesNotInZwaveTable", value: missingZWave.toInteger())
    sendEvent(name: "devicesNotInZigbeeTable", value: missingZigbee.toInteger())
    sendEvent(name: "zwMatchesFound", value: zwmatches)
    sendEvent(name: "zbMatchesFound", value: zbmatches)
    
    def ghosts = device.currentValue("ghosts")
    
    if (descLog)
    {   
        log.info "------------------------------------------------------------------------"
        log.info " "
        log.warn "Zwave Devices that are not in the ZWave Table: ${missingZWave}"
        log.warn "Zigbee Devices that are not in the Zigbee Table: ${missingZigbee}"
        log.warn "Zwave table elements that are not in the Device List (Ghosts): $ghosts"
        log.info ""
        log.info "Zigbee Devices that were found in the Zigbee table: $zbmatches"
        log.info "Zwave Devices that were found in the Zwave table: $zwmatches"
        log.info "Total Zigbee nodes: ${state.zigbeeDevices?.size()}"
        log.info "Total Zwave Nodes: ${state.zwDevices?.size()}"
        log.info "Total Devices: ${howmany}"
        log.info "------------------------------------------------------------------------"
        log.info "Zwave/Zigbee/Device Table Comparison Stats:"
    }
    
        String theResults = "------------------------------------------------------------------------<br>"
        theResults = theResults + "Zwave/Zigbee/Device Table Comparison Stats:<br><"
        theResults = theResults + "Total Devices: ${howmany} <br>"
        theResults = theResults + "Total Zwave Nodes: ${state.zwDevices?.size()} <br>" 
        theResults = theResults + "Total Zigbee nodes: ${state.zigbeeDevices?.size()} <br>"
        theResults = theResults + "Zwave Devices that were found in the Zwave table: $zwmatches <br>"
        theResults = theResults + "Zigbee Devices that were found in the Zigbee table: $zbmatches <br><br>"
      
        theResults = theResults +"Zwave Devices that are not in the ZWave Table: ${missingZWave}<br> "
        theResults = theResults + "Zigbee Devices that are not in the Zigbee Table: ${missingZigbee} <br>"
        theResults = theResults + "Zwave table elements that are not in the Device List (Ghosts): $ghosts <br>"
        theResults = theResults + "------------------------------------------------------------------------"
  
        sendEvent(name: "zzrunResults", value: theResults)
}

String getDeviceType(deviceid)
{
    params = [
            uri    : "http://127.0.0.1:8080",
            path   : "/device/fullJson/$deviceid",        
            timeout: 20]    
    try {
        
    httpGet(params) { resp ->
      //  log.warn "data = ${resp.data.device}"  
        def isZigbee = resp.data.device.zigbee
        def isZWave = resp.data.device.ZWave
        
      //  log.warn "is zigbee = $isZigbee, isZWave = $isZWave"
        if (isZigbee) return  "zigbee"
        else if (isZWave) return "zwave"
        else return "other"
        
    }
    }
   catch (Exception e) {
        log.error "EXCEPTION CAUGHT: ${e.message} ON LINE ${e.stackTrace.find{it.className.contains("user_")}?.lineNumber}"
    }
}

boolean findZWNodeInDevices(deviceid)
{
  if (debug) log.debug "In find zw nmode in devices id: $deviceid"
    
  def found = false
  def deviceTable = state.hubDevices
    
    if (debug) log.debug "device table size: ${deviceTable.size()}"
    //log.debug "device table $deviceTable"
    
    for (it in deviceTable)
    {
        //if (debug) log.debug "[${it.id}, ${it.displayName}]"
     
        if (it.id == deviceid)
        {
         found = true
         break
        }  
    }
    
    if (found == false)
    {
       if (debug) log.debug "Didn't find it"
    }
  return found
}

boolean isInZwaveTable(deviceid)
{   
    zwdevices = state.zwDevices
    def found = false
    if (debug) log.debug "in is in zwave table size = ${zwdevices.size()}"
    
   for (it in zwdevices)
    {
       //log.debug "key = ${it.key} value = ${it.value}"
      if (debug) log.debug "id = ${it.value.id}"
      if (it.value.id == deviceid)
        {
           if (debug) log.debug "found it"
            found = true
            break
        }
    }
    if (found == false)
    {
       if (debug) log.debug "didn't find it"
    }
    return found
}


boolean isInZigbeeTable(deviceid)
{   
    zigbeedevices = state.zigbeeDevices
    def found = false
    if (debug) log.debug "in is in zigbee table size = ${zigbeedevices.size()}"
    
   for (it in zigbeedevices)
    {

      if (debug) log.debug "id = ${it.id} name = ${it.name}"
      if (it.id == deviceid)
        {
            if (debug) log.debug "found it"
            found = true
            break
        }
    }
    if (found == false)
    {
        if (debug) log.debug "didn't find it"
    }
    return found
}

void compare()
{  
    log.info "LGKahn Device/Zwave/Zigbee Database consistency checker version: ${getVersion()}"
    
    def now = new Date().format('MM/dd/yyyy h:mm:ss a',location.timeZone)
    sendEvent(name: "lastRunStart", value: now)  
    
    state.zwDevices = "" 
    state.zigbeeDevices = ""
    state.hubDevices = ""
    
    sendEvent(name: "numberOfDevices", value: 0)
    sendEvent(name: "numberOfZwaveNodes", value: 0)
    sendEvent(name: "numberOfZigbeeNodes", value: 0)
    sendEvent(name: "devicesNotInZwaveTable", value: 0)
    sendEvent(name: "devicesNotInZigbeeTable", value: 0)
    sendEvent(name: "ghosts", value: 0)
    sendEvent(name: "zwMatchesFound", value: 0)
    sendEvent(name: "zbMatchesFound", value: 0)

    getZwaveTable()
    getZigbeeTable()
    getDeviceList() 
    if (debug) listZWaveTable()
    if (debug) listZigbeeTable()
    compareZWaveTable()
    compareDeviceTable() 
    
    def finish = new Date().format('MM/dd/yyyy h:mm:ss a',location.timeZone)
    sendEvent(name: "lastUpdate", value: finish)
    
    if (clearTableStatesAfterRun)
    {
      log.info "Clearing state variables."
      state.zwDevices = "" 
      state.zigbeeDevices = ""
      state.hubDevices = "" 
    }
 
}

String getVersion()
{
    return "1.1"
}

        
    

