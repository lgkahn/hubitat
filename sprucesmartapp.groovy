/**
 *  SmartApp that will turn a Spruce zone on and off based on Spruce sensor moisture level and (optionally) daily
 *  Spruce specific commands:
 *		zone commands: z1on, z1off, z2on, z2off, z3on, z3off, ... 			// up to 16
 *		controller commands: programOn, programEnd 							// interactions with standard Spruce scheduler SmartApp
 * 							 notify(status,message)							// updates controller status display
 *		zone events: switch1.z1off, switch2.z2off, switch3.z3off, ...		// up to 16 - allows for manual zone shutoff
 * 9/3/20 port to hubit lg kahn  	
 */
definition(
    name: "Spruce Smart Zone",
    namespace: "plaidsystems",
    author: "plaidsystems",
    description: "Spruce zone controlled by Spruce moisture sensor. Runs a single zone whenever soil moisture falls below set percentage.",
    category: "My Apps",
    iconUrl: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    iconX2Url: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    iconX3Url: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png")

preferences {
	page(name: 'startPage')
}

def startPage() {
	dynamicPage(name: 'startPage', title: 'Spruce Smart Zone setup V1.01', install: true, uninstall: true) {
		section('') {
            paragraph(image: 'http://www.plaidsystems.com/smartthings/st_settings.png', title: 'Smart Zone settings', required: false,
					  '')
    		label(title: 'Smart Zone Name:', description: 'Name this schedule', required: false)
			input('controller', 'capability.switch', title: 'Spruce Irrigation Controller:', description: 'Select a Spruce controller',
				required: true, multiple: false)
       		input('enable', 'bool', title: 'Enable watering:', defaultValue: 'true', metadata: [values: ['true', 'false']])
       		input('startTime', 'time', title: 'Daily Watering time (optional)', required: false)            
		}
	    
   	 	section('') {
			paragraph(image: 'http://www.plaidsystems.com/smartthings/st_flowers_225_r.png', title: 'Smart Zone Control', required: false,
					  '')
			input(name: "zone", title: "Zone Number", multiple: false, 
				metadata: [values: ['1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16']], type: "enum")
		}
		
		section('') {
			paragraph(image: 'http://www.plaidsystems.com/smartthings/st_sensor_200_r.png', title: 'Moisture Sensor Setup', required: false,
					  '')
        	input("sensor", 'capability.relativeHumidityMeasurement', title: 'Spruce Moisture Sensor', value: "humidity", 
				required: true, multiple: false)
            int lMin = 6
			input(name: "low", title: "Turn On when moisture is below (5-59%)", type: 'number', range: "5..59",
				required: true, submitOnChange: true)
			input(name: "high", type: "number", title: "Turn Off when moisture reaches (${(low? low+1 : lMin)}-60%)", range: "${(low? low+1 : lMin)}..60", required: false)
		}
		
		section('') {
        	paragraph(image: 'http://www.plaidsystems.com/smartthings/st_timer.png', title: 'Maximum Watering Duration\n(1-60 minutes)',
					  required: false, '')
			input(name: "duration", title: "Duration?", type: "number", range: "1..60")
		}
	
		section(''){
    		paragraph(image: 'http://www.plaidsystems.com/smartthings/st_pause.png', title: 'Pause Control Contacts & Switches (optional)',
					  required: false, 'When any selected contact sensor is opened or closed, or any selected switch is ' +
            		  'toggled, watering immediately stops and will not resume until all of the contact sensors and ' +
            		  'switches are reset.\n\nCaution: if all contacts or switches are left in the stop state, the dependent ' +
            		  'schedule(s) will never run.')
        	input(name: 'contacts', title: 'Pause watering contact sensors', type: 'capability.contactSensor', multiple: true, 
            	required: false, submitOnChange: true)        
			if (contacts)
				input(name: 'contactStop', title: 'Pause watering when sensors are...', type: 'enum', required: (settings.contacts != null), 
					options: ['open', 'closed'], defaultValue: 'open')
			input(name: 'toggles', title: 'Pause watering switches', type: 'capability.switch', multiple: true, required: false, 
				submitOnChange: true)
			if (toggles) 
				input(name: 'toggleStop', title: 'Pause watering when switches are...', type: 'enum', 
					required: (settings.toggles != null), options: ['on', 'off'], defaultValue: 'off')
			input(name: 'contactDelay', type: 'number', title: 'Restart watering how many seconds after all contacts and switches ' +
				'are reset? (minimum 10s)', defaultValue: '10', required: false)
		}
		
		section(''){
    		paragraph(image: 'http://www.plaidsystems.com/smartthings/st_spruce_controller_250.png',
        			  title: 'Controller Synchronization', required: false,  
           			  'For multiple controllers only.  This schedule will wait for the selected controller to finish before ' +
            		  'starting. Do not set with a single controller!')
       		input(name: 'sync', type: 'capability.switch', title: 'Monitored Spruce Controller', 
				description: 'Only use this setting with multiple controllers', required: false, multiple: false)
		 
			paragraph(title: 'Push Notifications', required: false, '') 
			input(name: 'notify', type: 'enum', title: 'Select what push notifications to receive.', required: false, 
                	multiple: true, metadata: [values: ['Delays', 'Warnings', 'Events']])
          //  input('recipients', 'contact', title: 'Send push notifications to', required: false, multiple: true)
         input "sendPushMessage", "capability.notification", title: "Notification Devices: Hubitat PhoneApp or Pushover", multiple: true, required: false

            input(name: 'logAll', type: 'bool', title: 'Log all notices to Hello Home?', defaultValue: 'false', options: ['true', 'false'])
        } 
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()    
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    initialize()	             
}

def initialize(){
	if (atomicState.run == null)	 atomicState.run = false			// true once we've started
	if (atomicState.delayed == null) atomicState.delayed = false		// true if either this controller or sync controller is busy
	if (atomicState.paused == null)	 atomicState.paused = false			// true if watering paused by contact/switch
	
	unschedule()
    unsubscribe()
	subscribe(app, appTouch)
	
	log.debug "${app.label}: ${startTime}, ${low}, ${high}"
	
    String sched = ''
    if (enable && (startTime != null)) {		//if time is set, schedule every day
    	def runTime = timeToday(startTime, location.timeZone)
        schedule(runTime, startWatering)
        sched = " Scheduled and"
    }
	if (enable) {
    	subscribe(sensor, "humidity", humidityHandler)
    	note('schedule', "${app.label}:${sched} Monitoring ${sensor.displayName}", 'e')
    }
    else note('disable', "${app.label}: Disabled", 'e')
}

// enable the "Play" button in SmartApp list
def appTouch(evt) {
	log.debug "appTouch"
	startWatering()
}

//called whenever sensor reports humidity value
def humidityHandler(evt){ 
	log.debug "Soil Moisture is ${evt.value}%"   
    if (evt.numberValue < low) startWatering()
    else if ( high && (evt.numberValue >= high) && (atomicState.run || atomicState.delayed || atomicState.paused)) stopWatering()
}

// called to start watering cycle
def startWatering() {
    if (atomicState.run || atomicState.delayed) return	// don't start this twice
    if (!enable) return
    
    // make sure that the sensor has reported within the last 48 hours
   	int hours = 48
   	def yesterday = new Date(now() - (/* 1000 * 60 * 60 */ 3600000 * hours).toLong())  
   	def lastHumDate = settings.sensor.latestState('humidity').date
   	if (lastHumDate < yesterday)
   		note('warning', "${app.label}: Please check sensor ${settings.sensor}, no humidity reports for ${hours}+ hours", 'w')

	if (sensor.currentHumidity > low) return	// don't need to water right now either
	
	// Is the controller busy?
	if ((controller.currentSwitch != 'off') || (controller.currentStatus == 'pause')) {
		log.debug "watering delayed, ${controller.displayName} busy"
		atomicState.delayed = true
		subscribe(controller, "switch.off", endDelay)
		// we don't change the status so that we don't disrupt the running schedule (since we aren't in control yet)
		note('delayed', "${app.label}: Waiting for current schedule to complete", 'd')
		return
	}
	
	// Is the sync controller busy?
	if (settings.sync) {
		if ((settings.sync.currentSwitch != 'off') || settings.sync.currentStatus == 'pause') {
			log.debug "watering sync delayed, ${sync.displayName} busy"
			atomicState.delayed = true
           	subscribe(settings.sync, 'switch.off', syncOn)
			note('delayed', "${app.label}: Waiting for ${settings.sync.displayName} to complete", 'd')
			return
		}
	}

	// looks like we are good to go
	atomicState.run = true
	
	if (isWaterPaused()) {
		// we have to check this first, in case a pause was effected while no other schedule was running
		String pauseList = getWaterPauseList()
		log.debug "watering paused, ${pauseList}"
		subWaterUnpause()
		controller.programWait()	// Make sure that the status reflects that we are waiting
		note('pause', "${app.label}: Watering paused, ${pauseList}", 'd')
		return
	}

	log.debug "watering starting"
	controller.programOn()
	if (high) subscribe(sensor, "humidity", humidityHandler) 
	subWaterPause()
    subscribe(controller, "switch${zone}.z${zone}off", zoneOffHandler)		// watch for zone being manually turned off
	controller."z${zone}on"()
	atomicState.startTime = now()
	atomicState.pauseSecs = 0
	runIn(duration * 60, stopWatering)    //sets off time
	String s = ''
	if (duration > 1) s = 's'
	note("active", "${app.label}: Zone ${zone} turned on for ${duration} min${s}", 'e')
}

// called when a running schedule turns off the controller 
def endDelay(evt) {
	unsubscribe(controller)
	atomicState.delayed = false
	Random rand = new Random() 						// just in case there are multiple schedules waiting on the same controller
	int randomSeconds = rand.nextInt(120) + 15
    runIn(randomSeconds, startWatering)	
}
							  
def stopWatering() {
	unsubscribe()
    if (enable) subscribe(sensor, "humidity", humidityHandler)
	atomicState.delayed = false
	atomicState.paused = false
	if (atomicState.run) {
		atomicState.run = false
		controller."z${zone}off"()
		note("finished", "${app.label}: Zone ${zone} turned off", 'e')
		controller.programEnd()
	}
}

def zoneOffHandler(evt) {
	atomicState.run = false
	unsubscribe(controller)
	unsubWaterPausers()
	note("finished", "${app.label}: Zone ${zone} was manually turned off", 'e')
	controller.programEnd()
}

// true if one of the stoppers is in Stop state
private boolean isWaterPaused() {
	if (settings.contacts && settings.contacts.currentContact.contains(settings.contactStop)) return true
	if (settings.toggles && settings.toggles.currentSwitch.contains(settings.toggleStop)) return true
	return false
}

// watch for water stoppers
private def subWaterPause() {
	if (settings.contacts) {
		unsubscribe(settings.contacts)
		subscribe(settings.contacts, "contact.${settings.contactStop}", waterPause)
	}
	if (settings.toggles) {
		unsubscribe(settings.toggles)
		subscribe(settings.toggles, "switch.${settings.toggleStop}", waterPause)
	}
}

// watch for water starters
private def subWaterUnpause() {
	if (settings.contacts) {
		unsubscribe(settings.contacts)
		def cond = (settings.contactStop == 'open') ? 'closed' : 'open'
		subscribe(settings.contacts, "contact.${cond}", waterUnpause)
	}
	if (settings.toggles) {
		unsubscribe(settings.toggles)
		def cond = (settings.toggleStop == 'on') ? 'off' : 'on'
		subscribe(settings.toggles, "switch.${cond}", waterUnpause)
	}
}

// stop watching water stoppers and starters
private def unsubWaterPausers() {
	if (settings.contacts) 	unsubscribe(settings.contacts)
	if (settings.toggles) 	unsubscribe(settings.toggles)
}

// which of the stoppers are in stop mode?
private String getWaterPauseList() {
	String deviceList = ''
	int i = 1
	if (settings.contacts) {
		settings.contacts.each {
			if (it.currentContact == settings.contactStop) {
				if (i > 1) deviceList += ', '
				deviceList = "${deviceList}${it.displayName} is ${settings.contactStop}"
				i++
			}
		}
	}
	if (settings.toggles) {
		settings.toggles.each {
			if (it.currentSwitch == settings.toggleStop) {
				if (i > 1) deviceList += ', '
				deviceList = "${deviceList}${it.displayName} is ${settings.toggleStop}"
				i++
			}
		}
	}
	return deviceList
}

// called after a pause to continue the interrupted watering session
def restartWatering() {
	if (!isWaterPaused()) {					// make sure we weren't paused while we were waiting to run
		atomicState.paused = false
		atomicState.pauseSecs += Math.round((now() - state.pauseTime) / 1000)
		atomicState.pauseTime = null
		
		log.debug "restart watering"
		controller.programOn()
		if (high) subscribe(sensor, "humidity", humidityHandler)    	
    	subscribe(controller, "switch${zone}.z${zone}off", zoneOffHandler)		// watch for zone being manually turned off
		controller."z${zone}on"()
		
		def secsLeft = atomicState.timeRemaining
		if (secsLeft < 10) secsLeft = 10
		runIn(secsLeft, stopWatering)    //sets off time
		String s = ''
		if (secsLeft > 1) s = 's'
		note("active", "${app.label}: Zone ${zone} unpaused for ${secsLeft} more sec${s}", 'd')
	}
}
							  
// handle end of pause session     
def waterUnpause(evt){
	if (!isWaterPaused()){ 					// only if ALL of the selected contacts are not open
		def cDelay = 10
        if (settings.contactDelay > 10) cDelay = settings.contactDelay
        runIn(cDelay, restartWatering)
		
		// unsubscribe(settings.controller)
		subWaterPause()							// allow stopping again while we wait for cycleOn to start
		
		log.debug "waterUnpause(): enabling device is ${evt.device} ${evt.value}"
		
		String cond = evt.value
		switch (cond) {
			case 'open':
				cond = 'opened'
				break
			case 'on':
				cond = 'switched on'
				break
			case 'off':
				cond = 'switched off'
				break
			//case 'closed':
			//	cond = 'closed'
			//	break
			case null:
				cond = '????'
				break
			default:
				break
		}
        note('pause', "${app.label}: ${evt.displayName} ${cond}, watering in ${cDelay} seconds", 'd')
	} 
	else {
		log.debug "waterUnpause(): one down - ${evt.displayName}"
	}
}
							  
// handle start of pause session
def waterPause(evt){
	log.debug "waterStop: ${evt.displayName}"

	if (!atomicState.paused) {
		unsubscribe(settings.controller)	// so we can turn off the zone without ending the program
		unschedule(startWatering)			// in case we got stopped again before we restart watering
		unschedule(restartWatering)
		unschedule(stopWatering)

		subWaterUnpause()
		atomicState.paused = true
		atomicState.pauseTime = now()			// figure out how much time is left
		atomicState.timeRemaining = (duration * 60) - Math.round(((now() - automicState.startTime) / 1000) - atomicState.pauseSecs)
		
		String cond = evt.value
		switch (cond) {
			case 'open':
				cond = 'opened'
				break
			case 'on':
				cond = 'switched on'
				break
			case 'off':
				cond = 'switched off'
				break
			//case 'closed':
			//	cond = 'closed'
			//	break
			case null:
				cond = '????'
				break
			default:
				break
		}
	    note('pause', "${app.label}: Watering paused - ${evt.displayName} ${cond}", 'd') // set to Paused
	}
	
	if ( controller.currentValue("switch${zone}") != "z${zone}off" ) {
		runIn(30, subOff)
		controller."z${zone}off"()								// stop the water
	}
	else 
		subscribe(controller, "switch${zone}.z${zone}off", zoneOffHandler) // allow manual off while paused
}
							  
// This is a hack to work around the delay in response from the controller to the above programOff command...
// We frequently see the off notification coming a long time after the command is issued, so we try to catch that so that
// we don't prematurely exit the cycle.
def subOff() {
	subscribe(controller, "switch${zone}.z${zone}off", zoneOffHandler)
}

def syncOn(evt){
	// double check that the switch is actually finished and not just paused
	if ((settings.sync.currentSwitch == 'off') && (settings.sync.currentStatus != 'pause')) {
		atomicState.delayed = false
		unsubscribe(settings.sync)
    	Random rand = new Random() 						// just in case there are multiple schedules waiting on the same controller
		int randomSeconds = rand.nextInt(120) + 15
    	runIn(randomSeconds, waterStart)					// no message so we don't clog the system
    	note('schedule', "${app.label}: ${settings.sync} finished, starting in ${randomSeconds} seconds", 'd')
	} // else, it is just pausing...keep waiting for the next "off"
}

//notifications to device, pushed if requested
def note(String statStr, String msg, String msgType) {

	// send to debug first (near-zero cost)
	log.debug "${statStr}: ${msg}"

	// notify user second (small cost)
	boolean notifyController = true
    if(settings.notify || settings.logAll) {
    	String spruceMsg = "Spruce ${msg}"
    	switch(msgType) {
		case 'd':
  				if (settings.notify && settings.notify.contains('Delays')) {
      				sendIt(spruceMsg)
      			}
      			else if (settings.logAll) {
      				sendNotificationEvent(spruceMsg)
      			}
      			break
      		case 'e':
      			if (settings.notify && settings.notify.contains('Events')) {
      				sendIt(spruceMsg)
      				//notifyController = false					// no need to notify controller unless we don't notify the user
      			}
      			else if (settings.logAll) {
      				sendNotificationEvent(spruceMsg)
      			}
      			break
      		case 'w':
      			notifyController = false						// no need to notify the controller, ever
      			if (settings.notify && settings.notify.contains('Warnings')) {
      				sendIt(spruceMsg)
      			} else
      				sendNotificationEvent(spruceMsg)					// Special case - make sure this goes into the Hello Home log, if not notifying
      			break
      		default:
      			break
	  	}
    }
	// finally, send to controller DTH, to change the state and to log important stuff in the event log
	if (notifyController) {		// do we really need to send these to the controller?
		// only send status updates to the controller if WE are running, or nobody else is
		if (atomicState.run || ((settings.controller.currentSwitch == 'off') && (settings.controller.currentStatus != 'pause'))) {
    		settings.controller.notify(statStr, msg)

		}	
		else { // we aren't running, so we don't want to change the status of the controller
			// send the event using the current status of the switch, so we don't change it 
			//log.debug "note - direct sendEvent()"
			settings.controller.notify(settings.controller.currentStatus, msg)

	  	}
    }
}


def sendIt(String msg) {
 sendPushMessage.deviceNotification(msg)
}
