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
// lgk new version with options for number of blinks and delay settings for alerts. Also remove tap trigger .. not avail in hubitat on apps
// alert reporting has changed so also search through the overall alert description for words like snow, sleet etc.. also fix wrong color pushed for sleet.
// lgk new version 2.0 refined the purple color to work better with hue bulbs.
// also since dark sky is going away. change to use open weather api ... various changes based on this.. for instance there can be both rain and freezing rain in the same hour which was not handled previously.
// lgk 2.1 fix issue where open weather is different than dark sky.. dark sky would drop through if there was a percent probablity of perciptation but no snow or rain and assume sleet.
// in open weather is some hourly forecasts it shows a high percent probiblity but no rain and it is just cloudy ie

//{"dt":1644523200,"temp":46.98,"feels_like":41.56,"pressure":1005,"humidity":68,"dew_point":36.99,"uvi":0.55,"clouds":48,"visibility":10000,"wind_speed":12.19,"wind_deg":236,"wind_gust"
// :26.31,"weather":[{"id":802,"main":"Clouds","description":"scattered clouds","icon":"03d"}],"pop":0.86},
// obviously no sleet here with a temp of 47 but it just says cloudy but a pop percent probablity of precip - .86 
// seems like a bug so ignore it .

// new version may 2022, updates to logging, change some debugs to info, add a couple of warnings, make sure most output is either under debug or descLog
// and comment out unused old darksky weather function. Allso add more info if desc?Log is on about if it will sleet/rain/snow how many hours from now.

// lgk 12/22 slight change to yellow color to better reflect newer hue bulbs.
// add option to enable 3.0 api calls for the newer openweather apis.

// lgk 4/24 convert to open weather 3 api calls now that 2.5 is going away.

//
import java.util.regex.*

definition(
	name: "ColorCast Weather Lamp V2",
	namespace: "lgkapps",
	author: "laurence kahn",
	description: "Get a simple visual indicator for the days weather whenever you leave home. ColorCast will change the color of one or more Hue lights to match the weather forecast whenever it senses motion",
	category: "Convenience",
	iconUrl: "http://apps.shiftedpixel.com/weather/images/icons/colorcast.png",
	iconX2Url: "http://apps.shiftedpixel.com/weather/images/icons/colorcast@2x.png",
	iconX3Url: "http://apps.shiftedpixel.com/weather/images/icons/colorcast@2x.png"
)

preferences {
	page(name: "pageAPI", title: "API Key", nextPage: "pageSettings", install: false, uninstall: true) {
	
		section("First Things First") {
			paragraph "To use this SmartApp you need an API Key from openweathermap.org (https://openweathermap.org/faq). To obtain your key, you will need to register a free account on their site. You need to sign up for the One Call Api."
			paragraph "You will be asked for payment information, but you can ignore that part. Payment is only required if you access the data more than 1,000 times per day. If you don't give a credit card number and you somehow manage to exceed the 1,000 calls, the app will stop working until the following day when the counter resets."
		}
	
		section("API Key") {
			href(name: "hrefNotRequired",
			title: "Get your openweather One Call API key",
			required: false,
			style: "external",
			url: "https://openweathermap.org/full-price#onecall",
			description: "tap to view openweather website in mobile browser")
	
			input "apiKey", "text", title: "Enter your new key", required:true
            input "refreshTime", "enum", title: "How often to refresh?",options: ["Disabled","1-Hour", "30-Minutes", "15-Minutes", "10-Minutes", "5-Minutes"],  required: true, defaultValue: "15-Minutes"
            input "useVersion30API", "bool", title: "Use the newer Version 3.0 api/key? Default is false/off, which uses original ver 2.5 api.", required: true, defaultValue: false
            input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true)
  
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

	/* lgk  no tap trigger avail on hubitat
       section() {
			input "tapTrigger", "bool", title: "Enable App Tap Trigger?" 
         
		}
 */
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false //Allow custom name for app. Usefull if the app is installed multiple times for different modes
			mode title: "Set for specific mode(s)", required: false //Allow app to be assigned to different modes. Usefull if user wants different setting for different modes
	       input "numberOfBlinks", "integer", title: "Number of times to blink for alerts?", required: true, defaultValue: 5
           input "delayBetweenBlinks", "integer" , title: "Delay in Miliseconds between each blink", required: true, defaultValue: 600
              
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
	log.info "Installed with settings: ${settings}"
	initialize()
}

def initialize() {
	log.info "Initializing, subscribing to motion event at ${motionHandler} on ${motion_detector}"
	state.current = []

	getWeather()
    
    log.info "Refresh time currently set to: $refreshTime"
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
        log.info "Disabling..."
    }
    
	//schedule("0 0/15 * * * ?", getWeather)

	subscribe(motion_detector, "motion", motionHandler)
	// no tap trigger aval on hubitat no commands avail for apps, if (tapTrigger) subscribe(app, appTouchHandler)
}

