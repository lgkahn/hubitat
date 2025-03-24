/*
Custom Laundry monitor device for Aeon HEM V1 
* 3/25 modified by lgkahn add preferences to enable either dryer or washer tracking and check so as not to do extra send events
* also check current dryer and washer state so as not to send extra on events when already on so as not to trigger extra rule firings.
* also add dryer and washer ignore watts setting default 15000 so as to ignore eroneous data.
* also updated fx to dump out status when saving preferences. Also add missing voltage update before washer/dryer turn off so you can see event.
*
*/

metadata {
	definition (name: "Aeon HEM V1 Laundry DTH", namespace:	"MikeMaxwell", author: "Mike Maxwell/lgkahn") 
	{
		capability "Configuration"
		capability "Switch"
        capability "PushableButton"
        //capability "Energy Meter"

        attribute "washerWatts", "string"
        attribute "dryerWatts", "string"
        attribute "washerState", "string"
        attribute "dryerState", "string"
        
        command "configure"
        
		fingerprint deviceId: "0x2101", inClusters: " 0x70,0x31,0x72,0x86,0x32,0x80,0x85,0x60"
	}

	preferences {
       	input name: "washerRW", type: "number", title: "Washer running watts:", description: "", defaultValue: 300, required: true
        input name: "dryerRW", type: "number", title: "Dryer running watts:", description: "", defaultValue: 300, required: true
        input name: "dryerIgnoreRW", type: "number", title: "Over this many watts ignore for dryer?", description: "", defaultValue: 15000, required: true
        input name: "washerIgnoreRW", type: "number", title: "Over this many watts ignore for washer?", description: "", defaultValue: 15000, required: true
        input name: "debug", type: "bool", title: "Enable debug logging?", required: true, defaultValue: false
        input name: "enableWasherTracking", type: "bool", title: "Enable washer Tracking?", required: true, defaultValue: false
        input name: "enableDryerTracking", type: "bool", title: "Enable dryer Tracking?", required: true, defaultValue: false     
    }
}

def installed() {
	configure()					
}

def updated()
{
    log.info "Dryer hem updated"
    if (debug) log.info "Debug on"
    else log.info "Debug off"
    
    if (enableWasherTracking)
      {
        log.info "Tracking washer status and operation"
        log.info "Washer running Watts set to $washerRW"
        log.info "Washer Ignore watts set to $washerIgnoreRW"
      }
    
    if (enableDryerTracking)
      {
        log.info "Tracking dryer status and operation"
        log.info "Dryer running Watts set to $dryerRW"
        log.info "Dryer Ignore watts set to $dryerIgnoreRW"
      }
}
    
def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x32: 1, 0x60: 3])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	if (result) { 
		if (debug) log.debug "Parse returned ${result?.descriptionText}"
		return result
	} else {
	}
}

