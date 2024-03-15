/**
 * Driver:     Ecowitt WiFi Gateway
 * Author:     Mirco Caramori
 * Repository: https://github.com/mircolino/ecowitt
 * Import URL: https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_gateway.groovy
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
 * Change Log:
 *
 * 2020.04.24 - Initial implementation
 * 2020.04.29 - Added GitHub versioning
 *            - Added support for more sensors: WH40, WH41, WH43, WS68 and WS80
 * 2020.04.29 - Added sensor battery range conversion to 0-100%
 * 2020.05.03 - Optimized state dispatch and removed unnecessary attributes
 * 2020.05.04 - Added metric/imperial unit conversion
 * 2020.05.05 - Gave child sensors a friendlier default name
 * 2020.05.08 - Further state optimization and release to stable
 * 2020.05.11 - HTML templates
 *            - Normalization of floating values
 * 2020.05.12 - Added windDirectionCompass, ultravioletDanger, ultravioletColor, aqiIndex, aqiDanger, aqiColor attributes
 * 2020.05.13 - Improved PM2.5 -> AQI range conversion
 *            - HTML template syntax checking and optimization
 *            - UI error handling using red-colored state text messages
 * 2020.05.14 - Major refactoring and architectural change
 *            - PWS like the WS2902 are recognized and no longer split into multiple child sensors
 *            - Rain (WH40), Wind and Solar (WH80) and Outdoor Temp/Hum (WH32) if detected, are combined into a single
 *              virtual WS2902 PWS to improve HTML Templating
 *            - Fixed several imperial-metric conversion issues
 *            - Metric pressure is now converted to hPa
 *            - Laid the groundwork for identification and support of sensors WH41, WH55 and WH57
 *            - Added several calculated values such as windChill, dewPoint, heatIndex etc. with color and danger levels
 *            - Time of data received converted from UTC to hubitat default locale format
 *            - Added error handling using state variables
 *            - Code optimization
 * 2020.05.22 - Added orphaned sensor garbage collection using "Resync Sensors" commands
 * 2020.05.23 - Fixed a bug in the PM2.5 to AQI conversion
 * 2020.05.24 - Fixed a possible command() and parse() race condition
 * 2020.05.26 - Added icons support in the HTML template
 * 2020.05.30 - Added HTML template repository
 *            - Added support for multiple (up to 5) HTML template to each child sensor
 *            - Fixed wind icon as direction is reported as "from" where the wind originates
 * 2020.06.01 - Fixed a cosmetic bug where "pending" status would not be set on non-existing attributes
 * 2020.06.02 - Added visual confirmation of "resync sensors pending"
 * 2020.06.03 - Added last data received timestamp to the child drivers to easily spot if data is not being received from the sensor
 *            - Added battery icons (0%, 20%, 40%, 60%, 80%, 100%)
 *            - Reorganized error e/o status reporting, now displayed in a dedicated "status" attribute
 * 2020.06.04 - Added the ability to enter the MAC address directly as a DNI in the parent device creation page
 * 2020.06.05 - Added support for both MAC and IP addresses (since MACs don't work across VLANs)
 * 2020.06.06 - Add importURL for easier updating
 * 2020.06.08 - Added support for Lightning Detection Sensor (WH57)
 * 2020.06.08 - Added support for Multi-channel Water Leak Sensor (WH55)
 * 2020.06.21 - Added support for pressure correction to sea level based on altitude and temperature
 * 2020.06.22 - Added preference to let the end-user decide whether to compound or not outdoor sensors
 *              Added custom battery attributes in bundled PWS sensors
 * 2020.08.27 - Added user defined min/max voltage values to fine-tune battery status in sensors reporting it as voltage range
 *              Added Hubitat Package Manager repository tags
 * 2020.08.27 - Fixed null exception caused by preferences being set asynchronously
 *            - Removed sensor "time" attribute which could cause excessive sendEvent activity
 * 2020.08.31 - Added support for new Indoor Air Quality Sensor (WH45)
 *            - Optimized calculation of inferred values: dewPoint, heatIndex, windChill and AQI
 * 2020.09.08 - Added support for Water/Soil Temperature Sensor (WH34)
 * 2020.09.17 - Added (back) real-time AQI index, color and danger
 * 2020.09.20 - https://github.com/lymanepp: Added Summer Simmer Index attributes
 *            - Added preferences to selectively calculate HeatIndex, SimmerIndex, WindChill and DewPoint on a per-sensor basis
 * 2020.09.21 - https://github.com/lymanepp: Improved accuracy of dew point calculations
 * 2020.10.02 - Added WeatherFlow Smart Weather Stations local UDP support
 * 2020.10.06 - Fixed a minor issue with lightning attributes
 *            - Added new templates to the template repository
 * 2020.10.09 - Fixed a regression causing a null exception when the lightning sensor reports no strikes
 * 2020.10.27 - Changed the sensor DNI naming scheme which prevented the support for multiple gateways
 * 2020.10.29 - In a virtual (bundled) PWS, now each individual component is correctly identified if orphaned
 *            - Added safeguards for heat, summer simmer and wind chill indexes to prevent invalid values when temperature is
 *              above or below a certain threshold
 * 2021.02.04 - Added support for humidityAbs (absolute humidity) based on current relative humidity and temperature
 * 2021.02.06 - Fixed WH45 temperature and humidity signature
 * 2021.11-25 -lgk change attribute from time to lastUpdate to avoid weird output on device page.. also remove timeUtcToLocalOlf function and improve way to query date/time.
 * 11/30/21   - lgk add parameters for using a weather device in a remote location so that dynamic dns ddns will work to resolve name and change ip address if it changes. Also
 *             add the function for this.
 *
 * 06/22 lgk add code for ws90
 * 3/24 add dniupdatecmd so that it can be called via a rule as the auto schedule seems to be failing at times.
 * also call dni update regardless if the field is not blank to see if that fixes issue with loosing connection.
 */