def updated() {
	log.info "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}


def getWeather()
{
    def forecastUrl = "https://api.openweathermap.org/data/2.5/onecall?lat=$location.latitude&lon=$location.longitude&mode=json&units=imperial&appid=$apiKey&exclude=daily,flags,minutely"

   if (useVersion30API)
    {
        if (descLog) log.info "Overriding url for ver 3.0 api key."
        forecastUrl = "https://api.openweathermap.org/data/3.0/onecall?lat=$location.latitude&lon=$location.longitude&mode=json&units=imperial&appid=$apiKey&exclude=daily,flags,minutely"
    }
    
  if (descLog) log.info "Forecast URL = $forecastUrl"
	//Exclude additional unneded data from api url.
	if (forecastRange=='Current conditions') {
		forecastUrl+=',hourly' //If we're checking current conditions we can exclude hourly data
	} else {
		forecastUrl+=',currently' //If we're checking hourly conditions we can exclude current data
	}
	
	if (alertFlash==null) {
		forecastUrl+=',alerts' //If alert event is disabled then we can also exclude alert data
	}
	
	if (descLog) log.info forecastUrl
	
	httpGet(forecastUrl) {response -> 
		if (response.data) {
            
            //log.debug "got response - $response.data" 
			state.weatherData = response.data
			def d = new Date()
			state.forecastTime = d.getTime()
			if (descLog) log.info("Open Weather: Successfully retrieved weather.")
		} else {
			runIn(60, getWeather)
			log.warn("Open Weather: Failed to retrieve weather.")
		}
	}
}

/*
def getWeatherold() {
	def forecastUrl="https://api.forecast.io/forecast/$apiKey/$location.latitude,$location.longitude?exclude=daily,flags,minutely" //Create api url. Exclude unneeded data 
    if (descLog) log.info "Forecaset URL = $forecastUrl"
	//Exclude additional unneded data from api url.
	if (forecastRange=='Current conditions') {
		forecastUrl+=',hourly' //If we're checking current conditions we can exclude hourly data
	} else {
		forecastUrl+=',currently' //If we're checking hourly conditions we can exclude current data
	}
	
	if (alertFlash==null) {
		forecastUrl+=',alerts' //If alert event is disabled then we can also exclude alert data
	}
	
	if (descLog) log.info forecastUrl
	
	httpGet(forecastUrl) {response -> 
		if (response.data) {
			state.weatherData = response.data
			def d = new Date()
			state.forecastTime = d.getTime()
			if (descLog) log.info("Successfully retrieved weather.")
		} else {
			runIn(60, getWeather)
			log.warn("Failed to retrieve weather.")
		}
	}
*/

def checkForWeatherOW() {

    //log.debug "in check for weather"
	def d = new Date()
	if ((d.getTime() - state.forecastTime) / 1000 / 60 > 65)
    {
        
        if (descLog) log.info "Weather not checked for more than an hour! Rescheduling Job!"
		//unschedule()
		//schedule("0 0/15 * * * ?", getWeather)
        
      if (descLog) log.info "Refresh time currently set to: $refreshTime"
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
        if (descLog) log.info "Disabling..."
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

        // log.debug "got weather data - $state.weatherData"
		def i=0
			def lookAheadHours=1
			def forecastData=[]
			if (forecastRange=="Current conditions") { //Get current weather conditions
				forecastData.push(response.currently)
			} else {
				forecastData=response.hourly
                //forecastData=response.hourly
				lookAheadHours=forecastRange.replaceAll(/\D/,"").toInteger()
			}
		//log.debug "got forcastdata = $forecastData"
        
		for (hour in forecastData){ //Iterate over hourly data
			if (lookAheadHours<++i) { //Break if we've processed all of the specified look ahead hours. Need to strip non-numeric characters(i.e. "hours") from string so we can cast to an integer
				break
			} else {
                
                def rainThisHour = false
                def sleetThisHour = false
                def snowThisHour = false
                
              if(debug)
                { 
                    log.debug "in weather loop hour = $hour"              
                    log.debug "pop = $hour.pop"
                    log.debug "ptype = $hour.weather.main"
                    log.debug "description = $hour.weather.description"
                    log.debug "temp = $hour.temp, feels like = $hour.feels_like"
                }
               
               if (snowColor!='Disabled' || rainColor!='Disabled' || sleetColor!='Disabled') {
					if (hour.pop.floatValue()>=0.15) { //Consider it raining/snowing if precip probabilty is greater than 15%
                            
				 		if (hour.weather.main.indexOf('Rain') != -1) {
                            if (hour.weather.description.indexOf('freezing rain') != -1)
                            {
                                willSleet=true // preciptation is sleet
                                sleetThisHour=true
                            }
                            else
                            {
                                willRain=true //Precipitation type is rain 
                                rainThisHour=true
                            }
						} else if (hour.weather.main.indexOf('Snow') != -1) {
                            
                                willSnow=true
                                snowThisHour=true
                           
						} else {
							//willSleet=true no op.
						}
					}
				}
			
                if (debug) log.debug "in loop willSleet = $willSleet, willRain = $willRain, willSnow = $willSnow"
                if (descLog)
                {
                    if (i == 1)
                    {
                      if (sleetThisHour) log.info "It will SLeet in the next hour!"
                      if (rainThisHour) log.info "It will Rain in the next hour!"
                      if (snowThisHour) log.info "It will Snow in the next hour!"
                    }
                    else
                    {
                      if (sleetThisHour) log.info "It will SLeet $i hours from now!"
                      if (rainThisHour) log.info "It will Rain $i hours from now!"
                      if (snowThisHour) log.info "It will Snow $i hours from now!"  
                    }
                }
                    
				if (tempMinColor!='Disabled') {
					if (tempMinType=='Actual') {
						if (tempLow==null || tempLow>hour.temp) tempLow=hour.temp //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
					} else {
						if (tempLow==null || tempLow>hour.feels_like) tempLow=hour.feels_like //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
					}
				}
			
				if (tempMaxColor!='Disabled') {
					if (tempMaxType=='Actual') {
						if (tempHigh==null || tempHigh<hour.temp) tempHigh=hour.temp //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
					} else {
						if (tempHigh==null || tempHigh<hour.feels_like) tempHigh=hour.feels_like //Compare the stored low temp to the current iteration temp. If it's lower overwrite the stored low with this temp
					}
				}
			
                if (debug) log.debug "hour: ($i) windspeed = $hour.wind_speed!"
            
				if (windColor!='Disabled' && hour.wind_speed>=windTrigger) windy=true //Compare to user defined value for wid speed.
				if (cloudPercentColor!='Disabled' && hour.clouds>=cloudPercentTrigger) cloudy=true //Compare to user defined value for wind speed.
				if (dewPointColor!='Disabled' && hour.dew_point>=dewPointTrigger) humid=true //Compare to user defined value for wind speed.
			}
		}
       //log.debug "after hourly loop"
      if (debug)
        {
            log.debug "min temp = $tempLow"
            log.debug "max temp = $tempHigh"
        }
        
       if (debug) log.debug "done with hour data number of alerts in report = $response.alerts!"
   
		if (response.alerts) { //See if Alert data is included in response
           
			response.alerts.each { //If it is iterate through all Alerts
				def thisAlert=it.event;
				if (descLog) log.info thisAlert
                def overallDesc = it.description;
                          
				alertFlash.each{ //Iterate through all user specified alert types
                 if (debug) log.debug "in alert this alert = $thisAlert it = $it!"
                  
					if (thisAlert.toLowerCase().indexOf(it)>=0) { //If this user specified alert type matches this alert response
						if (descLog) log.info "ALERT: "+it
                        
                        // try to find color for specific event based on snow wind sleet rain etc.
                      if ((thisAlert.toLowerCase().indexOf('wind chill') >= 0) && (tempMinColor != 'Disabled'))
                          {
                            if (descLog) log.info "Found alert of type min temp!"
                            colors.push(tempMinColor)
                            if (descLog) log.info "wind chill/temp min"
                            if (descLog) log.info tempMinColor
                           }
                          
                      if ((thisAlert.toLowerCase().indexOf('wind') >= 0) && (thisAlert.toLowerCase().indexOf('wind chill') == -1)  && (windColor != 'Disabled'))
                          {
                            if (descLog) log.info "Found alert of type Wind!"
                            colors.push(windColor)
                            if (descLog) log.info "Wind"
                            if (descLog) log.info windColor
                           }
                           
                        if ((thisAlert.toLowerCase().indexOf('rain') >= 0) && (rainColor != 'Disabled'))
                        
                          {
                            if (descLog) log.info "Found alert of type Rain!"
                            colors.push(rainColor)
                            if (descLog) log.info "Rain"
                            if (descLog) log.info rainColor
                           }
                   if (((thisAlert.toLowerCase().indexOf('Severe Thunderstorm') >= 0) || (overallDesc.toLowerCase().indexOf('Severe Thunderstorm') >= 0)) && (rainColor != 'Disabled'))
                        
                          {
                            if (descLog) log.info "Found alert of type Severe Thunderstorm!"
                            colors.push(rainColor)
                            if (descLog) log.info "Rain"
                            if (descLog) log.info rainColor
                           }             
                        if (((thisAlert.toLowerCase().indexOf('snow ') >= 0) || (overallDesc.toLowerCase().indexOf('snow ') >= 0)) && (snowColor != 'Disabled'))
                          {
                            if (descLog) log.info "Found alert of type Snow!"
                            colors.push(snowColor)
                            if (descLog) log.info "Snow"
                            if (descLog) log.info snowColor
                           }
                        if (((thisAlert.toLowerCase().indexOf('sleet') >= 0) || (overallDesc.toLowerCase().indexOf('sleet' ) >= 0)) && (sleetColor != 'Disabled'))
                          {
                            if (descLog) log.info "Found alert of type Sleet!"
                            colors.push(sleetColor)
                            if (descLog) log.info "Sleet"
                            if (descLog) log.info sleetColor
                           }
                           
						weatherAlert=true //Is there currently a weather alert
					}
				}
			}
		}

	if (debug) log.debug "weatherAlert = $weatherAlert"
		//Add color strings to the colors array to be processed later
		if (tempMinColor!='Disabled' && tempLow<=tempMinTrigger.floatValue()) {
			colors.push(tempMinColor)
			if (descLog) log.info "Cold"
			if (descLog) log.info tempMinColor
		}
		if (tempMaxColor!='Disabled' && tempHigh>=tempMaxTrigger.floatValue()) {
			colors.push(tempMaxColor)
			if (descLog) log.info "Hot"
			if (descLog) log.info tempMaxColor
		}
		if (humidityColor!='Disabled' && humid) {
			colors.push(dewPointColor)
			if (descLog) log.info "Humid"
			if (descLog) log.info dewPointColor
		}
		if (snowColor!='Disabled' && willSnow) {
			colors.push(snowColor)
			if (descLog) log.info "Snow"
			if (descLog) log.info snowColor			
		}
		if (sleetColor!='Disabled' && willSleet) {
			colors.push(sleetColor)
			if (descLog) log.info "Sleet"
			if (descLog) log.info sleetColor			
		}
		if (rainColor!='Disabled' && willRain) {
			colors.push(rainColor)
			if (descLog) log.info "Rain"
			if (descLog) log.info rainColor
		}
		if (windColor!='Disabled' && windy) {
			colors.push(windColor)
			if (descLog) log.info "Windy"
			if (descLog) log.info windColor
		}
		if (cloudPercentColor!='Disabled' && cloudy) {
			colors.push(cloudPercentColor)
			if (descLog) log.info "Cloudy"
			if (descLog) log.info cloudPercentColor
		}
	}
    
   if ((colors.size() == 0) && (weatherAlert == true))
     {
       if (descLog) log.info "Found a weather alert, but no specific class of weather triggererd!"  
       //log.debug "trying to find color for specific alert event!"  
       if (descLog) log.info "Setting color to default Alert Color = $defaultAlertColor"
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
	log.info colors


	def delay=2000 //The amount of time to leave each color on
	def iterations=1 //The number of times to show each color
	if (weatherAlert) {
         if (descLog) log.info "Weather Alert!"
		//When there's an active weather alert, shorten the duration that each color is shown but show the color multiple times. This will cause individual colors to flash when there is a weather alert
		delay = delayBetweenBlinks.toInteger()
		iterations=numberOfBlinks.toInteger()
	}
	
	colors.each { //Iterate over each color
		for (int i = 0; i<iterations; i++) {
			sendcolor(it) //Turn light on with specified color
			pause(delay) //leave the light on for the specified time
			if (weatherAlert) {
				//When a weather alert is active, each color will be looped x times, creating the blinking effect by turning the light on then off x times              
				hues.off()
				pause(delay)
			}
		}
        // extra off
       
      if ((colors.size() > 0) || (weatherAlert == true))
        {
          hues.off()
        }
	}
	}
	 if ((colors.size() > 0) || (weatherAlert == true)) setLightsToOriginal() //The colors have been sent to the lamp and all colors have been shown. Now revert the lights to their original settings
}


/* commend out checkforweather now use the open weather version

def checkForWeather() {

    //log.debug "in check for weather"
	def d = new Date()
	if ((d.getTime() - state.forecastTime) / 1000 / 60 > 65)
    {
        
        log.info "Weather not checked for more than an hour! Rescheduling Job!"
		//unschedule()
		//schedule("0 0/15 * * * ?", getWeather)
        
      log.info "Refresh time currently set to: $refreshTime"
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
        log.info "Disabling..."
    }
      if (refreshTime != "Disabled")
         getWeather()
        // getOWeather()
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

        // log.debug "got weather data - $state.weatherData"
		def i=0
			def lookAheadHours=1
			def forecastData=[]
			if (forecastRange=="Current conditions") { //Get current weather conditions
				forecastData.push(response.currently)
			} else {
				forecastData=response.hourly.data
                //forecastData=response.hourly
				lookAheadHours=forecastRange.replaceAll(/\D/,"").toInteger()
			}
		//log.debug "got forcastdata = $forecastData"
        
		for (hour in forecastData){ //Iterate over hourly data
			if (lookAheadHours<++i) { //Break if we've processed all of the specified look ahead hours. Need to strip non-numeric characters(i.e. "hours") from string so we can cast to an integer
				break
			} else {
              if (descLog) log.info "in weather loop hour = $hour"
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
			if (descLog) log.info "hour: ($i) windspeed = $hour.windSpeed!"
            
				if (windColor!='Disabled' && hour.windSpeed>=windTrigger) windy=true //Compare to user defined value for wid speed.
				if (cloudPercentColor!='Disabled' && hour.cloudCover*100>=cloudPercentTrigger) cloudy=true //Compare to user defined value for wind speed.
				if (dewPointColor!='Disabled' && hour.dewPoint>=dewPointTrigger) humid=true //Compare to user defined value for wind speed.
			}
		}
// log.debug "after hourly loop"
if (debug)
        {
            log.debug "min temp = $tempLow"
             log.debug "max temp = $tempHigh"
              log.debug "done with hour data number of alerts in report = $response.alerts!"
        }
        
		if (response.alerts) { //See if Alert data is included in response
            log.debug "in alert loop!"
			response.alerts.each { //If it is iterate through all Alerts
				def thisAlert=it.title;
				log.debug thisAlert
                def overallDesc = it.description;
                //log.debug "overall desc = $overallDesc"
                
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
                   if (((thisAlert.toLowerCase().indexOf('Severe Thunderstorm') >= 0) || (overallDesc.toLowerCase().indexOf('Severe Thunderstorm') >= 0)) && (rainColor != 'Disabled'))
                        
                          {
                            log.debug "Found alert of type Severe Thunderstorm!"
                            colors.push(rainColor)
                            log.debug "Rain"
                            log.debug rainColor
                           }             
                        if (((thisAlert.toLowerCase().indexOf('snow') >= 0) || (overallDesc.toLowerCase().indexOf('snow') >= 0)) && (snowColor != 'Disabled'))
                          {
                            log.debug "Found alert of type Snow!"
                            colors.push(snowColor)
                            log.debug "Snow"
                            log.debug snowColor
                           }
                        if (((thisAlert.toLowerCase().indexOf('sleet') >= 0) || (overallDesc.toLowerCase().indexOf('sleet' ) >= 0)) && (sleetColor != 'Disabled'))
                          {
                            log.debug "Found alert of type Sleet!"
                            colors.push(sleetColor)
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
       //log.debug "trying to find color for specific alert event!"  
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
		delay = delayBetweenBlinks.toInteger()
		iterations=numberOfBlinks.toInteger()
	}
	
	colors.each { //Iterate over each color
		for (int i = 0; i<iterations; i++) {
			sendcolor(it) //Turn light on with specified color
			pause(delay) //leave the light on for the specified time
			if (weatherAlert) {
				//When a weather alert is active, each color will be looped x times, creating the blinking effect by turning the light on then off x times              
				hues.off()
				pause(delay)
			}
		}
        // extra off
       
      if ((colors.size() > 0) || (weatherAlert == true))
        {
          hues.off()
        }
	}
	}
	 if ((colors.size() > 0) || (weatherAlert == true)) setLightsToOriginal() //The colors have been sent to the lamp and all colors have been shown. Now revert the lights to their original settings
}
*/
    
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
			hueColor = 17
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 73
			saturation = 99
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

def setLightsToOriginal()
    {
	if (rememberLevel) {    
		hues.eachWithIndex { it, i -> 
			it.setColor(state.current[i])
			lf (debug) log.debug ("RESET: " + state.current[i])
           // huses.off()
		}
	} else {
		hues.off()
	}
}

/// HANDLE MOTION
def motionHandler(evt)
    {
    //log.debug "in motion handler"
	if (evt.value == "active") {// If there is movement then trigger the weather display
		if (descLog) log.info "Motion detected, in ColorCastWeather!"
		checkForWeatherOW()
	} 
}

def appTouchHandler(evt)
    {
    // If the button is pressed then trigger the weather display
	checkForWeatherOW()	
	if (descLog) log.info "App triggered with button press."
}