def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	if (debug) log.info "mc3v cmd: ${cmd}"
	if (cmd.commandClass == 50) {  
    def cmd2 = cmd.encapsulatedCommand([0x31: 1, 0x32: 1, 0x60: 3])
    	def encapsulatedCommand = cmd.encapsulatedCommand([0x30: 1, 0x31: 1, 0x60: 3])
     // if (debug) log.debug "cmd 2 = $cmd2"
      if (encapsulatedCommand) {
      		def endpt = cmd.sourceEndPoint
        	def scale = encapsulatedCommand.scale
        	def value = encapsulatedCommand.scaledMeterValue
            def source = cmd.sourceEndPoint
            def mt = encapsulatedCommand.meterType
           if (debug) log.debug "mt = $mt value = $value, scale = $scale, source = $source, endpt = $endpt"
			def str = ""
            def name = ""
            def desc = ""
        	if (scale == 2 ){ //watts
            	str = "watts"
                if (source == 1)
                {
                // washer tracking
                    if (enableWasherTracking == true)
                    {
                      //  log.debug "in washer case"
                	  name = "washerWatts"
                      desc = "Washer power is " + value + " Watts"
                      def washerState = device.currentValue("WasherState") 
                      if (debug) log.debug "washer current state = $washerState" 
                        
                      if (value >= settings.washerRW.toInteger())
                        {
                         if (washerState == "off")
                            {
                    	     //washer is on
                             if (debug) log.debug "Washer turned on"
                             sendEvent(name: "washerState", value: "on", displayed: false)
                             state.washerIsRunning = true
                            }
                        } else 
                        {
                          //washer is off
                          if (state.washerIsRunning == true)
                            {
                              if (washerState == "on")
                                {
                        	    //button event
                                if (debug) log.debug "Washer turned off"
                                sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Washer has finished.( watts: $value)", isStateChange: true)
                                }
                            }
                          if (washerState == "on")
                            {   
                             sendEvent(name: "washerState", value: "off", displayed: false)
                             state.washerIsRunning = false
                            }
                        }
                    } 
                } else {
                    if (enableDryerTracking == true)
                     {
                      //  log.debug "in dryer case"
                	  name = "dryerWatts"
                      desc = "Dryer power is " + value + " Watts"   
                      def dryerState = device.currentValue("dryerState")
                      if (debug) log.debug "dryer current state = $dryerState"

                      if ((value >= settings.dryerRW.toInteger()) && (value < settings.dryerIgnoreRW.toInteger()))
                        {
                      
                        //if (debug) 
                        if (dryerState == "off")
                        {
                          log.info "Dryer turned on"
                    	  //dryer is on
                          sendEvent(name: "dryerState", value: "on", displayed: false)
                        }
                        state.dryerIsRunning = true
                        } else
                        {
                          if (state.dryerIsRunning == true)
                            {
                              if (dryerState == "on")
                                { 
                                  log.info "Dryer turned off"
                                  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Dryer has finished. (watts: $value)", isStateChange: true)
                                }
                            }
                          if (dryerState == "on")
                             { 
                              sendEvent(name: "dryerState", value: "off", displayed: false)
                              state.dryerIsRunning = false
                             }
                        }
                     }
                }
                if ((state.dryerIsRunning) && (enableDryerTracking == true))
                {
                    if (debug) log.debug "Dryer has started"
                	sendEvent(name: "switch", value: "on", descriptionText: "Dryer has started...", displayed: true)
                }
                if ((state.washerIsRunning) && (enableWasherTracking == true))
                {
                    if (debug) log.debug "Washer has started"
                	sendEvent(name: "switch", value: "on", descriptionText: "Washer has started...", displayed: true)
                }
                if  ((state.dryerIsRunning == false) && (state.washerIsRunning == false))
                {
                    if (device.currentValue("switch") == "on")
                      {
                        if (debug) log.debug "Global switch turned off"
                	    sendEvent(name: "switch", value: "off", displayed: false)
                      }
                }
                //log.debug "mc3v- name: ${name}, value: ${value}, unit: ${str}"
                if (debug) "returning $name ${value}"
                // missing 0 event
            	return [name: name, value: value.toInteger(), unit: str, displayed: true, descriptionText: desc]
            }
        }
    }
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
    //log.debug "Unhandled event ${cmd}"
	[:]
}

def configure() {
    log.debug "configure() v2"
    
	def cmd = delayBetween([
    	//zwave.configurationV1.configurationSet(parameterNumber: 100, size: 4, scaledConfigurationValue:1).format(),	//reset if not 0
        //zwave.configurationV1.configurationSet(parameterNumber: 110, size: 4, scaledConfigurationValue: 1).format(),	//reset if not 0
//        	zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 6912).format(), 
//zwave.configurationV1.configurationSet(parameterNumber: 255, size: 4, scaledConfigurationValue: 1).format(), 
// was above.. not sure what that is only want watts ie 512
	
  //  zwave.configurationV1.configurationSet(parameterNumber: 100, size: 4, scaledConfigurationValue:1).format(),	//reset if not 0
   //     zwave.configurationV1.configurationSet(parameterNumber: 110, size: 4, scaledConfigurationValue: 1).format(),	//reset if not 0
    	zwave.configurationV1.configurationSet(parameterNumber: 1, size: 2, scaledConfigurationValue: 120).format(),		// assumed voltage
		zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 0).format(),			// Disable (=0) selective reporting
		zwave.configurationV1.configurationSet(parameterNumber: 9, size: 1, scaledConfigurationValue: 10).format(),			// Or by 10% (L1)
      	zwave.configurationV1.configurationSet(parameterNumber: 10, size: 1, scaledConfigurationValue: 10).format(),			// Or by 10% (L2)
		zwave.configurationV1.configurationSet(parameterNumber: 20, size: 1, scaledConfigurationValue: 1).format(),			//usb = 1
        zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 512).format(),  
        zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 256).format(),
        zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 120).format(), 		// Every 2 minutes seconds
        zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 120).format() 
	], 2000)

	return cmd
}
