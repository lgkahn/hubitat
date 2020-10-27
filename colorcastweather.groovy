/**
 *  ColorCast Weather Lamp
 *
 *	Inspired by and based in large part on the original Color Changing Smart Weather lamp by Jim Kohlenberger.
 *	See Jim's original SmartApp at http://community.smartthings.com/t/color-changing-smart-weather-lamp-app/12046 which includes an option for high pollen notifications
 *	
 *	This weather lantern app turns on with motion and turns a Phillips hue (or LifX) lamp different colors based on the weather.  
 *	It uses dark sky's weather API to micro-target weather. 
 *
 *	Colors definitions
 *
 *	Purple 		Rain: Rain is forecast for specified time period.
 *	Blue 		Cold: It's going to be at or below the specified minimum temperature
 *	Pink		Snow: Snow is forecast for specified time period.
 *	Red 		Hot:  It's going to be at or above the specified maximum temperature
 *	Yellow 		Wind: Wind is forecast to meet or exceed the specified maximum wind speed
 *	Green		All clear
 *	Blinking any color indicates that there is a weather advisory for your location
 *
 *  With special thanks to insights from the SmartThings Hue mood lighting script and the light on motion script by kennyyork@centralite.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * modified by lgk to be able to disable the all clear. ie no light on.
 * location is quite a bit off in smartthings allow location override
 */
// lgk new version with option to decide how often the weather update runs.. used to be every 15 minutes.. Motion triggers still the same.
import java.util.regex.*

definition(
	name: "ColorCast Weather Lamp",
	namespace: "lgkapps",
	author: "Joe DiBenedetto",
	description: "Get a simple visual indicator for the days weather whenever you leave home. ColorCast will change the color of one or more Hue lights to match the weather forecast whenever it senses motion",
	category: "Convenience",
	iconUrl: "http://apps.shiftedpixel.com/weather/images/icons/colorcast.png",
	iconX2Url: "http://apps.shiftedpixel.com/weather/images/icons/colorcast@2x.png",
	iconX3Url: "http://apps.shiftedpixel.com/weather/images/icons/colorcast@2x.png"
)

