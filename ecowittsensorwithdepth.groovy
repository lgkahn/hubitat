/**
 * Driver:     Ecowitt RF Sensor
 * Author:     Mirco Caramori
 * Repository: https://github.com/mircolino/ecowitt
 * Import URL: https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_sensor.groovy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Change Log: shared with ecowitt_gateway.groovy
 * lgk add debugging with auto turn off so we can see if it is getting temp because it wont show an update event in hubitat logs if the same as last temp.
 * lgk add lastTemperatuer and lastHumidity attributes so we can easily write rules to trigger if the temp or humidity is going up or down.
 * I use this to alert when the hot tub will be at max in xx amount of time etc.
 *
 * lgk add code for capablity Air Quality using aqi
 * lgk add srain_piezo and raining = true or false, also capacitorVoltage and firmware versions, stick the raing on the rain device and cap voltate and firmware version on the wind device
 *
* lgk add srain_piezo and raining = true or false, also capacitorVoltage and firmware versions, stick the raing on the rain device and cap voltate and firmware version on the wind device
* lgk fixed missing break statement when processing srain_piezo and raining attributes
* lgk remove ws90 possibly going to batterysolar attrs leave for ws80 for now
* add code for capacitorPercent attribute
* lgk 8/25 add last24hrain and last24hrain_piezo 

* lgk 10/25 all code changes for wh54 depth sensor and code for snow depth sensing.
* note to end yearly depth record .. and reset you must manually call the fx from the device panel as snowfalls levels dont conveniently
* end on jan 1st.

*/

