/**
 *  Child Alexa TTS
 *
 *  https://raw.githubusercontent.com/ogiewon/Hubitat/master/AlexaTTS/Drivers/child-alexa-tts.src/child-alexa-tts.groovy
 *
 *  Copyright 2018 Daniel Ogorchock
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
 *  Change History:
 *
 *    Date        Who             	What
 *    ----        ---             	----
 *    2018-10-20  Dan Ogorchock   	Original Creation
 *    2018-11-18  Stephan Hackett	Added support for Virtual Containers
 *    2020-07-29  nh.schottfam      Remove characters from message text that may cause issues
 *    2020-09-25  lgk add volume
 */


metadata {
    definition (name: "Child Alexa TTS", namespace: "ogiewon", author: "Dan Ogorchock", importUrl: "https://raw.githubusercontent.com/ogiewon/Hubitat/master/AlexaTTS/Drivers/child-alexa-tts.src/child-alexa-tts.groovy") {

        capability "Speech Synthesis"
	capability "Configuration"
	capability "Refresh"

	attribute "level", "number"

	command "initialize"
	command "setLevel", ["number"]
    }
}


preferences {
	input("level", "integer", title: "Volume level for this device (0-100)?", range: "1..100", defaultValue: 75, required: true)
}

def refresh(){
	// this should query current volume setting and match device state to it
	//updateDevState(num)
}

def updateDevState(num){
	num=num!=null ? num.toInteger() : -1
	if(num>=0 && num<=100) {
		sendEvent(name: 'level', value: num)
		device.updateSetting('level', [value:num, type:'integer'])
		state.level = num.toInteger()
	}
}

def updateVolume(num) {
	log.debug "updateVolume($num)"

	num=num!=null ? num.toInteger() : -1
	if(num>=0 && num<=100) {
		def name = device.deviceNetworkId.split("-")[-1]
		def vId = device.data.vcId
		if(vId) parent.childComm("setVolume", num, vId)
		else parent.setVolume(num, name)

		updateDevState(num) // or it could have called refresh to ensure it gets current value
	}
}

def setLevel(num){
    log.debug "setLevel($num)"
    updateVolume(num)
}

def configure() {
}

def updated() { 
	log.debug "updated current volume = $level,  prior volume = $state.level"
	if (state.level != level) {
		updateVolume(level)
	}
}

def installed() {
    log.debug "installed"
    initialize()
}

def initialize(){
	log.debug "initialize"
// really should call refresh() after a runIn
	Integer vol = level!=null ? level.toInteger() : 75
	if (state.level != vol) {
		updateDevState(vol)
	}
}

def speak(message) {
    String nmsg = message.replaceAll(/(\r\n|\r|\n|\\r\\n|\\r|\\n)+/, " ")
    log.debug "Speaking message = '${nmsg}'"
    def name = device.deviceNetworkId.split("-")[-1]
	def vId = device.data.vcId
	if(vId) parent.childComm("speakMessage", nmsg.toString(), vId)
	else parent.speakMessage(nmsg.toString(), name)
}
