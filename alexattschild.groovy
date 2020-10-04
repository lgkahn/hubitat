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
 *    2020-10-04  lgk add set volume as command so it can be called from rule machione.
 */

attribute "currentVolume", "number"
 capability "Configuration"

 command "setVolume", [[name: "volumeLevel", type: "intteger", range: "1..100"]]

metadata {
    definition (name: "Child Alexa TTS", namespace: "ogiewon", author: "Dan Ogorchock", importUrl: "https://raw.githubusercontent.com/ogiewon/Hubitat/master/AlexaTTS/Drivers/child-alexa-tts.src/child-alexa-tts.groovy") {
        capability "Speech Synthesis"
    }
}


preferences {
	input("volumeLevel", "integer", title: "Volume level for this device (0-99)?", range: "1…100", defaultValue: 75, required: true)
}


def updated()

{ 
    log.debug "in updated current volume = $volumeLevel,  prior volume = $state.currentVolume"
    if (state.currentVolume != volumeLevel)
    {
        log.debug "Resetting Volume"
        sendEvent(name: "currentVolume", value: volumeLevel)
        updateVolume()
    }
}

def updateVolume(newLevel)
{
   log.debug "In update volume ... new level = $newLevel!"

  def name = device.deviceNetworkId.split("-")[-1]
  def vId = device.data.vcId
    
  if(vId) parent.childComm("setVolume", newLevel, vId)
	else parent.setVolume(newLevel.toInteger(), name)   
    
}
def updateVolume()
{
    updateVolume(volumeLevel)
}

def installed()
{
    log.debug "in initialize"
    state.currentVolume = volumeLevel
}

def setVolume(level)
{
     if (level)
    {
      log.debug "In setVolume command: new level = $level"
      sendEvent(name: "currentVolume", value: level)
      state.currentVolume = level
      updateVolume(level)
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