public static String version() { return "v1.23.17"; }

// Metadata -------------------------------------------------------------------------------------------------------------------

metadata {
  definition(name: "Ecowitt WiFi Gateway", namespace: "mircolino", author: "Mirco Caramori", importUrl: "https://raw.githubusercontent.com/mircolino/ecowitt/master/ecowitt_gateway.groovy") {
    capability "Sensor";

    command "resyncSensors";

    // Gateway info
    attribute "driver", "string";                              // Driver version (new version notification)
    attribute "mac", "string";                                 // Address (either MAC or IP)
    attribute "model", "string";                               // Model number
    attribute "firmware", "string";                            // Firmware version
    attribute "rf", "string";                                  // Sensors radio frequency
    attribute "passkey", "string";                             // PASSKEY

    attribute "status", "string";                              // Display current driver status
    attribute "lastUpdate", "string";
    attribute "dynamicIPResult","STRING"
    attribute "upTime", "string"
    command "DNIUpdateCMD"
  }
    
  
    
  preferences {
    input(name: "macAddress", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>MAC / IP Address</font>", description: "<font style='font-size:12px; font-style: italic'>Wi-Fi gateway MAC or IP address</font>", defaultValue: "", required: true);
    input(name: "DDNSName", type: "text", title: "Dynamic DNS Name to use to resolve a changing ip address. Leave Blank if not used.", description: "Enter DDNS Name", required: false)
    input(name: "DDNSRefreshTime", type: "number", title: "How often (in Hours) to check/resolve the DDNS Name to discover an IP address change on a remote weather station? (Range 1 - 1440, Default 24)?", range: "1..1440", defaultValue: 3, required: false)
    input(name: "bundleSensors", type: "bool", title: "<font style='font-size:12px; color:#1a77c9'>Compound Outdoor Sensors</font>", description: "<font style='font-size:12px; font-style: italic'>Combine sensors in a virtual PWS array</font>", defaultValue: true);
    input(name: "unitSystem", type: "enum", title: "<font style='font-size:12px; color:#1a77c9'>System of Measurement</font>", description: "<font style='font-size:12px; font-style: italic'>Unit system all values are converted to</font>", options: [0:"Imperial", 1:"Metric"], multiple: false, defaultValue: 0, required: true);
    input(name: "logLevel", type: "enum", title: "<font style='font-size:12px; color:#1a77c9'>Log Verbosity</font>", description: "<font style='font-size:12px; font-style: italic'>Default: 'Debug' for 30 min and 'Info' thereafter</font>", options: [0:"Error", 1:"Warning", 2:"Info", 3:"Debug", 4:"Trace"], multiple: false, defaultValue: 3, required: true);
  }
}

/*
 * Data variables used by the driver:
 *
 * "sensorResync"                                              // User command triggered condition to cleanup/resynchronize the sensors
 * "sensorMap"                                                 // Map of whether sensors have been combined or not into a PWS
 * "sensorBundled"                                             // "true" is we have an actual bundled PWS
 * "sensorList"                                                // List of children IDs
 */

// Preferences ----------------------------------------------------------------------------------------------------------------

import groovy.json.JsonSlurper;

private String gatewayMacAddress() {
  //
  // Return the MAC or IP address as entered by the user, or the current DNI if one hasn't been entered yet
  //
  String address = settings.macAddress as String;

  if (address == null) {
    //
    // *** This is a timing hack ***
    // When the users sets the DNI at installation, we update the settings before
    // calling update() but when we get here the setting is still null!
    //
    address = device.getDeviceNetworkId();
  }

  return (address);
}

// ------------------------------------------------------------

private Boolean bundleOutdoorSensors() {
  //
  // Return true if outdoor sensors are to be bundled together
  //
  if (settings.bundleSensors != null) return (settings.bundleSensors);
  return (true);
}

// ------------------------------------------------------------

 Boolean unitSystemIsMetric() {
  //
  // Return true if the selected unit system is metric
  // Declared public because it's being used by the child-devices
  //
  if (settings.unitSystem != null) return (settings.unitSystem.toInteger() != 0);
  return (false);
}

// ------------------------------------------------------------

Integer logGetLevel() {
  //
  // Get the log level as an Integer:
  //
  //   0) log only Errors
  //   1) log Errors and Warnings
  //   2) log Errors, Warnings and Info
  //   3) log Errors, Warnings, Info and Debug
  //   4) log Errors, Warnings, Info, Debug and Trace/diagnostic (everything)
  //
  // If the level is not yet set in the driver preferences, return a default of 2 (Info)
  // Declared public because it's being used by the child-devices as well
  //
  if (settings.logLevel != null) return (settings.logLevel.toInteger());
  return (2);
}

// Versioning -----------------------------------------------------------------------------------------------------------------

private Map versionExtract(String ver) {
  //
  // Given any version string (e.g. version 2.5.78-prerelease) will return a Map as following:
  //   Map.major version
  //   Map.minor version
  //   Map.build version
  //   Map.desc  version
  // or "null" if no version info was found in the given string
  //
  Map val = null;

  if (ver) {
    String pattern = /.*?(\d+)\.(\d+)\.(\d+).*/;
    java.util.regex.Matcher matcher = ver =~ pattern;

    if (matcher.groupCount() == 3) {
      val = [:];
      val.major = matcher[0][1].toInteger();
      val.minor = matcher[0][2].toInteger();
      val.build = matcher[0][3].toInteger();
      val.desc = "v${val.major}.${val.minor}.${val.build}";
    }
  }

  return (val);
}

// ------------------------------------------------------------

Boolean versionUpdate() {
  //
  // Return true is a new version is available
  //
  logDebug("versionUpdate()");

  Boolean ok = false;
  String attribute = "driver";

  try {
    // Retrieve current version
    Map verCur = versionExtract(version());
    if (verCur) {
      // Retrieve latest version from GitHub repository manifest
      // If the file is not found, it will throw an exception
      Map verNew = null;
      String manifestText = "https://raw.githubusercontent.com/mircolino/ecowitt/master/packageManifest.json".toURL().getText();
      if (manifestText) {
        // text -> json
        Object parser = new groovy.json.JsonSlurper();
        Object manifest = parser.parseText(manifestText);

        verNew = versionExtract(manifest.version);
        if (verNew) {
          // Compare versions
          if (verCur.major > verNew.major) verNew = null;
          else if (verCur.major == verNew.major) {
            if (verCur.minor > verNew.minor) verNew = null;
            else if (verCur.minor == verNew.minor) {
              if (verCur.build >= verNew.build) verNew = null;
            }
          }
        }
      }

      String version = verCur.desc;
      if (verNew) version = "<font style='color:#3ea72d'>${verCur.desc} (${verNew.desc} available)</font>";
      ok = attributeUpdateString(version, attribute);
    }
  }
  catch (Exception e) {
    logError("Exception in versionUpdate(): ${e}");
  }

  return (ok);
}

// DNI ------------------------------------------------------------------------------------------------------------------------

private Map dniIsValid(String str) {
  //
  // Return null if not valid
  // otherwise return both hex and canonical version
  //
  List<Integer> val = [];

  try {
    List<String> token = str.replaceAll(" ", "").tokenize(".:");
    if (token.size() == 4) {
      // Regular IPv4
      token.each {
        Integer num = Integer.parseInt(it, 10);
        if (num < 0 || num > 255) throw new Exception();
        val.add(num);
      }
    }
    else if (token.size() == 6) {
      // Regular MAC
      token.each {
        Integer num = Integer.parseInt(it, 16);
        if (num < 0 || num > 255) throw new Exception();
        val.add(num);
      }
    }
    else if (token.size() == 1) {
      // Hexadecimal IPv4 or MAC
      str = token[0];
      if ((str.length() != 8 && str.length() != 12) || str.replaceAll("[a-fA-F0-9]", "").length()) throw new Exception();
      for (Integer idx = 0; idx < str.length(); idx += 2) val.add(Integer.parseInt(str.substring(idx, idx + 2), 16));
    }
  }
  catch (Exception ignored) {
    val.clear();
  }

  Map dni = null;

  if (val.size() == 4) {
    dni = [:];
    dni.hex = sprintf("%02X%02X%02X%02X", val[0], val[1], val[2], val[3]);
    dni.canonical = sprintf("%d.%d.%d.%d", val[0], val[1], val[2], val[3]);
  }

  if (val.size() == 6) {
    dni = [:];
    dni.hex = sprintf("%02X%02X%02X%02X%02X%02X", val[0], val[1], val[2], val[3], val[4], val[5]);
    dni.canonical = sprintf("%02x:%02x:%02x:%02x:%02x:%02x", val[0], val[1], val[2], val[3], val[4], val[5]);
  }

  return (dni);
}

// ------------------------------------------------------------

def DNIUpdateCMD()
{
    log.warn "Calling dniUpdate()"
    dniUpdate()
}

private String dniUpdate() {
  //
  // Get the gateway address (either MAC or IP) from the properties and, if valid and not done already, update the driver DNI
  // Return "error") invalid address entered by the user
  //           null) same address as before
  //             "") new valid address
  //
  logDebug("dniUpdate()");

  String error = "";
  String attribute = "mac";
  String setting = gatewayMacAddress();

  Map dni = dniIsValid(setting);
  if (dni) {

    if ((device.currentValue(attribute) as String) == dni.canonical) {
      // The address hasn't changed: we do nothing
           log.debug "setting dni to ${dni.hex}"
      device.setDeviceNetworkId(dni.hex); 
      error = null;
    }
    else {
      // Save the new address as an attribute for later comparison
      attributeUpdateString(dni.canonical, attribute);

      // Update the DNI
        log.debug "setting dni to ${dni.hex}"
      device.setDeviceNetworkId(dni.hex);
    }
  }
  else {
    error = "\"${setting}\" is not a valid MAC or IP address";
  }

  return (error);
}

// Conversion -----------------------------------------------------------------------------------------------------------------

// Logging --------------------------------------------------------------------------------------------------------------------

void logDebugOff() {
  //
  // runIn() callback to disable "Debug" logging after 30 minutes
  // Cannot be private
  //
  if (logGetLevel() > 2) device.updateSetting("logLevel", [type: "enum", value: "2"]);
}

// ------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (logGetLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (logGetLevel() > 1) log.info(str); }
private void logDebug(String str) { if (logGetLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (logGetLevel() > 3) log.trace(str); }

// ------------------------------------------------------------

private void logData(Map data) {
  //
  // Log all data received from the wifi gateway
  // Used only for diagnostic/debug purposes
  //
  if (logGetLevel() > 3) {
    data.each {
      logTrace("$it.key = $it.value");
    }
  }
}

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

// Sensor handling ------------------------------------------------------------------------------------------------------------

String sensorIdToDni(String sid) {
  String pid = device.getId().concat("-"); 

  if (sid.startsWith(pid)) return (sid);
  return (pid.concat(sid));
}

// ------------------------------------------------------------

String sensorDniToId(String dni) {
  String pid = device.getId().concat("-"); 

  if (dni.startsWith(pid)) return (dni.substring(pid.size()));
  return (dni); 
}

// ------------------------------------------------------------

/*
 *            Outdoor
 *            Temperature                 Wind
 *            & Humidity    Rain          & Solar
 *            ------------- ------------- --------------
 *      WH26  X
 *      WH40                X
 *      WH68                              X
 *      WH80  X                           X
 * WH65/WH69  X             X             X
 * WS90       X             X             X
 *
 */

private void sensorMapping(Map data) {
  //
  // Remap sensors, boundling or decoupling devices, depending on what's present
  //
  //                     0       1       2       3       4       5       6       7       8       9       10       11      12
  String[] sensorMap =  ["WH69", "WH25", "WH26", "WH31", "WH40", "WH41", "WH51", "WH55", "WH57", "WH80", "WH34","WFST", "WS90"];

  logDebug("sensorMapping()");
   // log.debug "data = $data"

  // Detect outdoor sensors by their battery signature
  Boolean wh26 = data.containsKey("wh26batt");
  Boolean wh40 = data.containsKey("wh40batt");
  Boolean wh68 = data.containsKey("wh68batt");
  Boolean wh80 = data.containsKey("wh80batt");
  Boolean wh69 = data.containsKey("wh65batt");
  Boolean ws90 = data.containsKey("ws90batt");

  // Count outdoor sensor
  Integer outdoorSensors = 0;
  if (wh26) outdoorSensors += 1;
  if (wh40) outdoorSensors += 1;
  if (wh68) outdoorSensors += 1;
  if (wh80) outdoorSensors += 1;
  if (ws90) outdoorSensors += 1;

  // A bit of sanity check
  if (wh69 && outdoorSensors) logWarning("The PWS should be the only outdoor sensor");
  if (ws90 && outdoorSensors) logWarning("The PWS should be the only outdoor sensor");
  if (wh80 && wh26) logWarning("Both WH80 and WH26 are present with overlapping sensors");

  if (wh80) {
    //
    // WH80 (includes temp & humidity)
    //
    sensorMap[2] = sensorMap[9];
  }

  if (wh69) {
    //
    // We have a real WH65/WH69/ws90 PWS
    //
    sensorMap[2] = sensorMap[0];
    sensorMap[4] = sensorMap[0];
    sensorMap[9] = sensorMap[0];
  }
    
  if (ws90) {
    //
    // We have a real ws90 PWS
    //
    sensorMap[2] = sensorMap[12];
    sensorMap[4] = sensorMap[12];
    sensorMap[9] = sensorMap[12];
    
  }
    
    
 
  else if (bundleOutdoorSensors() && outdoorSensors > 1) {
    //
    // We are requested to bundle outdoor sensors and we have more than 1
    //
    sensorMap[2] = sensorMap[0];
    sensorMap[4] = sensorMap[0];
    sensorMap[9] = sensorMap[0];

    device.updateDataValue("sensorBundled", "WH69");
  }

  // Save the mapping in the state variables
  device.updateDataValue("sensorMap", sensorMap.toString());
}

// ------------------------------------------------------------

String sensorModel(Integer id) {

  // assert (id >= 0 && id <= 10);

  //                      0     1     2     3     4     5     6     7     8     9     10    11     12
  // String sensorMap = "[WH69, WH25, WH26, WH31, WH40, WH41, WH51, WH55, WH57, WH80, WH34, WFST, WS90]";
  //
  String sensorMap = device.getDataValue("sensorMap");

  id *= 6;

  return (sensorMap.substring(id + 1, id + 5));
}

// ------------------------------------------------------------

private String sensorName(Integer id, Integer channel) {

  Map sensorId = ["WH69": "PWS Sensor",
                  "WH25": "Indoor Ambient Sensor",
                  "WH26": "Outdoor Ambient Sensor",
                  "WH31": "Ambient Sensor",
                  "WH40": "Rain Gauge Sensor",
                  "WH41": "Air Quality Sensor",
                  "WH51": "Soil Moisture Sensor",
                  "WH55": "Water Leak Sensor",
                  "WH57": "Lightning Detection Sensor",
                  "WH80": "Wind Solar Sensor",
                  "WH34": "Water/Soil Temperature Sensor",
                  "WFST": "WeatherFlow Station",
                  "WS90": "WittBoy Weather Station"];

  String model = sensorId."${sensorModel(id)}";

  return (channel? "${model} ${channel}": model);
}

// ------------------------------------------------------------

private String sensorId(Integer id, Integer channel) {

  String model = sensorModel(id);

  return (channel? "${model}_CH${channel}": model);
}

// ------------------------------------------------------------

private Boolean sensorIsBundled(Integer id, Integer channel) {

  return (sensorModel(id) == device.getDataValue("sensorBundled"));
}

// ------------------------------------------------------------

private void sensorGarbageCollect() {
  //
  // Match the new (soon to be created) sensor list with the existing one
  // and delete sensors in the existing list that are not in the new one
  //
  ArrayList<String> sensorList = [];

  String value = device.getDataValue("sensorList");
  if (value) sensorList = value.tokenize("[, ]");

  List<com.hubitat.app.ChildDeviceWrapper> list = getChildDevices();
  if (list) list.each {
    String dni = it.getDeviceNetworkId();
    if (sensorList.contains(sensorDniToId(dni)) == false) deleteChildDevice(dni);
  }
}

// ------------------------------------------------------------

private Boolean sensorEnumerate(String key, String value, Integer id = null, Integer channel = null) {
  //
  // Enumerate sensors needed for the current data
  //
  if (id) {
    String sid = sensorId(id, channel);

    ArrayList<String> sensorList = [];

    value = device.getDataValue("sensorList");
    if (value) sensorList = value.tokenize("[, ]");

    if (sensorList.contains(sid) == false) {
      sensorList.add(sid);

      // Save the list in the state variables
      device.updateDataValue("sensorList", sensorList.toString());
    }
  }

  return (true);
}

// ------------------------------------------------------------

private Boolean sensorUpdate(String key, String value, Integer id = null, Integer channel = null) {
  //
  // If not present, add the child sensor corresponding to the specified id/channel
  // and, if child sensor is present, update the attribute
  //
  // If id is null we broadcast to all children
  //
  Boolean updated = false;

  try {
    if (id) {
      String dni = sensorIdToDni(sensorId(id, channel));
      logDebug("dna = $dni")

      com.hubitat.app.ChildDeviceWrapper sensor = getChildDevice(dni);
       logDebug("got sensor ${sensor}")
      if (sensor == null) {
        //
        // Support for sensors with legacy DNI (without the parent ID)
        //
        sensor = getChildDevice(sensorDniToId(dni)); 
        if (sensor) {
          // Found existing sensor with legacy name: update it
          sensor.setDeviceNetworkId(dni);
          logDebug( "found existing sensor")
        }
        else {
          //
          // Sensor doesn't exist: we need to create it
          //
           logDebug("didnt find existing ")
          sensor = addChildDevice("Ecowitt RF Sensor", dni, [name: sensorName(id, channel), isComponent: true]);
          if (sensor && sensorIsBundled(id, channel)) sensor.updateDataValue("isBundled", "true");
        }

        ztatus("OK", "green");
      }

      if (sensor) updated = sensor.attributeUpdate(key, value);
    }
    else {
      // We broadcast to all children
      List<com.hubitat.app.ChildDeviceWrapper> list = getChildDevices();
      if (list) list.each { if (it.attributeUpdate(key, value)) updated = true; }
    }
  }
  catch (Exception e) {
    if (e instanceof com.hubitat.app.exception.UnknownDeviceTypeException) {
      logError("Unable to create child sensor device. Please make sure the \"ecowitt_sensor.groovy\" driver is installed.");
      ztatus("Unable to create child sensor device. Please make sure the \"ecowitt_sensor.groovy\" driver is installed", "red");
    }
    else logError("Exception in sensorUpdate(${id}, ${channel}): ${e}");
  }

  return (updated);
}

// Attribute handling ---------------------------------------------------------------------------------------------------------

private Boolean attributeUpdateString(String val, String attribute) {
  //
  // Only update "attribute" if different
  // Return true if "attribute" has actually been updated/created
  //
   // log.debug "in attributeupdatestring attr = $attribute value = $val"
  if ((device.currentValue(attribute) as String) != val) {
    sendEvent(name: attribute, value: val);
    return (true);
  }

  return (false);
}

// ------------------------------------------------------------

private Boolean attributeUpdate(Map data, Closure sensor) {
  //
  // Dispatch parent/childs attribute changes to hub
  //
  Boolean updated = false;

  data.each {
    switch (it.key) {
    //
    // Gateway attributes
    //
    case "model":
      // Eg: model = GW1000_Pro
      updated = attributeUpdateString(it.value, "model");
      break;

    case "stationtype":
      // Eg: firmware = GW1000B_V1.5.7
      Map ver = versionExtract(it.value);
      if (ver) it.value = ver.desc;
      updated = attributeUpdateString(it.value, "firmware");
      break;

    case "freq":
      // Eg: rf = 915M
      updated = attributeUpdateString(it.value, "rf");
      break;

    case "PASSKEY":
      // Eg: passkey = 15CF2C872932F570B34AC469540099A4
      updated = attributeUpdateString(it.value, "passkey");
      break;

    //
    // Integrated/Indoor Ambient Sensor (WH25)
    //
    case "wh25batt":
    case "tempinf":
    case "humidityin":
    case "baromrelin":
    case "baromabsin":
      updated = sensor(it.key, it.value, 1);
      break;

    //
    // Outdoor Ambient Sensor (WH26 -> WH80 -> WH69)
    //
    case "wh26batt":
    case "tempf":
    case "humidity":
      updated = sensor(it.key, it.value, 2);
      break;

    //
    // Multi-channel Ambient Sensor (WH31)
    //
    case ~/batt([1-8])/:
    case ~/temp([1-8])f/:
    case ~/humidity([1-8])/:
      updated = sensor(it.key, it.value, 3, java.util.regex.Matcher.lastMatcher.group(1).toInteger());
      break;

    //
    // Rain Gauge Sensor (WH40 -> WH69)
    //
    case "wh40batt":
    case "rainratein":
    case "eventrainin":
    case "hourlyrainin":
    case "dailyrainin":
    case "weeklyrainin":
    case "monthlyrainin":
    case "yearlyrainin":
    case "totalrainin":
      updated = sensor(it.key, it.value, 4);
      break;

      // Rain ws90)

    case "rrain_piezo":
    case "erain_piezo":
    case "hrain_piezo":
    case "drain_piezo":
    case "wrain_piezo":
    case "mrain_piezo":
    case "yrain_piezo":
    case "train_piezo": 
      updated = sensor(it.key, it.value, 4);
      break;
    //
    // Multi-channel Air Quality Sensor (WH41)
    //
    case ~/pm25batt([1-4])/:
    case ~/pm25_ch([1-4])/:
    case ~/pm25_avg_24h_ch([1-4])/:
      updated = sensor(it.key, it.value, 5, java.util.regex.Matcher.lastMatcher.group(1).toInteger());
      break;

    //
    // Air Quality Monitor (WH45)
    //
    case "tf_co2":
    case "humi_co2":
    case "pm25_co2":
    case "pm25_24h_co2":
    case "pm10_co2":
    case "pm10_24h_co2":
    case "co2":
    case "co2_24h":
    case "co2_batt":
      updated = sensor(it.key, it.value, 5);
      break;

    //
    // Multi-channel Soil Moisture Sensor (WH51)
    //
    case ~/soilbatt([1-8])/:
    case ~/soilmoisture([1-8])/:
      updated = sensor(it.key, it.value, 6, java.util.regex.Matcher.lastMatcher.group(1).toInteger());
      break;

    //
    // Multi-channel Water Leak Sensor (WH55)
    //
    case ~/leakbatt([1-4])/:
    case ~/leak_ch([1-4])/:
      updated = sensor(it.key, it.value, 7, java.util.regex.Matcher.lastMatcher.group(1).toInteger());
      break;

    //
    // Lightning Detection Sensor (WH57)
    //
    case "wh57batt":
    case "lightning":
    case "lightning_num":
    case "lightning_time":
      updated = sensor(it.key, it.value, 8);
      break;

    //
    // Wind & Solar Sensor (WH80 -> WH69, ws90)
    //
    case "wh65batt":
    case "wh68batt":
    case "wh80batt":
    case "ws90batt":
    case "wh90batt":
    case "winddir":
    case "winddir_avg10m":
    case "windspeedmph":
    case "windspdmph_avg10m":
    case "windgustmph":
    case "maxdailygust":
    case "uv":
    case "solarradiation":
      updated = sensor(it.key, it.value, 9);
      break;

    //
    // Multi-channel Water Leak Sensor (WH34)
    //
    case ~/tf_batt([1-8])/:
    case ~/tf_ch([1-8])/:
      updated = sensor(it.key, it.value, 10, java.util.regex.Matcher.lastMatcher.group(1).toInteger());
      break;

    case ~/batt_wf([1-8])/:
    case ~/tempf_wf([1-8])/:
    case ~/humidity_wf([1-8])/:
    case ~/baromrelin_wf([1-8])/:
    case ~/baromabsin_wf([1-8])/:
    case ~/lightning_wf([1-8])/:
    case ~/lightning_time_wf([1-8])/:
    case ~/lightning_energy_wf([1-8])/:
    case ~/lightning_num_wf([1-8])/:
    case ~/uv_wf([1-8])/:
    case ~/solarradiation_wf([1-8])/:
    case ~/rainratein_wf([1-8])/:
    case ~/eventrainin_wf([1-8])/:
    case ~/hourlyrainin_wf([1-8])/:
    case ~/dailyrainin_wf([1-8])/:
    case ~/weeklyrainin_wf([1-8])/:
    case ~/monthlyrainin_wf([1-8])/:
    case ~/yearlyrainin_wf([1-8])/:
    case ~/totalrainin_wf([1-8])/:
    case ~/winddir_wf([1-8])/:
    case ~/winddir_avg10m_wf([1-8])/:
    case ~/windspeedmph_wf([1-8])/:
    case ~/windspdmph_avg10m_wf([1-8])/:
    case ~/windgustmph_wf([1-8])/:
    case ~/maxdailygust_wf([1-8])/:
      updated = sensor(it.key, it.value, 11, java.util.regex.Matcher.lastMatcher.group(1).toInteger());
      break;

    case "runtime":
        def ut = formatUpTime(it.value)
        attributeUpdateString(ut, "upTime")
        break;
        
    case "endofdata":
      // Special key to notify all drivers (parent and children) of end-od-data status
      updated = sensor(it.key, it.value);
     
      // Last thing we do on the driver
      if (attributeUpdateString(it.value, "lastUpdate")) updated = true;    
      break;

    default:
      logDebug("Unrecognized attribute: ${it.key} = ${it.value}");
      break;
    }
  }

  return (updated);
}

// Commands -------------------------------------------------------------------------------------------------------------------

void resyncSensors() {
  //
  // This will trigger a sensor remapping and cleanup
  //
  try {
    logDebug("resyncSensors()");

    if (dniIsValid(device.getDeviceNetworkId())) {
      // We have a valid gateway dni
      ztatus("Sensor sync pending", "blue");

      device.updateDataValue("sensorResync", "true");
    }
  }
  catch (Exception e) {
    logError("Exception in resyncSensors(): ${e}");
  }
}

// Driver lifecycle -----------------------------------------------------------------------------------------------------------

void installed() {
  //
  // Called once when the driver is created
  //
  try {
    logDebug("installed()");

    Map dni = dniIsValid(device.getDeviceNetworkId());
    if (dni) {
      device.updateSetting("macAddress", [type: "string", value: dni.canonical]);
      updated();
    }

  }
  catch (Exception e) {
    logError("Exception in installed(): ${e}");
  }
}

def nsCallback(resp, data)
{  
    logDebug("in callback")
    
   // test change
    
    def jSlurp = new JsonSlurper()
    Map ipData = (Map)jSlurp.parseText((String)resp.data)
    def String newIP = ipData.Answer.data[0]
    sendEvent(name:"dynamicIPResult", value:ipData.Answer.data[0])
    
    // now compare ip to our own and if different reset and log
    if ((newIP != null) && (newIP != ""))
    {
        def String currentIP = settings.macAddress
        logInfo("Comparing resolved IP: $newIP to $currentIP")
        
        if (currentIP != newIP)
        {
            logInfo("IP address has Changed !!! Resetting DNI !")
             Map dni = dniIsValid(newIP);
             // Update Device Network ID
            logDebug("got back dni = $dni")
            if (dni) 
            { 
            device.updateSetting("macAddress", [type: "string", value: dni.canonical]);
            dniUpdate();
            resyncSensors();
            }
        }
        dniUpdate()   
    }
}

void DNSCheckCallback()
{
    logInfo("Dns Update Check Callback Startup")
    updated()
}

void updated() {
  //
  // Called everytime the user saves the driver preferences
  //
  try {
    logDebug("updated()");

    // Clear previous states
    state.clear();

    // Unschedule possible previous runIn() calls
    unschedule();

     // lgk if ddns name resolve this first and do ip check before dniupdatr.. ALSO schedule the re-check.
     def String ddnsname = settings.DDNSName
     def Number ddnsupdatetime = settings.DDNSRefreshTime
                                          
     logDebug("DDNS Name = $ddnsname")
     logDebug("DDNS Refresh Time = $ddnsupdatetime")
                                          
     if ((ddnsname != null) && (ddnsname != ""))
       {
           logDebug("Got ddns name $ddnsname")
           // now resolve
   
     Map params = [
        uri: "https://8.8.8.8/resolve?name=$ddnsname&type=A",
        contentType: "text/plain",
        timeout: 20
    ]

    logDebug("calling dns Update url = $params")
    asynchttpGet("nsCallback", params)
}   
      // now schedule next run of update
      if ((ddnsupdatetime != null) && (ddnsupdatetime != 00))
          {
              def thesecs = ddnsupdatetime * 3600
             logInfo("Rescheduling IP Address Check to run again in $thesecs seconds.")
             runIn(thesecs, "DNSCheckCallback");
          }
          
    // Update Device Network ID
    log.warn "Calling dniUpdate"
    String error = dniUpdate();
    if (error == null) {
      // The gateway dni hasn't changed: we set OK only if a resync sensors is not pending
      if (device.getDataValue("sensorResync")) ztatus("Sensor sync pending", "blue");
      else ztatus("OK", "green");
    }
    else if (error != "") ztatus(error, "red");
    else resyncSensors();

    // Update driver version now and every Sunday @ 2am
   // versionUpdate();
   // schedule("0 0 2 ? * 1 *", versionUpdate);

    // Turn off debug log in 30 minutes
    if (logGetLevel() > 2) runIn(1800, logDebugOff);
        
    // lgk get rid of now unused time attribute
     device.deleteCurrentState("time")   
      
    
  }
  catch (Exception e) {
    logError("Exception in updated(): ${e}");
  }
}

// ------------------------------------------------------------

void uninstalled() {
  //
  // Called once when the driver is deleted
  //
  try {
    // Delete all children
    List<com.hubitat.app.ChildDeviceWrapper> list = getChildDevices();
    if (list) list.each { deleteChildDevice(it.getDeviceNetworkId()); }

    logDebug("uninstalled()");
  }
  catch (Exception e) {
    logError("Exception in uninstalled(): ${e}");
  }
}

void uninstalledChildDevice(String dni) {
  //
  // Called by the children to notify the parent they are being uninstalled
  //
}

// ------------------------------------------------------------

void parse(String msg) {
  //
  // Called everytime a POST message is received from the WiFi Gateway
  //
  try {
    logDebug("parse()");

    // Parse POST message
    Map data = parseLanMessage(msg);

    // Save only the body and discard the header
    String body = data["body"];

    // Build a map with one key/value pair for each field we receive
    data = [:];
    body.split("&").each {
      String[] keyValue = it.split("=");
      data[keyValue[0]] = (keyValue.size() > 1)? keyValue[1]: "";
    }

    // "dewPoint" and "heatIndex" are based on "tempf" and "humidity"
    // for them to be calculated properly, in "data", "humidity", if present, must come after "tempf"

    // "windchill" is based on "tempf" and "windspeedmph"
    // for it to be calculated properly, in "data", "windspeedmph", if present, must come after "tempf"

    // "aqi" is based on "pm25_24h_co2" and "pm10_24h_co2"
    // for it to be calculated properly, in "data", "pm10_24h_co2", if present, must come after "pm25_24h_co2"

    // Inject a special key (at the end of the data map) to notify all the driver of end-of-data status. Value is local time
      
     def now = new Date().format('MM/dd/yy h:mm a',location.timeZone)
     data["endofdata"] = now
   // data["endofdata"] = timeUtcToLocal(data["dateutc"]);
  //  data.remove("dateutc");

    logData(data);

    if (device.getDataValue("sensorResync")) {
      // We execute this block only the first time we receive data from the wifi gateway
      // or when the user presses the "Resynchronize Sensors" command
      device.updateDataValue("sensorResync", null);
      device.data.remove("sensorResync");

      // (Re)create sensor map
      device.updateDataValue("sensorBundled", null);
      device.data.remove("sensorBundled");      
      device.updateDataValue("sensorMap", null);
      sensorMapping(data);

      // (Re)create sensor list
      device.updateDataValue("sensorList", null);
      attributeUpdate(data, this.&sensorEnumerate);

      // Match the new (soon to be created) sensor list with the existing one
      // and delete sensors in the existing list that are not in the new one
      sensorGarbageCollect();

      // Clear pending status and start processing data
      ztatus("OK", "green");
    }

    attributeUpdate(data, this.&sensorUpdate);
  }
  catch (Exception e) {
    logError("Exception in parse(): ${e}");
  }
}

// lgk format updatime                  
def formatUpTime(runtime)
{       
        def uptimeString = ""
        Long ut = runtime.toLong()
        Integer days = Math.floor(ut/(3600*24)).toInteger()
        Integer hrs = Math.floor((ut - (days * (3600*24))) /3600).toInteger()
        Integer min = Math.floor( (ut -  ((days * (3600*24)) + (hrs * 3600))) /60).toInteger()
        Integer sec = Math.floor(ut -  ((days * (3600*24)) + (hrs * 3600) + (min * 60))).toInteger()

        if (days > 0)
         uptimeString = uptimeString + days.toString() + " days, "
    
        uptimeString = uptimeString + hrs.toString() + " hours "
        uptimeString = uptimeString + min.toString() + " minutes, "
        uptimeString = uptimeString + sec.toString() + " seconds"
        
    return uptimeString
}


// Recycle bin ----------------------------------------------------------------------------------------------------------------

/*

synchronized(this) {

}


@Field static java.util.concurrent.Semaphore mutex = new java.util.concurrent.Semaphore(1)

if (!mutex.tryAcquire())

mutex.release()


*/

// EOF ------------------------------------------------------------------------------------------------------------------------