preferences {
	page(name: "pageAPI", title: "API Key", nextPage: "pageSettings", install: false, uninstall: true) {
	
		section("First Things First") {
			paragraph "To use this SmartApp you need an API Key from forecast.io (https://developer.forecast.io/). To obtain your key, you will need to register a free account on their site."
			paragraph "You will be asked for payment information, but you can ignore that part. Payment is only required if you access the data more than 1,000 times per day. If you don't give a credit card number and you somehow manage to exceed the 1,000 calls, the app will stop working until the following day when the counter resets."
		}
	
		section("API Key") {
			href(name: "hrefNotRequired",
			title: "Get your Forecast.io API key",
			required: false,
			style: "external",
			url: "https://developer.forecast.io/",
			description: "tap to view Forecast.io website in mobile browser")
	
			input "apiKey", "text", title: "Enter your new key", required:true
              input "refreshTime", "enum", title: "How often to refresh?",options: ["Disabled","1-Hour", "30-Minutes", "15-Minutes", "10-Minutes", "5-Minutes"],  required: true, defaultValue: "15-Minutes"
      
		}
	}
	
	page(name: "pageSettings", title: "General Settings", nextPage: "pageTriggers", install: false, uninstall: true) {
		section("Select Motion Detector") {
			input "motion_detector", "capability.motionSensor", title: "Where?", required:false //Select motion sensor(s). Optional because app can be triggered manually
		}
		section("Control these bulbs...") {
			input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true //Select bulbs
			input "brightnessLevel", "number", title: "Brightness Level (1-100)?", required:false, defaultValue:100 //Select brightness
			paragraph	"Do you want to set the light(s) back to the color/level they were at before the weather was displayed? Due to the way SmartThings polls devices this may not always work as expected."
			input (
				name:			"rememberLevel", 
				type:			"bool", 
				title: 			"Remember light settings",
				defaultValue:	true
			)
		}

		section ("Forecast Range") {
			// Get the number of hours to look ahead. Weather for the next x hours will be parsed to compare against user specified values.
			input "forecastRange", "enum", title: "Get weather for the next...", options: [
				"Current conditions", 
				"1 Hour", 
				"2 Hours", 
				"3 Hours", 
				"4 Hours", 
				"5 Hours", 
				"6 Hours", 
				"7 Hours", 
				"8 Hours", 
				"9 Hours", 
				"10 Hours", 
				"11 Hours", 
				"12 Hours", 
				"13 Hours", 
				"14 Hours", 
				"15 Hours", 
				"16 Hours", 
				"17 Hours", 
				"18 Hours", 
				"19 Hours", 
				"20 Hours", 
				"21 Hours", 
				"22 Hours", 
				"23 Hours", 
				"24 Hours"
			], required: true, defaultValue:"Current conditions"
		}

		section() {
			input "tapTrigger", "bool", title: "Enable App Tap Trigger?"
		}

		section([mobileOnly:true]) {
			label title: "Assign a name", required: false //Allow custom name for app. Usefull if the app is installed multiple times for different modes
			mode title: "Set for specific mode(s)", required: false //Allow app to be assigned to different modes. Usefull if user wants different setting for different modes
		}

	}
	page(name: "pageTriggers", title: "Set Weather Triggers", install: true, uninstall: true) {
		def colors=["Blue","Purple","Red","Pink","Orange","Yellow","Green","White"]
		def colorsWithDisabled=["Disabled","Blue","Purple","Red","Pink","Orange","Yellow","Green","White"]
		section ("All Clear") {
			input "allClearColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"Disabled",  multiple:false            
		}
		section ("Low Temperature") {
			input "tempMinTrigger", "number", title: "Low Temperature - °F", required: true, defaultValue:35 //Set the minumum temperature to trigger the "Cold" color
 			input "tempMinType", "enum", title: "Temperature Type", options: [
				"Actual",
				"Feels like"
			], required: true, defaultValue:"Actual" 
			input "tempMinColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"Blue",  multiple:false            
		}
		section ("High Temperature") {
			input "tempMaxTrigger", "number", title: "High Temperature - °F", required: true, defaultValue:80 //Set the minumum temperature to trigger the "Cold" color
			input "tempMaxType", "enum", title: "Temperature Type", options: [
				"Actual",
				"Feels like"
			], required: true, defaultValue:"Actual" 
			input "tempMaxColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"Red",  multiple:false            
		}

		section ("Rain") {      
			input "rainColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"Purple",  multiple:false            
		} 
		section ("Snow") {      
			input "snowColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"Pink",  multiple:false            
		}
		section ("Sleet\r\n(applies to freezing rain, ice pellets, wintery mix, or hail)") {      
			input "sleetColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"Pink",  multiple:false            
		}

		section ("Cloudy") {
			input "cloudPercentTrigger", "number", title: "Cloud Cover %", required: true, defaultValue:50 //Set the minumum temperature to trigger the "Cold" color
			input "cloudPercentColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"White",  multiple:false            
		}
		section ("Dew Point\r\n(Sometimes refered to as humidity)") {
			input "dewPointTrigger", "number", title: "Dew Point - °F", required: true, defaultValue:65 //Set the minumum temperature to trigger the "Cold" color
			input "dewPointColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"Orange",  multiple:false   
			href(name: "hrefNotRequired",
				title: "Learn more about \"Dew Point\"",
				required: false,
				style: "external",
				url: "http://www.washingtonpost.com/blogs/capital-weather-gang/wp/2013/07/08/weather-weenies-prefer-dew-point-over-relative-humidity-and-you-should-too/",
				description: "A Dew Point above 65° is generally considered \"muggy\"\r\nTap here to learn more about dew point"
			)
		}

		section ("Wind") {
			input "windTrigger", "number", title: "High Wind Speed", required: true, defaultValue:24 //Set the minumum temperature to trigger the "Cold" color           
			input "windColor", "enum", title: "Color", options: colorsWithDisabled, required: true, defaultValue:"Yellow",  multiple:false            
		}

		section ("Weather Alerts") {      
			input "alertFlash", "enum", title: "Flash Lights For...", options: [
				"warning":"Warnings", 
				"watch":"Watches", 
				"advisory":"Advisories"
			], required: true, defaultValue:["Warnings","Watches","Advisories"],  multiple:true   
            input "defaultAlertColor", "enum", title: "Color", options: colors, required: true, defaultValue:"Purple",  multiple:false            
	
		}  
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def initialize() {
	log.info "Initializing, subscribing to motion event at ${motionHandler} on ${motion_detector}"
	state.current = []

	getWeather()
    
    log.debug "Refresh time currently set to: $refreshTime"
    unschedule()  
   
    if (refreshTime == "1-Hour")
    schedule("33 0 0/1 * * ?", getWeather)
      else if (refreshTime == "30-Minutes")
     schedule("10 0/30 * * * ?", getWeather)
     else if (refreshTime == "15-Minutes")
      schedule("10 0/15 * * * ?", getWeather)
     else if (refreshTime == "10-Minutes")
     schedule("10 0/10 * * * ?", getWeather)
     else if (refreshTime == "5-Minutes")
     schedule("10 0/5 * * * ?", getWeather)
    else if (refreshTime == "Disabled")
    {
        log.debug "Disabling..."
    }
    
	//schedule("0 0/15 * * * ?", getWeather)

	subscribe(motion_detector, "motion", motionHandler)
	if (tapTrigger) subscribe(app, appTouchHandler)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}

def getWeather() {
	def forecastUrl="https://api.forecast.io/forecast/$apiKey/$location.latitude,$location.longitude?exclude=daily,flags,minutely" //Create api url. Exclude unneeded data 
 log.debug "url = $forecastUrl"
	//Exclude additional unneded data from api url.
	if (forecastRange=='Current conditions') {
		forecastUrl+=',hourly' //If we're checking current conditions we can exclude hourly data
	} else {
		forecastUrl+=',currently' //If we're checking hourly conditions we can exclude current data
	}
	
	if (alertFlash==null) {
		forecastUrl+=',alerts' //If alert event is disabled then we can also exclude alert data
	}
	
	log.debug forecastUrl
	
	httpGet(forecastUrl) {response -> 
		if (response.data) {
			state.weatherData = response.data
			def d = new Date()
			state.forecastTime = d.getTime()
			log.debug("Successfully retrieved weather.")
		} else {
			runIn(60, getWeather)
			log.debug("Failed to retrieve weather.")
		}
	}
}

def checkForWeather() {

    //log.debug "in check for weather"
	def d = new Date()
	if ((d.getTime() - state.forecastTime) / 1000 / 60 > 65)
    {
        
        log.debug "Weather not checked for more than an hour! Rescheduling Job!"
		//unschedule()
		//schedule("0 0/15 * * * ?", getWeather)
        
      log.debug "Refresh time currently set to: $refreshTime"
      unschedule()  
   
      if (refreshTime == "1-Hour")
       schedule("33 0 0/1 * * ?", getWeather)
      else if (refreshTime == "30-Minutes")
       schedule("10 0/30 * * * ?", getWeather)
      else if (refreshTime == "15-Minutes")
       schedule("10 0/15 * * * ?", getWeather)
      else if (refreshTime == "10-Minutes")
       schedule("10 0/10 * * * ?", getWeather)
      else if (refreshTime == "5-Minutes")
       schedule("10 0/5 * * * ?", getWeather)
    else if (refreshTime == "Disabled")
    {
        log.debug "Disabling..."
    }
      if (refreshTime != "Disabled")
         getWeather()
	}

	state.current.clear()
	hues.each {
		state.current.add([switch: it.currentValue('switch'), hue: it.currentValue('hue'), saturation: it.currentValue('saturation'), level: it.currentValue('level')] )
	}

	def colors = [] //Initialze colors array

	//Initialize weather events
	def willRain=false;
	def willSnow=false;
	def willSleet=false;
	def windy=false;
	def tempLow
	def tempHigh
    def cloudy=false;
    def humid=false;
	def weatherAlert=false

	def response = state.weatherData

	if (state.weatherData) { //API response was successfull

		def i=0
			def lookAheadHours=1
			def forecastData=[]
			if (forecastRange=="Current conditions") { //Get current weather conditions
				forecastData.push(response.currently)
			} else {
				forecastData=response.hourly.data
				lookAheadHours=forecastRange.replaceAll(/\D/,"").toInteger()
			}
		//log.debug "got forcasedata = $forcastData"
        
		for (hour in forecastData){ //Iterate over hourly data
			if (lookAheadHours<++i) { //Break if we've processed all of the specified look ahead hours. Need to strip non-numeric characters(i.e. "hours") from string so we can cast to an integer
				break
			} else {
             // log.debug "in weather loop hour = $hour"
				if (snowColor!='Disabled' || rainColor!='Disabled' || sleetColor!='Disabled') {
					if (hour.precipProbability.floatValue()>=0.15) { //Consider it raining/snowing if precip probabilty is greater than 15%
						if (hour.precipType=='rain') {
							willRain=true //Precipitation type is rain
						} else if (hour.precipType=='snow') {
							willSnow=true
						} else {
							willSleet=true
						}
					}
				}
			
				if (tempMinColor!='Disabled') {
					if (tempMinType=='Actual') {
						if (tempLow==null || tempLow>hour.temperature) tempLow=hour.temperature //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
					} else {
						if (tempLow==null || tempLow>hour.apparentTemperature) tempLow=hour.apparentTemperature //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
					}
				}
			
				if (tempMaxColor!='Disabled') {
					if (tempMaxType=='Actual') {
						if (tempHigh==null || tempHigh<hour.temperature) tempHigh=hour.temperature //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
					} else {
						if (tempHigh==null || tempHigh<hour.apparentTemperature) tempHigh=hour.apparentTemperature //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
					}
				}
			log.debug "hour windspeed = $hour.windSpeed!"
            
				if (windColor!='Disabled' && hour.windSpeed>=windTrigger) windy=true //Compare to user defined value for wid speed.
				if (cloudPercentColor!='Disabled' && hour.cloudCover*100>=cloudPercentTrigger) cloudy=true //Compare to user defined value for wind speed.
				if (dewPointColor!='Disabled' && hour.dewPoint>=dewPointTrigger) humid=true //Compare to user defined value for wind speed.
			}
		}
// log.debug "after hourly loop"
 log.debug "min temp = $tempLow"
 log.debug "max temp = $tempHigh"
  log.debug "done with hour data number of alerts in report = $response.alerts!"
   
		if (response.alerts) { //See if Alert data is included in response
            log.debug "in alert loop!"
			response.alerts.each { //If it is iterate through all Alerts
				def thisAlert=it.title;
				log.debug thisAlert
				alertFlash.each{ //Iterate through all user specified alert types
                  log.debug "in alert this alert = $thisAlert it = $it!"
                  
					if (thisAlert.toLowerCase().indexOf(it)>=0) { //If this user specified alert type matches this alert response
						log.debug "ALERT: "+it
                        
                        // try to find color for specific event based on snow wind sleet rain etc.
                      if ((thisAlert.toLowerCase().indexOf('wind chill') >= 0) && (tempMinColor != 'Disabled'))
                          {
                            log.debug "Found alert of type min temp!"
                            colors.push(tempMinColor)
                            log.debug "wind chill/temp min"
                            log.debug tempMinColor
                           }
                          
                      if ((thisAlert.toLowerCase().indexOf('wind') >= 0) && (thisAlert.toLowerCase().indexOf('wind chill') == -1)  && (windColor != 'Disabled'))
                          {
                            log.debug "Found alert of type Wind!"
                            colors.push(windColor)
                            log.debug "Wind"
                            log.debug windColor
                           }
                           
                        if ((thisAlert.toLowerCase().indexOf('rain') >= 0) && (rainColor != 'Disabled'))
                        
                          {
                            log.debug "Found alert of type Rain!"
                            colors.push(rainColor)
                            log.debug "Rain"
                            log.debug rainColor
                           }
                   if ((thisAlert.toLowerCase().indexOf('Severe Thunderstorm') >= 0) && (rainColor != 'Disabled'))
                        
                          {
                            log.debug "Found alert of type Severe Thunderstorm!"
                            colors.push(rainColor)
                            log.debug "Rain"
                            log.debug rainColor
                           }             
                        if ((thisAlert.toLowerCase().indexOf('snow') >= 0) && (snowColor != 'Disabled'))
                          {
                            log.debug "Found alert of type Snow!"
                            colors.push(snowColor)
                            log.debug "Snow"
                            log.debug snowColor
                           }
                           
                        if ((thisAlert.toLowerCase().indexOf('sleet') >= 0) && (sleetColor != 'Disabled'))
                          {
                            log.debug "Found alert of type Sleet!"
                            colors.push(windColor)
                            log.debug "Sleet"
                            log.debug sleetColor
                           }
                           
						weatherAlert=true //Is there currently a weather alert
					}
				}
			}
		}

		log.debug "after alerts weatherAlert = $weatherAlert"
		//Add color strings to the colors array to be processed later
		if (tempMinColor!='Disabled' && tempLow<=tempMinTrigger.floatValue()) {
			colors.push(tempMinColor)
			log.debug "Cold"
			log.debug tempMinColor
		}
		if (tempMaxColor!='Disabled' && tempHigh>=tempMaxTrigger.floatValue()) {
			colors.push(tempMaxColor)
			log.debug "Hot"
			log.debug tempMaxColor
		}
		if (humidityColor!='Disabled' && humid) {
			colors.push(dewPointColor)
			log.debug "Humid"
			log.debug dewPointColor
		}
		if (snowColor!='Disabled' && willSnow) {
			colors.push(snowColor)
			log.debug "Snow"
			log.debug snowColor			
		}
		if (sleetColor!='Disabled' && willSleet) {
			colors.push(sleetColor)
			log.debug "Sleet"
			log.debug sleetColor			
		}
		if (rainColor!='Disabled' && willRain) {
			colors.push(rainColor)
			log.debug "Rain"
			log.debug rainColor
		}
		if (windColor!='Disabled' && windy) {
			colors.push(windColor)
			log.debug "Windy"
			log.debug windColor
		}
		if (cloudPercentColor!='Disabled' && cloudy) {
			colors.push(cloudPercentColor)
			log.debug "Cloudy"
			log.debug cloudPercentColor
		}
	}
    
   if ((colors.size() == 0) && (weatherAlert == true))
     {
       log.debug "Found a weather alert, but no specific class of weather triggererd!"  
       log.debug "trying to find color for specific alert event!"  
       log.debug "Setting color to default Alert Color = $defaultAlertColor"
       colors.push(defaultAlertColor)
   }
   
	//If the colors array is empty, assign the "all clear" color
	if ((colors.size() > 0) || ((colors.size() == 0) && (allClearColor != 'Disabled')))
    {
    
    if (colors.size()==0)
    {
   		 colors.push(allClearColor)
    }
    
	colors.unique()
	log.debug colors


	def delay=2000 //The amount of time to leave each color on
	def iterations=1 //The number of times to show each color
	if (weatherAlert) {
         log.debug "Weather Alert!"
		//When there's an active weather alert, shorten the duration that each color is shown but show the color multiple times. This will cause individual colors to flash when there is a weather alert
		delay = 600
		iterations=5
	}
	
	colors.each { //Iterate over each color
		for (int i = 0; i<iterations; i++) {
			sendcolor(it) //Turn light on with specified color
			pause(delay) //leave the light on for the specified time
			if (weatherAlert) {
				//If there's a weather alert, turn off the light for the same amount of time it was on
				//When a weather alert is active, each color will be looped x times, creating the blinking effect by turning the light on then off x times
                
				hues.off()
				pause(delay)
			}
		}
        // extra off
        hues.off()
        hues.off()
	}
	}
	setLightsToOriginal() //The colors have been sent to the lamp and all colors have been shown. Now revert the lights to their original settings
}

def sendcolor(color) {
	//Initialize the hue and saturation
	def hueColor = 0
	def saturation = 100

	//Use the user specified brightness level. If they exceeded the min or max values, overwrite the brightness with the actual min/max
	if (brightnessLevel<1) {
		brightnessLevel=1
	} else if (brightnessLevel>100) {
		brightnessLevel=100
	}

	//Set the hue and saturation for the specified color.
	switch(color) {
		case "White":
			hueColor = 0
			saturation = 0
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 
			break;
		case "Blue":
			hueColor = 65
			break;
		case "Green":
			hueColor = 33
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 82
			saturation = 100
			break;
		case "Pink":
			hueColor = 90.78
			saturation = 67.84
			break;
		case "Red":
			hueColor = 0
			break;
	}

	//Change the color of the light
	def newValue = [hue: hueColor, saturation: saturation, level: brightnessLevel]  
	hues*.setColor(newValue)
}

def setLightsToOriginal() {
	if (rememberLevel) {    
		hues.eachWithIndex { it, i -> 
			it.setColor(state.current[i])
			log.debug ("RESET: " + state.current[i])
           // huses.off()
		}
	} else {
		hues.off()
	}
}

/// HANDLE MOTION
def motionHandler(evt) {
    //log.debug "in motion handler"
	if (evt.value == "active") {// If there is movement then trigger the weather display
		log.debug "Motion detected, turning on light"
		checkForWeather()
	} 
}

def appTouchHandler(evt) {// If the button is pressed then trigger the weather display
	checkForWeather()	
	log.debug "App triggered with button press."
}