metadata {
  definition(name: "Ecowitt RF Sensor", namespace: "mircolino", author: "Mirco Caramori", importUrl: "https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_sensor.groovy") {
    capability "Sensor";

    capability "Battery";
    capability "Temperature Measurement";
    capability "Relative Humidity Measurement";
    capability "Pressure Measurement";
    capability "Ultraviolet Index";
    capability "Illuminance Measurement";
    capability "Water Sensor";
    capability "CarbonDioxide Measurement";
    capability "Air Quality";

 // attribute "battery", "number";                             // 0-100%
    attribute "batteryIcon", "number";                         // 0, 20, 40, 60, 80, 100
    attribute "batteryOrg", "number";                          // original/un-translated battery value returned by the sensor

    attribute "batteryTemp", "number";                         //
    attribute "batteryTempIcon", "number";                     // Only created/used when a WH32 is bundled in a PWS
    attribute "batteryTempOrg", "number";                      //

    attribute "batteryRain", "number";                         //
    attribute "batteryRainIcon", "number";                     // Only created/used when a WH40 is bundled in a PWS
    attribute "batteryRainOrg", "number";                      //

    attribute "batteryWind", "number";                         //
    attribute "batteryWindIcon", "number";                     // Only created/used when a WH68/WH80 is bundled in a PWS
    attribute "batteryWindOrg", "number";                      //
    attribute "capacitorVoltage", "number";
    attribute "capacitorPercent", "number" 
    attribute "ws90Firmware", "string";

 // attribute "temperature", "number";                         // °F

 // attribute "humidity", "number";                            // 0-100%
    attribute "humidityAbs", "number";                         // oz/yd³ or g/m³     
    attribute "dewPoint", "number";                            // °F - calculated using outdoor "temperature" & "humidity"
    attribute "heatIndex", "number";                           // °F - calculated using outdoor "temperature" & "humidity"
    attribute "heatDanger", "string";                          // Heat index danger level
    attribute "heatColor", "string";                           // Heat index HTML color
    attribute "simmerIndex", "number";                         // °F - calculated using outdoor "temperature" & "humidity"
    attribute "simmerDanger", "string";                        // Summer simmmer index danger level
    attribute "simmerColor", "string";                         // Summer simmer index HTML color

 // attribute "pressure", "number";                            // inHg - relative pressure corrected to sea-level
    attribute "pressureAbs", "number";                         // inHg - absolute pressure
    attribute "vpd", "number"                                  // new field vapor pressure difference.
      
    attribute "rainRate", "number";                            // in/h - rainfall rate
    attribute "rainEvent", "number";                           // in - rainfall in the current event
    attribute "rainHourly", "number";                          // in - rainfall in the current hour
    attribute "rainDaily", "number";                           // in - rainfall in the current day
    attribute "rainWeekly", "number";                          // in - rainfall in the current week
    attribute "rainMonthly", "number";                         // in - rainfall in the current month
    attribute "rainYearly", "number";                          // in - rainfall in the current year
    attribute "rainTotal", "number";                           // in - rainfall total since sensor installation
    attribute "raining", "string";
    attribute "rainLast24Hrs", "number";  

    attribute "pm25", "number";                                // µg/m³ - PM2.5 particle reading - current
    attribute "pm25_avg_24h", "number";                        // µg/m³ - PM2.5 particle reading - average over the last 24 hours
    attribute "pm10", "number";                                // µg/m³ - PM10 particle reading - current
    attribute "pm10_avg_24h", "number";                        // µg/m³ - PM10 particle reading - average over the last 24 hours
   // attribute "co2", "number";                               // ppm - CO2 concetration - current
    attribute "carbonDioxide_avg_24h", "number";               // ppm - CO2 concetration - average over the last 24 hours

    attribute "aqi", "number";                                 // AQI (0-500)
    attribute "aqiDanger", "string";                           // AQI danger level
    attribute "aqiColor", "string";                            // AQI HTML color

    attribute "aqi_avg_24h", "number";                         // AQI (0-500) - average over the last 24 hours
    attribute "aqiDanger_avg_24h", "string";                   // AQI danger level - average over the last 24 hours
    attribute "aqiColor_avg_24h", "string";                    // AQI HTML color - average over the last 24 hours

 // attribute "water", "enum", ["dry", "wet"];                 // "dry" or "wet"
    attribute "waterMsg", "string";                            // dry) "Dry", wet) "Leak detected!"
    attribute "waterColor", "string";                          // dry) "ffffff", wet) "ff0000" to colorize the icon

    attribute "lightningTime", "string";                       // Strike time - local time
    attribute "lightningDistance", "number";                   // Strike distance - km
    attribute "lightningEnergy", "number";                     // Strike energy - MJ/m
    attribute "lightningCount", "number";                      // Strike total count

 // attribute "ultravioletIndex", "number";                    // UV index (0-11+)
    attribute "ultravioletDanger", "string";                   // UV danger (0-2.9) Low, (3-5.9) Medium, (6-7.9) High, (8-10.9) Very High, (11+) Extreme
    attribute "ultravioletColor", "string";                    // UV HTML color

 // attribute "illuminance", "number";                         // lux
    attribute "solarRadiation", "number";                      // W/m²

    attribute "windDirection", "number";                       // 0-359°
    attribute "windCompass", "string";                         // NNE
    attribute "windDirection_avg_10m", "number";               // 0-359° - average over the last 10 minutes
    attribute "windCompass_avg_10m", "string";                 // NNE - average over the last 10 minutes
    attribute "windSpeed", "number";                           // mph
    attribute "windSpeed_avg_10m", "number";                   // mph - average over the last 10 minutes
    attribute "windGust", "number";                            // mph
    attribute "windGustMaxDaily", "number";                    // mph - max in the current day
    attribute "windChill", "number";                           // °F - calculated using outdoor "temperature" & "windSpeed"
    attribute "windDanger", "string";                          // Windchill danger level
    attribute "windColor", "string";                           // Windchill HTML color

    attribute "html", "string";                                //
    attribute "html1", "string";                               //
    attribute "html2", "string";                               // e.g. "<div>Temperature: ${temperature}°F<br>Humidity: ${humidity}%</div>"
    attribute "html3", "string";                               //
    attribute "html4", "string";                               //

    attribute "status", "string";                              // Display current driver status

    attribute "orphaned", "enum", ["false", "true"];           // Whether or not the unbundled sensor is still receiving data from the gateway
    attribute "orphanedTemp", "enum", ["false", "true"];       // Whether or not the bundled WH32 is still receiving data from the gateway
    attribute "orphanedRain", "enum", ["false", "true"];       // Whether or not the bundled WH40 is still receiving data from the gateway
    attribute "orphanedWind", "enum", ["false", "true"];       // Whether or not the bundled WH68/WH80 sensor is still receiving data from the gateway            
    attribute "orphanedDepth", "enum", ["false", "true"];       // Whether or not the bundled WH54 sensor is still receiving data from the gateway            
    attribute "lastUpdate", "string"
    attribute "bundled", "string"
    attribute "rainLastUpdate", "string"
    attribute "windLastUpdate", "string"
    attribute "temperatureLastUpdate", "string"
    attribute "humidityLastUpdate", "string"
    attribute "lightningLastUpdate", "string"
    attribute "depthLastUpdate", "string"
      
    attribute "lastTemperature", "number"
    attribute "lastHumidity", "number"
    attribute "temperatureChange", "number"
    attribute "humidityChange", "number"
      
      
    // snow depth attris
    attribute "airHeight" , "number"
    attribute "totalHeight", "number"
    attribute "depth", "number"
    attribute "heatLDS", "number"
    attribute "snowDaily", "number"
    attribute "snowMonthly", "number"
    attribute "snowLastMonth", "number"
    attribute "yearDepthStats", "string"
    attribute "monthDepthStats", "string"
    attribute "snowYearly", "number"      
    attribute "sensorID", "string"
    attribute "snowLastYear", "number"
    attribute "snowHourly", "number"
      
    command "testfx"
    command "testHourlyfx"
    command "testDailyfx"
    command "testMonthlyfx"
    command "accumulateAndResetYearlyTotals"
      
    command "startDepthRecording"
    command "disableDepthRecording"
    command "resetDepthTotals"
      
  }

  preferences {
                                                                                                                                                                                                                                                                                                                                                                                                                                                                input(name: "htmlTemplate", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>Tile HTML Template(s)</font>", description: "<font style='font-size:12px; font-style: italic'>See <u><a href='https://github.com/mircolino/ecowitt/blob/master/readme.md#templates' target='_blank'>documentation</a></u> for input formats</font>", defaultValue: "");
    input("debug", "bool", title: "Enable logging?", required: true, defaultValue: false)
      
    input("pressureinMbar", "bool", title: "Display pressure in millibars?", required:true, defaultValue:false)
      
     if (localAltitude != null) {
      input(name: "localAltitude", type: "string", title: "<font style='font-size:12px; color:#1a77c9'><u><a href='https://www.advancedconverter.com/map-tools/altitude-on-google-maps' target='_blank'>Altitude</a></u> to Correct Sea Level Pressure</font>", description: "<font style='font-size:12px; font-style: italic'>Examples: \"378 ft\" or \"115 m\"</font>", defaultValue: "", required: true);
    }
    if (voltageMin != null) {
      input(name: "voltageMin", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>Empty Battery Voltage</font>", description: "<font style='font-size:12px; font-style: italic'>Sensor value when battery is empty</font>", defaultValue: "", required: true);
      input(name: "voltageMax", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>Full Battery Voltage</font>", description: "<font style='font-size:12px; font-style: italic'>Sensor value when battery is full</font>", defaultValue: "", required: true);
    }
    if (calcDewPoint != null) {
      input(name: "calcDewPoint", type: "bool", title: "<font style='font-size:12px; color:#1a77c9'>Calculate Dew Point & Absolute Humidity</font>", description: "<font style='font-size:12px; font-style: italic'>Temperature below which water vapor will condense & amount of water contained in a parcel of air</font>", defaultValue: false);
    }
    if (calcHeatIndex != null) {
      input(name: "calcHeatIndex", type: "bool", title: "<font style='font-size:12px; color:#1a77c9'>Calculate Heat Index</font>", description: "<font style='font-size:12px; font-style: italic'>Perceived discomfort as a result of the combined effects of the air temperature and humidity</font>", defaultValue: false);
    }
    if (calcSimmerIndex != null) {
      input(name: "calcSimmerIndex", type: "bool", title: "<font style='font-size:12px; color:#1a77c9'>Calculate Summer Simmer Index</font>", description: "<font style='font-size:12px; font-style: italic'>Similar to the Heat Index but using a newer and more accurate formula</font>", defaultValue: false);
    }
    if (calcWindChill != null) {
      input(name: "calcWindChill", type: "bool", title: "<font style='font-size:12px; color:#1a77c9'>Calculate Wind-chill Factor</font>", description: "<font style='font-size:12px; font-style: italic'>Lowering of body temperature due to the passing-flow of lower-temperature air</font>", defaultValue: false);
    }
      // option parameter to enable wh54 stuff
      input(name: "WH54LaserDensityDeviceEnabled", type: "bool", title:"This device is a WH54 Laser Density Sensor. Auto Disabled on submit if sensor is found to be something else.", defaultValue:false) 
      
      if (WH54LaserDensityDeviceEnabled)
      
      {
         input(name: "useWh54ForSnowDepthCalculations", type: "bool", title: "Use the WH54 to calculate snow depth and enable statistics?", defaultValue:false) 
         input(name: "debugDepthStatisics", type: "bool", title: "Turn on debugging for the Snow Depth Statistics Calculations?", defaultValue:false) 
     }
      
  }
}

import java.time.LocalDate

/*
 * State variables used by the driver:
 *
 * sensor                      \
 * sensorTemp                   | null) not present, 0) waiting to receive data, 1) processing data
 * sensorRain                   |
 * sensorWind                  /
 *
 */

/*
 * Data variables used by the driver:
 *
 * "isBundled"                                                 // "true" if we are a bundled PWS (set by the parent at creation time)
 * "htmlTemplate"                                              // User template 0
 * "htmlTemplate1"                                             // User template 1
 * "htmlTemplate2"                                             // User template 2
 * "htmlTemplate3"                                             // User template 3
 * "htmlTemplate4"                                             // User template 4
 */

// Logging --------------------------------------------------------------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (getParent().logGetLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (getParent().logGetLevel() > 1) log.info(str); }
private void logDebug(String str) { if (getParent().logGetLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (getParent().logGetLevel() > 3) log.trace(str); }

// Ztatus ---------------------------------------------------------------------------------------------------------------------

private Boolean ztatus(String str, String color = null) {

  if (color) str = "<font style='color:${color}'>${str}</font>";

  return (attributeUpdateString(str, "status"));
}

// ------------------------------------------------------------

private Boolean ztatusIsError() {
  
  String str = device.currentValue("status") as String;

  if (str && str.contains("<font style='color:red'>")) return (true);
  return (false);
}

// Conversions ----------------------------------------------------------------------------------------------------------------

private Boolean unitSystemIsMetric() {
  //
  // Return true if the selected unit system is metric
  //
    
    def isM = parent.unitSystemIsMetric()
    
    if (isM == null)
    return false
    else return isM

    //return (getParent().unitSystemIsMetric());
}


// ------------------------------------------------------------

private String timeEpochToLocal(String time) {
  //
  // Convert Unix Epoch time (seconds) to local time with locale format
  //
  try {
    Long epoch = time.toLong() * 1000L;

    Date date = new Date(epoch);

    java.text.SimpleDateFormat format = new java.text.SimpleDateFormat();
    time = format.format(date);
  }
  catch (Exception e) {
    logError("Exception in timeEpochToLocal(): ${e}");
  }

  return (time);
}

// ------------------------------------------------------------

private BigDecimal convertRange(BigDecimal val, BigDecimal inMin, BigDecimal inMax, BigDecimal outMin, BigDecimal outMax, Boolean returnInt = true) {
  // Let make sure ranges are correct
  assert (inMin <= inMax);
  assert (outMin <= outMax);

  // Restrain input value
  if (val < inMin) val = inMin;
  else if (val > inMax) val = inMax;

  val = ((val - inMin) * (outMax - outMin)) / (inMax - inMin) + outMin;
  if (returnInt) {
    // If integer is required we use the Float round because the BigDecimal one is not supported/not working on Hubitat
    val = val.toFloat().round().toBigDecimal();
  }

  return (val);
}

// ------------------------------------------------------------

private BigDecimal convert_F_to_C(BigDecimal val) {
  return ((val - 32) / 1.8);
}

// ------------------------------------------------------------

private BigDecimal convert_C_to_F(BigDecimal val) {
  return ((val * 1.8) + 32);
}

// ------------------------------------------------------------

private BigDecimal convert_inHg_to_hPa(BigDecimal val) {
  return (val * 33.863886666667);
}

// ------------------------------------------------------------

private BigDecimal convert_hPa_to_inHg(BigDecimal val) {
  return (val / 33.863886666667);
}

// ------------------------------------------------------------

private BigDecimal convert_in_to_mm(BigDecimal val) {
  return (val * 25.4);
}

// ------------------------------------------------------------

private BigDecimal convert_mm_to_in(BigDecimal val) {
  return (val / 25.4);
}

// ------------------------------------------------------------

private BigDecimal convert_ft_to_m(BigDecimal val) {
  return (val / 3.28084);
}

// ------------------------------------------------------------

private BigDecimal convert_m_to_ft(BigDecimal val) {
  return (val * 3.28084);
}

// ------------------------------------------------------------

private BigDecimal convert_mi_to_km(BigDecimal val) {
  return (val * 1.609344);
}

// ------------------------------------------------------------

private BigDecimal convert_km_to_mi(BigDecimal val) {
  return (val / 1.609344);
}

// ------------------------------------------------------------

private BigDecimal convert_Wm2_to_lux(BigDecimal val) {
  return (val / 0.0079);
}

// ------------------------------------------------------------

private BigDecimal convert_lux_to_Wm2(BigDecimal val) {
  return (val * 0.0079);
}

// ------------------------------------------------------------

private BigDecimal convert_gm3_to_ozyd3(BigDecimal val) {
  return (val / 37.079776);
}

// ------------------------------------------------------------

private BigDecimal convert_ozyd3_to_gm3(BigDecimal val) {
  return (val * 37.079776);
}

// Attribute handling ----------------------------------------------------------------------------------------------------------

private Boolean attributeUpdateString(String val, String attribute) {
  //
  // Only update "attribute" if different
  // Return true if "attribute" has actually been updated/created
  //
 if (debug) log.debug "in attrupstring val = $val , attr = $attribute"
 
    if ((device.currentValue(attribute) as String) != val) {
    sendEvent(name: attribute, value: val);
    return (true);
  }

  return (false);
}

// ------------------------------------------------------------

private Boolean attributeUpdateNumber(BigDecimal val, String attribute, String measure = null, Integer decimals = -1) {
  //
  // Only update "attribute" if different
  // Return true if "attribute" has actually been updated/created
  //
    
  // If rounding is required we use the Float one because the BigDecimal is not supported/not working on Hubitat
   if (debug) log.debug "in attr update number val = $val attribute = $attribute"
    
  if (decimals >= 0) val = val.toFloat().round(decimals).toBigDecimal();

  BigDecimal integer = val.toBigInteger();

  // We don't strip zeros on an integer otherwise it gets converted to scientific exponential notation
  val = (val == integer)? integer: val.stripTrailingZeros();

  // Coerce Object -> BigDecimal
  if ((device.currentValue(attribute) as BigDecimal) != val) {
    if (measure) sendEvent(name: attribute, value: val, unit: measure);
    else sendEvent(name: attribute, value: val);
    return (true);
  }

  return (false);
}

// ------------------------------------------------------------

private List<String> attributeEnumerate(Boolean existing = true) {
  //
  // Return a list of all available attributes
  // If "existing" == true return only those that have been already created (non-null ones)
  // Returned list can be empty but never return null
  //
  List<String> list = [];
  List<com.hubitat.hub.domain.Attribute> attrib = device.getSupportedAttributes();
  if (attrib) {
    attrib.each {
      if (existing == false || device.currentValue(it.name) != null) list.add(it.name);
    }
  }

  return (list);
}

// ------------------------------------------------------------

private Boolean attributeUpdateBattery(String val, String attribBattery, String attribBatteryIcon, String attribBatteryOrg, Integer type) {
  //
  // Convert all different batteries returned values to a 0-100% range
  // Type: 1) voltage: range from 1.30V (empty) to 1.65V (full)
  //       2) pentastep: range from 0 (empty) to 5 (full)
  //       0) binary: 0 (full) or 1 (empty)
  //
  BigDecimal original = val.toBigDecimal();
  BigDecimal percent;
  BigDecimal icon;
  String unitOrg;

   // log.warn "in update battery attr = $attribBattery, type = $type"
    
  switch (type) {
  case 1:
    // Change range from voltage to (0% - 100%)
    BigDecimal vMin, vMax;
      
    if (!(settings.voltageMin) || !(settings.voltageMax)) {
      // First time: initialize and show the preference
      vMin = 1.3;
      vMax = 1.65;
      device.updateSetting("voltageMin", [value: vMin, type: "string"]);
      device.updateSetting("voltageMax", [value: vMax, type: "string"]);
    }
    else {
      vMin = (settings.voltageMin).toBigDecimal();
      vMax = (settings.voltageMax).toBigDecimal();    
    }

    percent = convertRange(original, vMin, vMax, 0, 100);
    unitOrg = "V";
    break;

  case 2:
    // Change range from (0 - 5) to (0% - 100%)
    percent = convertRange(original, 0, 5, 0, 100);
    unitOrg = "level";
    break;

  default:
    // Change range from (0  or 1) to (100% or 0%)
    percent = (original == 0)? 100: 0;
    unitOrg = "!bool";
  }

  if (percent < 10) icon = 0;
  else if (percent < 30) icon = 20;
  else if (percent < 50) icon = 40;
  else if (percent < 70) icon = 60;
  else if (percent < 90) icon = 80;
  else icon = 100;

  Boolean updated = attributeUpdateNumber(percent, attribBattery, "%", 0);
  if (attributeUpdateNumber(icon, attribBatteryIcon, "%")) updated = true;
  if (attributeUpdateNumber(original, attribBatteryOrg, unitOrg)) updated = true;

  return (updated);
}

// -----------------------------

private Boolean attributeUpdateLowestBattery() {
  BigDecimal percent = 100;
  String org = "0";
  Integer type = 0;

  BigDecimal temp = device.currentValue("batteryTemp") as BigDecimal;
  BigDecimal rain = device.currentValue("batteryRain") as BigDecimal;
  BigDecimal wind = device.currentValue("batteryWind") as BigDecimal;

  if (temp != null) {
    percent = temp;
    org = device.currentValue("batteryTempOrg") as String;
    type = 0;
  }

  if (rain != null && rain < percent) {
    percent = rain;
    org = device.currentValue("batteryRainOrg") as String;
    type = 1;
  }

  if (wind != null && wind < percent) {
    percent = wind;
    org = device.currentValue("batteryWindOrg") as String;
    type = 1;
  }

  return (attributeUpdateBattery(org, "battery", "batteryIcon", "batteryOrg", type));
}

// ------------------------------------------------------------

private Boolean attributeUpdateTemperature(String val, String attribTemperature) {

  BigDecimal degrees = val.toBigDecimal();
  String measure = "°F";

  // Convert to metric if requested
  if (unitSystemIsMetric()) {
    degrees = convert_F_to_C(degrees);
    measure = "°C";
  }
    
  Boolean hasChanged = (attributeUpdateNumber(degrees, attribTemperature, measure, 1))  
    
   // only do this if actually temp not other attributes that come through here like dew pt     
   if (attribTemperature == "temperature")
    {
        
  def lastTemp = (device.currentValue(attribTemperature) as BigDecimal)
  BigDecimal change = 0.00
   if (lastTemp != null) 
    change = (degrees - lastTemp as BigDecimal)
   else lastTemp = 0.00
    
  attributeUpdateNumber(lastTemp,"lastTemperature",measure,1)

  if (debug) log.debug "In update temp val = $val , measure = $measure, attribute = $attribTemperature, lastTemp = $lastTemp, change = $change, hasChanged = $hasChanged"
    
  // only log difference if we have a changed value.
  if (hasChanged == true)
    {
      sendEvent(name: "temperatureChange", value: change)
    }
  else 
  {
      sendEvent(name: "temperatureChange", value: 0.00)
  }
    
    }
    
  return hasChanged
}

// ------------------------------------------------------------

private Boolean attributeUpdateHumidity(String val, String attribHumidity) {

  BigDecimal percent = val.toBigDecimal();
  
  def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
  sendEvent(name: "humidityLastUpdate", value: now)
    
  def lastHumid = (device.currentValue(attribHumidity) as BigDecimal)
  BigDecimal change = 0.00
  if (lastHumid != null) 
     change = (percent - lastHumid as BigDecimal)
   else lastHumid = 0.00
  
  attributeUpdateNumber(lastHumid,"lastHumidity","%",0)
  Boolean hasChanged = (attributeUpdateNumber(percent, attribHumidity, "%", 0))

  // only log difference if we have a changed value.
  if (hasChanged == true)
     sendEvent(name: "humidityChange", value: change)
  else  sendEvent(name: "humidityChange", value: 0.00)
          
  return hasChanged
}

// ------------------------------------------------------------

private Boolean attributeUpdatePressure(String val, String attribPressure, String attribPressureAbs) {

  // Get unit system
  Boolean metric = unitSystemIsMetric();

   // log.debug "in aattribute update pressure in mbar = $pressureinMbar"
  // Get pressure in hectopascal
  BigDecimal absolute = convert_inHg_to_hPa(val.toBigDecimal());

  // Get altitude in meters
  val = settings.localAltitude;
  if (!val) {
    // First time: initialize and show the preference
    val = metric? "0 m": "0 ft";
    device.updateSetting("localAltitude", [value: val, type: "string"]);
  }

  BigDecimal altitude;
  try {
    String[] field = val.split();
    altitude = field[0].toBigDecimal();
    if (field.size() == 1) {
      // No unit found: let's use the parent setting
      if (!metric) altitude = convert_ft_to_m(altitude);
    }
    else {
      // Found a unit: convert accordingly
      if (field[1][0] == "f" || field[1][0] == "F") altitude = convert_ft_to_m(altitude);
    }
  }
  catch(Exception ignored) {
    altitude = 0;
  }

  // Get temperature in celsious
  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature == null) temperature = 18;
  else if (!metric) temperature = convert_F_to_C(temperature);

  // Correct pressure to sea level using this conversion formula: https://keisan.casio.com/exec/system/1224575267
  BigDecimal relative = absolute * Math.pow(1 - ((altitude * 0.0065) / (temperature + (altitude * 0.0065) + 273.15)), -5.257);

    
  // Convert to imperial if requested
  if (pressureinMbar) 
    val = "hPa";
  else {
    absolute = convert_hPa_to_inHg(absolute);
    relative = convert_hPa_to_inHg(relative);
    val = "inHg";
  }  
  

  Boolean updated = attributeUpdateNumber(relative, attribPressure, val, 2);
  if (attributeUpdateNumber(absolute, attribPressureAbs, val, 2)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateRain(String val, String attribRain, Boolean hour = false) {

  BigDecimal amount = val.toBigDecimal();
  String measure = hour? "in/h": "in";

  // Convert to metric if requested
  if (unitSystemIsMetric()) {
    amount = convert_in_to_mm(amount);
    measure = hour? "mm/h": "mm";
  }
 
  def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
  sendEvent(name: "rainLastUpdate", value: now)
    
  attributeUpdateString("false", "orphanedRain");

  return (attributeUpdateNumber(amount, attribRain, measure, 2));
}

// ------------------------------------------------------------


private Boolean attributeUpdateDepth(String val, String attribDepth) {

    // bug in ecowitt all depth attrs are returned in mm irregardless of foot/m setting on sensor
    
  BigDecimal amount1 = val.toBigDecimal();
  BigDecimal amount = amount1 * 0.003280840
  String measure = "ft";

  if (debug) "In attr update depth: $val"
    
  // if metric assume in meters
  if (unitSystemIsMetric()) {
    measure = "m";
  }
 
  def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
  sendEvent(name: "depthLastUpdate", value: now)
    
  attributeUpdateString("false", "orphanedDepth");

  return (attributeUpdateNumber(amount, attribDepth, measure, 6));
}

// ------------------------------------------------------------

private Boolean attributeUpdatePM(String val, String attribPm) {

  BigDecimal pm = val.toBigDecimal();

  return (attributeUpdateNumber(pm, attribPm, "µg/m³"));
}

// ------------------------------------------------------------

private Boolean attributeUpdateAQI(String val, Boolean pm25, String attribAqi, String attribAqiDanger, String attribAqiColor) {
  //
  // Conversions based on https://en.wikipedia.org/wiki/Air_quality_index
  //
  BigDecimal pm = val.toBigDecimal();

  BigDecimal aqi;

  if (pm25) {
    // PM2.5
    if      (pm <  12.1) aqi = convertRange(pm,   0.0,  12.0,   0,  50);
    else if (pm <  35.5) aqi = convertRange(pm,  12.1,  35.4,  51, 100);
    else if (pm <  55.5) aqi = convertRange(pm,  35.5,  55.4, 101, 150);
    else if (pm < 150.5) aqi = convertRange(pm,  55.5, 150.4, 151, 200);
    else if (pm < 250.5) aqi = convertRange(pm, 150.5, 250.4, 201, 300);
    else if (pm < 350.5) aqi = convertRange(pm, 250.5, 350.4, 301, 400);
    else                 aqi = convertRange(pm, 350.5, 500.4, 401, 500);
  }
  else {
    // PM10
    if      (pm <  55)   aqi = convertRange(pm,   0,    54,     0,  50);
    else if (pm < 155)   aqi = convertRange(pm,  55,   154,    51, 100);
    else if (pm < 255)   aqi = convertRange(pm, 155,   254,   101, 150);
    else if (pm < 355)   aqi = convertRange(pm, 255,   354,   151, 200);
    else if (pm < 425)   aqi = convertRange(pm, 355,   424,   201, 300);
    else if (pm < 505)   aqi = convertRange(pm, 425,   504,   301, 400);
    else                 aqi = convertRange(pm, 505,   604,   401, 500);

    // Choose the highest AQI between PM2.5 and PM10
    BigDecimal aqi25 = (device.currentValue(attribAqi) as BigDecimal);
    if (aqi < aqi25) aqi = aqi25;
  }
  
    
  String danger;
  String color;

  if      (aqi <  51) { danger = "Good";                           color = "3ea72d"; }
  else if (aqi < 101) { danger = "Moderate";                       color = "fff300"; }
  else if (aqi < 151) { danger = "Unhealthy for Sensitive Groups"; color = "f18b00"; }
  else if (aqi < 201) { danger = "Unhealthy";                      color = "e53210"; }
  else if (aqi < 301) { danger = "Very Unhealthy";                 color = "b567a4"; }
  else if (aqi < 401) { danger = "Hazardous";                      color = "7e0023"; }
  else {                danger = "Hazardous";                      color = "7e0023"; }

 
   // lgk set airQualityIndex only if actual aqi not avg
   if (attribAqi == "aqi") attributeUpdateNumber(aqi, "airQualityIndex", "AQI");
  
   Boolean updated = attributeUpdateNumber(aqi, attribAqi, "AQI"); 
    
  if (attributeUpdateString(danger, attribAqiDanger)) updated = true;
  if (attributeUpdateString(color, attribAqiColor)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateCarbonDioxide(String val, String attribCo2) {

  BigDecimal co2 = val.toBigDecimal();
  return (attributeUpdateNumber(co2, attribCo2, "ppm"));
}

// ------------------------------------------------------------

private Boolean attributeUpdateLeak(String val, String attribWater, String attribWaterMsg, String attribWaterColor) {

  BigDecimal leak = (val.toBigDecimal())? 1: 0;
  String water, message, color;

  if (leak) {
    water = "wet";
    message = "Leak detected!";
    color = "ff0000";
  }
  else {
    water = "dry";
    message = "Dry";
    color = "ffffff";
  }

  Boolean updated = attributeUpdateString(water, attribWater);
  if (attributeUpdateString(message, attribWaterMsg)) updated = true;
  if (attributeUpdateString(color, attribWaterColor)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateLightningDistance(String val, String attrib) {

  if (!val) val = "0";

  BigDecimal distance = val.toBigDecimal();
  String measure = "km";

  // Convert to imperial if requested
  if (unitSystemIsMetric() == false) {
    distance = convert_km_to_mi(distance);
    measure = "mi";
  }

  return (attributeUpdateNumber(distance, attrib, measure, 1));
}

// ------------------------------------------------------------

private Boolean attributeUpdateLightningCount(String val, String attrib) {

  if (!val) val = "0";
 
  def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
  sendEvent(name: "lightningLastUpdate", value: now)
 
  return (attributeUpdateNumber(val.toBigDecimal(), attrib));
}
// ------------------------------------------------------------

private Boolean attributeUpdateHeatCount(String val, String attrib) {

  if (!val) val = "0";
 
  def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
  sendEvent(name: lastUpdate, value: now)
 
  return (attributeUpdateNumber(val.toBigDecimal(), attrib));
}

// ------------------------------------------------------------

private Boolean attributeUpdateLightningTime(String val, String attrib) {

  val = (!val || val == "0")? "n/a": timeEpochToLocal(val);

  return (attributeUpdateString(val, attrib));
}

// ------------------------------------------------------------

private Boolean attributeUpdateLightningEnergy(String val, String attrib) {

  if (!val) val = "0";

  return (attributeUpdateNumber(val.toBigDecimal(), attrib, "MJ/m", 1));
}

// ------------------------------------------------------------

private Boolean attributeUpdateUV(String val, String attribUvIndex, String attribUvDanger, String attribUvColor) {
  //
  // Conversions based on https://en.wikipedia.org/wiki/Ultraviolet_index
  //
  BigDecimal index = val.toBigDecimal();

  String danger;
  String color;

  if (index < 3)       { danger = "Low";       color = "3ea72d"; }
  else if (index < 6)  { danger = "Medium";    color = "fff300"; }
  else if (index < 8)  { danger = "High";      color = "f18b00"; }
  else if (index < 11) { danger = "Very High"; color = "e53210"; }
  else                 { danger = "Extreme";   color = "b567a4"; }

  Boolean updated = attributeUpdateNumber(index, attribUvIndex, "uvi");
  if (attributeUpdateString(danger, attribUvDanger)) updated = true;
  if (attributeUpdateString(color, attribUvColor)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateLight(String val, String attribSolarRadiation, String attribIlluminance) {

  BigDecimal light = val.toBigDecimal();

  Boolean updated = attributeUpdateNumber(light, attribSolarRadiation, "W/m²");
  if (attributeUpdateNumber(convert_Wm2_to_lux(light), attribIlluminance, "lux", 0)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateWindSpeed(String val, String attribWindSpeed) {

  BigDecimal speed = val.toBigDecimal();
  String measure = "mph";

  // Convert to metric if requested
  if (unitSystemIsMetric()) {
    speed = convert_mi_to_km(speed);
    measure = "km/h";
  }

   def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
   sendEvent(name: "windLastUpdate", value: now)
    
   attributeUpdateString("false", "orphanedWind");
    
  return (attributeUpdateNumber(speed, attribWindSpeed, measure, 1));
}

// ------------------------------------------------------------

private Boolean attributeUpdateCapacitorVoltage(String val, String attribCap, String attribPercent) {

  BigDecimal voltage = val.toBigDecimal();
    
  BigDecimal percent;
  BigDecimal vMin, vMax;
  Boolean updated = false;
    
  vMin = 3.2;
  vMax = 5.3;

  if (voltage > 5.3) voltage = 5.3
 
  percent = convertRange(voltage, vMin, vMax, 0, 100);
  
  if (attributeUpdateNumber(percent, attribPercent, "%")) updated = true 
  if (attributeUpdateNumber(voltage, attribCap, "V")) updated = true
    
  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateWindDirection(String val, String attribWindDirection, String attribWindCompass) {

  BigDecimal direction = val.toBigDecimal();

  // BigDecimal doesn't support modulo operation so we roll up our own
  direction = direction - (direction.divideToIntegralValue(360) * 360);

  String compass;

  if (direction >= 348.75 || direction < 11.25) compass = "N";
  else if (direction < 33.75)                   compass = "NNE";
  else if (direction < 56.25)                   compass = "NE";
  else if (direction < 78.75)                   compass = "ENE";
  else if (direction < 101.25)                  compass = "E";
  else if (direction < 123.75)                  compass = "ESE";
  else if (direction < 146.25)                  compass = "SE";
  else if (direction < 168.75)                  compass = "SSE";
  else if (direction < 191.25)                  compass = "S";
  else if (direction < 213.75)                  compass = "SSW";
  else if (direction < 236.25)                  compass = "SW";
  else if (direction < 258.75)                  compass = "WSW";
  else if (direction < 281.25)                  compass = "W";
  else if (direction < 303.75)                  compass = "WNW";
  else if (direction < 326.25)                  compass = "NW";
  else                                          compass = "NNW";

  Boolean updated = attributeUpdateNumber(direction, attribWindDirection, "°");
  if (attributeUpdateString(compass, attribWindCompass)) updated = true;

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateDewPoint(String val, String attribDewPoint, String attribHumidityAbs) {
  Boolean updated = false;

  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature != null) {
    if (settings.calcDewPoint == null) {
      // First time: initialize and show the preference
      device.updateSetting("calcDewPoint", [value: false, type: "bool"]);
    }
    else if (settings.calcDewPoint) {

      if (!unitSystemIsMetric()) {
        // Convert temperature to C
        temperature = convert_F_to_C(temperature);
      }

      // Calculate dewPoint based on https://web.archive.org/web/20150209041650/http://www.gorhamschaffler.com:80/humidity_formulas.htm
      BigDecimal humidity = val.toBigDecimal();

      double tC = temperature as double;

      // Calculate saturation vapor pressure in millibars
      BigDecimal e = (tC < 0) ?
        6.1115 * Math.exp((23.036 - (tC / 333.7)) * (tC / (279.82 + tC))) :
        6.1121 * Math.exp((18.678 - (tC / 234.4)) * (tC / (257.14 + tC)));

      // Calculate current vapor pressure in millibars
      e *= humidity / 100;

      BigDecimal degrees = (-430.22 + 237.7 * Math.log(e)) / (-Math.log(e) + 19.08);

      // Calculate humidityAbs based on https://carnotcycle.wordpress.com/2012/08/04/how-to-convert-relative-humidity-to-absolute-humidity/
      BigDecimal volume = ((6.1121 * Math.exp((17.67 * tC) / (tC + 243.5)) * (humidity as double) * 2.1674)) / (tC + 273.15);

      if (!unitSystemIsMetric()) {
        degrees = convert_C_to_F(degrees);
        volume = convert_gm3_to_ozyd3(volume);
      }

      if (attributeUpdateTemperature(degrees.toString(), attribDewPoint)) updated = true;
      if (attributeUpdateNumber(volume, attribHumidityAbs, unitSystemIsMetric()? "g/m³": "oz/yd³", 2)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateHeatIndex(String val, String attribHeatIndex, String attribHeatDanger, String attribHeatColor) {
  Boolean updated = false;

  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature != null) {
    if (settings.calcHeatIndex == null) {
      // First time: initialize and show the preference
      device.updateSetting("calcHeatIndex", [value: false, type: "bool"]);
    }
    else if (settings.calcHeatIndex) {
      if (unitSystemIsMetric()) {
        // Convert temperature back to F
        temperature = convert_C_to_F(temperature);
      }

      // Calculate heatIndex based on https://en.wikipedia.org/wiki/Heat_index
      BigDecimal degrees;
      String danger;
      String color;

      if (temperature < 80)  {
        degrees = temperature;
        danger = "Safe";
        color = "ffffff";
      }
      else {
        BigDecimal humidity = val.toBigDecimal();

        degrees = -42.379 +
                 (  2.04901523 * temperature) +
                 ( 10.14333127 * humidity) -
                 (  0.22475541 * (temperature * humidity)) -
                 (  0.00683783 * (temperature ** 2)) -
                 (  0.05481717 * (humidity ** 2)) +
                 (  0.00122874 * ((temperature ** 2) * humidity)) +
                 (  0.00085282 * (temperature * (humidity ** 2))) -
                 (  0.00000199 * ((temperature ** 2) * (humidity ** 2)));

        if      (degrees < 80)  { danger = "Safe";            color = "ffffff"; }
        else if (degrees < 91)  { danger = "Caution";         color = "ffff66"; }
        else if (degrees < 104) { danger = "Extreme Caution"; color = "ffd700"; }
        else if (degrees < 126) { danger = "Danger";          color = "ff8c00"; }
        else                    { danger = "Extreme Danger";  color = "ff0000"; }
      }

      updated = attributeUpdateTemperature(degrees.toString(), attribHeatIndex);
      if (attributeUpdateString(danger, attribHeatDanger)) updated = true;
      if (attributeUpdateString(color, attribHeatColor)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateSimmerIndex(String val, String attribSimmerIndex, String attribSimmerDanger, String attribSimmerColor) {
  Boolean updated = false;

  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature != null) {
    if (settings.calcSimmerIndex == null) {
      // First time: initialize and show the preference
      device.updateSetting("calcSimmerIndex", [value: false, type: "bool"]);
    }
    else if (settings.calcSimmerIndex) {
      if (unitSystemIsMetric()) {
        // Convert temperature back to F
        temperature = convert_C_to_F(temperature);
      }

      // Calculate heatIndex based on https://www.vcalc.com/wiki/rklarsen/Summer+Simmer+Index
      BigDecimal degrees;
      String danger;
      String color;       

      if (temperature < 70)  {
        degrees = temperature;
        danger = "Cool";
        color = "ffffff";
      }
      else {
        BigDecimal humidity = val.toBigDecimal();

        degrees = 1.98 * (temperature - (0.55 - (0.0055 * humidity)) * (temperature - 58.0)) - 56.83;

        if      (degrees < 70)  { danger = "Cool";                          color = "ffffff"; }
        else if (degrees < 77)  { danger = "Slightly Cool";                 color = "0099ff"; }
        else if (degrees < 83)  { danger = "Comfortable";                   color = "2dca02"; }
        else if (degrees < 91)  { danger = "Slightly Warm";                 color = "9acd32"; }
        else if (degrees < 100) { danger = "Increased Discomfort";          color = "ffb233"; }
        else if (degrees < 112) { danger = "Caution Heat Exhaustion";       color = "ff6600"; }
        else if (degrees < 125) { danger = "Danger Heatstroke";             color = "ff3300"; }
        else if (degrees < 150) { danger = "Extreme Danger";                color = "ff0000"; }
        else                    { danger = "Circulatory Collapse Imminent"; color = "cc3300"; }
      }

      updated = attributeUpdateTemperature(degrees.toString(), attribSimmerIndex);
      if (attributeUpdateString(danger, attribSimmerDanger)) updated = true;
      if (attributeUpdateString(color, attribSimmerColor)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateWindChill(String val, String attribWindChill, String attribWindDanger, String attribWindColor) {
  Boolean updated = false;

  BigDecimal temperature = (device.currentValue("temperature") as BigDecimal);
  if (temperature != null) {
    if (settings.calcWindChill == null) {
      // First time: initialize and show the preference
      device.updateSetting("calcWindChill", [value: false, type: "bool"]);
    }
    else if (settings.calcWindChill) {
      if (unitSystemIsMetric()) {
        // Convert temperature back to F
        temperature = convert_C_to_F(temperature);
      }

      // Calculate windChill based on https://en.wikipedia.org/wiki/Wind_chill
      BigDecimal degrees;
      String danger;
      String color;   

      BigDecimal windSpeed = val.toBigDecimal();

      if (temperature > 50 || windSpeed < 3) {
        degrees = temperature;
        danger = "Safe";
        color = "ffffff";
      }
      else {
        degrees = 35.74 +
                ( 0.6215 * temperature) -
                (35.75 * (windSpeed ** 0.16)) +
                ((0.4275 * temperature) * (windSpeed ** 0.16));

        if      (degrees < -69) { danger = "Frostbite certain";  color = "2d2c52"; }
        else if (degrees < -19) { danger = "Frostbite likely";   color = "1f479f"; }
        else if (degrees < 1)   { danger = "Frostbite possible"; color = "0c6cb5"; }
        else if (degrees < 21)  { danger = "Very Unpleasant";    color = "2f9fda"; }
        else if (degrees < 41)  { danger = "Unpleasant";         color = "9dc8e6"; }
        else                    { danger = "Safe";               color = "ffffff"; }
      }

      updated = attributeUpdateTemperature(degrees.toString(), attribWindChill);
      if (attributeUpdateString(danger, attribWindDanger)) updated = true;
      if (attributeUpdateString(color, attribWindColor)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Boolean attributeUpdateHtml(String templHtml, String attribHtml) {

  Boolean updated = false;

  String pattern = /\$\{([^}]+)\}/;

  String index;
  String val;

  for (Integer idx = 0; idx < 16; idx++) {
    index = idx? "${idx}": "";

    val = device.getDataValue("${templHtml}${index}");
    if (!val) break;

    val = val.replaceAll(~pattern) { java.util.ArrayList match -> (device.currentValue(match[1].trim()) as String); }
    if (attributeUpdateString(val, "${attribHtml}${index}")) updated = true;
  }

  return (updated);
}

// ------------------------------------------------------------

Boolean attributeUpdate(String key, String val) {
  //
  // Dispatch attributes changes to hub
  //

  Boolean updated = false;
  Boolean bundled = device.getDataValue("isBundled");
 
  def now = new Date().format('MM/dd/yyyy h:mm a',location.timeZone)
  sendEvent(name: "lastUpdate", value: now)
  if (debug) log.debug "got key $key val = $val"
      
  switch (key) {

      
  case "wh26batt":
    if (bundled) {
      state.sensorTemp = 1;
      updated = attributeUpdateBattery(val, "batteryTemp", "batteryTempIcon", "batteryTempOrg", 0);  // !boolean
    }
    else {
      state.sensor = 1;
      updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 0);
    }
    break;

  case "wh40batt":
    if (bundled) {
      state.sensorRain = 1;
      updated = attributeUpdateBattery(val, "batteryRain", "batteryRainIcon", "batteryRainOrg", 1);  // voltage
    }
    else {
      state.sensor = 1;
      updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 1);
    }
    break;

  case "wh68batt":
  case "wh80batt":
    if (bundled) {
      state.sensorWind = 1;
      updated = attributeUpdateBattery(val, "batteryWind", "batteryWindIcon", "batteryWindOrg", 1);  // voltage
    }
    else {
      state.sensor = 1;
      updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 1);
    }
    break;

  case ~/batt[1-8]/:
  case "wh25batt":
  case "wh65batt":
  case "ws90batt":
  case "wh90batt":
    state.sensor = 1;
    updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 0);  // !boolean
    break;

   case ~/ws90cap_volt[1-8]/:
   case "ws90cap_volt":
    state.sensor = 1;
    updated = attributeUpdateCapacitorVoltage(val, "capacitorVoltage", "capacitorPercent");
    break;
 
   case ~/ws90_ver[1-8]/:
   case "ws90_ver":
    state.sensor = 1;
    updated = attributeUpdateString(val, "ws90Firmware");
    break;  
      
  case ~/batt_wf[1-8]/:
  case ~/soilbatt[1-8]/:
  case ~/tf_batt[1-8]/:
  case ~/ldsbatt[1-4]/:  
    state.sensor = 1;
    updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 1);  // voltage
    break;

  case ~/pm25batt[1-4]/:
  case ~/leakbatt[1-4]/:
 
  case "wh57batt":
  case "co2_batt":
    state.sensor = 1; 
    updated = attributeUpdateBattery(val, "battery", "batteryIcon", "batteryOrg", 2);  // 0 - 5
    break;

   // lgk wh54 laser depth sensor attributes
  case ~/ldsheat_ch[1-4]/:
    state.sensor = 1
    updated = attributeUpdateHeatCount(val, "heatLDS");  
    break;
      
  case ~/air_ch[1-4]/:
    state.sensor = 1;
    updated = attributeUpdateDepth(val,"airHeight")
    break;
 
  case ~/thi_ch[1-4]/:
    state.sensor = 1;
    updated = attributeUpdateDepth(val,"totalHeight")
    break;
      
  case ~/depth_ch[1-4]/:
    state.sensor = 1;
      // lgk but here all numbers are returned in mm irregardless of foot setting of device so convert
    if (debugDepthStatisics) log.info "got depth = $val"
    updated = attributeUpdateDepth(val,"depth")
    if (useWh54ForSnowDepthCalculations) state.rawDepth = val;
    break;  
      
  case "tempinf":
  case "tempf":
  case ~/tempf_wf[1-8]/:
  case ~/temp[1-8]f/:
  case ~/tf_ch[1-8]/:
  case "tf_co2":
      
    state.sensor = 1;
    if (debug) log.debug "Updating Temp: $val"
      
    updated = attributeUpdateTemperature(val, "temperature");
      
    if (updated)  sendEvent(name: "temperatureLastUpdate", value: now)
    break;

  case "humidity":
  case "humidityin":
  case ~/humidity_wf[1-8]/:
  case ~/humidity[1-8]/:
  case ~/soilmoisture[1-8]/:
  case "humi_co2":
    state.sensor = 1;
    if (debug) log.debug "Updating humidity: $val"
      
    updated = attributeUpdateHumidity(val, "humidity");
    if (attributeUpdateDewPoint(val, "dewPoint", "humidityAbs")) updated = true;
    if (attributeUpdateHeatIndex(val, "heatIndex", "heatDanger", "heatColor")) updated = true;
    if (attributeUpdateSimmerIndex(val, "simmerIndex", "simmerDanger", "simmerColor")) updated = true;
    break;

  case ~/baromrelin_wf[1-8]/:
  case "baromrelin":
    state.sensor = 1;
    // we ignore this value as we do our own correction
    break;

  case ~/baromabsin_wf[1-8]/:
  case "baromabsin":
    state.sensor = 1;
    updated = attributeUpdatePressure(val, "pressure", "pressureAbs");
    break;

  case ~/rainratein_wf[1-8]/:
  case "rainratein":
  case "rrain_piezo":
    state.sensor = 1;
    state.sensorRain = 1;
    updated = attributeUpdateRain(val, "rainRate", true);
    break;

  case ~/srain_piezo[1-8]/:
  case "srain_piezo":
    if (debug) log.debug "Updating raining: $val"  
    state.sensor = 1
    if (val == "1")
      updated = attributeUpdateString("true","raining");
    else updated = attributeUpdateString("false","raining"); 
    break;
      
  case ~/eventrainin_wf[1-8]/:
  case "eventrainin":
  case "erain_piezo":
    state.sensor = 1;
    state.sensorRain = 1;  
    updated = attributeUpdateRain(val, "rainEvent");
    break;

  case ~/hourlyrainin_wf[1-8]/:
  case "hourlyrainin":
  case "hrain_piezo":
    state.sensor = 1;
    updated = attributeUpdateRain(val, "rainHourly");
    break;

  case ~/dailyrainin_wf[1-8]/:
  case "dailyrainin":
  case "drain_piezo":
    state.sensor = 1
    state.sensorRain = 1; 
    updated = attributeUpdateRain(val, "rainDaily");
    break;
      
  case ~/last24hrainin_wf[1-8]/:
  case "last24hrainin":
  case "last24hrain_piezo":
    updated = attributeUpdateRain(val, "rainLast24Hrs");
    break;    
      
  case ~/weeklyrainin_wf[1-8]/:
  case "weeklyrainin":
  case "wrain_piezo": 
    state.sensor = 1;
    state.sensorRain = 1;  
    updated = attributeUpdateRain(val, "rainWeekly");
    break;

  case ~/monthlyrainin_wf[1-8]/:
  case "monthlyrainin":
  case "mrain_piezo": 
    state.sensor = 1;
    state.sensorRain = 1;  
    updated = attributeUpdateRain(val, "rainMonthly");
    break;

  case ~/yearlyrainin_wf[1-8]/:
  case "yearlyrainin":
  case "yrain_piezo": 
    state.sensor = 1;
    state.sensorRain = 1;
    updated = attributeUpdateRain(val, "rainYearly");
    break;

  case ~/totalrainin_wf[1-8]/:
  case "totalrainin":
  case "train_piezo": 
  case "totalainin":
    state.sensor = 1;
    state.sensorRain = 1;  
    updated = attributeUpdateRain(val, "rainTotal"); 
    break;

  case ~/pm25_ch[1-4]/:
  case "pm25_co2":
    state.sensor = 1;
    updated = attributeUpdatePM(val, "pm25");
    if (attributeUpdateAQI(val, true, "aqi", "aqiDanger", "aqiColor")) updated = true;
    break;

  case ~/pm25_avg_24h_ch[1-4]/:
  case "pm25_24h_co2":
    state.sensor = 1;
    updated = attributeUpdatePM(val, "pm25_avg_24h");
    if (attributeUpdateAQI(val, true, "aqi_avg_24h", "aqiDanger_avg_24h", "aqiColor_avg_24h")) updated = true;
    break;

  case "pm10_co2":
    state.sensor = 1;
    updated = attributeUpdatePM(val, "pm10");
    if (attributeUpdateAQI(val, false, "aqi", "aqiDanger", "aqiColor")) updated = true;
    break;

  case "pm10_24h_co2":
    state.sensor = 1;
    updated = attributeUpdatePM(val, "pm10_avg_24h");
    if (attributeUpdateAQI(val, false, "aqi_avg_24h", "aqiDanger_avg_24h", "aqiColor_avg_24h")) updated = true;
    break;

  case "co2":
    state.sensor = 1;
    updated = attributeUpdateCarbonDioxide(val, "carbonDioxide");
    break;

  case "co2_24h":
    state.sensor = 1;
    updated = attributeUpdateCarbonDioxide(val, "carbonDioxide_avg_24h");
    break;

  case ~/leak_ch[1-4]/:
    state.sensor = 1;
    updated = attributeUpdateLeak(val, "water", "waterMsg", "waterColor");
    break;

  case ~/lightning_wf[1-8]/:
  case "lightning":
    state.sensor = 1;
    updated = attributeUpdateLightningDistance(val, "lightningDistance");
    break;

  case ~/lightning_num_wf[1-8]/:
  case "lightning_num":
    state.sensor = 1;
    updated = attributeUpdateLightningCount(val, "lightningCount");
    break;

  case ~/lightning_time_wf[1-8]/:
  case "lightning_time":
    state.sensor = 1;
    updated = attributeUpdateLightningTime(val, "lightningTime");
    break;

  case ~/lightning_energy_wf[1-8]/:
    state.sensor = 1;
    updated = attributeUpdateLightningEnergy(val, "lightningEnergy");
    break;

  case ~/uv_wf[1-8]/:
  case "uv":
    state.sensor = 1;
    updated = attributeUpdateUV(val, "ultravioletIndex", "ultravioletDanger", "ultravioletColor");
    break;

  case ~/solarradiation_wf[1-8]/:
  case "solarradiation":
    state.sensor = 1;
    updated = attributeUpdateLight(val, "solarRadiation", "illuminance");
    break;

  case ~/winddir_wf[1-8]/:
  case "winddir":
    state.sensor = 1;
    state.sensorWind = 1;
    updated = attributeUpdateWindDirection(val, "windDirection", "windCompass");
    break;

  case ~/winddir_avg10m_wf[1-8]/:
  case "winddir_avg10m":
    state.sensor = 1;
    state.sensorWind = 1;
    updated = attributeUpdateWindDirection(val, "windDirection_avg_10m", "windCompass_avg_10m");
    break;

  case ~/windspeedmph_wf[1-8]/:
  case "windspeedmph":
    state.sensor = 1;
    state.sensorWind = 1;
    updated = attributeUpdateWindSpeed(val, "windSpeed");
    if (attributeUpdateWindChill(val, "windChill", "windDanger", "windColor")) updated = true;
    break;

  case ~/windspdmph_avg10m_wf[1-8]/:
  case "windspdmph_avg10m":
    state.sensor = 1;
    state.sernsorWind = 1;
    updated = attributeUpdateWindSpeed(val, "windSpeed_avg_10m");
    break;

  case ~/windgustmph_wf[1-8]/:
  case "windgustmph":
    state.sensor = 1;
    state.sensorWind = 1;
    updated = attributeUpdateWindSpeed(val, "windGust");
    break;

  case ~/maxdailygust_wf[1-8]/:
  case "maxdailygust":
    state.sensor = 1;
    state.sensorWind = 1;
    updated = attributeUpdateWindSpeed(val, "windGustMaxDaily");
    break;

  // lgk new code 5/25
 case ~/vpd[1-8]/:
 case "vpd":
    state.sensor = 1    
     Boolean metric = unitSystemIsMetric();   
     if (metric) attributeUpdateNumber(val.toBigDecimal(), "vpd", "inHg",4);
      else attributeUpdateNumber(val.toBigDecimal(), "vpd", "kPa",4);
     break;
    
  //
  // End Of Data
  //

  case "endofdata":
    if (updateSensorStatus(bundled)) {
      // Sensor or part the PWS bundle is not receiving data
      if (!ztatusIsError()) ztatus("Orphaned", "orange");
    }
    else {
      // Sensor or all parts of the PWS bundle are receiving data      
      if (!ztatusIsError()) ztatus("OK", "green"); 

      // If we are a bundled PWS sensor, at the endofdata we update the "virtual" battery with the lowest of all the "physical" batteries
      if (bundled) updated = attributeUpdateLowestBattery();

      // Update templates if any
      if (attributeUpdateHtml("htmlTemplate", "html")) updated = true;
    }
    break;

  default:
    logDebug("Unrecognized attribute: ${key} = ${val}");
    break;
  }

  return (updated);
}

// -------------------------------------------------------------

Boolean updateSensorStatus(bundled) {
  Boolean orphaned = false; 

     if (debug) log.debug "In update sensors status bundled - $bundled"
         
  if (bundled) {
    if (state.sensorTemp != null) {
      if (debug) log.debug "In update temp sensor"
      if (state.sensorTemp == 0) orphaned = true;
      attributeUpdateString(state.sensorTemp? "false": "true", "orphanedTemp");
      state.sensorTemp = 0;
    }
    
    if (state.sensorRain != null) {
        
      if (debug) log.debug "in update rain sensor"
      if (state.sensorRain == 0) orphaned = true;
      attributeUpdateString(state.sensorRain? "false": "true", "orphanedRain");
      state.sensorRain = 0;
    }
     
    if (state.sensorWind != null) { 
        if (debug) log.debug "in update wind sensor"
      if (state.sensorWind == 0) orphaned = true;
      attributeUpdateString(state.sensorWind? "false": "true", "orphanedWind");
      state.sensorWind = 0;
    }      
  }
  else {
    if (state.sensor != null) {
        if (debug) log.debug "in update ${state.sensor} sensor"
      if (state.sensor == 0) orphaned = true;
      attributeUpdateString(state.sensor? "false": "true", "orphaned");
    }
  }

  if (state.sensor != null) state.sensor = 0;

  return (orphaned);
}

// HTML templates --------------------------------------------------------------------------------------------------------------

private Object htmlGetRepository() {
  //
  // Return an Object containing all the templates
  // or null if something went wrong
  //
    //log.warn "in html get repostitory"
    
  Object repository = null;

  try {
  //  String repositoryText = "https://mircolino.github.io/ecowitt/ecowitt.json".toURL().getText(); 
      String repositoryText = "http://mail.lgk.com/ecowitt1.css".toURL().getText();
     // log.debug "got json = $repositoryText"
    if (repositoryText) {
      // text -> json
      Object parser = new groovy.json.JsonSlurper();
      repository = parser.parseText(repositoryText);
    }
  }
  catch (Exception e) {
    logError("Exception in versionUpdate(): ${e}");
  }

  return (repository);
}

// ------------------------------------------------------------

private Integer htmlCountAttributes(String htmlAttrib) {
  //
  // Return the number of html attributes the driver has
  //
  Integer count = 0;

  // Get a list of all attributes (present/null or not)
  List<String> attribDrv = attributeEnumerate(false);
  String attrib;

  for (Integer idx = 0; idx < 16; idx++) {
    attrib = idx? "${htmlAttrib}${idx}": htmlAttrib;

    if (attribDrv.contains(attrib) == false) break;
    count++;
  }

  return (count);
}

// ------------------------------------------------------------

private Boolean htmlSetAttributes(String val, String htmlAttrib, Integer count, Boolean onlyPresent) {

  Boolean updated = false;

  String attrib;

  for (Integer idx = 0; idx < count; idx++) {
    attrib = idx? "${htmlAttrib}${idx}": htmlAttrib;

    if (onlyPresent == false || device.currentValue(attrib) != null) {
      if (attributeUpdateString(val, attrib)) updated = true;
    }
  }

  return (updated);
}

// ------------------------------------------------------------

private Integer htmlValidateTemplate(String htmlTempl, String htmlAttrib, Integer count) {
  //
  // Return  <0) number of invalid attributes in "htmlTempl"
  //        >=0) number of valid attributes in "htmlTempl"
  // Template is valid only if return > 0
  //
  String pattern = /\$\{([^}]+)\}/;

  // Build a list of valid attributes names excluding the null ones and ourself (for obvious reasons)
  List<String> attribDrv = attributeEnumerate();
  String attrib;

  for (Integer idx = 0; idx < count; idx++) {
    attrib = idx? "${htmlAttrib}${idx}": htmlAttrib;

    attribDrv.remove(attrib);
  }

  // Go through all the ${attribute} expressions in the htmlTempl and collect both good and bad ones
  List<String> attribOk = [];
  List<String> attribErr = [];

  htmlTempl.findAll(~pattern) { java.util.ArrayList match ->
    attrib = match[1].trim();

    if (attribDrv.contains(attrib)) attribOk.add(attrib);
    else attribErr.add(attrib);
  }

  if (attribErr.size() != 0) return (-attribErr.size());
  return (attribOk.size());
}

// ------------------------------------------------------------

private List<String> htmlGetUserInput(String input, Integer count) {
  //
  // Return null if user input is null or empty
  // Return empty list if user input is invalid: template(s) not found, duplicates, too many, etc.
  // Otherwise return a list of (unvalidated) templates entered by the user
  //
  if (!input) return (null);

  List<String> templateList = [];

  if (input.find(/[<>{};:=\'\"#&\$]/)) {
    // If input has at least one typical html character, then it's a real template
    templateList.add(input);
  }
  else {
    // Input is an array of repository template IDs
    List<String> idList = input.tokenize("[, ]");
    if (idList) {
      // We found at least one template ID in the user input, make sure they are not too many
      Object repository = htmlGetRepository();
      if (repository) {
        Boolean metric = unitSystemIsMetric();

        for (Integer idx = 0; idx < idList.size(); idx++) {
          // Try first the normal templates
          input = repository.templates."${idList[idx]}";

          // If not found try the unit templates
          if (!input) input = metric? repository.templatesMetric."${idList[idx]}": repository.templatesImperial."${idList[idx]}";

          // If still not found, or already found, or exceeded number of templates, return error
          if (!input || templateList.contains(input) || templateList.size() == count) return ([]);

          // Good one, let's add it
          templateList.add(input);
        }
      }
    }
  }

  return (templateList);
}

// ------------------------------------------------------------

private String htmlUpdateUserInput(String input) {
  //
  // Return true if HTML templates have been pre-processed sucesfully
  //
  String htmlTemplate = "htmlTemplate";
  String htmlAttrib = "html";

  String template;

  // Get the maximum number of supported templates
  Integer count = htmlCountAttributes(htmlAttrib);
    
  if (!count) {
    // Return if we do not support HTML templates
    return ("");
  }

  // Cleanup previous states
  htmlSetAttributes("n/a", htmlAttrib, count, true);

  for (Integer idx = 0; idx < count; idx++) {
    template = idx? "${htmlTemplate}${idx}": htmlTemplate;
      
    if (device.getDataValue(template)) device.updateDataValue(template, null);
    device.data.remove(template);
  }

  // Parse user input
  List<String> templateList = htmlGetUserInput(input, count);
    
  if (templateList == null) {
    // Templates are disabled/empty
    return ("");
  }

  if (templateList.size() == 0) {
    // Invalid user input
    return ("Invalid template(s) id, count or repetition");
  }

  for (Integer idx = 0; idx < templateList.size(); idx++) {
    // We have valid templates: let's validate them
    if (htmlValidateTemplate(templateList[idx], htmlAttrib, count) < 1) {
      // Invalid or no attribute in template
      log.warn "found invalid id = $idx"
      return ("Invalid attribute or template for the current sensor");
    }
  }

  // Finally! We have a (1 <= number <= count) of valid templates: let's write them down
    //log.debug "template size = ${templateList.size()}"
  for (Integer idx = 0; idx < templateList.size(); idx++) {
    template = idx? "${htmlTemplate}${idx}": htmlTemplate;

    device.updateDataValue(template, templateList[idx]);
  }

  htmlSetAttributes("pending", htmlAttrib, templateList.size(), false);

  return ("");
}

// Driver lifecycle -----------------------------------------------------------------------------------------------------------

void installed() {
  try {
    logDebug("addedSensor(${device.getDeviceNetworkId()})");
  }
  catch (Exception e) {
    logError("Exception in installed(): ${e}");
  }
}

// ------------------------------------------------------------

void updated() {
  try {
  
    sendEvent(name: "temperatureChange", value: 0.00) 
    sendEvent(name: "humidityChange",value: 0.00)
      
   if (debug)
    {
        log.debug "Turning off logging in 1/2 hour!"
        runIn(1800,logsOff)
    }     
    
     Boolean bundled = device.getDataValue("isBundled")
     if (bundled == null)
      {
        sendEvent(name: "bundled", value: "false");
      }
     else
      {   
       sendEvent(name: "bundled", value: "true");   
      }
          
   
    // Clear previous states
    //state.clear();

      // lgk add code if sensor id is wh54 enable stats
      
      sensorid = device.currentValue("sensorID")
      log.info "sensor id = $sensorid"
      if (sensorid == "WH54")
      {
          log.warn "Found WH54 Laser Density Sensor... Enable depth recording setting (via command on device panel) if you want to record snow depth, otherwise just raw depth is recored!!"
          device.updateSetting("WH54LaserDensityDeviceEnabled",[value: true, type: "bool"])
          
          def mv = device.getSetting("voltageMin")
          def mm = device.getSetting("voltageMax")
          log.warn "MinVoltage input setting: $mv, MaxVoltage input setting: $mm, recommend you configure these so max is near 3.0 volts!"
          
      }
      else 
      {
          log.info "Device is Not a WH54!"
          device.updateSetting("WH54LaserDensityDeviceEnabled", [value: false, type: "bool"])
      }
      
       def ed = device.getSetting("useWh54ForSnowDepthCalculations")
       log.info "WH54 enable snow depth recording = $ed"
         
    if ((sensorid == "WH54") && (ed == true))
      { 
          log.info "Enable Snow/Density/Depth is on!"
          initializeGlobals(false)
      }
    
    // Pre-process HTML templates (if any)
    String error = htmlUpdateUserInput(settings.htmlTemplate as String);
    if (error) ztatus(error, "red");
    else ztatus("OK", "green");
   
   }
  catch (Exception e) {
    logError("Exception in updated(): ${e}");
  }
}

// ------------------------------------------------------------

void uninstalled() {
  try {
    // Notify the parent we are being deleted
    getParent().uninstalledChildDevice(device.getDeviceNetworkId());

    logDebug("deletedSensor(${device.getDeviceNetworkId()})");
  }
  catch (Exception e) {
    logError("Exception in uninstalled(): ${e}");
  }
}

// ------------------------------------------------------------

void parse(String msg) {
  try {
  }
  catch (Exception e) {
    logError("Exception in parse(): ${e}");
  }
}

def logsOff()
{
    log.debug "Turning off Logging!"
    device.updateSetting("debug",[value:"false",type:"bool"])
}


// Recycle bin ----------------------------------------------------------------------------------------------------------------

// lgk code for lds01 to accumulate snow depth in inches converted from feet by keep track of daily and monthly numbers only when it goes up
// from one day to another for now just assume inches or mm if metric.. assume that the sensors is set to the correct feet or meters to match

void addToDays(id, type)
{
   //if (debugDepthStatisics) log.debug "in add to day list: [$id, $type]"
    if (state.globalDays != null)
    { 
        globalDays = state.globalDays
    }
    
    globalDays.put(id.toString(), type) 
    state.globalDays = globalDays
}

void listDays()
{
    if (debugDepthStatisics)  log.debug "In list days"
    globalDays = state.globalDays
    if (debugDepthStatisics) log.debug "days = $globalDays"    
}

void addToMonths(id, type)
{
   // if (debugDepthStatisics) log.debug "in add to device list: [$id, $type]"
    if (state.globalMonths != null)
    {
        globalMonths = state.globalMonths
    }
    globalMonths.put(id.toString(), type)   
    state.globalMonths = globalMonths
}

void listMonths()
{
    log.info "In list Months"
    globalMonths = state.globalMonths
    log.info "months = $globalMonths"    
}

def initializeGlobals(force)
{
    log.info "In init globals - force init = $force"
  
    
    def gi = state.globalsInitialized  
    if ((gi != true) || (force == true))
    {
        
      log.info "Initializing globals for the first time"
          
      state.globalDays = [:]
      state.globalMonths = [:]
      state.lastRawDepth = 0.00
      state.dayDepth = 0.00
      state.currentMonthDepth = 0.00
      state.currentYearDepth = 0.00
      state.rawDepth = 0.00
      state.lastHourlyDepth = 0.00
      
      if (useWh54ForSnowDepthCalculations)
        { 
         sendEvent([name: 'snowHourly', value: 0.00, isStateChange: true]) 
         sendEvent([name: 'snowDaily', value: 0.00, isStateChange: true]) 
         sendEvent([name: 'snowMonthly', value: 0.00, isStateChange: true]) 
         sendEvent([name: 'snowYearly', value: 0.00, isStateChange: true])         
        
          for (i in 1..31)
           {
            addToDays(i,0.0.toFloat())
           }
    
          for (j in 1..12)
           {
            addToMonths(j,0.0.toFloat())
           }  
           
        } //use for snow depth   
        state.globalsInitialized = true
        
    } // FIRST TIME
    else
    {
        log.info "Globals already intialized"
    }
    
    // schedule daily and monthly jobs do this regardless of initialized or not in case they get removed.
    if (useWh54ForSnowDepthCalculations)
    {    
     unschedule("storeDailyDepth")
      schedule("0 55 23 ? * * *", "storeDailyDepth")
   
     unschedule("storeMonthlyDepth")
     schedule("0 2 0 1 1-12 ? *","storeMonthlyDepth")
        
     unschedule("storeHourlyDepth")
     schedule(" 0 5 * ? * * *","storeHourlyDepth")
    }
    else
    {
     unschedule("storeDailyDepth")
     unschedule("storeMonthlyDepth")
     unschedule("storeHourlyDepth")
    }
}


def storeHourlyDepth()
{ 
    log.info "In store hourly depth"
   
    if ((state.rawDepth != null) && (state.rawDepth != 0))
    { 
         // compare state lastRwDepth to rawDepth and only if positive store it
         def BigDecimal ld = state.lastHourlyDepth.toFloat().round(4).toBigDecimal();
         def BigDecimal nd = state.rawDepth.toFloat().round(4).toBigDecimal();
        
        if (ld == null) ld = 0.00
        
          if (debugDepthStatisics) log.info "In store hourly depth  last hourly Depth = ${ld}, current depth = ${nd}"
    
            // convert to inches
    
          if (nd > ld)
          {
            if (debugDepthStatisics) log.info "Depth has gone up!"
            def BigDecimal depthDiff = nd - ld
            if (debugDepthStatisics) log.info "Depth diff = $depthDiff"
        
            def BigDecimal dd = depthDiff
            // depth diff is always in mm
            if (unitSystemIsMetric() == false) 
              {
               //convert to inches or mm if decimal
               // lgk this is in mm so covert to feet then inches
               dd = (depthDiff * 0.003280840) * 12.0000000
              }
              
            if (debugDepthStatisics) log.info "new hourly depth in inches/mm = $dd"
            def BigDecimal rounddd = dd.toFloat().round(1).toBigDecimal();
            sendEvent([name: 'snowHourly', value: rounddd, isStateChange: true]) 
             
            if (debugDepthStatisics) 
              {
                  log.info "Accumulating hourly depth total:  = $dd"
                  log.info "Resetting last hourly depth!"
              }
              
            state.lastHourlyDepth = nd          
          }
      else 
          {
           if (debugDepthStatisics) log.info "Hourly Depth has not gone up!"
           sendEvent([name: 'snowHourly', value: 0.0, isStateChange: true]) 
           // leave last depth the same    
          } 
      }
}

def storeDailyDepth()
{ 
    log.info "In store daily depth"
   
    if ((state.rawDepth != null) && (state.rawDepth != 0))
    { 
         // compare state lastRwDepth to rawDepth and only if positive store it
         def BigDecimal ld = state.lastRawDepth.toFloat().round(4).toBigDecimal();
         def BigDecimal nd = state.rawDepth.toFloat().round(4).toBigDecimal();
        
        if (ld == null) ld = 0.00
        
          if (debugDepthStatisics) log.info "In store daily depth  last Raw Depth = ${ld}, current depth = ${nd}"
    
            // convert to inches
    
          if (nd > ld)
          {
            if (debugDepthStatisics) log.info "Depth has gone up!"
            def BigDecimal depthDiff = nd - ld
            if (debugDepthStatisics) log.info "Depth diff = $depthDiff"
        
            
            def BigDecimal dd = depthDiff
            // depth diff is always in mm
            if (unitSystemIsMetric() == false) 
              {
               //convert to inches or mm if decimal
               // lgk this is in mm so covert to feet then inches
               dd = (depthDiff * 0.003280840) * 12.0000000
              }  
           
            if (debugDepthStatisics) log.info "New depth in inches/mm = $dd"
   
            // total day status
          
            def BigDecimal rounddd =  dd.toFloat().round(1).toBigDecimal();
            state.lastDayDepth = rounddd
              
            if (debugDepthStatisics) 
              {
                  log.info "Accumulating day depth total:  = $dd"
                  log.info "Resetting last depth to todays!"
              }
              
            state.lastRawDepth = nd          
          }
        
      else if ((nd < ld) && (nd > 0.0000)) // if went down not zero and not negative
          {
             if (debugDepthStatisics) log.info "Depth has gone down (melting?) old: $ld, new: $nd - resetting last depth to current depth!"
             state.lastRawDepth = nd
             state.lastDayDepth = 0.00
             if (debugDepthStatisics) log.info "Also resetting/reducing lastHourlyDepth to last reading: $nd from ${state.lastHourlyDepth} - otherwise hourly depths never go up due to melting!"
             state.lastHourlyDepth = nd
          }
        
      else 
          {
           if (debugDepthStatisics) log.info "Depth has not gone up!"
           state.lastDayDepth = 0.00
           // leave last depth the same but put 0 in array
          } 
          
        // now store in table for current day
        def now = new Date().format('dd', location.timeZone) 
        def intday = now.toInteger() 
        
        // test only uncomment for test a different day
        //intday = intday + 1
        
         if (debugDepthStatisics) log.info "Day of month: $intday"
          
         addToDays(intday,state.lastDayDepth) 
          if (debugDepthStatisics) log.info "day total: ${state.lastDayDepth}, global day stats: ${state.globalDays}"    
    
         sendEvent([name: 'snowDaily', value: state.lastDayDepth, isStateChange: true]) 
         sendEvent([name: 'monthDepthStats', value: state.globalDays, isStateChange: true])         
    
        // now add last days depth to the monthly and yearly running totals
        state.currentMonthDepth = state.currentMonthDepth + state.lastDayDepth
        state.currentYearDepth = state.currentYearDepth + state.lastDayDepth
        
        def BigDecimal roundmonthly =  state.currentMonthDepth.toFloat().round(1).toBigDecimal();
        def BigDecimal roundyearly =  state.currentYearDepth.toFloat().round(1).toBigDecimal();
        sendEvent([name: 'snowMonthly', value: roundmonthly, isStateChange: true]) 
        sendEvent([name: 'snowYearly', value: roundyearly, isStateChange: true])  
    
        // reset raw depth in that way if we dont get any readings it will still be zero and nothing will be logged.
        state.rawDepth = 0.000
      }
}
                 
def storeMonthlyDepth()
{ 
   // monthly depth is not an average so just add up depth from each day
    //listDays()   
    
    log.info "In store Monthly depth"
    // called on the first of month so need to go back a month
    
     def themonth = new Date().format('MM').toInteger()
     if (debugDepthStatisics)  log.info "Current month: $themonth"
    
     def lastmonth = 0
    
    if (themonth == 1)
       lastmonth = 12
     else lastmonth = themonth - 1
      
    // test  only uncomment to force test a specific month
    //lastmonth = 11
    
     if (debugDepthStatisics) log.info "Last Month: $lastmonth"
    
     def theday = new Date().format('dd')
     def theyear = new Date().format('YYYY')
     
     def LocalDate ldate = LocalDate.of(theyear.toInteger(),lastmonth.toInteger(),1)
     def int mdays = ldate.lengthOfMonth();
     if (debugDepthStatisics) log.info "days in last month = $mdays"
    
     // now loop through those days and get totals
    def float runningTotal = 0.00
    
    def int loopctr = 1
    globalDays = state.globalDays
 
    while (loopctr <= mdays)
    {
     def dayvalue = globalDays.get(loopctr.toString())
     if (debugDepthStatisics) log.info "day: $loopctr, value: $dayvalue"
     runningTotal = runningTotal + dayvalue
     loopctr++
    }
    
    if (debugDepthStatisics) log.info "end of loop total = $runningTotal"
    
   def BigDecimal monthstat = runningTotal.toFloat().round(1).toBigDecimal()
   state.lastMonthDepth = monthstat
    
   addToMonths(lastmonth,monthstat)   
   log.info "month total: $runningTotal, days: $mdays, depth: ${state.lastMonthDepth}, global month stats: ${globalMonths}"   
    
   sendEvent([name: 'snowLastMonth', value: monthstat, isStateChange: true]) 
   sendEvent([name: 'yearDepthStats', value: state.globalMonths, isStateChange: true])  
    
   // reset day array on month turnover
   for (i in 1..31)
    {
     addToDays(i,0.0.toFloat())
    }
    
   // reset to start current month over
   state.currentMonthDepth = 0.00
   sendEvent([name: 'currentMonthDepth', value: 0.00, isStateChange: true])  
}
                
def storeYearlyDepth()
{ 
   // monthly depth is not an average so just add up depth from each day
    //listDays()   
    
    log.info "In store Yearly Stats"
  
     // now loop through month array and get totals
    def float runningTotal = 0.00
    
    def int loopctr = 1
    globalMonths = state.globalMonths
 
    while (loopctr <= 12)
    {
     def monthvalue = globalMonths.get(loopctr.toString())
     if (debugDepthStatisics) log.info "month: $loopctr, value: $monthvalue"
     runningTotal = runningTotal + monthvalue
     loopctr++
    }
    
   if (debugDepthStatisics) log.info "end of loop total = $runningTotal"
    
   def BigDecimal yearstat = runningTotal.toFloat().round(1).toBigDecimal()   
   sendEvent([name: 'snowLastYear', value: yearstat, isStateChange: true])  
   
   // reset to start current month over
   state.currentYearDepth = 0.00
   sendEvent([name: 'snowYearly', value: 0.0, isStateChange: true])  
  
   // reset to start current month over
   state.currentMonthDepth = 0.00
   sendEvent([name: 'currentMonthDepth', value: 0.00, isStateChange: true])  
    
    // reset day  and month array
   for (i in 1..31)
    {
     addToDays(i,0.0.toFloat())
    } 
    
   for (j in 1..12)
    {
     addToMonths(j,0.0.toFloat())
    }  
    
  resetDepthTotals()
    
}


def testHourlyfx()
{
   storeHourlyDepth()
}

def testDailyfx()
{
   storeDailyDepth()
    
}

def testMonthlyfx()
{
    storeMonthlyDepth()
}

def accumulateAndResetYearlyTotals()
{
   storeYearlyDepth()
}


def testfx()
{   
   // initializeGlobals(true)
    //state.rawDepth = 23.09
//sendEvent([name: 'snowDaily', value: 0.00, isStateChange: true]) 
 //   sendEvent([name: 'snowMonthly', value: 0.00, isStateChange: true]) 
 //   sendEvent([name: 'snowYearly', value: 0.00, isStateChange: true])  
   // state.globalsInitialized = true
   // state.lastHourlyDepth = 0 
    state.lastRawDepth = 0.0
   // state.rawDepth = 40
   // storeHourlyDepth()
  //  storeDailyDepth() 
    //listDays()
   // storeMonthlyDepth()
      
}

def disableStats()
{
    log.info "Disabling statistics!"
    unschedule("storeDailyDepth")
    unschedule("storeMonthlyDepth")
    unschedule("storeHourlyDepth")
   
    // clear state variables
    state.rawDepth = 0
    state.lastRawDepth = 0
    state.lastDayDepth = 0
    state.globalDays = [:]
    state.globalMonths = [:]
    state.monthDepth = 0
    state.currentMonthDepth = 0
    state.currentYearDepth = 0
    state.rawDepth = 0.00
    state.globalsInitialized = false
    
    state.remove("monthDepth") 
    state.remove("lastDayDepth")
    state.remove("globalDays")
    state.remove("globalMonths")
    state.remove("monthDepth")
    state.remove("lastRawDepth")
    state.remove("rawDepth")
    state.remove("currentMonthDepth")
    state.remove("currentYearDepth")
    state.remove("lastDayRawDepth")
    state.remove("dayDepth")
    state.remove("lastMonthDepth")
    state.remove("newRawDepth")
    state.remove("lastHourlyDepth")
 
    device.deleteCurrentState("lastDayDepth")
    device.deleteCurrentState("monthDepthStats")
    device.deleteCurrentState("yearDepthStats")
    device.deleteCurrentState("lastMonthDepth")
    device.deleteCurrentState("currentMonthDepth")
    device.deleteCurrentState("currentYearDepth")
    device.deleteCurrentState("lastYearsTotalDepth")
    device.deleteCurrentState("hourlyDepth")
    
    device.deleteCurrentState("snowDaily")
    device.deleteCurrentState("monthDepthStats")
    device.deleteCurrentState("yearDepthStats")
    device.deleteCurrentState("snowLastMonth")
    device.deleteCurrentState("snowMonthly")
    device.deleteCurrentState("snowYearly")
    device.deleteCurrentState("snowLastYear")
    device.deleteCurrentState("snowHourly")
    device.deleteCurrentState("depth")
    device.deleteCurrentState("airHeight")
    device.deleteCurrentState("totalHeight")
    device.deleteCurrentState("heatLDS")
    device.deleteCurrentState("orphanedDepth")
    device.deleteCurrentState("depthLastUpdate")

    device.updateSetting("debugDepthStatisics",false)
    device.updateSetting("useWh54ForSnowDepthCalculations",false)   
}
  
def startDepthRecording()
{   
     
   device.updateSetting("useWh54ForSnowDepthCalculations",true)  
   sendEvent([name: 'snowHourly', value: 0.00, isStateChange: true]) 
   sendEvent([name: 'snowDaily', value: 0.00, isStateChange: true]) 
   sendEvent([name: 'snowMonthly', value: 0.00, isStateChange: true]) 
   sendEvent([name: 'snowYearly', value: 0.00, isStateChange: true])   
     
   state.globalDays = [:]
   state.globalMonths = [:]
   state.lastRawDepth = 0.00
   state.dayDepth = 0.00
   state.currentMonthDepth = 0.00
   state.currentYearDepth = 0.00
   state.rawDepth = 0.00
   state.lastHourlyDepth = 0.00
      
    for (i in 1..31)
       {
         addToDays(i,0.0.toFloat())
       }
    
     for (j in 1..12)
       {
         addToMonths(j,0.0.toFloat())
       }  
    
   initializeGlobals(true)

}

def disableDepthRecording()
{
    disableStats()
}

def resetDepthTotals()
{
    log.info "Resetting monthly/Yearly depth totals! (Leaves last years total along)"
 
     // clear state variables
    state.newRawDepth = 0
    state.lastDayDepth = 0
    state.globalDays = [:]
    state.globalMonths = [:]
    state.monthDepth = 0
    state.currentMonthDepth = 0
    state.currentYearDepth = 0
    // purposly dont clear lastrawdepth or rollover wont be correct.
    state.rawDepth = 0.00
    state.lastHourlyDepth = 0.00
    state.lastMonthDepth = 0.00
    
    sendEvent([name: 'snowLastMonth', value: state.monthDepth, isStateChange: true]) 
    sendEvent([name: 'yearDepthStats', value: state.globalMonths, isStateChange: true])  
    sendEvent([name: 'lastDayDepth', value: state.lastDayDepth, isStateChange: true]) 
    sendEvent([name: 'monthDepthStats', value: state.globalDays, isStateChange: true])         
    sendEvent([name: 'snowMonthly', value: 0.00, isStateChange: true]) 
    sendEvent([name: 'snowYearly', value: 0.00, isStateChange: true])   
    sendEvent([name: 'snowHourly', value: 0.00, isStateChange: true])        
    
    initializeGlobals(true)   
}


// EOF ------------------------------------------------------------------------------------------------------------------------
